package com.demo.tools;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.demo.entity.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JwtTokenUtil {

	/**
	 * 加密秘钥
	 */
	private static final String SECRET = "30ddc86d107c4d558db7186b3ce0a3e5";

	/**
	 * jackson
	 */
	private static ObjectMapper mapper = new ObjectMapper();

	/**
	 * header数据
	 * 
	 * @return
	 */
	private static Map<String, Object> createHead() {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("typ", "JWT");
		map.put("alg", "HS256");
		return map;
	}

	/**
	 * 生成Token
	 * 
	 * @param obj
	 *            放入要生成token的值
	 * @param maxAge
	 *            token的有效期(为毫秒)
	 * @param userName 用户名称
	 * @param userPass	用户密码
	 * @return
	 * @throws JsonProcessingException
	 * @throws IllegalArgumentException
	 * @throws JWTCreationException
	 * @throws UnsupportedEncodingException
	 */
	public static <T> String sign(T obj, long maxAge,String userName,String userPass) throws Exception {
		JWTCreator.Builder bulider = JWT.create(); // header-头部信息
		bulider.withHeader(createHead()).withSubject(mapper.writeValueAsString(obj)); // payload-载荷
		bulider.withClaim("userName",userName);
		bulider.withClaim("userPwd",userPass);
		if (maxAge >= 0) {
			long expMillis = System.currentTimeMillis() + maxAge;
			Date exp = new Date(expMillis);
			bulider.withExpiresAt(exp);
		}
		return bulider.sign(Algorithm.HMAC256(SECRET));
	}

	/**
	 * 解密(获取token的载荷信息)
	 * 
	 * @param token
	 *            token字符串
	 * @param classT
	 *            返回值的类型
	 * @return 返回的是未加密前的token
	 * @throws IOException 
	 */
	public static <T> T unsign(String token, Class<T> classT) throws IOException{
		DecodedJWT jwt=null;
		try {
			jwt = getJwt(token);
		} catch (TokenExpiredException e) {
			return null;
		}catch(JWTDecodeException e){
			return null;
		}catch(Exception e){
			return null;
		}
		Date exp = jwt.getExpiresAt();
		if (exp != null && exp.after(new Date())) {
			String subject = jwt.getSubject();
			return mapper.readValue(subject, classT);
		}
		return null;
	}

	/**
	 * 解析token所包含的标题数据
	 * @param token
	 * @return
	 */
	public static User unsign(String token){
		try{
			DecodedJWT jwt=getJwt(token);
			return mapper.readValue(jwt.getSubject(),User.class);
		}catch(TokenExpiredException e){
			DecodedJWT jwt=JWT.decode(token);
			try{
				return mapper.readValue(jwt.getSubject(),User.class);
			}catch(Exception e2){
			}
		}catch(Exception e){
		}
		return null;
	}
	
	/**
	 * 判断用户账户信息是否匹配
	 * @param token
	 * @param user 用户对象
	 * @return
	 */
	public static boolean judgeUserInfo(String token, User user){
		DecodedJWT jwt=null;
		try {
			jwt = getJwt(token);
			String userName=jwt.getClaim("userName").asString();
			String userPwd=jwt.getClaim("userPwd").asString();
			if(userName.equals(user.getUsername()) && userPwd.equals(user.getUserpassword())){
				return true;
			}
		} catch (TokenExpiredException e) {
			return false;
		}catch(JWTDecodeException e){
			return false;
		}catch(Exception e){
			return false;
		}
		return false;
	}

	/**
	 * 判断token是否过期，true为过期，false为不过期
	 * 
	 * @param token
	 * @return
	 * 
	 * 
	 * 
	 * @throws Exception
	 */
	public static boolean isOverdue(String token) {
		DecodedJWT jwt = null;
		try {
			jwt = getJwt(token);
		} catch (Exception e) {
			return true;
		}
		Date exp = jwt.getExpiresAt();
		if (exp == null || new Date().after(exp)) {
			return true;
		}
		return false;
	}



	/**
	 * 获取DecodedJWT对象 解析JWT包含的东西
	 * 
	 * @param token
	 * @return
	 * @throws IllegalArgumentException
	 * @throws UnsupportedEncodingException
	 */
	public static DecodedJWT getJwt(String token) throws IllegalArgumentException, UnsupportedEncodingException {
		JWTVerifier verifier = JWT.require(Algorithm.HMAC256(SECRET)).build();
		DecodedJWT jwt = verifier.verify(token);
		return jwt;
	}

	public static void main(String[] args) {
		/*User user=new User();
		user.setId(1);
		user.setUsername("test");
		user.setUserpassword("test");
		try{
			System.out.println(sign(user,60000,"test","test"));
		}catch(Exception e){
			Throwables.getStackTraceAsString(e);
		}*/
		/*String token="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ7XCJpZFwiOjEsXCJ1c2VybmFtZVwiOlwidGVzdFwiLFwidXNlcnBhc3N3b3JkXCI6XCJ0ZXN0XCJ9IiwidXNlclB3ZCI6InRlc3QiLCJ1c2VyTmFtZSI6InRlc3QiLCJleHAiOjE1NjkwNjA1Mzh9.SDHXFjyCDcpY6hFDfBBt7bhCUFEtZMMQpLz2Fsy8h9M\n";
		System.out.println(isOverdue(token));*/
		String token="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ7XCJpZFwiOjEsXCJ1c2VybmFtZVwiOlwidGVzdFwiLFwidXNlcnBhc3N3b3JkXCI6XCJ0ZXN0XCJ9IiwidXNlclB3ZCI6InRlc3QiLCJ1c2VyTmFtZSI6InRlc3QiLCJleHAiOjE1NjkwNjA4MzV9.gbOItVWtzFC1eC9IKkmhzZ3NR0N1tsY8VtF_Xi7Ouc4\n";
		try{
			User user=unsign(token,User.class);
			System.out.println(user);
		}catch(Exception e){
			Throwables.getStackTraceAsString(e);
		}
	}
}
