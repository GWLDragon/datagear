/*
 * Copyright 2018 datagear.tech. All Rights Reserved.
 */

package org.datagear.web.security;

import org.datagear.management.domain.User;
import org.datagear.management.service.UserService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCrypt;

/**
 * {@linkplain UserDetailsService}实现类。
 * 
 * @author datagear@163.com
 *
 */
public class UserDetailsServiceImpl implements UserDetailsService
{
	private UserService userService;

	public UserDetailsServiceImpl()
	{
		super();
	}

	public UserDetailsServiceImpl(UserService userService)
	{
		super();
		this.userService = userService;
	}

	public UserService getUserService()
	{
		return userService;
	}

	public void setUserService(UserService userService)
	{
		this.userService = userService;
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException
	{
		User user = this.userService.getByName(username);

		if (user == null)
			throw new UsernameNotFoundException("user name [" + username + "] not found");

		AuthUser authUser = new AuthUser(user);
	/*	boolean checkpw1 = BCrypt.checkpw("Aa123456", "2af9531a2711bf83ce44740916d1140c1ef0919c244827279440a111321b85e47139e85d83a9b4c8");
		boolean checkpw2 = BCrypt.checkpw("Aa123456", "$2a$10$vnWWtW.uMrS/XKUVY1TKxOubUe6sFusCLLagQrB59WLrUN/ru41ZG");
		boolean checkpw3 = BCrypt.checkpw("2af9531a2711bf83ce44740916d1140c1ef0919c244827279440a111321b85e47139e85d83a9b4c8", "$2a$10$vnWWtW.uMrS/XKUVY1TKxOubUe6sFusCLLagQrB59WLrUN/ru41ZG");*/
		return authUser;
	}
}
