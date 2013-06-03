package me.shumei.open.oks.youxin;

import java.io.IOException;
import java.util.HashMap;

import org.json.JSONObject;
import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;

import android.content.Context;
import android.telephony.TelephonyManager;

/**
 * 使签到类继承CommonData，以方便使用一些公共配置信息
 * @author wolforce
 *
 */
public class Signin extends CommonData {
	String resultFlag = "false";
	String resultStr = "未知错误！";
	
	/**加密串*/
	private final static String KEY = "k1oET&Yh7@EQnp2XdTP1o/Vo=";
	
	/**
	 * <p><b>程序的签到入口</b></p>
	 * <p>在签到时，此函数会被《一键签到》调用，调用结束后本函数须返回长度为2的一维String数组。程序根据此数组来判断签到是否成功</p>
	 * @param ctx 主程序执行签到的Service的Context，可以用此Context来发送广播
	 * @param isAutoSign 当前程序是否处于定时自动签到状态<br />true代表处于定时自动签到，false代表手动打开软件签到<br />一般在定时自动签到状态时，遇到验证码需要自动跳过
	 * @param cfg “配置”栏内输入的数据
	 * @param user 用户名
	 * @param pwd 解密后的明文密码
	 * @return 长度为2的一维String数组<br />String[0]的取值范围限定为两个："true"和"false"，前者表示签到成功，后者表示签到失败<br />String[1]表示返回的成功或出错信息
	 */
	public String[] start(Context ctx, boolean isAutoSign, String cfg, String user, String pwd) {
		//把主程序的Context传送给验证码操作类，此语句在显示验证码前必须至少调用一次
		CaptchaUtil.context = ctx;
		//标识当前的程序是否处于自动签到状态，只有执行此操作才能在定时自动签到时跳过验证码
		CaptchaUtil.isAutoSign = isAutoSign;
		
		try{
			//存放Cookies的HashMap
			HashMap<String, String> cookies = new HashMap<String, String>();
			//Jsoup的Response
			Response res;
			
			String loginUrl = getLoginUrl(user, pwd,getIMEI(ctx));
			String signinUrl = getSigninUrl();
			
			//登录账号获取Cookies
			//{"result":4}//账号密码不正确
			//{"uid":"19908123","result":0,"expire":24,"phone":"13677721234","uploadlog":1,"epayurl":"http:\/\/epay.keepc.com\/epay\/gateway\/alipay_security.act","improxylist":["117.121.55.200:8080"],"rtpplist":["113.31.81.110"],"sipreg":1,"vip":0,"recharge":0,"phoneserver":"113.31.81.112:50004","actionreport":1,"imserver":"113.31.81.110:8060","voip":"113.31.81.110:518","voiplist":["117.121.55.205:518"]}
			res = Jsoup.connect(loginUrl).userAgent(UA_CHROME).timeout(TIME_OUT).ignoreContentType(true).method(Method.GET).execute();
			cookies.putAll(res.cookies());
			JSONObject jsonObj = new JSONObject(res.body());
			int result = jsonObj.getInt("result");
			switch (result) {
				case 0:
					//登录成功，提交签到信息
					//{"content":"恭喜签到成功\n赚得4分钟奖励，明天继续哦!","result":0,"award_money":20,"alert":1,"content2":"您今天已签到过\n赚了4分钟，请明天再来!","alert_title":"分享一次可获得1分钟奖励","alert_type":"weibo","alert_msg":"山不在高，有仙则名，水不在深，有龙则灵。话费不在多，免费就行！今日个在#有信-免费电话#中签到只赚得4分钟的免费通话时长！明天继续走起~@最爱有信"}
					//{"content": "您今天已签到过\n赚了4分钟，请明天再来!", "award_money": 20, "result": 21, "content2": "您今天已签到过\n赚了4分钟，请明天再来!"}
					res = Jsoup.connect(signinUrl).cookies(cookies).userAgent(UA_CHROME).timeout(TIME_OUT).ignoreContentType(true).method(Method.GET).execute();
					jsonObj = new JSONObject(res.body());
					System.out.println(signinUrl);
					System.out.println(res.body());
					int signinResult = jsonObj.getInt("result");
					if(signinResult == 0 || signinResult == 21)
					{
						//0=>签到成功，21=>已签过到
						String content = jsonObj.getString("content");
						resultFlag = "true";
						resultStr = content;
						
					}
					else
					{
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
		
		return new String[]{resultFlag, resultStr};
	}
	
	
	/**
	 * 构造登录URL
	 * @param user
	 * @param pwd
	 * @param imei
	 * @return
	 */
	private String getLoginUrl(String user, String pwd, String imei)
	{
		String sn = String.valueOf(Math.round(1000.0D * Math.random()));
		String sign = MD5.md5(sn + user + KEY);
		StringBuilder sb = new StringBuilder();
		sb.append("http://im.uxin.com:8887/login?sn=");
		sb.append(sn);
		sb.append("&sign=");
		sb.append(sign);
		sb.append("&account=");
		sb.append(user);
		sb.append("&pwd=");
		sb.append(MD5.md5(pwd));
		sb.append("&pv=android&v=2.3.0&netmode=1&brand=Xiaomi&model=MI-ONE+Plus&osv=2.3.5");
		sb.append("&imei=");
		sb.append(imei);
		return sb.toString();
	}
	
	/**
	 * 获取签到URL
	 * @return
	 */
	private String getSigninUrl()
	{
		String sn = String.valueOf(Math.round(1000.0D * Math.random()));
		String sign = MD5.md5(sn + KEY);
		StringBuilder sb = new StringBuilder();
		sb.append("http://im.uxin.com:8887/signin?sn=");
		sb.append(sn);
		sb.append("&sign=");
		sb.append(sign);
		return sb.toString();
	}
	
	
	/**
	 * 获取手机的IMEI
	 * @return String
	 */
	public static String getIMEI(Context context)
	{
		String imei = "456156451534587";//随机串
		try {
			TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
			imei = telephonyManager.getDeviceId();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return imei;
	}
	
}
