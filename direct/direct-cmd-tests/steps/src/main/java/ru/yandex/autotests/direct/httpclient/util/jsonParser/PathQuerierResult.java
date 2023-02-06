package ru.yandex.autotests.direct.httpclient.util.jsonParser;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 02.03.15
 */
public class PathQuerierResult {

    private Object result;
    private String parentNodePath;

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public String getParentNodePath() {
        return parentNodePath;
    }

    public void setParentNodePath(String parentNodePath) {
        this.parentNodePath = parentNodePath;
    }

    public PathQuerierResult(Object result, String parentNodePath) {
        this.result = result;
        this.parentNodePath = parentNodePath;
    }

    public PathQuerierResult() {
    }
}
