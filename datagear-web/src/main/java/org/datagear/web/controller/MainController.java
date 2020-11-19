/*
 * Copyright 2018 datagear.tech. All Rights Reserved.
 */

package org.datagear.web.controller;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.datagear.management.domain.User;
import org.datagear.util.Global;
import org.datagear.util.version.Version;
import org.datagear.util.version.VersionContent;
import org.datagear.web.util.ChangelogResolver;
import org.datagear.web.util.WebUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 主页控制器。
 *
 * @author datagear@163.com
 */
@Controller
public class MainController extends AbstractController {
    public static final String LATEST_VERSION_SCRIPT_LOCATION = Global.WEB_SITE + "/latest-version.js";

    public static final String COOKIE_DETECT_NEW_VERSION_RESOLVED = "DETECT_NEW_VERSION_RESOLVED";

    @Value("${disableRegister}")
    private boolean disableRegister = false;

    @Autowired
    private ChangelogResolver changelogResolver;

    @Autowired
    private LoginController loginController;

    @Value("${disableDetectNewVersion}")
    private boolean disableDetectNewVersion;

    public MainController() {
        super();
    }

    public boolean isDisableRegister() {
        return disableRegister;
    }

    public void setDisableRegister(boolean disableRegister) {
        this.disableRegister = disableRegister;
    }

    public boolean isDisableDetectNewVersion() {
        return disableDetectNewVersion;
    }

    public void setDisableDetectNewVersion(boolean disableDetectNewVersion) {
        this.disableDetectNewVersion = disableDetectNewVersion;
    }

    public ChangelogResolver getChangelogResolver() {
        return changelogResolver;
    }

    public void setChangelogResolver(ChangelogResolver changelogResolver) {
        this.changelogResolver = changelogResolver;
    }

    /**
     * 打开主页面。
     *
     * @param request
     * @param model
     * @return
     */
    @RequestMapping({"", "/", "/index.html"})
    public String main(HttpServletRequest request, HttpServletResponse response, Model model) {
        try {

            HttpClient httpClient = new DefaultHttpClient();
            //2、创建一个HttpPost请求
            HttpPost response1 = new HttpPost("http://localhost:9999/datagear_webapp_war_exploded/login/doLogin");

            //3、设置参数
            //建立一个NameValuePair数组，用于存储欲传送的参数
            List<NameValuePair> params = new ArrayList<NameValuePair>();

            //添加参数
            params.add(new BasicNameValuePair("name", "maruko"));
            params.add(new BasicNameValuePair("password", "123456"));
            params.add(new BasicNameValuePair("autoLogin", "1"));

            //4、设置参数到请求对象中
            try {
                response1.setEntity(new UrlEncodedFormEntity(params, "utf-8"));
            } catch (UnsupportedEncodingException e1) {
                e1.printStackTrace();
            }  //5、设置header信息
            response1.setHeader("Content-type", "application/x-www-form-urlencoded");
            response1.setHeader("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)");
            //添加cookie到头文件

            //6、设置编码
            //response.setEntity(new UrlEncodedFormEntity(params,HTTP.UTF_8));
            //7、执行post请求操作，并拿到结果（同步阻塞）
            CloseableHttpResponse httpResponse;

            httpResponse = (CloseableHttpResponse) httpClient.execute(response1);
            //获取结果实体
            HttpEntity entity = httpResponse.getEntity();
            System.err.println(entity);

        } catch (Exception e) {

        }

        request.setAttribute("disableRegister", this.disableRegister);
        request.setAttribute("currentUser", User.copyWithoutPassword(WebUtils.getUser(request, response)));
        request.setAttribute("currentVersion", Global.VERSION);
        resolveDetectNewVersionScript(request, response);

        return "/main";
    }

    @RequestMapping("/about")
    public String about(HttpServletRequest request) {
        request.setAttribute("version", Global.VERSION);

        return "/about";
    }

    @RequestMapping("/changelog")
    public String changelog(HttpServletRequest request) throws IOException {
        Version version = null;

        try {
            version = Version.valueOf(Global.VERSION);
        } catch (IllegalArgumentException e) {
        }

        List<VersionContent> versionChangelogs = new ArrayList<>();

        if (version != null) {
            VersionContent versionChangelog = this.changelogResolver.resolveChangelog(version);
            versionChangelogs.add(versionChangelog);
        }

        request.setAttribute("versionChangelogs", versionChangelogs);

        return "/changelog";
    }

    @RequestMapping("/changelogs")
    public String changelogs(HttpServletRequest request) throws IOException {
        List<VersionContent> versionChangelogs = this.changelogResolver.resolveAll();

        request.setAttribute("versionChangelogs", versionChangelogs);
        request.setAttribute("allListed", true);

        return "/changelog";
    }

    @RequestMapping(value = "/changeThemeData")
    public String changeThemeData(HttpServletRequest request, HttpServletResponse response) {
        response.setContentType(CONTENT_TYPE_JSON);

        return "/change_theme_data";
    }

    protected void resolveDetectNewVersionScript(HttpServletRequest request, HttpServletResponse response) {
        boolean disable = this.disableDetectNewVersion;
        String script = "";

        if (!disable) {
            String resolved = WebUtils.getCookieValue(request, COOKIE_DETECT_NEW_VERSION_RESOLVED);
            disable = "true".equalsIgnoreCase(resolved);
        }

        if (!disable) {
            script = "<script src=\"" + LATEST_VERSION_SCRIPT_LOCATION + "\" type=\"text/javascript\"></script>";
            // 每12小时检测一次
            WebUtils.setCookie(request, response, COOKIE_DETECT_NEW_VERSION_RESOLVED, "true", 60 * 60 * 12);
        }

        request.setAttribute("detectNewVersionScript", script);
    }
}