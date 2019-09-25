package com.demo.tools;

import javax.servlet.http.HttpServletRequest;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 对字符串操作的类
 * @author 欧阳彦祖
 *
 */
public class StringUnits<main> {
	
	/**
	 * 判断该字符串是否存在
	 * 存在为false
	 * 不存在为true
	 * @param str
	 * @return
	 */
	public static boolean isEmpty(String str){
		if(str==null || str.trim().equals("")){
			return true;
		}
		return false;
	}

	public static void main(String[] args){
		System.out.println(UUID.randomUUID().toString());
	}


	/**
	 * 获取实体类所有属性名称
	 * @param obj	要获取的实体类
	 * @return
	 */
	public static List<String> getClassFieldName(Object obj){
		List<String> resultList=new ArrayList<String>();
		if(obj==null){	//如果对象为null，则返回一个实例化元素为空的List(避免空异常)
			return resultList;
		}
		Field[] fields=obj.getClass().getDeclaredFields();
		for(Field field:fields){
			resultList.add(field.getName());
		}
		return resultList;
	}


	/**
	 * 获取实体类部分属性名称
	 * @param obj 要获取的实体类
	 * @param removeName 剔除属性名称的数组
	 * @return
	 */
	public static List<String> getClassFieldName(Object obj,String[] removeName){
		List<String> resultList=getClassFieldName(obj);
		//如果剔除属性名称数组为空则返回所有属性名称
		if(removeName==null || removeName.length<=0){
			return resultList;
		}
		for(String field:removeName){
			resultList.remove(field);
		}
		return resultList;
	}

	
	/**
	 * 判断是否为数字
	 * @param str
	 * @return
	 */
	public static boolean isInteger(String str){
		Pattern pattern=Pattern.compile("[0-9]*");
		Matcher isNum=pattern.matcher(str);
		if(!isNum.matches()){
			return false;
		}
		return true;
	}
	
	/**
	 * 随机生成数字 长度自己指定
	 * @param length 长度
	 * @return
	 */
	public static String randomCode(int length){
		String str="0123456789";
		StringBuilder sb=new StringBuilder(4);
		for(int i=0;i<length;i++)
		{
		char ch=str.charAt(new Random().nextInt(str.length()));
		sb.append(ch);
		}
		return sb.toString();
	}
	
	
	/**
     * 判断是否含有特殊字符
     *
     * @param str
     * @return true为包含，false为不包含
     */
    public static boolean isSpecialChar(String str) {
        String regEx = "[^a-zA-Z0-9]";
        Pattern p = Pattern.compile(regEx);
        Matcher m = p.matcher(str);
        return m.find();
    }
    
    
	/**
	 * 获取传进来的参数
	 * @param request
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public static String getParam(HttpServletRequest request){
		Map params=request.getParameterMap();
		Iterator it = params.keySet().iterator();
		StringBuffer sb=new StringBuffer("请求方式："+request.getMethod()+";");
		while(it.hasNext()){
		    String paramName = (String) it.next();
		    String paramValue = request.getParameter(paramName);
		    sb.append("参数："+paramName);
		    sb.append(",值："+paramValue+";");
		}
		return sb.toString();
	}
	
	/**
	 * 获取request中的名称和值
	 * @param request
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public static Map<String,Object> getParamValue(HttpServletRequest request) {
		Map<String,Object> resultMap=new HashMap<String, Object>();
		Map params=request.getParameterMap();
		Iterator it = params.keySet().iterator();
		while(it.hasNext()){
		    String paramName = (String) it.next();
		    String paramValue = request.getParameter(paramName);
		    resultMap.put(paramName, paramValue);
		}
		return resultMap;
	}
	
	
	/**
	 * 替换字符串
	 * @param str 原字符串
	 * @param startIndex 开始小标
	 * @param endIndex 结束下标
	 * @param replaceWord 需要替换的字符串
	 * @param wordQuantity 需要替换的字符串数量
	 * @return
	 */
	public static String getReplaceWord(String str,int startIndex,int endIndex,String replaceWord,int wordQuantity){
		String result="";
		for(int i=startIndex;i<wordQuantity;i++){
			result+=replaceWord;
		}
		result+=str.substring(endIndex+1, str.length());
		return result;
	}
	
	/**
	 * 生成UUID
	 * @return
	 */
	public static String getUUID(){
		return UUID.randomUUID().toString().replace("-","").toLowerCase();
	}

	
	
	/**
	 * 获取request中的名称和值
	 * @param request
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public static Map<String,Object> getRequestValue(HttpServletRequest request) {
		Map<String,Object> resultMap=new HashMap<String, Object>();
		Map params=request.getParameterMap();
		Iterator it = params.keySet().iterator();
		while(it.hasNext()){
		    String paramName = (String) it.next();
		    String paramValue = request.getParameter(paramName);
		    resultMap.put(paramName, paramValue);
		}
		return resultMap;
	}

	/**
	 * 获取request中的名称和值
	 * @param request
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public static String isNullRequestValue(HttpServletRequest request) {
		Map<String,Object> resultMap=new HashMap<String, Object>();
		Map params=request.getParameterMap();
		if(params==null || params.isEmpty()){
			return "请您携带参数请求。";
		}
		Iterator it = params.keySet().iterator();
		while(it.hasNext()){
			String paramName = (String) it.next();
			System.out.println(paramName);
			String paramValue = request.getParameter(paramName);
			System.out.println(paramName+"="+paramValue);
			if(StringUnits.isEmpty(paramValue)){
				return paramName+"参数不可为空。";
			}
		}
		return null;
	}

	/**
	 * 判断map是否有key为空值
	 * @param params
	 * @return
	 */
	public static String isNullMapValue(Map<String,Object> params) {
		if(params==null || params.isEmpty()){
			return "请您携带参数请求。";
		}
		Iterator it = params.keySet().iterator();
		while(it.hasNext()){
			String paramName = (String) it.next();
			String paramValue = params.get(paramName).toString();
			System.out.println(paramName+"="+paramValue);
			if(StringUnits.isEmpty(paramValue)){
				return paramName+"参数不可为空。";
			}
		}
		return null;
	}

	/**
	 * 根据paramsName是否有包含在Map中
	 * @param params 参数Map
	 * @param paramNames	参数名称集合
	 * @return
	 */
	public static String isNullMapValue(Map<String,Object> params,List<String> paramNames){
		if(params==null || params.isEmpty()){
			return "请您携带参数请求。";
		}
		if(paramNames==null || paramNames.size()<=0){
			return "请您携带参数名称。";
		}
		for(String paramName:paramNames){
			if(!params.containsKey(paramName)){
				return paramName+"参数不存在。";
			}else{
				if(StringUnits.isEmpty(params.get(paramName).toString())){
					return paramName+"参数不可为空。";
				}
			}
		}
		return null;
	}
	
	
	//获取properties文件里值得主要方法，根据它的key来获取
    public static String getSourcingValueBykey(String key,InputStream inStream){
        String value="";
        try{
            Properties properties = new Properties();
            properties.load(inStream);
            inStream.close();
            
            value = properties.getProperty(key);
            if(value == null || value.equals("")){
                System.out.println("The value for key: " +  key + " doesn't exist.");
                System.out.println("Please check the content of the properties file.");
                
            }
            }catch(Exception e){
                e.printStackTrace();
            }
            return value;
    }
	
    public static String replaceBlank(String str) {
		String dest = "";
		if (str!=null) {
			Pattern p = Pattern.compile("\r|\n");
			Matcher m = p.matcher(str);
			dest = m.replaceAll("");
		}
		return dest;
	}
	
    /**
	 * 中文转换unicode字符
	 * @param cn
	 * @return
	 */
	public static String cnToUnicode(String cn) {
	    char[] chars = cn.toCharArray();
	    String returnStr = "";
	    for (int i = 0; i < chars.length; i++) {
	      returnStr += "\\u" + Integer.toString(chars[i], 16);
	    }
	    return returnStr;
	}
	
	
	/**
	 * unicode转换成中文
	 * @param unicode
	 * @return
	 */
	public static String unicodeToCn(String unicode) {
	    /** 以 \ u 分割，因为java注释也能识别unicode，因此中间加了一个空格*/
	    String[] strs = unicode.split("\\\\u");
	    String returnStr = "";
	    // 由于unicode字符串以 \ u 开头，因此分割出的第一个字符是""。
	    for (int i = 1; i < strs.length; i++) {
	      returnStr += (char) Integer.valueOf(strs[i], 16).intValue();
	    }
	    return returnStr;
	}

	/**
	 * 获取服务器真实ip地址
	 * @return
	 */
	public static String getInet4Address() {
		Enumeration<NetworkInterface> nis;
		String ip = null;
		try {
			nis = NetworkInterface.getNetworkInterfaces();
			for (; nis.hasMoreElements();) {
				NetworkInterface ni = nis.nextElement();
				Enumeration<InetAddress> ias = ni.getInetAddresses();
				for (; ias.hasMoreElements();) {
					InetAddress ia = ias.nextElement();
					//ia instanceof Inet6Address && !ia.equals("")
					if (ia instanceof Inet4Address && !ia.getHostAddress().equals("127.0.0.1")) {
						ip = ia.getHostAddress();
					}
				}
			}
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ip;
	}

}
