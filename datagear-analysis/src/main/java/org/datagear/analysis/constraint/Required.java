/*
 * Copyright (c) 2018 datagear.tech. All Rights Reserved.
 */

/**
 * 
 */
package org.datagear.analysis.constraint;

/**
 * 约束-必填。
 * 
 * @author datagear@163.com
 *
 */
public class Required extends AbstractValueConstraint<Boolean>
{
	public Required()
	{
		super();
	}

	public Required(boolean value)
	{
		super(value);
	}
}
