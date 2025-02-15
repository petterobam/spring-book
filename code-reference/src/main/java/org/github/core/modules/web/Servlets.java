/*******************************************************************************
 * Copyright (c) 2005, 2014 springside.github.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 *******************************************************************************/
package org.github.core.modules.web;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.Validate;
import org.github.core.modules.utils.Collections3;
import org.github.core.modules.utils.Encodes;
import org.github.core.modules.utils.MD5Tool;
import org.springframework.web.servlet.mvc.multiaction.InternalPathMethodNameResolver;
import org.springframework.web.servlet.mvc.multiaction.MethodNameResolver;
import org.springframework.web.servlet.mvc.multiaction.NoSuchRequestHandlingMethodException;

import com.google.common.base.Charsets;
import com.google.common.net.HttpHeaders;

/**
 * Http与Servlet工具类.
 * 
 * @author calvin
 */
public class Servlets {

	// -- 常用数值定义 --//
	public static final long ONE_YEAR_SECONDS = 60 * 60 * 24 * 365;

	/**
	 * 设置客户端缓存过期时间 的Header.
	 */
	public static void setExpiresHeader(HttpServletResponse response, long expiresSeconds) {
		// Http 1.0 header, set a fix expires date.
		response.setDateHeader(HttpHeaders.EXPIRES, System.currentTimeMillis() + (expiresSeconds * 1000));
		// Http 1.1 header, set a time after now.
		response.setHeader(HttpHeaders.CACHE_CONTROL, "private, max-age=" + expiresSeconds);
	}

	/**
	 * 设置禁止客户端缓存的Header.
	 */
	public static void setNoCacheHeader(HttpServletResponse response) {
		// Http 1.0 header
		response.setDateHeader(HttpHeaders.EXPIRES, 1L);
		response.addHeader(HttpHeaders.PRAGMA, "no-cache");
		// Http 1.1 header
		response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, max-age=0");
	}

	/**
	 * 设置LastModified Header.
	 */
	public static void setLastModifiedHeader(HttpServletResponse response, long lastModifiedDate) {
		response.setDateHeader(HttpHeaders.LAST_MODIFIED, lastModifiedDate);
	}

	/**
	 * 设置Etag Header.
	 */
	public static void setEtag(HttpServletResponse response, String etag) {
		response.setHeader(HttpHeaders.ETAG, etag);
	}

	/**
	 * 根据浏览器If-Modified-Since Header, 计算文件是否已被修改.
	 * 
	 * 如果无修改, checkIfModify返回false ,设置304 not modify status.
	 * 
	 * @param lastModified 内容的最后修改时间.
	 */
	public static boolean checkIfModifiedSince(HttpServletRequest request, HttpServletResponse response,
			long lastModified) {
		long ifModifiedSince = request.getDateHeader(HttpHeaders.IF_MODIFIED_SINCE);
		if ((ifModifiedSince != -1) && (lastModified < (ifModifiedSince + 1000))) {
			response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
			return false;
		}
		return true;
	}

	/**
	 * 根据浏览器 If-None-Match Header, 计算Etag是否已无效.
	 * 
	 * 如果Etag有效, checkIfNoneMatch返回false, 设置304 not modify status.
	 * 
	 * @param etag 内容的ETag.
	 */
	public static boolean checkIfNoneMatchEtag(HttpServletRequest request, HttpServletResponse response, String etag) {
		String headerValue = request.getHeader(HttpHeaders.IF_NONE_MATCH);
		if (headerValue != null) {
			boolean conditionSatisfied = false;
			if (!"*".equals(headerValue)) {
				StringTokenizer commaTokenizer = new StringTokenizer(headerValue, ",");

				while (!conditionSatisfied && commaTokenizer.hasMoreTokens()) {
					String currentToken = commaTokenizer.nextToken();
					if (currentToken.trim().equals(etag)) {
						conditionSatisfied = true;
					}
				}
			} else {
				conditionSatisfied = true;
			}

			if (conditionSatisfied) {
				response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
				response.setHeader(HttpHeaders.ETAG, etag);
				return false;
			}
		}
		return true;
	}

	/**
	 * 设置让浏览器弹出下载对话框的Header.
	 * 
	 * @param fileName 下载后的文件名.
	 */
	public static void setFileDownloadHeader(HttpServletResponse response, String fileName) {
		// 中文文件名支持
		String encodedfileName = new String(fileName.getBytes(), Charsets.ISO_8859_1);
		response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedfileName + "\"");

	}
	public static Map<String, Object> getParametersStartingWith(HttpServletRequest request, String prefix) {
		return getParametersStartingWith(request, prefix,false);
	}
	public static String getParametersStr(HttpServletRequest request){
		Enumeration paramNames = request.getParameterNames();
		StringBuffer sb = new StringBuffer();
		int index = 0;
		while ((paramNames != null) && paramNames.hasMoreElements()) {
			String paramName = (String) paramNames.nextElement();
			String[] values = request.getParameterValues(paramName);
			
			if(values!=null && values.length>=1){
				for(int i=0;i<values.length;i++){
					
					if(index==0){
						sb.append(paramName).append("=").append(values[i]);
					}else{
						sb.append("&").append(paramName).append("=").append(values[i]);
					}
					index++;
				}
			}else{
				if(index==0){
					sb.append(paramName).append("=");
				}else{
					sb.append("&").append(paramName).append("=");
				}
				index++;
			}
		}
		return sb.toString();
	}
	public static Map<String, Object> getParametersStartingWith(Map request, String prefix,boolean validView) {
		Validate.notNull(request, "Request must not be null");
		Set<String> set = request.keySet();
		Iterator<String> paramNames = set.iterator();
		Map<String, Object> params = new TreeMap<String, Object>();
		//Map<String, Object> searchparams = new TreeMap<String, Object>();
		if (prefix == null) {
			prefix = "";
		}
		TreeSet viewParam = new TreeSet();
		while ((paramNames != null) && paramNames.hasNext()) {
			String paramName = (String) paramNames.next();
			if ("".equals(prefix) || paramName.startsWith(prefix)) {
				String unprefixed =paramName;
				Object values = request.get(paramName);
				params.put(unprefixed, values);
				if(validView){
					viewParam.add(paramName);
				}
			}
		}
		if(validView && !viewParam.isEmpty()){
			String tmp = viewParam.toString()+"_VIEW";
			
			String md5 = MD5Tool.MD5Encode(tmp, "UTF-8");
			
			String VIEW = (String)request.get("VIEW");
			//System.out.println("测试=="+tmp+"=md5="+md5+"=view="+VIEW);
			if(!md5.toUpperCase().equalsIgnoreCase(VIEW)){
				throw new RuntimeException("VIEW验证错误");
			}
		}
		params.put("orderBy", request.get("orderBy"));
		return params;
	}
	/**
	 * 取得带相同前缀的Request Parameters, copy from spring WebUtils.
	 * 
	 * 返回的结果的Parameter名已去除前缀.
	 */
	public static Map<String, Object> getParametersStartingWith(HttpServletRequest request, String prefix,boolean validView) {
		Validate.notNull(request, "Request must not be null");
		Enumeration paramNames = request.getParameterNames();
		Map<String, Object> params = new TreeMap<String, Object>();
		Map<String, Object> searchparams = new TreeMap<String, Object>();
		if (prefix == null) {
			prefix = "";
		}
		TreeSet viewParam = new TreeSet();
		while ((paramNames != null) && paramNames.hasMoreElements()) {
			String paramName = (String) paramNames.nextElement();
			if ("".equals(prefix) || paramName.startsWith(prefix)) {
				String unprefixed =paramName;
				String[] values = request.getParameterValues(paramName);
				if ((values == null) || (values.length == 0)) {
					// Do nothing, no values found at all.
				} else if (values.length > 1) {
					params.put(unprefixed, values);
					searchparams.put(paramName, values);
					if(validView){
						viewParam.add(paramName);
					}
				} else {
					params.put(unprefixed, values[0]);
					searchparams.put(paramName, values[0]);
					if(validView){
						viewParam.add(paramName);
					}
				}
			}
		}
		if(validView){
			String tmp = viewParam.toString()+"_VIEW";
			
			String md5 = MD5Tool.MD5Encode(tmp, "UTF-8");
			
			String VIEW = request.getParameter("VIEW");
			//System.out.println("测试=="+tmp+"=md5="+md5+"=view="+VIEW);
			if(!md5.toUpperCase().equalsIgnoreCase(VIEW)){
				throw new RuntimeException("VIEW验证错误");
			}
		}
		String page = request.getParameter("pageNum");
		String pageSzie = request.getParameter("pageSize");
		searchparams.put("pageNum", page);
		searchparams.put("pageSize", pageSzie);
		params.put("orderBy",request.getParameter("orderBy"));
		HttpSession httpSession = request.getSession();
		httpSession.setAttribute("SEARCH_PARAM", searchparams);
		return params;
	}

	/**
	 * 组合Parameters生成Query String的Parameter部分, 并在paramter name上加上prefix.
	 * 
	 * @see #getParametersStartingWith
	 */
	public static String encodeParameterStringWithPrefix(Map<String, Object> params, String prefix) {
		if (Collections3.isEmpty(params)) {
			return "";
		}

		if (prefix == null) {
			prefix = "";
		}

		StringBuilder queryStringBuilder = new StringBuilder();
		Iterator<Entry<String, Object>> it = params.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, Object> entry = it.next();
			queryStringBuilder.append(prefix).append(entry.getKey()).append('=').append(entry.getValue());
			if (it.hasNext()) {
				queryStringBuilder.append('&');
			}
		}
		return queryStringBuilder.toString();
	}

	/**
	 * 客户端对Http Basic验证的 Header进行编码.
	 */
	public static String encodeHttpBasic(String userName, String password) {
		String encode = userName + ":" + password;
		return "Basic " + Encodes.encodeBase64(encode.getBytes());
	}
	/**
	 * 获取请求路径
	 * @param request
	 * @return [参数说明]
	 * 
	 * @return String [返回类型说明]
	 */
	public static String getRequestUriWithoutParm(HttpServletRequest request){
		String requestUrl=request.getRequestURI();
		if(StringUtils.isBlank(requestUrl)){
			return null;
		}
		if(requestUrl.lastIndexOf("/view")>-1){
			return requestUrl;
		}
		MethodNameResolver methodResolver=new InternalPathMethodNameResolver();
		String methodName;
		try {
			methodName = methodResolver.getHandlerMethodName(request);
		} catch (NoSuchRequestHandlingMethodException e) {
			return null;
		}
		methodName=StringUtils.substringBeforeLast(methodName, "_");
		return StringUtils.substringBeforeLast(requestUrl,"/"+methodName)+"/"+methodName;
	}
	/**
	 * 获取request映射的方法名
	 * @param request
	 * @return String [返回类型说明]
	 */
	public static String getMethodNameByRequest(HttpServletRequest request){
		MethodNameResolver methodResolver=new InternalPathMethodNameResolver();
		String methodName;
		try {
			methodName = methodResolver.getHandlerMethodName(request);
		} catch (NoSuchRequestHandlingMethodException e) {
			return null;
		}
		return StringUtils.substringBeforeLast(methodName, "_");
	}
}
