/*
 * Copyright 2018 datagear.tech. All Rights Reserved.
 */

package org.datagear.web.controller;

import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.datagear.analysis.*;
import org.datagear.analysis.support.html.*;
import org.datagear.analysis.support.html.HtmlTplDashboardRenderAttr.WebContext;
import org.datagear.analysis.support.html.HtmlTplDashboardWidgetRenderer.AddPrefixHtmlTitleHandler;
import org.datagear.management.domain.ChartDataSetVO;
import org.datagear.management.domain.HtmlChartWidgetEntity;
import org.datagear.management.domain.RenderChartEntity;
import org.datagear.management.domain.User;
import org.datagear.management.mybatis.SomeMapper;
import org.datagear.management.service.AnalysisProjectService;
import org.datagear.management.service.HtmlChartWidgetEntityService;
import org.datagear.management.service.HtmlChartWidgetEntityService.ChartWidgetSourceContext;
import org.datagear.persistence.Order;
import org.datagear.persistence.PagingData;
import org.datagear.util.IDUtil;
import org.datagear.util.IOUtil;
import org.datagear.web.OperationMessage;
import org.datagear.web.util.WebUtils;
import org.datagear.web.vo.APIDDataFilterPagingQuery;
import org.datagear.web.vo.ShowDashboardVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.context.request.WebRequest;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

/**
 * 图表控制器。
 *
 * @author datagear@163.com
 */
@Controller
@RequestMapping("/analysis/chart")
@Slf4j
public class ChartController extends AbstractChartPluginAwareController implements ServletContextAware {
    static {
        AuthorizationResourceMetas.registerForShare(HtmlChartWidgetEntity.AUTHORIZATION_RESOURCE_TYPE, "chart");
    }

    @Autowired
    private HtmlChartWidgetEntityService htmlChartWidgetEntityService;

    @Autowired
    private AnalysisProjectService analysisProjectService;

    @Autowired
    private ChartPluginManager chartPluginManager;

    @Autowired
    private SomeMapper someMapper;

    //    public static final String url = "http://86.100.13.235:9999";
//    public static final String url = "http://86.100.13.233:9999";
//    public static final String url = "http://127.0.0.1:9999";

    @Autowired
    @Qualifier("chartShowHtmlTplDashboardWidgetHtmlRenderer")
    private HtmlTplDashboardWidgetHtmlRenderer chartShowHtmlTplDashboardWidgetHtmlRenderer;

    private ServletContext servletContext;

    public ChartController() {
        super();
    }

    public HtmlChartWidgetEntityService getHtmlChartWidgetEntityService() {
        return htmlChartWidgetEntityService;
    }

    public void setHtmlChartWidgetEntityService(HtmlChartWidgetEntityService htmlChartWidgetEntityService) {
        this.htmlChartWidgetEntityService = htmlChartWidgetEntityService;
    }

    public AnalysisProjectService getAnalysisProjectService() {
        return analysisProjectService;
    }

    public void setAnalysisProjectService(AnalysisProjectService analysisProjectService) {
        this.analysisProjectService = analysisProjectService;
    }

    public ChartPluginManager getChartPluginManager() {
        return chartPluginManager;
    }

    public void setChartPluginManager(ChartPluginManager chartPluginManager) {
        this.chartPluginManager = chartPluginManager;
    }

    public HtmlTplDashboardWidgetHtmlRenderer getChartShowHtmlTplDashboardWidgetHtmlRenderer() {
        return chartShowHtmlTplDashboardWidgetHtmlRenderer;
    }

    public void setChartShowHtmlTplDashboardWidgetHtmlRenderer(
            HtmlTplDashboardWidgetHtmlRenderer chartShowHtmlTplDashboardWidgetHtmlRenderer) {
        this.chartShowHtmlTplDashboardWidgetHtmlRenderer = chartShowHtmlTplDashboardWidgetHtmlRenderer;
    }

    public ServletContext getServletContext() {
        return servletContext;
    }

    @Override
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    @RequestMapping("/add")
    public String add(HttpServletRequest request, HttpServletResponse response, org.springframework.ui.Model model) {
        HtmlChartWidgetEntity chart = new HtmlChartWidgetEntity();
        setCookieAnalysisProject(request, response, chart);

        model.addAttribute("chart", chart);
        model.addAttribute("chartPluginVO", toWriteJsonTemplateModel(null));
        model.addAttribute(KEY_TITLE_MESSAGE_KEY, "chart.addChart");
        model.addAttribute(KEY_FORM_ACTION, "save");

        return "/analysis/chart/chart_form";
    }

    @RequestMapping("/edit")
    public String edit(HttpServletRequest request, HttpServletResponse response, org.springframework.ui.Model model,
                       @RequestParam("id") String id) {
        //todo 切换用户体系
        User user = WebUtils.getUser(request, response);
//		String authorization = request.getHeader("Authorization");
//		User user = HttpUtil.doGet(authorization);

        HtmlChartWidgetEntity chart = this.htmlChartWidgetEntityService.getByIdForEdit(user, id);

        if (chart == null)
            throw new RecordNotFoundException();

        HtmlChartPluginVO chartPluginVO = (chart.getPlugin() != null
                ? getHtmlChartPluginVO(request, chart.getPlugin().getId())
                : null);

        model.addAttribute("chart", chart);
        model.addAttribute("chartPluginVO", toWriteJsonTemplateModel(chartPluginVO));
        model.addAttribute("chartDataSets", toWriteJsonTemplateModel(toChartDataSetViewObjs(chart.getChartDataSets())));
        model.addAttribute(KEY_TITLE_MESSAGE_KEY, "chart.editChart");
        model.addAttribute(KEY_FORM_ACTION, "save");

        return "/analysis/chart/chart_form";
    }

    @RequestMapping(value = "/save", produces = CONTENT_TYPE_JSON)
    @ResponseBody
    public ResponseEntity<OperationMessage> save(HttpServletRequest request, HttpServletResponse response,
                                                 @RequestBody HtmlChartWidgetEntity entity){

        //todo 切换用户体系
        User user = WebUtils.getUser(request, response);

        trimAnalysisProjectAwareEntityForSave(entity);

        HtmlChartPlugin paramPlugin = entity.getHtmlChartPlugin();

        if (isEmpty(entity.getId())) {
            String chartPluginName = entity.getName();
            int i = someMapper.queryChartNameNum(chartPluginName, user.getId());
            if (i > 0) {
                return buildOperationMessageFailResponseEntity(request,HttpStatus.BAD_REQUEST,"新增失败，存在同名图表","新增失败，存在同名图表");
            }
            entity.setId(IDUtil.randomIdOnTime20());
            entity.setCreateUser(User.copyWithoutPassword(user));
            inflateHtmlChartWidgetEntity(entity, request);

            checkSaveEntity(entity);

            this.htmlChartWidgetEntityService.add(user, entity);

            //将数据保存至数据表中
//            JSONObject jsonObject = JSONObject.fromObject(smallMap);
        } else {
            int i = someMapper.queryChartNameById(entity.getId(), user.getId(), entity.getName());
            if (i > 0) {
                return buildOperationMessageFailResponseEntity(request,HttpStatus.BAD_REQUEST,"编辑失败，存在同名图表","编辑失败，存在同名图表");
            }
            inflateHtmlChartWidgetEntity(entity, request);
            checkSaveEntity(entity);
            this.htmlChartWidgetEntityService.update(user, entity);
        }

        // 返回参数不应该完全加载插件对象
        entity.setHtmlChartPlugin(paramPlugin);
        return buildOperationMessageSaveSuccessResponseEntity(request, entity);
    }

    @RequestMapping("/view")
    public String view(HttpServletRequest request, HttpServletResponse response, org.springframework.ui.Model model,
                       @RequestParam("id") String id) {
        //todo 切换用户体系
        User user = WebUtils.getUser(request, response);
//		String authorization = request.getHeader("Authorization");
//		User user = HttpUtil.doGet(authorization);

        HtmlChartWidgetEntity chart = this.htmlChartWidgetEntityService.getById(user, id);

        if (chart == null)
            throw new RecordNotFoundException();

        HtmlChartPluginVO chartPluginVO = (chart.getPlugin() != null
                ? getHtmlChartPluginVO(request, chart.getPlugin().getId())
                : null);

        model.addAttribute("chart", chart);
        model.addAttribute("chartPluginVO", toWriteJsonTemplateModel(chartPluginVO));
        model.addAttribute("chartDataSets", toWriteJsonTemplateModel(toChartDataSetViewObjs(chart.getChartDataSets())));
        model.addAttribute(KEY_TITLE_MESSAGE_KEY, "chart.viewChart");
        model.addAttribute(KEY_READONLY, true);

        return "/analysis/chart/chart_form";
    }

    @RequestMapping(value = "/delete", produces = CONTENT_TYPE_JSON)
    @ResponseBody
    public ResponseEntity<OperationMessage> delete(HttpServletRequest request, HttpServletResponse response,
                                                   @RequestBody String[] ids) {
        //todo 切换用户体系
        User user = WebUtils.getUser(request, response);
//		String authorization = request.getHeader("Authorization");
//		User user = HttpUtil.doGet(authorization);
        //去查询这些图标是否在智能首页引用 如果引用无法删除
        if (ids.length > 0) {
            Integer num = someMapper.deleteChartById(ids);
            if (num > 0) {
                return buildOperationMessageFailResponseEntity(request, HttpStatus.BAD_REQUEST, "删除失败，数据已被引用", "删除失败，数据已被引用");

            }
        }

        for (int i = 0; i < ids.length; i++) {
            String id = ids[i];
            this.htmlChartWidgetEntityService.deleteById(user, id);
        }

        return buildOperationMessageDeleteSuccessResponseEntity(request);
    }

    @RequestMapping("/pagingQuery")
    public String pagingQuery(HttpServletRequest request, HttpServletResponse response,
                              org.springframework.ui.Model model) {
        //todo 切换用户体系
        User user = WebUtils.getUser(request, response);
//		String authorization = request.getHeader("Authorization");
//		User user = HttpUtil.doGet(authorization);
        model.addAttribute("currentUser", user);

        model.addAttribute(KEY_TITLE_MESSAGE_KEY, "chart.manageChart");

        return "/analysis/chart/chart_grid";
    }

    @RequestMapping(value = "/select")
    public String select(HttpServletRequest request, HttpServletResponse response, org.springframework.ui.Model model) {
        model.addAttribute(KEY_TITLE_MESSAGE_KEY, "chart.selectChart");
        model.addAttribute(KEY_SELECT_OPERATION, true);
        setIsMultipleSelectAttribute(request, model);

        return "/analysis/chart/chart_grid";
    }

    @RequestMapping(value = "/pagingQueryData", produces = CONTENT_TYPE_JSON)
    @ResponseBody
    public PagingData<HtmlChartWidgetEntity> pagingQueryData(HttpServletRequest request, HttpServletResponse response,
                                                             final org.springframework.ui.Model springModel,
                                                             @RequestBody(required = false) APIDDataFilterPagingQuery pagingQueryParam) throws Exception {
        //todo 切换用户体系
        User user = WebUtils.getUser(request, response);
//		String authorization = request.getHeader("Authorization");
//		User user = HttpUtil.doGet(authorization);
//        if (null == user) {
//            throw new Exception("未获取到Authorization");
//        }
        if (null != pagingQueryParam) {
            Order[] orders = pagingQueryParam.getOrders();
            if (null != orders && orders.length > 0) {
                for (Order order : orders) {
                    String name = order.getName();
                    if (name.equals("chartPluginName")) {
                        order.setName("name");
                    }
                }
            }

        }
        final APIDDataFilterPagingQuery pagingQuery = inflateAPIDDataFilterPagingQuery(request, pagingQueryParam);

        PagingData<HtmlChartWidgetEntity> pagingData = this.htmlChartWidgetEntityService.pagingQuery(user, pagingQuery,
                pagingQuery.getDataFilter(), pagingQuery.getAnalysisProjectId());
        setChartPluginNames(request, pagingData.getItems());

        return pagingData;
    }

    /**
     * 展示图表。
     *
     * @param request
     * @param response
     * @param model
     * @param id
     * @throws Exception
     */
    @RequestMapping("/show/{id}/")
    public void show(HttpServletRequest request, HttpServletResponse response, org.springframework.ui.Model model,
                     @PathVariable("id") String id) throws Exception {
        try {

            //todo 切换用户体系
//        User user = WebUtils.getUser(request, response);
            User user = someMapper.queryUserByChartId(id);
            HtmlChartWidgetEntity chart = this.htmlChartWidgetEntityService.getById(user, id);

            showChart(request, response, model, user, chart);
        } catch (Exception e) {
             buildOperationMessageFailResponseEntity(request, HttpStatus.NOT_FOUND,"数据未找到","数据未找到");
//            response.sendError(404,"找不到数据");
//            response.setStatus(-1);
//            response.setStatus(-1,"未查询到数据");
//            response.setContentType("application/json; charset=utf-8");
//
//            PrintWriter writer = response.getWriter();
//            writer.write(404);

        }

    }

    @RequestMapping("/addRule/{chartId}/{ruleId}")
    public void addRule(@PathVariable("chartId") String chartId, @PathVariable("ruleId") Long ruleId) {
        someMapper.addRuleToChart(chartId, ruleId);
    }

    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/catchChart")
    public String catchChart() {
        String result = "";
        try {
            CloseableHttpClient httpClient = HttpClients.createDefault();
            HttpPost httpGet = new HttpPost("http://localhost:9999/datagear_webapp_war_exploded/analysis/chart/showData;jsessionid=BDA1833F001D8779CED7509D151ACA5E");
            //3.发送请求
            CloseableHttpResponse response = httpClient.execute(httpGet);

            HttpEntity entity = response.getEntity();
            //使用工具类EntityUtils，从响应中取出实体表示的内容并转换成字符串
            result = EntityUtils.toString(entity, "utf-8");
        } catch (Exception e) {

        }

        return result;
    }

    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/renderChart", produces = CONTENT_TYPE_JSON)
    @ResponseBody
    public ResponseEntity<ShowDashboardVo> renderChart(@RequestBody RenderChartEntity renderChartEntity) {
//        CloseableHttpClient httpClient = HttpClients.createDefault();
//        id += "/";
//        String authorization = request.getHeader("Authorization");
//        authorization = URLEncoder.encode(authorization, "utf-8");
//        HttpGet httpGet = new HttpGet(url + "/datagear_webapp_war_exploded/analysis/chart/show/" + id );
        //3.发送请求
//        CloseableHttpResponse response = httpClient.execute(httpGet);
//        HttpEntity entity = response.getEntity();
        //使用工具类EntityUtils，从响应中取出实体表示的内容并转换成字符串
//        String result = EntityUtils.toString(entity, "utf-8");

/*        Long ruleId = someMapper.queryRuleId(id);
        if (null != ruleId) {
            String dsId = someMapper.queryDsId(id);
            String jsonResult = someMapper.queryResult(dsId);
            //todo 调用预警接口
        }*/

        String result = renderChartEntity.getResult();
        ShowDashboardVo vo = new ShowDashboardVo();
        vo.setRandom(UUID.randomUUID().toString());
        if (!Strings.isNullOrEmpty(result)) {
            //以body为截取字符获取首尾地址
            int bodyBegin = result.indexOf("<body");

            int bodyEnd = result.lastIndexOf("</body>");
            if (bodyEnd > 0) {
                result = result.substring(bodyBegin, bodyEnd + 7);
                if (!Strings.isNullOrEmpty(result)) {
                    //截取javascript部分
                    int scriptBegin = result.indexOf("<script");
                    int scriptEnd = result.lastIndexOf("</script>");
                    if (scriptEnd > 0) {
                        String javaScript = result.substring(scriptBegin, scriptEnd + 10);
                        result = result.replace(javaScript, "");
                        javaScript = javaScript.replace("<script type=\"text/javascript\">", "");
                        javaScript = javaScript.replace("</script>", "");
                        vo.setJavaScript(javaScript);
                    }
                    //截取div部分
                    List<String> divs = new ArrayList<>();

                    substringDiv(result, divs);
                    vo.setDivs(divs);
                }
            }
        }
        return new ResponseEntity(vo, HttpStatus.OK);

    }

    /**
     * 加载展示图表的资源。
     *
     * @param request
     * @param response
     * @param webRequest
     * @param model
     * @param id
     * @throws Exception
     */
    @RequestMapping("/show/{id}/**/*")
    public void showResource(HttpServletRequest request, HttpServletResponse response, WebRequest webRequest,
                             org.springframework.ui.Model model, @PathVariable("id") String id) throws Exception {
        //todo 切换用户体系
        User user = WebUtils.getUser(request, response);
//		String authorization = request.getHeader("Authorization");
//		User user = HttpUtil.doGet(authorization);
        HtmlChartWidgetEntity chart = this.htmlChartWidgetEntityService.getById(user, id);

        String resName = resolvePathAfter(request, "/show/" + id + "/");

        if (isEmpty(resName)) {
            showChart(request, response, model, user, chart);
        } else {
            TemplateDashboardWidgetResManager resManager = this.chartShowHtmlTplDashboardWidgetHtmlRenderer
                    .getTemplateDashboardWidgetResManager();

            setContentTypeByName(request, response, servletContext, resName);

            long lastModified = resManager.lastModifiedResource(id, resName);
            if (webRequest.checkNotModified(lastModified))
                return;

            InputStream in = resManager.getResourceInputStream(id, resName);
            OutputStream out = response.getOutputStream();

            try {
                IOUtil.write(in, out);
            } finally {
                IOUtil.close(in);
            }
        }
    }

    /**
     * 展示数据。
     *
     * @param request
     * @param response
     * @param model
     * @throws Exception
     */
    @RequestMapping(value = "/showData", produces = CONTENT_TYPE_JSON)
    @ResponseBody
    public Map<String, DataSetResult[]> showData(HttpServletRequest request, HttpServletResponse response,
                                                 org.springframework.ui.Model model, @RequestBody Map<String, ?> paramData) throws Exception {
        WebContext webContext = createWebContext(request);
        return getDashboardData(request, response, model, webContext, paramData);
    }

    /**
     * 展示图表。
     *
     * @param request
     * @param response
     * @param model
     * @throws Exception
     */
    protected void showChart(HttpServletRequest request, HttpServletResponse response,
                             org.springframework.ui.Model model, User user, HtmlChartWidgetEntity chart) throws Exception {
        if (chart == null)
            throw new RecordNotFoundException();

        ChartWidgetSourceContext.set(new ChartWidgetSourceContext(user));

        String id = chart.getId();

        String htmlTitle = chart.getName();
        HtmlTplDashboardWidget dashboardWidget = new HtmlTplDashboardWidget(id,
                this.chartShowHtmlTplDashboardWidgetHtmlRenderer.simpleTemplateContent("UTF-8", htmlTitle,
                        "  position:absolute;\n  left:1em;\n  right:1em;\n  top:1em;\n  bottom:1em;\n  margin:0 0;\n  width:auto;\n  height:auto;\n",
                        new String[]{id}),
                this.chartShowHtmlTplDashboardWidgetHtmlRenderer);

        String responseEncoding = dashboardWidget.getTemplateEncoding();
        response.setCharacterEncoding(responseEncoding);
        response.setContentType(CONTENT_TYPE_HTML);

        HtmlTplDashboardRenderAttr renderAttr = createHtmlTplDashboardRenderAttr();
        RenderContext renderContext = createHtmlRenderContext(request, response, renderAttr, createWebContext(request),
                getChartShowHtmlTplDashboardWidgetHtmlRenderer());
        AddPrefixHtmlTitleHandler htmlTitleHandler = new AddPrefixHtmlTitleHandler(
                getMessage(request, "chart.show.htmlTitlePrefix", getMessage(request, "app.name")));
        renderAttr.setHtmlTitleHandler(renderContext, htmlTitleHandler);

        HtmlTplDashboard dashboard = dashboardWidget.render(renderContext);

        SessionHtmlTplDashboardManager dashboardManager = getSessionHtmlTplDashboardManagerNotNull(request);
        dashboardManager.put(dashboard);
    }

    protected WebContext createWebContext(HttpServletRequest request) {
        HttpSession session = request.getSession();

        String contextPath = getWebContextPath(request).get(request);
//        String contextPath = url;
        WebContext webContext = new WebContext(contextPath,
                addJsessionidParam(contextPath + "/analysis/chart/showData", session.getId()),
                addJsessionidParam(contextPath + "/analysis/dashboard/loadChart", session.getId()));

        webContext.setExtraValues(new HashMap<String, Object>());
        addHeartBeatValue(request, webContext);

        return webContext;
    }

    protected void setChartPluginNames(HttpServletRequest request, List<HtmlChartWidgetEntity> entities) {
        if (entities == null)
            return;

        Locale locale = WebUtils.getLocale(request);

        for (HtmlChartWidgetEntity entity : entities)
            entity.updateChartPluginName(locale);
    }

    protected void inflateHtmlChartWidgetEntity(HtmlChartWidgetEntity entity, HttpServletRequest request) {
        HtmlChartPlugin htmlChartPlugin = entity.getHtmlChartPlugin();

        if (htmlChartPlugin != null) {
            htmlChartPlugin = (HtmlChartPlugin) this.chartPluginManager.get(htmlChartPlugin.getId());

            entity.setHtmlChartPlugin(htmlChartPlugin);
        }

        ChartDataSetVO[] chartDataSetVOs = entity.getChartDataSetVOs();
        if (chartDataSetVOs != null) {
            for (ChartDataSetVO vo : chartDataSetVOs) {
                List<DataSetParam> params = vo.getDataSet().getParams();

                if (params != null && !params.isEmpty()) {
                    Map<String, Object> paramValues = getDataSetParamValueConverter().convert(vo.getParamValues(),
                            params);
                    vo.setParamValues(paramValues);
                }
            }
        }
    }

    protected void setCookieAnalysisProject(HttpServletRequest request, HttpServletResponse response,
                                            HtmlChartWidgetEntity entity) {
        setCookieAnalysisProjectIfValid(request, response, this.analysisProjectService, entity);
    }

    protected void checkSaveEntity(HtmlChartWidgetEntity chart) {
        if (isBlank(chart.getName()))
            throw new IllegalInputException();

        if (isEmpty(chart.getPlugin()))
            throw new IllegalInputException();
    }

    protected void substringDiv(String result, List<String> divs) {
        if (result.contains("</div>")) {
            int begin = result.indexOf("<div");
            int end = result.indexOf("</div>");
            String substring = result.substring(begin, end + 6);
            divs.add(substring);
            result = result.replace(substring, "");
            substringDiv(result, divs);
        }
    }
}
