#include <jni.h>
#include <dlfcn.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <unistd.h>
#include <poll.h>
#include <fcntl.h>
#include <errno.h>
#include <cstring>
#include <chrono>
#include "Dobby/dobby.h"

// TCP-only experimental routing. UDP/raw sockets are denied, never forwarded directly.
// Does not intercept hand-written syscalls. No security boundary against hostile guest code.
static int (*real_connect)(int,const sockaddr*,socklen_t);
static int (*real_socket)(int,int,int);
static ssize_t (*real_sendto)(int,const void*,size_t,int,const sockaddr*,socklen_t);
static ssize_t (*real_sendmsg)(int,const msghdr*,int);
static int (*real_sendmmsg)(int,mmsghdr*,unsigned int,int);
static unsigned short port4,port6;
static unsigned char secret[32];
static bool installed=false;
static bool inet_family(int family){return family==AF_INET||family==AF_INET6;}
static bool inet_socket(int fd){sockaddr_storage a{};socklen_t n=sizeof(a);return getsockname(fd,(sockaddr*)&a,&n)==0&&inet_family(a.ss_family);}
static bool stream_socket(int fd){int type=0;socklen_t n=sizeof(type);return getsockopt(fd,SOL_SOCKET,SO_TYPE,&type,&n)==0&&type==SOCK_STREAM;}
static int blocked(){errno=ENETUNREACH;return -1;}
static int gated_socket(int domain,int type,int protocol){
 if(inet_family(domain)&&((type&0xf)!=SOCK_STREAM||(protocol!=0&&protocol!=IPPROTO_TCP)))return blocked();
 return real_socket(domain,type,protocol);
}
static ssize_t gated_sendto(int fd,const void* b,size_t n,int f,const sockaddr* a,socklen_t l){
 if(inet_socket(fd)&&(!stream_socket(fd)||a!=nullptr))return blocked();
 return real_sendto(fd,b,n,f,a,l);
}
static ssize_t gated_sendmsg(int fd,const msghdr* m,int f){
 if(inet_socket(fd)&&(!stream_socket(fd)||(m&&m->msg_name)))return blocked();
 return real_sendmsg(fd,m,f);
}
static int gated_sendmmsg(int fd,mmsghdr* m,unsigned n,int f){
 if(inet_socket(fd)){if(!stream_socket(fd))return blocked();for(unsigned i=0;i<n;i++)if(m[i].msg_hdr.msg_name)return blocked();}
 return real_sendmmsg(fd,m,n,f);
}
static int gated_connect(int fd,const sockaddr* a,socklen_t n){
 if(!a||n<sizeof(sa_family_t)||!inet_family(a->sa_family))return real_connect(fd,a,n);
 if(!stream_socket(fd))return blocked();
 unsigned char header[55]{};memcpy(header,"RSP1",4);memcpy(header+4,secret,32);
 sockaddr_storage local{};socklen_t ln;
 if(a->sa_family==AF_INET){
  if(n<sizeof(sockaddr_in)||!port4)return blocked();auto src=(const sockaddr_in*)a;
  if((ntohl(src->sin_addr.s_addr)>>24)==127||src->sin_addr.s_addr==0)return blocked();
  header[36]=1;memcpy(header+37,&src->sin_addr,4);memcpy(header+53,&src->sin_port,2);
  auto dst=(sockaddr_in*)&local;dst->sin_family=AF_INET;dst->sin_port=htons(port4);dst->sin_addr.s_addr=htonl(INADDR_LOOPBACK);ln=sizeof(*dst);
 }else{
  if(n<sizeof(sockaddr_in6)||!port6)return blocked();auto src=(const sockaddr_in6*)a;
  if(IN6_IS_ADDR_LOOPBACK(&src->sin6_addr)||IN6_IS_ADDR_UNSPECIFIED(&src->sin6_addr))return blocked();
  header[36]=4;memcpy(header+37,&src->sin6_addr,16);memcpy(header+53,&src->sin6_port,2);
  auto dst=(sockaddr_in6*)&local;dst->sin6_family=AF_INET6;dst->sin6_port=htons(port6);dst->sin6_addr=in6addr_loopback;ln=sizeof(*dst);
 }
 int oldFlags=fcntl(fd,F_GETFL,0);if(oldFlags<0)return -1;
 if(fcntl(fd,F_SETFL,oldFlags|O_NONBLOCK)<0)return -1;
 int result=real_connect(fd,(sockaddr*)&local,ln);
 if(result<0&&errno!=EINPROGRESS){fcntl(fd,F_SETFL,oldFlags);return -1;}
 auto deadline=std::chrono::steady_clock::now()+std::chrono::milliseconds(500);
 size_t sent=0;bool ok=true;
 while(sent<sizeof(header)){
  auto remaining=std::chrono::duration_cast<std::chrono::milliseconds>(deadline-std::chrono::steady_clock::now()).count();
  if(remaining<=0){ok=false;break;}pollfd p{fd,POLLOUT,0};int r=poll(&p,1,(int)remaining);
  if(r<0&&errno==EINTR)continue;if(r<=0||p.revents&(POLLERR|POLLHUP|POLLNVAL)){ok=false;break;}
  int error=0;socklen_t en=sizeof(error);if(getsockopt(fd,SOL_SOCKET,SO_ERROR,&error,&en)<0||error){ok=false;break;}
  ssize_t count=real_sendto(fd,header+sent,sizeof(header)-sent,MSG_NOSIGNAL|MSG_DONTWAIT,nullptr,0);
  if(count<0&&(errno==EINTR||errno==EAGAIN))continue;if(count<=0){ok=false;break;}sent+=count;
 }
 fcntl(fd,F_SETFL,oldFlags);
 if(!ok){shutdown(fd,SHUT_RDWR);errno=ECONNREFUSED;return -1;}
 return 0; // The local relay is connected; remote failure closes it without a direct retry.
}
static bool hook(const char* name,void* replacement,void** original){void* symbol=dlsym(RTLD_DEFAULT,name);return symbol&&DobbyHook(symbol,replacement,original)==0&&*original;}
extern "C" JNIEXPORT jboolean JNICALL Java_top_niunaijun_blackbox_core_network_SocketGate_install(JNIEnv* e,jclass,jint p4,jint p6,jbyteArray token){
 if(installed)return false;if(p4<1||p4>65535||p6<1||p6>65535||e->GetArrayLength(token)!=32)return false;
 port4=p4;port6=p6;e->GetByteArrayRegion(token,0,32,(jbyte*)secret);
 if(!hook("sendto",(void*)gated_sendto,(void**)&real_sendto))return false;
 if(!hook("sendmsg",(void*)gated_sendmsg,(void**)&real_sendmsg))return false;
 if(!hook("sendmmsg",(void*)gated_sendmmsg,(void**)&real_sendmmsg))return false;
 if(!hook("socket",(void*)gated_socket,(void**)&real_socket))return false;
 if(!hook("connect",(void*)gated_connect,(void**)&real_connect))return false;
 installed=true;return true;
}
extern "C" JNIEXPORT jboolean JNICALL Java_top_niunaijun_blackbox_core_network_SocketGate_probeNonblocking(JNIEnv* e,jclass,jbyteArray address,jint port){
 if(e->GetArrayLength(address)!=4)return false;int fd=socket(AF_INET,SOCK_STREAM|SOCK_NONBLOCK,0);if(fd<0)return false;
 sockaddr_in dst{};dst.sin_family=AF_INET;dst.sin_port=htons(port);e->GetByteArrayRegion(address,0,4,(jbyte*)&dst.sin_addr);
 bool ok=connect(fd,(sockaddr*)&dst,sizeof(dst))==0&&(fcntl(fd,F_GETFL,0)&O_NONBLOCK)!=0;close(fd);return ok;
}
