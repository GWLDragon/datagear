package org.datagear.web.util;

import net.sf.json.JSONObject;
import org.apache.commons.collections.CollectionUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.datagear.management.domain.User;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.Map;

/**
 * 〈一句话功能简述〉<br>
 * 〈〉
 *
 * @author gwl
 * @date 2020/10/20 10:38
 * @since 1.0.0
 */

public class HttpUtil {

//    public static String url = "http://192.168.2.69:8084/current/userInfo";
public static String url = "http://86.100.13.235:8084/current/userInfo";
//public static String url = "http://86.100.13.234:8084/current/userInfo";
    public static User doGet(String authorization) {
        try {
            String result = "";
            BufferedReader in;
            HttpGet htGet = new HttpGet(url);
            htGet.addHeader("Authorization", authorization);
            CloseableHttpClient httpClient = HttpClientBuilder.create().build();
            CloseableHttpResponse response = httpClient.execute(htGet);
            HttpEntity responseEntity = response.getEntity();
            in = new BufferedReader(new InputStreamReader(responseEntity.getContent(), "UTF-8"));
            String line;
            while ((line = in.readLine()) != null) {
                result += line;
            }

            JSONObject json = JSONObject.fromObject(result);
            Map<String, Object> data = (Map) json.get("data");
            User user = new User();
            if (null == data || data.size() == 0) {
                return user;
            }

            Object id = data.get("id");
            user.setId(id.toString());
            Object name = data.get("username");
            user.setName(name.toString());
            Object password = data.get("password");
            user.setPassword(password.toString());
            Object realName = data.get("nickName");
            user.setRealName(realName.toString());
            Object email = data.get("email");
            user.setEmail(email.toString());
            Object tenantId = data.get("tenantId");
            if (tenantId.equals("-1")) {
                user.setAdmin(true);
            } else {
                user.setAdmin(false);
            }
            user.setAnonymous(false);
            user.setCreateTime(new Date());
            return user;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
