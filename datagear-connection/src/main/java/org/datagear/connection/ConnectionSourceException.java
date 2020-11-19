/*
 * Copyright 2018 datagear.tech. All Rights Reserved.
 */

package org.datagear.connection;

/**
 * {@linkplain ConnectionSource}异常。
 * 
 * @author datagear@163.com
 *
 */
public class ConnectionSourceException extends RuntimeException
{
	private static final long serialVersionUID = 1L;

	public ConnectionSourceException()
	{
		super();
	}

	public ConnectionSourceException(String message)
	{
		super(message);
	}

	public ConnectionSourceException(Throwable cause)
	{
		super(cause);
	}

	public ConnectionSourceException(String message, Throwable cause)
	{
		super(message, cause);
	}
}
