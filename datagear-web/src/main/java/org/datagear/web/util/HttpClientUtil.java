package org.datagear.web.util;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * 〈一句话功能简述〉<br>
 * 〈〉
 *
 * @author gwl
 * @date 2020/10/20 9:53
 * @since 1.0.0
 */

public class HttpClientUtil {

    private static final Logger log = LoggerFactory.getLogger(HttpClientUtil.class);

    public static String url = "http://192.168.0.212:8084/current/userInfo";

    public static String encoding = "UTF-8";

    public static String httpGet(String authorization) throws Exception {


        log.debug("收到HTTP GET请求");

        String result = "";


        CloseableHttpClient httpclient = HttpClients.createDefault();

        try {
            // 创建httpget.
            HttpGet httpget = new HttpGet(url);
            httpget.addHeader("Authorization",authorization);
            // 执行get请求.
            CloseableHttpResponse response = httpclient.execute(httpget);
            try {

                log.debug("--------------------------------------"); // 获取响应实体
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    result = EntityUtils.toString(entity, encoding);
                    // 打印响应内容
                    log.debug("Response content: " + result);
                }
                log.debug("------------------------------------");
            } finally {
                response.close();
            }
        } catch (Exception e) {
            throw e;
        } finally {
            // 关闭连接,释放资源
            try {
                httpclient.close();
            } catch (IOException e) {

            }
        }
        return result;
    }
}
