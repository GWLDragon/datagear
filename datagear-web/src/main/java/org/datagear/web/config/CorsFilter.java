package org.datagear.web.config;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 〈一句话功能简述〉<br>
 * 〈〉
 *
 * @author gwl
 * @date 2020/11/3 10:02
 * @since 1.0.0
 */

public class CorsFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;
        httpResponse.addHeader("Access-Control-Allow-Origin", "*");
        httpResponse.setHeader("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept");
        httpResponse.setHeader("Access-Control-Allow-Methods", "GET, PUT, DELETE, POST");
        filterChain.doFilter(servletRequest, servletResponse);
    }

    @Override
    public void destroy() {

    }
}
