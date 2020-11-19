/*
 * Copyright (c) 2018 datagear.tech. All Rights Reserved.
 */

/**
 * 
 */
package org.datagear.management.impl;

import static org.junit.Assert.assertEquals;

import org.datagear.management.domain.User;
import org.datagear.management.service.impl.UserServiceImpl;
import org.junit.Test;

/**
 * {@linkplain UserServiceImpl}单元测试类。
 * 
 * @author datagear@163.com
 *
 */
public class UserServiceImplTest extends ServiceImplTestSupport
{
	private UserServiceImpl userServiceImpl;

	public UserServiceImplTest()
	{
		super();
		this.userServiceImpl = new UserServiceImpl(getSqlSessionFactory());
	}

	@Test
	public void test()
	{
		String id = "id-for-test";
		String name = "name-for-test";

		try
		{
			this.userServiceImpl.add(new User(id, name, "psd"));

			User user = this.userServiceImpl.getById(id);

			assertEquals(id, user.getId());
			assertEquals(name, user.getName());
		}
		finally
		{
			this.userServiceImpl.deleteById(id);
		}
	}
}
