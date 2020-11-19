/*
 * Copyright 2018 datagear.tech. All Rights Reserved.
 */

package org.datagear.web.security;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.datagear.web.util.HttpClientUtil;
import org.junit.Assert;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * {@linkplain SmtpSettingPasswordEncryptorImpl}单元测试类。
 *
 * @author datagear@163.com
 */
public class SmtpSettingPasswordEncryptorImplTest {
    private SmtpSettingPasswordEncryptorImpl smtpSettingPasswordEncryptorImpl = new SmtpSettingPasswordEncryptorImpl();

    @Test
    public void test() {
        String password = "i am a password";

        String encryptedPassword = smtpSettingPasswordEncryptorImpl.encrypt(password);

        String decryptedPassword = smtpSettingPasswordEncryptorImpl.decrypt(encryptedPassword);

        Assert.assertEquals(password, decryptedPassword);
    }

    @Test
    public void test1() throws Exception {
        HttpClientUtil.httpGet("Bearer 04c06872-c4f1-41a6-84aa-36842a504798");
    }

/*    @Test
    public void test2() {
        User data = HttpUtil.doGet("Bearer 04c06872-c4f1-41a6-84aa-36842a504798");
        System.out.println(data);
    }*/

    @Test
    public void test3() throws Exception{
        String result="";
        HttpPost htGet = new HttpPost("http://localhost:9999/datagear_webapp_war_exploded/login/doLogin?name=maruko&password=123456&autoLogin=1");
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        CloseableHttpResponse response = httpClient.execute(htGet);
        HttpEntity responseEntity = response.getEntity();
        BufferedReader in = new BufferedReader(new InputStreamReader(responseEntity.getContent(), "UTF-8"));
        String line;
        while ((line = in.readLine()) != null) {
            result += line;
        }
        System.out.println(result);

    }

}
