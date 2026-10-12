package pl.hoodstories.hoodphone.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.Font;
import net.minecraft.world.item.ItemStack;
import pl.hoodstories.hoodphone.IPhone18ProItem;
import pl.hoodstories.hoodphone.PhoneFinish;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Premium 242x430 phone canvas for Forge 1.19.2.
 * All rendering is client-only; calls, contacts, messages and social backends remain unchanged.
 * Icons and wallpaper artwork are original, not bundled Apple artwork.
 */
public final class PhoneUi extends GuiComponent {
    public static final int PHONE_W = 242;
    public static final int PHONE_H = 430;
    public static final int BEZEL = 8;
    public static final int SCREEN_W = 226;
    public static final int SCREEN_H = 414;
    private PhoneUi() {}

    public static int left(int windowWidth) { return windowWidth / 2 - PHONE_W / 2; }
    public static int top(int windowHeight) { return Math.max(5, windowHeight / 2 - PHONE_H / 2); }
    public static int sx(int windowWidth) { return left(windowWidth) + BEZEL; }
    public static int sy(int windowHeight) { return top(windowHeight) + BEZEL; }

    public static PhoneFinish finish(Minecraft mc) {
        if (mc != null && mc.player != null) {
            PhoneFinish f = finishOf(mc.player.getMainHandItem());
            if (f != null) return f;
            f = finishOf(mc.player.getOffhandItem());
            if (f != null) return f;
            for (int slot = 0; slot < mc.player.getInventory().getContainerSize(); slot++) {
                f = finishOf(mc.player.getInventory().getItem(slot));
                if (f != null) return f;
            }
        }
        return PhoneFinish.BLACK;
    }
    private static PhoneFinish finishOf(ItemStack stack) {
        if (stack != null && !stack.isEmpty() && stack.getItem() instanceof IPhone18ProItem phone)
            return phone.finish();
        return null;
    }
    public static int rgb(int color) { return color | 0xff000000; }
    public static int withAlpha(int color, int alpha) { return ((alpha & 255) << 24) | (color & 0x00ffffff); }
    public static int mix(int c0, int c1, float amount) {
        float t = Math.min(1f, Math.max(0f, amount));
        int r = (int) (((c0 >> 16) & 255) * (1f - t) + ((c1 >> 16) & 255) * t);
        int g = (int) (((c0 >> 8) & 255) * (1f - t) + ((c1 >> 8) & 255) * t);
        int b = (int) ((c0 & 255) * (1f - t) + (c1 & 255) * t);
        return (r << 16) | (g << 8) | b;
    }
    public static void roundRect(PoseStack p, int x, int y, int w, int h, int rad, int col) {
        if (w <= 0 || h <= 0) return;
        int r = Math.max(0, Math.min(rad, Math.min(w, h) / 2));
        if (r == 0) { fill(p,x,y,x+w,y+h,col); return; }
        fill(p,x+r,y,x+w-r,y+h,col);
        for (int dy=0; dy<r; dy++) {
            double off = r-dy-0.5;
            int cut = r-(int)Math.floor(Math.sqrt(Math.max(0.0,r*r-off*off)));
            fill(p,x+cut,y+dy,x+w-cut,y+dy+1,col);
            fill(p,x+cut,y+h-1-dy,x+w-cut,y+h-dy,col);
        }
        fill(p,x,y+r,x+w,y+h-r,col);
    }
    public static void circle(PoseStack p, int x, int y, int radius, int col) {
        if(radius<1) return;
        for(int dy=-radius;dy<=radius;dy++) {
            int dx=(int)Math.sqrt((long)radius*radius-(long)dy*dy);
            fill(p,x-dx,y+dy,x+dx+1,y+dy+1,col);
        }
    }

    public static void drawDevice(PoseStack p, int width, int height, PhoneFinish finish, boolean wallpaper) {
        int x=left(width), y=top(height);
        int body=finish.bodyRgb();
        // Shadow and illuminated colour-matched precision-milled rim.
        roundRect(p,x-5,y+4,PHONE_W+10,PHONE_H+8,27,0x48000000);
        roundRect(p,x-2,y-2,PHONE_W+4,PHONE_H+4,24,rgb(mix(body,0xffffff,.34f)));
        roundRect(p,x-1,y-1,PHONE_W+2,PHONE_H+2,23,rgb(mix(body,0x141416,.46f)));
        roundRect(p,x+1,y+1,PHONE_W-2,PHONE_H-2,22,0xff08090d);
        roundRect(p,x+5,y+5,PHONE_W-10,PHONE_H-10,18,0xff000000);
        if(wallpaper) drawWallpaper(p,x+BEZEL,y+BEZEL,SCREEN_W,SCREEN_H,finish);
        else roundRect(p,x+BEZEL,y+BEZEL,SCREEN_W,SCREEN_H,14,0xff0e121b);
        // Hardware side buttons and thin specular glass edge.
        fill(p,x-4,y+75,x-2,y+115,rgb(mix(body,0xffffff,.48f)));
        fill(p,x-4,y+127,x-2,y+160,rgb(mix(body,0xffffff,.48f)));
        fill(p,x+PHONE_W+2,y+111,x+PHONE_W+4,y+157,rgb(mix(body,0xffffff,.48f)));
        fill(p,x+4,y+47,x+5,y+PHONE_H-47,0x337f9ab6);
        // Dynamic Island, lens highlight and privacy indicator.
        roundRect(p,x+PHONE_W/2-39,y+11,78,17,9,0xff030305);
        circle(p,x+PHONE_W/2+26,y+19,4,0xff111927);
        circle(p,x+PHONE_W/2+25,y+18,1,0xff284864);
    }

    public static void drawWallpaper(PoseStack p,int x,int y,int w,int h,PhoneFinish finish) {
        int top, middle, bottom, glow;
        switch(finish) {
            case SILVER -> { top=0x0e2945; middle=0x46657e; bottom=0x19243d; glow=0xb1c8e3; }
            case GLACIER -> { top=0x091b40; middle=0x12496e; bottom=0x152f58; glow=0x7bb9ee; }
            case BURGUNDY -> { top=0x300d26; middle=0x6a244d; bottom=0x17132f; glow=0xc978ab; }
            default -> { top=0x080c1a; middle=0x25375f; bottom=0x070e28; glow=0x7771ed; }
        }
        roundRect(p,x,y,w,h,14,rgb(top));
        // Lightweight, pre-determined 2-pixel gradient strips; no per-tick network work.
        for (int iy=14;iy<h-14;iy+=2) {
            float v=iy/(float)h;
            int col=v<.58f?mix(top,middle,v/.58f):mix(middle,bottom,(v-.58f)/.42f);
            fill(p,x+3,y+iy,x+w-3,y+iy+2,rgb(col));
        }
        // Layered curved aurora ribbons, clipped naturally inside the screen boundaries.
        for(int i=0;i<86;i++) {
            int cy=y+116+i*2;
            int bend=(int)(31*Math.sin((i+12)*.045));
            int x0=x+53+bend+(int)(12*Math.sin(i*.10));
            int width=Math.max(10,110-(int)Math.abs(i-40)*2);
            if(cy < y+h-20) {
                int col=withAlpha(glow,Math.max(24,150-Math.abs(i-38)*3));
                fill(p,Math.max(x+7,x0),cy,Math.min(x+w-7,x0+width),cy+1,col);
            }
        }
        for(int i=0;i<50;i++) {
            int cy=y+206+i*2;
            int dx=(int)(20*Math.sin(i*.07));
            int radius=76-(int)Math.abs(i-26)*2;
            if(radius>0 && cy<y+h-18)
                fill(p,Math.max(x+7,x+105-radius+dx),cy,Math.min(x+w-7,x+105+radius+dx),cy+1,withAlpha(glow,27));
        }
        // Surface dimming helps retain readable clock and labels.
        fill(p,x+5,y+35,x+w-5,y+54,0x12000000);
    }

    public static void drawStatusBar(PoseStack p,Font font,int winW,int winH,int textColour) {
        int x=sx(winW), y=sy(winH), color=textColour;
        String time=new SimpleDateFormat("HH:mm",Locale.ROOT).format(new Date());
        font.draw(p,time,x+12,y+10,color);
        // Native-look signal bars, Wi-Fi approximation and battery.
        for(int i=0;i<4;i++) fill(p,x+155+i*4,y+18-(i*2),x+158+i*4,y+20,color);
        fill(p,x+175,y+12,x+188,y+14,color);
        fill(p,x+178,y+15,x+185,y+17,color);
        circle(p,x+181,y+19,1,color);
        roundRect(p,x+196,y+10,20,10,3,0x77000000);
        roundRect(p,x+197,y+11,15,8,2,color);
        fill(p,x+216,y+13,x+218,y+17,color);
    }

    public static void drawHomeBar(PoseStack p,int winW,int winH,int unused) {
        int x=sx(winW), y=sy(winH);
        roundRect(p,x+79,y+SCREEN_H-15,68,4,3,0xeaffffff);
    }

    private static int iconColor(String type) {
        return switch(type) {
            case "phone","phone_white","hangup" -> 0xff36c868;
            case "messages" -> 0xff37c979;
            case "contacts" -> 0xff8f9ba9;
            case "social" -> 0xffda518b;
            case "notes" -> 0xfff4bd45;
            case "calculator" -> 0xff35363c;
            case "clock" -> 0xfff3f4f7;
            case "maps" -> 0xff559ee0;
            case "settings" -> 0xff747e8a;
            case "moon" -> 0xff6559bf;
            case "flashlight" -> 0xff34393c;
            case "camera" -> 0xffaaaaaf;
            default -> 0xff4e8bc3;
        };
    }
    public static void icon(PoseStack p,String type,int x,int y,int size) {
        boolean special="hangup".equals(type);
        int bg=special?0xffe44754:iconColor(type);
        roundRect(p,x,y,size,size,Math.max(4,size/4),bg);
        int cx=x+size/2, cy=y+size/2, white=0xfffdfdff;
        int q=Math.max(2,size/10);
        switch(type) {
            case "phone","phone_white","hangup" -> {
                // Distinctive curved handset with direction cue.
                roundRect(p,cx-size/5,cy-size/4,Math.max(3,size/7),size/2,3,white);
                roundRect(p,cx-size/6,cy+size/10,size/3,Math.max(3,size/7),3,white);
                circle(p,cx+size/7,cy+size/6,Math.max(2,size/10),white);
                if(special) fill(p,x+4,cy-1,x+size-4,cy+2,bg);
            }
            case "messages" -> {
                roundRect(p,x+size/5,y+size/4,size*3/5,size*2/5,size/6,white);
                fill(p,x+size/3,y+size*3/5,x+size/2,y+size*4/5,white);
            }
            case "contacts" -> {
                circle(p,cx,cy-size/7,Math.max(2,size/8),white);
                roundRect(p,cx-size/4,cy+size/15,size/2,size/4,size/7,white);
            }
            case "camera" -> {
                roundRect(p,x+size/5,y+size/3,size*3/5,size*2/5,3,0xff323541);
                circle(p,cx,cy+size/30,Math.max(3,size/7),white);
                circle(p,cx,cy+size/30,Math.max(1,size/11),0xff323541);
                fill(p,cx-size/5,y+size/4,cx+size/6,y+size/3,white);
            }
            case "clock" -> {
                circle(p,cx,cy,size/3,0xff2b313e);
                circle(p,cx,cy,size/4,white);
                fill(p,cx,cy-size/5,cx+1,cy+1,0xff2b313e);
                fill(p,cx,cy,cx+size/7,cy+1,0xff2b313e);
            }
            case "calculator" -> {
                fill(p,cx-size/5,cy-size/3,cx+size/5,cy-size/4,white);
                for(int r=0;r<2;r++) for(int c=0;c<2;c++)
                    roundRect(p,cx-size/5+c*size/5,cy-size/9+r*size/5,q,q,1,white);
            }
            case "settings" -> {
                circle(p,cx,cy,size/3,0xfff7f7fb);
                circle(p,cx,cy,size/6,0xff747e8a);
            }
            case "maps" -> {
                circle(p,cx,cy-size/10,size/4,white);
                circle(p,cx,cy-size/10,size/8,0xff559ee0);
                fill(p,cx-2,cy+size/9,cx+2,cy+size/3,white);
            }
            case "notes" -> {
                roundRect(p,x+size/4,y+size/5,size/2,size*3/5,2,white);
                for(int i=0;i<3;i++)fill(p,x+size/3,y+size/3+i*size/8,x+size*2/3,y+size/3+i*size/8+1,bg);
            }
            case "social" -> {
                roundRect(p,x+size/4,y+size/4,size/2,size/2,size/6,white);
                circle(p,cx,cy,size/7,bg);
                circle(p,cx+size/7,cy-size/7,Math.max(1,size/25),bg);
            }
            case "moon" -> {
                circle(p,cx,cy,size/4,white);
                circle(p,cx+size/7,cy-size/9,size/4,bg);
            }
            case "flashlight" -> {
                roundRect(p,cx-size/7,cy-size/7,size*2/7,size/3,3,white);
                fill(p,cx-size/4,cy-size/3,cx+size/4,cy-size/7,white);
            }
            default -> {
                circle(p,cx,cy,size/4,white);
                circle(p,cx,cy,size/8,bg);
            }
        }
    }
    public static void appIcon(PoseStack p,Font font,String icon,String label,int x,int y) {
        icon(p,icon,x,y,36);
        int sw=font.width(label);
        font.draw(p,label,x+18-sw/2f,y+39,0xfff8faff);
    }
    public static void avatar(PoseStack p,Font font,String name,int x,int y,int radius,int color) {
        circle(p,x,y,radius,color);
        String label=initials(name);
        float scale=Math.max(1f,radius/12f);
        p.pushPose();
        p.translate(x,y,0);
        p.scale(scale,scale,1f);
        font.draw(p,label,-font.width(label)/2f,-4f,0xffffffff);
        p.popPose();
    }
    public static String initials(String name) {
        if(name==null||name.isBlank())return "?";
        String[] arr=name.trim().split("\\s+");
        if(arr.length==1)return arr[0].substring(0,1).toUpperCase(Locale.ROOT);
        return (arr[0].substring(0,1)+arr[arr.length-1].substring(0,1)).toUpperCase(Locale.ROOT);
    }
    public static void card(PoseStack p,int x,int y,int w,int h,boolean active) {
        roundRect(p,x,y,w,h,10,active?0xdff3f5fb:0xb41f2835);
    }
    public static boolean hit(double mx,double my,int x,int y,int w,int h) {
        return mx>=x&&mx<x+w&&my>=y&&my<y+h;
    }
}
