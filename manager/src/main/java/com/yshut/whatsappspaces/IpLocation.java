package com.yshut.whatsappspaces;
import android.content.Context;
import android.net.Network;
import org.json.JSONObject;
import java.io.IOException;
import java.util.List;
import top.niunaijun.blackbox.entity.location.BLocation;
import top.niunaijun.blackbox.fake.frameworks.BLocationManager;

final class IpLocation {
 final String ip,country,region,city;final double latitude,longitude;final long observedAt;
 IpLocation(String ip,String country,String region,String city,double lat,double lon)throws IOException{
  if(!ip.matches("[0-9a-fA-F:.]{3,64}")||!Double.isFinite(lat)||!Double.isFinite(lon)||lat< -90||lat>90||lon< -180||lon>180)throw new IOException("IP 定位数据无效");
  this.ip=ip;this.country=country;this.region=region;this.city=city;latitude=lat;longitude=lon;observedAt=System.currentTimeMillis();
 }
 static IpLocation lookup(Network vpn,ResidentialConfig cfg)throws Exception{return parse(new JSONObject(ResidentialTransport.fetch(vpn,cfg,"ipwho.is","/")));}
 static IpLocation parse(JSONObject data)throws Exception{
  if(!data.optBoolean("success",false))throw new IOException("IP 位置查询失败，未更改虚拟定位");
  return new IpLocation(data.getString("ip"),data.optString("country",""),data.optString("region",""),data.optString("city",""),data.getDouble("latitude"),data.getDouble("longitude"));
 }
 String label(){return country+" / "+region+" / "+city+"（IP 估算）";}
 void remember(Context c)throws Exception{
  JSONObject j=new JSONObject().put("ip",ip).put("country",country).put("region",region).put("city",city).put("latitude",latitude).put("longitude",longitude).put("observedAt",observedAt);
  NetworkOptions.prefs(c).edit().putString("last-location",j.toString()).apply();
 }
 void apply(List<Integer> users)throws IOException{
  BLocationManager m=BLocationManager.get();
  for(int id:users){BLocation point=new BLocation(latitude,longitude);point.setAccuracy(100000f);m.setLocation(id,SpacesController.SOURCE,point);m.setPattern(id,SpacesController.SOURCE,BLocationManager.OWN_MODE);
   BLocation actual=m.getLocation(id,SpacesController.SOURCE);if(m.getPattern(id,SpacesController.SOURCE)!=BLocationManager.OWN_MODE||actual==null||Math.abs(actual.getLatitude()-latitude)>0.000001||Math.abs(actual.getLongitude()-longitude)>0.000001)throw new IOException("分身虚拟定位设置未通过核验");
  }
 }
 static void disable(List<Integer> users)throws IOException{
  BLocationManager m=BLocationManager.get();for(int id:users){m.setPattern(id,SpacesController.SOURCE,BLocationManager.CLOSE_MODE);if(m.getPattern(id,SpacesController.SOURCE)!=BLocationManager.CLOSE_MODE)throw new IOException("虚拟定位关闭未通过核验");}
 }
 static void beforeOpen(Context c)throws Exception{
  if(!NetworkOptions.location(c)){disable(SpacesController.spaces());return;}
  ResidentialConfig config=NetworkOptions.residential(c)?ResidentialConfig.load(c):null;
  IpLocation result=lookup(ResidentialTransport.vpn(c),config);result.apply(SpacesController.spaces());result.remember(c);
 }
}
