package com.yshut.whatsappspaces;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;

/** Small native view palette shared by the home and network screens. */
final class Ui {
 static final int BG=0xfff3f8f7,INK=0xff14272d,MUTED=0xff7d8993,GREEN=0xff09866c,PALE=0xffe5f5ee,LINE=0xffd6e8e2;
 static int dp(Context c,float n){return Math.round(n*c.getResources().getDisplayMetrics().density);}
 static void window(Activity a){a.getWindow().setStatusBarColor(BG);a.getWindow().setNavigationBarColor(BG);a.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);}
 static GradientDrawable shape(Context c,int color,int radius,int stroke){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(c,radius));if(stroke!=0)d.setStroke(dp(c,1),stroke);return d;}
 static android.graphics.drawable.Drawable ripple(Context c,int color,int radius,int stroke){return new RippleDrawable(ColorStateList.valueOf(0x2009866c),shape(c,color,radius,stroke),shape(c,Color.WHITE,radius,0));}
 static TextView text(Context c,String s,int size,int color,boolean bold){TextView t=new TextView(c);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setFontFeatureSettings("kern");if(bold)t.setTypeface(android.graphics.Typeface.create("sans-serif-medium",0));t.setIncludeFontPadding(false);return t;}
 static LinearLayout column(Context c){LinearLayout v=new LinearLayout(c);v.setOrientation(LinearLayout.VERTICAL);return v;}
 static LinearLayout row(Context c){LinearLayout v=new LinearLayout(c);v.setGravity(Gravity.CENTER_VERTICAL);return v;}
 static void pad(View v,int h,int vert){v.setPadding(dp(v.getContext(),h),dp(v.getContext(),vert),dp(v.getContext(),h),dp(v.getContext(),vert));}
 static LinearLayout card(Context c){LinearLayout v=column(c);v.setBackground(shape(c,Color.WHITE,20,0));pad(v,16,16);return v;}
 static void add(LinearLayout parent,View child,int top){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(parent.getContext(),top);parent.addView(child,lp);}
 static TextView note(LinearLayout parent,String s){TextView t=text(parent.getContext(),s,13,MUTED,false);t.setLineSpacing(dp(parent.getContext(),3),1);add(parent,t,10);return t;}
 static IconDrawable symbol(Context c,String name,int color,int size){IconDrawable d=new IconDrawable(name,color);d.setBounds(0,0,dp(c,size),dp(c,size));return d;}
 static ImageView icon(Context c,String name,int color,int size){ImageView v=new ImageView(c);v.setImageDrawable(symbol(c,name,color,size));v.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);return v;}
 static ImageButton iconButton(Context c,String name,String desc,Runnable action){ImageButton b=new ImageButton(c);b.setImageDrawable(symbol(c,name,MUTED,23));b.setBackground(ripple(c,Color.TRANSPARENT,14,0));b.setContentDescription(desc);pad(b,12,12);b.setOnClickListener(v->action.run());return b;}
 static Button button(Context c,String label,String icon,boolean filled,Runnable action){Button b=new Button(c);b.setText(label);b.setTextSize(14);b.setTextColor(filled?Color.WHITE:GREEN);b.setAllCaps(false);b.setTypeface(Typeface.create("sans-serif-medium",0));b.setLetterSpacing(0);b.setMinHeight(0);b.setMinimumHeight(0);b.setMinWidth(0);b.setMinimumWidth(0);b.setStateListAnimator(null);pad(b,14,0);b.setBackground(ripple(c,filled?GREEN:Color.WHITE,13,filled?0:LINE));if(icon!=null){b.setCompoundDrawables(symbol(c,icon,filled?Color.WHITE:GREEN,17),null,null,null);b.setCompoundDrawablePadding(dp(c,7));}b.setOnClickListener(v->action.run());return b;}
 static TextView badge(Context c,String s,boolean active,String icon){TextView t=text(c,s,12,active?GREEN:0xff5f6c75,true);pad(t,10,8);t.setBackground(shape(c,active?PALE:0xfff0f2f4,10,0));if(icon!=null){t.setCompoundDrawables(symbol(c,icon,active?GREEN:0xff6e7a82,14),null,null,null);t.setCompoundDrawablePadding(dp(c,5));}return t;}
 static void tint(Switch s){s.setThumbTintList(ColorStateList.valueOf(Color.WHITE));s.setTrackTintList(new ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{GREEN,0xffbcc9c5}));s.setSplitTrack(false);s.setShowText(false);s.setSwitchMinWidth(dp(s.getContext(),43));s.setMinimumHeight(dp(s.getContext(),48));}
 static void enabled(View v,boolean on){v.setEnabled(on);v.setAlpha(on?1:.45f);}
 static final class IconDrawable extends Drawable {
  final String name;final Paint p=new Paint(3);final int color;
  IconDrawable(String name,int color){this.name=name;this.color=color;}
  void line(Canvas c,float... pts){Path q=new Path();q.moveTo(pts[0],pts[1]);for(int i=2;i<pts.length;i+=2)q.lineTo(pts[i],pts[i+1]);c.drawPath(q,p);}
  void circle(Canvas c,float x,float y,float r){c.drawCircle(x,y,r,p);}
  @Override public void draw(Canvas canvas){canvas.save();Rect b=getBounds();canvas.translate(b.left,b.top);canvas.scale(b.width()/24f,b.height()/24f);p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);
   switch(name){
    case "logo":canvas.drawRoundRect(3,3,15,15,3,3,p);p.setColor(GREEN);p.setStyle(Paint.Style.FILL);canvas.drawRoundRect(9,9,22,22,3,3,p);p.setColor(color);p.setStyle(Paint.Style.STROKE);canvas.drawRoundRect(9,9,21,21,3,3,p);break;
    case "wifi":canvas.drawArc(2,4,22,23,219,102,false,p);canvas.drawArc(6,9,18,22,220,100,false,p);p.setStyle(Paint.Style.FILL);circle(canvas,12,19,1.6f);break;
    case "lock":canvas.drawRoundRect(5,10,19,22,2,2,p);canvas.drawArc(8,2,16,16,180,180,false,p);line(canvas,12,15,12,18);break;
    case "pin":Path pin=new Path();pin.moveTo(12,22);pin.cubicTo(9,18,4,13,4,9);pin.cubicTo(4,-1,20,-1,20,9);pin.cubicTo(20,13,15,18,12,22);canvas.drawPath(pin,p);circle(canvas,12,9,2.5f);break;
    case "sync":canvas.drawArc(3,3,21,21,205,145,false,p);canvas.drawArc(3,3,21,21,25,145,false,p);line(canvas,17,8,21,10,23,6);line(canvas,7,16,3,14,1,18);break;
    case "plus":line(canvas,12,3,12,21);line(canvas,3,12,21,12);break;
    case "arrow":line(canvas,9,5,16,12,9,19);break;
    case "back":line(canvas,15,5,8,12,15,19);break;
    case "more":p.setStyle(Paint.Style.FILL);circle(canvas,5,12,1.4f);circle(canvas,12,12,1.4f);circle(canvas,19,12,1.4f);break;
    case "play":p.setStyle(Paint.Style.FILL);Path play=new Path();play.moveTo(7,3);play.lineTo(21,12);play.lineTo(7,21);play.close();canvas.drawPath(play,p);break;
    case "external":line(canvas,13,3,21,3,21,11);line(canvas,21,3,11,13);line(canvas,9,5,4,5,4,21,20,21,20,15);break;
    case "check":line(canvas,4,12,9,17,20,6);break;
    case "gear":circle(canvas,12,12,6.5f);circle(canvas,12,12,2.5f);for(int i=0;i<8;i++){canvas.save();canvas.rotate(i*45,12,12);line(canvas,12,2,12,4);canvas.restore();}break;
    default:circle(canvas,12,12,9);line(canvas,12,10,12,17);p.setStyle(Paint.Style.FILL);circle(canvas,12,6,1);break;
   }canvas.restore();}
  @Override public void setAlpha(int alpha){p.setAlpha(alpha);}
  @Override public void setColorFilter(ColorFilter f){p.setColorFilter(f);}
  @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
 }
 private Ui(){}
}
