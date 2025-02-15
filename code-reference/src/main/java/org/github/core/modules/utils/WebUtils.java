package org.github.core.modules.utils;

import org.apache.commons.lang3.StringUtils;

import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;

/**
 * 所有应用方面的工具类
 *
 * @author zhanglei
 *
 */
public class WebUtils extends org.springframework.web.util.WebUtils {
	/**
	 * 取得应用部署的绝对路径
	 *
	 * @param path
	 *            为空的时取得的是ROOT目录 如果传入路径则取得ROOT+path路径
	 * @return 返回绝对路径 末尾包含/
	 *
	 * @return String [返回类型说明]
	 * @exception throws [违例类型] [违例说明]
	 * @see [类、类#方法、类#成员]
	 */
	public static String getRootPath(String path) {
		path = StringUtils.trimToEmpty(path);
		String filePath = WebUtils.class.getResource("").getPath().toString();
		filePath = filePath.replace("/WEB-INF/classes/org/github/core/modules/utils/", "");
		String p = "";
		if(path.startsWith("/")){
			p = filePath + "" + path;
		}else{
			p = filePath + "/" + path;
		}
		p = p.replace("\\", "/");
		if (!p.endsWith("/")) {
			p = p + "/";
		}
		return p;
	}
	/**
	 * 相对路径文件，转为绝对路径文件  root目录下的
	 *
	 * @return
	 */
	public static String getFileRootPath(String path) {
		path = StringUtils.trimToEmpty(path);
		String filePath = WebUtils.class.getResource("").getPath().toString();
		filePath = filePath.replace("/WEB-INF/classes/org/github/core/modules/utils/", "");
		String p = "";
		if(path.startsWith("/")){
			p = filePath + "" + path;
		}else{
			p = filePath + "/" + path;
		}
		return p;
	}
	/**
	 * 相对路径文件，转为绝对路径文件  class目录下的
	 *
	 * @return
	 */
	public static String getAbsoluteFile(String path) {
		path = StringUtils.trimToEmpty(path);
		String filePath = WebUtils.class.getResource("").getPath().toString();
		filePath = filePath.replace("/org/github/core/modules/utils/", "");
		String p = "";
		if(path.startsWith("/")){
			p = filePath + "" + path;
		}else{
			p = filePath + "/" + path;
		}
		return p;
	}

	/**
	 * 或得request中所有参数内容
	 *
	 * @param request
	 * @return
	 */
	public static String getRequest(HttpServletRequest request) {
		StringBuffer sb = new StringBuffer("来源IP" + WebUtils.getRemoteIp(request) + "请求内容{");
		boolean iffirst = true;
		Enumeration<?> names = request.getParameterNames();
		while (names != null && names.hasMoreElements()) {
			String name = (String) names.nextElement();
			String v[] = request.getParameterValues(name);
			String value = "";
			if (v != null && v.length > 0) {
				value = StringUtils.join(v, "  ");
			}
			if (!iffirst) {
				sb.append(",");
			}
			iffirst = false;
			sb.append(name + "=" + value);
		}
		sb.append("}");
		return sb.toString();
	}

	/**
	 * 从 request 中获得字符串流
	 *
	 * @param request
	 * @param charsetName
	 * @return
	 * @throws IOException
	 */
	public static String readStream2Str(HttpServletRequest request, String charsetName) throws IOException {
		ServletInputStream sis = null;
		BufferedReader br = null;
		StringBuffer str = new StringBuffer();
		try {
			sis = (ServletInputStream) request.getInputStream();
			br = new BufferedReader(new InputStreamReader(sis, charsetName));
			String line = null;
			while ((line = br.readLine()) != null) {
				str.append(line);
			}
		} finally {
			if (br != null) {
				br.close();
			}
		}
		return str.toString();
	}

	/**
	 * 或得独立请求参数
	 *
	 * @param param
	 * @param name
	 * @return
	 */
	public static String getParam(Map<String, String[]> param, String name) {
		String[] values = null;
		Set<String> keys = param.keySet();
		for (String key : keys) {
			if (name.equalsIgnoreCase(key)) {
				values = param.get(name);
				break;
			}
		}
		if (values == null) {
			return "";
		}
		if (values.length > 0) {
			return values[0];
		} else {
			return "";
		}
	}

	/**
	 * 获取客户端IP
	 *
	 * @return IP
	 */
	public static String getRemoteIp(HttpServletRequest request) {
		String ip = "";
		if (request != null) {
			ip = request.getHeader("x-forwarded-for");
			if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
				ip = request.getHeader("Proxy-Client-IP");
			}
			if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
				ip = request.getHeader("WL-Proxy-Client-IP");
			}
			if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
				ip = request.getRemoteAddr();
			}
		}
		return ip;
	}
	/**
	 * 设置cookies
	 * 不设置时间，只要关闭游览器就失效了
	 * @param name
	 *            名称
	 * @param value
	 *            值
	 */
	public static void addCookie(HttpServletResponse response, String name, String value) {
		addCookie(response,name,value,"/");
	}
	/**
	 * 设置cookies
	 * 不设置时间，只要关闭游览器就失效了
	 * @param name
	 *            名称
	 * @param value
	 *            值
	 */
	private static void addCookie(HttpServletResponse response, String name, String value,String path) {
		try {
			name = URLEncoder.encode(name, "UTF-8");
			value = URLEncoder.encode(value, "UTF-8");
		} catch (Exception e) {
			e.printStackTrace();
		}
		Cookie cookie = new Cookie(name, value);
		cookie.setPath(path);
		response.addCookie(cookie);
	}
	/**
	 * 设置cookies
	 *
	 * @param name
	 *            名称
	 * @param value
	 *            值
	 * @param minutes
	 *            时间  -1 会话级cookie，关闭浏览器失效   0 不记录cookie
	 */
	public static void addCookie(HttpServletResponse response, String name, String value, int minutes) {
		addCookie(response, name, value, minutes, "/");
	}

	/**
	 *
	 * @param response
	 * @param name
	 * @param value
	 * @param minutes -1 会话级cookie，关闭浏览器失效   0 不记录cookie
	 * @param path
	 */
	private static void addCookie(HttpServletResponse response, String name, String value, int minutes,String path) {
		try {
			name = URLEncoder.encode(name, "UTF-8");
			value = URLEncoder.encode(value, "UTF-8");
		} catch (Exception e) {
			e.printStackTrace();
		}
		Cookie cookie = new Cookie(name, value);
		cookie.setMaxAge(minutes*60);// 60 * 60 * 24 这是一天
		cookie.setPath(path);
		response.addCookie(cookie);
	}
	public static String getCookieValue(HttpServletRequest request, String name){
		Cookie cookie = getCookie(request,name);
		if (cookie != null) {
			return cookie.getValue();
		}
		return "";
	}

	public static Cookie getCookie(HttpServletRequest request, String name){
		if (name == null) {
			return null;
		}
		Cookie[] cookies = request.getCookies();
		if (cookies != null) {
			for (int i = 0; i < cookies.length; i++) {
				if (cookies[i] != null) {
					String value = cookies[i].getName();
					try {
						value = URLDecoder.decode(value,"UTF-8");
					} catch (Exception e) {
						e.printStackTrace();
					}
					// 查找用户名
					if (name.equals(value)) {
						return cookies[i];
					}
				}
			}
		}
		return null;
	}


	/**
	 * 清除cookies
	 *
	 * @param name
	 *            名称
	 */
	public static void clearCookie(HttpServletRequest request, HttpServletResponse response, String name) {
		clearCookie(request,response,name,"/");
	}
	/**
	 * 清除cookies
	 *
	 * @param name
	 *            名称
	 */
	public static void clearCookie(HttpServletRequest request, HttpServletResponse response, String name,String path) {
		Cookie cookie = getCookie(request,name);
		if (cookie != null) {
			cookie.setMaxAge(0);
			cookie.setPath(path);
			response.addCookie(cookie);
		}
	}
	/**
	 * 或得系统当天内存使用情况的信息
	 *
	 * @return
	 */
	public static String getMemory() {
		String info = "\r\nfreeMemory=" + Runtime.getRuntime().freeMemory() / (1024 * 1024) + "M,";
		info += "totalMemory=" + Runtime.getRuntime().totalMemory() / (1024 * 1024) + "M,";
		info += "maxMemory=" + Runtime.getRuntime().maxMemory() / (1024 * 1024) + "M";
		return info;
	}

	public static void main(String[] args) {
		//System.out.println(getAbsoluteFile("b2bpayfile/B2B_AIREPAY_WTG000000002.pfx"));
	}
}
