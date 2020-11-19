package org.datagear.web.vo;

import java.io.Serializable;
import java.util.List;

/**
 * 〈一句话功能简述〉<br>
 * 〈〉
 *
 * @author gwl
 * @date 2020/9/25 9:57
 * @since 1.0.0
 */
public class ShowDashboardVo implements Serializable {

    private List<String> divs;
    private String javaScript;

    private String random;

    public List<String> getDivs() {
        return divs;
    }

    public void setDivs(List<String> divs) {
        this.divs = divs;
    }

    public String getJavaScript() {
        return javaScript;
    }

    public void setJavaScript(String javaScript) {
        this.javaScript = javaScript;
    }

    public String getRandom() {
        return random;
    }

    public void setRandom(String random) {
        this.random = random;
    }
}
