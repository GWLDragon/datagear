/*
 * Copyright 2018 datagear.tech. All Rights Reserved.
 */

package org.datagear.connection;

import java.io.Serializable;
import java.sql.Connection;
import java.util.Properties;

import org.datagear.util.JdbcUtil;
import org.datagear.util.StringUtil;

/**
 * JDBC连接选项。
 * 
 * @author datagear@163.com
 *
 */
public class ConnectionOption implements Serializable
{
	private static final long serialVersionUID = 1L;

	public static final String PROPERTY_NAME_USER = "user";

	public static final String PROPERTY_NAME_PASSWORD = "password";

	private String url;

	private Properties properties = new Properties();

	public ConnectionOption()
	{
		super();
	}

	public ConnectionOption(String url)
	{
		super();
		this.url = url;
	}

	public ConnectionOption(String url, String user)
	{
		super();
		this.url = url;
		this.properties.put(PROPERTY_NAME_USER, user);
	}

	public ConnectionOption(String url, String user, String password)
	{
		super();
		this.url = url;
		this.properties.put(PROPERTY_NAME_USER, user);
		this.properties.put(PROPERTY_NAME_PASSWORD, password);
	}

	public String getUrl()
	{
		return url;
	}

	public void setUrl(String url)
	{
		this.url = url;
	}

	public Properties getProperties()
	{
		return properties;
	}

	public void setProperties(Properties properties)
	{
		this.properties = properties;
	}

	public String getUser()
	{
		return this.properties.getProperty(PROPERTY_NAME_USER);
	}

	public void setUser(String user)
	{
		this.properties.put(PROPERTY_NAME_USER, user);
	}

	public String getPassword()
	{
		return this.properties.getProperty(PROPERTY_NAME_PASSWORD);
	}

	public void setPassword(String password)
	{
		this.properties.put(PROPERTY_NAME_PASSWORD, password);
	}

	@SuppressWarnings("unchecked")
	public <T> T getProperty(String name)
	{
		return (T) this.properties.getProperty(name);
	}

	public void setProperty(String name, Object value)
	{
		this.properties.put(name, value);
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((properties == null) ? 0 : properties.hashCode());
		result = prime * result + ((url == null) ? 0 : url.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ConnectionOption other = (ConnectionOption) obj;
		if (properties == null)
		{
			if (other.properties != null)
				return false;
		}
		else if (!properties.equals(other.properties))
			return false;
		if (url == null)
		{
			if (other.url != null)
				return false;
		}
		else if (!url.equals(other.url))
			return false;
		return true;
	}

	@Override
	public String toString()
	{
		return getClass().getSimpleName() + " [url=" + url + ", properties=" + properties + "]";
	}

	/**
	 * 构建{@linkplain ConnectionOption}实例。
	 * 
	 * @return
	 */
	public static ConnectionOption valueOf()
	{
		return new ConnectionOption();
	}

	/**
	 * 构建{@linkplain ConnectionOption}实例。
	 * 
	 * @param url
	 * @return
	 */
	public static ConnectionOption valueOf(String url)
	{
		return new ConnectionOption(url);
	}

	/**
	 * 构建{@linkplain ConnectionOption}实例。
	 * 
	 * @param url
	 * @param user
	 * @return
	 */
	public static ConnectionOption valueOf(String url, String user)
	{
		return new ConnectionOption(url, user);
	}

	/**
	 * 构建{@linkplain ConnectionOption}实例。
	 * 
	 * @param url
	 * @param user
	 * @param password
	 * @return
	 */
	public static ConnectionOption valueOf(String url, String user, String password)
	{
		return new ConnectionOption(url, user, password);
	}

	/**
	 * 构建{@linkplain ConnectionOption}实例。
	 * 
	 * @param cn
	 * @return 返回{@code null}表示无法构建
	 */
	public static ConnectionOption valueOf(Connection cn)
	{
		String url = JdbcUtil.getURLIfSupports(cn);
		String username = JdbcUtil.getUserNameIfSupports(cn);

		if (StringUtil.isEmpty(url))
			return null;

		if (StringUtil.isEmpty(username))
			username = "";

		return new ConnectionOption(url, username);
	}

	/**
	 * 构建{@linkplain ConnectionOption}实例。
	 * <p>
	 * 如果无法构建，此方法将返回一个所有属性都为空的实例。
	 * </p>
	 * 
	 * @param cn
	 * @return
	 */
	public static ConnectionOption valueOfNonNull(Connection cn)
	{
		ConnectionOption connectionOption = valueOf(cn);

		return (connectionOption == null ?
				new ConnectionOption("") : connectionOption);
	}
}
