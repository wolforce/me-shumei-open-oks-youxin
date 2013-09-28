package me.shumei.open.oks.youxin;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.json.JSONObject;
import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;

import com.gl.softphone.HttpEncrypt;

import android.content.Context;
import android.telephony.TelephonyManager;

/**
 * 使签到类继承CommonData，以方便使用一些公共配置信息
 * 
 * @author wolforce
 * 
 */
public class Signin extends CommonData {
    String resultFlag = "false";
    String resultStr = "未知错误！";

    /** 加密串 */
    private final static String KEY = "~U!X@I#N$";

    /**
     * <p>
     * <b>程序的签到入口</b>
     * </p>
     * <p>
     * 在签到时，此函数会被《一键签到》调用，调用结束后本函数须返回长度为2的一维String数组。程序根据此数组来判断签到是否成功
     * </p>
     * 
     * @param ctx
     *            主程序执行签到的Service的Context，可以用此Context来发送广播
     * @param isAutoSign
     *            当前程序是否处于定时自动签到状态<br />
     *            true代表处于定时自动签到，false代表手动打开软件签到<br />
     *            一般在定时自动签到状态时，遇到验证码需要自动跳过
     * @param cfg
     *            “配置”栏内输入的数据
     * @param user
     *            用户名
     * @param pwd
     *            解密后的明文密码
     * @return 长度为2的一维String数组<br />
     *         String[0]的取值范围限定为两个："true"和"false"，前者表示签到成功，后者表示签到失败<br />
     *         String[1]表示返回的成功或出错信息
     */
    public String[] start(Context ctx, boolean isAutoSign, String cfg, String user, String pwd) {
        // 把主程序的Context传送给验证码操作类，此语句在显示验证码前必须至少调用一次
        CaptchaUtil.context = ctx;
        // 标识当前的程序是否处于自动签到状态，只有执行此操作才能在定时自动签到时跳过验证码
        CaptchaUtil.isAutoSign = isAutoSign;

        try {
            // 存放Cookies的HashMap
            HashMap<String, String> cookies = new HashMap<String, String>();
            // Jsoup的Response
            Response res;

            String loginUrl = getLoginUrl(ctx, user, pwd, getIMEI(ctx));
            String signinUrl = getSigninUrl();

            // 登录账号获取Cookies
            // {"result":4}//账号密码不正确
            // {"uid":"19908123","result":0,"expire":24,"phone":"13677721234","uploadlog":1,"epayurl":"http:\/\/epay.keepc.com\/epay\/gateway\/alipay_security.act","improxylist":["117.121.55.200:8080"],"rtpplist":["113.31.81.110"],"sipreg":1,"vip":0,"recharge":0,"phoneserver":"113.31.81.112:50004","actionreport":1,"imserver":"113.31.81.110:8060","voip":"113.31.81.110:518","voiplist":["117.121.55.205:518"]}
            res = Jsoup.connect(loginUrl).header("Accept-Charset", "utf-8").header("Connection", "close").header("SecurityFlag", getSecurityFlag()).timeout(TIME_OUT).ignoreContentType(true).method(Method.GET).execute();
            cookies.putAll(res.cookies());
            System.out.println(res.body());

            JSONObject jsonObj = new JSONObject(res.body());
            int result = jsonObj.getInt("result");
            switch (result) {
                case 0:
                    // 登录成功，提交签到信息
                    //{"signin_counts": 1, "max_counts": 1, "award_money": 32, "alert": 0, "content": "恭喜签到成功\n赚得4分钟奖励，明天继续哦!", "result": 0, "total_award_money": 32, "content2": "您今天已签到过\n赚了4分钟，请明天再来!"}
                    //{"signin_counts": 1, "max_counts": 1, "award_money": 32, "content": "您今天已签到过\n赚了4分钟，请明天再来!", "result": 21, "total_award_money": 32, "content2": "您今天已签到过\n赚了4分钟，请明天再来!"}
                    res = Jsoup.connect(signinUrl).cookies(cookies).header("Accept-Charset", "utf-8").header("Connection", "close").header("SecurityFlag", getSecurityFlag()).timeout(TIME_OUT).ignoreContentType(true).method(Method.GET).execute();
                    jsonObj = new JSONObject(res.body());
                    System.out.println(signinUrl);
                    System.out.println(res.body());
                    int signinResult = jsonObj.getInt("result");
                    if (signinResult == 0 || signinResult == 21) {
                        // 0=>签到成功，21=>已签过到
                        String content = jsonObj.getString("content");
                        resultFlag = "true";
                        resultStr = content;
                    } else {
                        resultFlag = "false";
                        resultStr = "登录成功，提交签到请求时服务器返回失败信息，请重试";
                    }
                    break;

                default:
                    resultFlag = "false";
                    resultStr = "账号密码有误或服务器返回信息异常";
                    break;
            }

        } catch (IOException e) {
            this.resultFlag = "false";
            this.resultStr = "连接超时";
            e.printStackTrace();
        } catch (Exception e) {
            this.resultFlag = "false";
            this.resultStr = "未知错误！";
            e.printStackTrace();
        }

        return new String[] { resultFlag, resultStr };
    }

    /**
     * 构造登录URL
     * 
     * @param user
     * @param pwd
     * @param imei
     * @return
     */
    private String getLoginUrl(Context ctx, String user, String pwd, String imei) {
        StringBuilder sb = new StringBuilder("http://im.uxin.com:8887/");
        sb.append("login?sn=");
        sb.append(getSn());
        sb.append("&account=");
        sb.append(user);
        sb.append("&pwd=");
        sb.append(MD5.md5(pwd + KEY));
        sb.append("&pv=android&v=2.11.0&netmode=1&brand=Xiaomi&model=MI-ONE%2BPlus&osv=2.3.5");
        sb.append("&imei=");
        sb.append(getIMEI(ctx));
        sb.append("&unionid=2&securityver=1");// unionid应该是有信的渠道号，已知的有2和5，2=>手机访问有信官网下载，5=>电脑访问有信官网下载
        String sign = getSignFromJNI(sb.toString());// login?sn=1380205573616&account=1367123456&pwd=10461234fa4b1234a011234960801aa9&pv=android&v=2.11.0&netmode=3&brand=generic&model=sdk&osv=2.1-update1&imei=000000000000000&unionid=5&securityver=1
        sb.append("&sign=");
        sb.append(sign);
        return sb.toString();
    }

    /**
     * 获取签到URL
     * 
     * @return
     */
    private String getSigninUrl() {
        // http://im.uxin.com:8887/signin?sn=1380300003877&resignin=0&confirm=0&securityver=1&sign=1c18d123482d0461234fec5c141234fe3e26b11f
        String sign = "";
        StringBuilder sb = new StringBuilder();
        sb.append("http://im.uxin.com:8887/signin?sn=");
        sb.append(getSn());
        sb.append("&resignin=0&confirm=0&securityver=1");
        sign = getSignFromJNI(sb.toString());
        sb.append("&sign=");
        sb.append(sign);
        return sb.toString();
    }

    /**
     * 获取SN
     * 
     * @return
     */
    private String getSn() {
        long time = System.currentTimeMillis();
        int randomSix = (int) (Math.random() * 6.0) + 1;
        return String.valueOf(((long) randomSix + time));
    }

    /**
     * 获取sign<br>
     * 把url的所有参数按参数名升序排列，然后逐一获取参数值， 连成一串字符串，再把这串字符串发给JNI加密函数处理得到sign
     * 
     * @return
     */
    public String getSignFromJNI(String url) {
        url = url.contains("?") ? url.substring(url.indexOf("?") + 1) : url;
        String[] b = url.split("&");
        if (b == null) {
            return "";
        }

        ArrayList<String> list = new ArrayList<String>();
        for (int i = 0; i < b.length; i++) {
            list.add(b[i]);
        }
        Collections.sort(list);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            String pointStr = list.get(i);
            int point = pointStr.indexOf("=");
            if (point >= 0) {
                pointStr = pointStr.substring(point + 1);
            }
            try {
                sb.append(URLDecoder.decode(pointStr, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        return HttpEncrypt.getInstance().pub_SignEncrypt(sb.toString());
    }

    /**
     * 预置10个以16进制表示的ASCII字符，然后随机生成一个数字字符串，遍历数字字符串，把数字作为下标取出预置数组的字符
     * 
     * @return
     */
    public static String getSecurityFlag() {
        String[] hexMap = { "64", "65", "79", "62", "69", "70", "76", "6b", "7a", "6f" };
        char[] charMap = new char[10];
        for (int i = 0; i < hexMap.length; i++) {
            int tempVal = Integer.valueOf(hexMap[i], 16);
            charMap[i] = (char) tempVal;
        }

        int x = (int) (Math.random() * 100.0) + 100;
        int y = (int) (Math.random() * 9999.0) + 1;
        int z = (y * 256) + x;
        String strc = String.valueOf(z);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < strc.length(); i++) {
            char ch = strc.charAt(i);
            int index = Integer.valueOf(ch + "");
            sb.append(charMap[index]);
        }
        return sb.toString();
    }

    /**
     * 获取手机的IMEI
     * 
     * @return String
     */
    public static String getIMEI(Context context) {
        String imei = "456156451534587";// 随机串
        try {
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            imei = telephonyManager.getDeviceId();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return imei;
    }

}
