package com.yshut.whatsappspaces;
import android.app.*;
import android.content.*;
import android.os.*;
import android.graphics.Color;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import org.json.JSONObject;

public final class NetworkSettingsActivity extends Activity {
 private EditText credential;private Spinner protocol;private TextView status,savedLabel,routeHint,history;private Button save,test;private Switch residential,location;private LinearLayout proxyFields;
 private ResidentialConfig existing;private boolean busy;
 private static final String[] PROTOCOLS={"AUTO","SOCKS5","HTTP","HTTPS"};
 private int dp(int n){return Ui.dp(this,n);}
 @Override public void onCreate(Bundle b){super.onCreate(b);Ui.window(this);getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
  LinearLayout body=Ui.column(this);body.setPadding(dp(16),dp(14),dp(16),dp(24));ScrollView scroll=new ScrollView(this);scroll.setBackgroundColor(Ui.BG);scroll.setFillViewport(true);scroll.addView(body);setContentView(scroll);
  LinearLayout header=Ui.row(this);ImageButton back=Ui.iconButton(this,"back","返回多开空间",this::finish);header.addView(back,new LinearLayout.LayoutParams(dp(48),dp(48)));TextView heading=Ui.text(this,"网络与定位",24,Ui.INK,true);header.addView(heading,new LinearLayout.LayoutParams(0,-2,1));ImageButton help=Ui.iconButton(this,"info","查看网络与定位说明",this::help);header.addView(help,new LinearLayout.LayoutParams(dp(48),dp(48)));body.addView(header);
  TextView intro=Ui.note(body,"所有空间共用以下设置。请先连接 v2rayNG，并将多开空间纳入 VPN。");intro.setPadding(dp(4),0,dp(4),0);
  LinearLayout mode=Ui.card(this);residential=new Switch(this);residential.setChecked(NetworkOptions.residential(this));residential.setContentDescription("使用住宅代理；关闭后仅使用 VPN");Ui.tint(residential);switchRow(mode,"住宅代理","在 VPN 之后，连接住宅出口",residential,"lock");routeHint=Ui.note(mode,"");savedLabel=Ui.text(this,"尚未保存住宅代理",13,Ui.GREEN,true);Ui.add(mode,savedLabel,14);
  proxyFields=Ui.column(this);Ui.add(mode,proxyFields,12);credential=new EditText(this);credential.setTextSize(14);credential.setTextColor(Ui.INK);credential.setHintTextColor(Ui.MUTED);credential.setHint("粘贴新代理；留空使用已保存配置");credential.setContentDescription("住宅代理地址、端口、用户名和密码");credential.setSingleLine(true);credential.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);credential.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS);credential.setSaveEnabled(false);credential.setBackground(Ui.shape(this,Ui.BG,12,Ui.LINE));Ui.pad(credential,12,12);proxyFields.addView(credential,new LinearLayout.LayoutParams(-1,dp(50)));Ui.note(proxyFields,"格式：地址:端口:用户名:密码");
  LinearLayout protocolRow=Ui.row(this);protocolRow.addView(Ui.text(this,"连接协议",14,Ui.INK,true),new LinearLayout.LayoutParams(0,-2,1));protocol=new Spinner(this);ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,PROTOCOLS);protocol.setAdapter(adapter);protocol.setContentDescription("住宅代理连接协议");protocolRow.addView(protocol,new LinearLayout.LayoutParams(dp(135),dp(48)));Ui.add(proxyFields,protocolRow,6);Ui.add(body,mode,18);
  LinearLayout geo=Ui.card(this);location=new Switch(this);Ui.tint(location);location.setChecked(NetworkOptions.location(this));location.setContentDescription("按出口 IP 设置分身虚拟定位");switchRow(geo,"IP 虚拟定位","使用当前出口的估算位置",location,"pin");Ui.note(geo,"开启后，每次打开分身前更新位置。关闭后停用本应用内的虚拟定位。");history=Ui.text(this,"尚无位置查询记录",12,Ui.MUTED,false);history.setLineSpacing(dp(3),1);Ui.add(geo,history,12);Ui.add(body,geo,12);
  save=Ui.button(this,"保存设置","check",true,()->work(false));save.setTextSize(16);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,dp(52));sp.topMargin=dp(20);body.addView(save,sp);test=Ui.button(this,"检测出口与更新定位","sync",false,()->work(true));LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(-1,dp(50));tp.topMargin=dp(10);body.addView(test,tp);
  status=Ui.text(this,"当前显示已保存的设置",14,Ui.GREEN,true);status.setLineSpacing(dp(4),1);status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);Ui.add(body,status,17);
  Ui.note(body,"关闭住宅代理会保留上次凭据，重新开启即可复用。保存网络设置会关闭正在运行的分身，重新打开后生效。");Ui.note(body,"IP 位置为城市级估算。当前为 TCP 实验版，语音和视频暂不保证可用。详细说明见右上角。");
  try{existing=ResidentialConfig.load(this);updateSaved();}catch(Exception e){status.setText("已保存的凭据无法读取，请重新填写");}
  residential.setOnCheckedChangeListener((v,on)->{controls();status.setText("网络模式已选择，保存后生效");});location.setOnCheckedChangeListener((v,on)->status.setText("定位设置已选择，保存后生效"));controls();updateHistory();
 }
 private void switchRow(LinearLayout card,String title,String subtitle,Switch toggle,String symbol){LinearLayout row=Ui.row(this);android.widget.ImageView icon=Ui.icon(this,symbol,Ui.GREEN,22);icon.setBackground(Ui.shape(this,Ui.PALE,20,0));Ui.pad(icon,10,10);row.addView(icon,new LinearLayout.LayoutParams(dp(40),dp(40)));LinearLayout words=Ui.column(this);words.addView(Ui.text(this,title,18,Ui.INK,true));TextView sub=Ui.text(this,subtitle,12,Ui.MUTED,false);Ui.add(words,sub,5);LinearLayout.LayoutParams wp=new LinearLayout.LayoutParams(0,-2,1);wp.leftMargin=dp(10);wp.rightMargin=dp(6);row.addView(words,wp);row.addView(toggle,new LinearLayout.LayoutParams(-2,dp(48)));row.setOnClickListener(v->{if(!busy)toggle.setChecked(!toggle.isChecked());});card.addView(row);}
 private void updateSaved(){if(existing==null)return;savedLabel.setText("已保存 · "+existing.host+":"+existing.port);for(int i=0;i<PROTOCOLS.length;i++)if(PROTOCOLS[i].equals(existing.protocol))protocol.setSelection(i);}
 private void updateHistory(){String last=NetworkOptions.prefs(this).getString("last-location",null);if(last!=null)try{JSONObject j=new JSONObject(last);String when=android.text.format.DateFormat.format("MM-dd HH:mm",j.optLong("observedAt",0)).toString();history.setText("上次查询 · "+j.optString("country")+" / "+j.optString("city")+"\n"+j.getString("ip")+"  ·  "+when+"（历史结果）");}catch(Exception ignored){}}
 private void controls(){Ui.enabled(residential,!busy);Ui.enabled(location,!busy);Ui.enabled(credential,!busy&&residential.isChecked());Ui.enabled(protocol,!busy&&residential.isChecked());Ui.enabled(save,!busy);Ui.enabled(test,!busy);proxyFields.setVisibility(residential.isChecked()?View.VISIBLE:View.GONE);routeHint.setText(residential.isChecked()?"连接路径：VPN → 住宅代理 → 目标应用服务":"连接路径：VPN → 目标应用服务");}
 private void help(){new AlertDialog.Builder(this).setTitle("网络与定位说明").setMessage("住宅账号与密码加密保存在本机，关闭住宅模式不会删除。使用 AUTO 时，请先点检测以识别可用协议。\n\n只检测出口时访问 api.ipify.org，必要时使用 Cloudflare Trace；开启 IP 定位时访问 ipwho.is。IP 位置是城市级估算，不是真实 GPS。轮换代理的后续连接可能使用不同出口。\n\n开启 IP 定位后，查询失败会暂停打开分身，避免使用旧坐标。设置只作用于本助手里的分身，不修改其他应用。具体定位接口兼容性仍在验证。\n\n当前转发覆盖 TCP，已拦截路径上的 UDP 被阻止。DNS 由系统 VPN 处理；完整流量覆盖验证尚未完成。语音与视频暂不保证可用。").setPositiveButton("知道了",null).show();}
 private void work(boolean detect){
  final boolean use=residential.isChecked(),geo=location.isChecked();final ResidentialConfig candidate;
  try{String raw=credential.getText().toString(),selected=PROTOCOLS[protocol.getSelectedItemPosition()];candidate=!raw.isEmpty()?ResidentialConfig.parse(raw,selected):existing==null?null:new ResidentialConfig(existing.host,existing.port,existing.user,existing.password,selected);if(use&&candidate==null)throw new IllegalArgumentException();}catch(Exception e){status.setText("住宅模式需要有效的 地址:端口:用户名:密码");return;}
  busy=true;controls();status.setText(detect?"正在保存并通过 VPN 检测…":"正在保存…");
  SpacesController.WORK.execute(()->{
   String message;ResidentialConfig accepted=candidate;boolean saved=false;
   try{
    apply(this,candidate,use,geo,null);existing=candidate;saved=true;
    if(detect){android.net.Network vpn=ResidentialTransport.vpn(this);IpLocation point=null;String ip=null;
     String[] choices=use&&"AUTO".equals(candidate.protocol)?new String[]{"SOCKS5","HTTP","HTTPS"}:new String[]{use?candidate.protocol:"VPN"};
     for(String choice:choices){ResidentialConfig trial=use?new ResidentialConfig(candidate.host,candidate.port,candidate.user,candidate.password,choice):null;
      try{if(geo){point=IpLocation.lookup(vpn,trial);ip=point.ip;}else ip=ResidentialTransport.test(vpn,trial);if(use)accepted=trial;break;}catch(java.io.IOException ignored){}
     }
     if(ip==null)throw new java.io.IOException("设置已保存，但连接检测未通过。请核对 VPN、代理协议和凭据。");
     apply(this,accepted,use,geo,point);existing=accepted;
     message=(use?"VPN＋住宅代理":"仅 VPN")+"检测通过；出口："+ip+(point==null?"":"\n虚拟定位："+point.label())+"\n重新打开分身后生效。";
    }else message=(use?"VPN＋住宅代理":"仅使用 VPN")+"设置已保存。住宅凭据会保留。"+(use&&"AUTO".equals(candidate.protocol)?"请先检测代理协议。":"")+(geo?"打开分身前将查询并应用 IP 位置。":"虚拟定位已关闭。");
   }catch(Exception e){message=e instanceof java.io.IOException?e.getMessage():"设置或检测未完成，请检查 VPN 与代理连接";}
   String result=message;boolean clear=saved;runOnUiThread(()->{if(!isDestroyed()){busy=false;controls();if(clear)credential.setText("");updateSaved();updateHistory();status.setText(result);}});
  });
 }
 static void apply(Context c,ResidentialConfig cfg)throws Exception{apply(c,cfg,true,NetworkOptions.location(c),null);}
 static void apply(Context c,ResidentialConfig cfg,boolean use,boolean geo,IpLocation point)throws Exception{
  for(int id:SpacesController.spaces())top.niunaijun.blackbox.BlackBoxCore.get().stopPackage(SpacesController.SOURCE,id);
  ResidentialService.stop(c);if(cfg!=null)cfg.save(c);NetworkOptions.save(c,use,geo);
  if(!geo)IpLocation.disable(SpacesController.spaces());else if(point!=null){point.apply(SpacesController.spaces());point.remember(c);}
  SpacesController.status(c,(use?"VPN＋住宅代理":"仅使用 VPN")+"已保存；IP 定位"+(geo?"已开启":"已关闭"));
 }
}
