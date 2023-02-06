package ru.yandex.autotests.direct.httpclient.util.jsonParser;

import com.googlecode.jxquery.utils.Constants;
import com.googlecode.jxquery.utils.ReflectionHelper;
import com.googlecode.jxquery.utils.XMLHelper;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.lang.reflect.Field;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 25.02.15
 */
public class JsonPathXQuerier {

    private final static javax.xml.xpath.XPath XPath = XPathFactory.newInstance().newXPath();

    public static PathQuerierResult get(String parentNodePath, Node xml, Field f, JsonPath xqf, boolean fromJSON, BeanType beanType)
            throws XPathExpressionException {
        Document absoluteXML = XMLHelper.getAbsoluteXML(xml);
        String query = getQueryString(parentNodePath, xqf, fromJSON, beanType);
        String nextParentNodePath = getParentNodePath(query);
        Object result = null;
        if (query != null && query.length() != 0) {
            XPathExpression xquery = XPath.compile(query);

            if (ReflectionHelper.isCollectionType(f.getType())) {

                result = (NodeList) xquery.evaluate(absoluteXML,
                        XPathConstants.NODESET);
            } else {

                result = (Node) xquery.evaluate(absoluteXML, XPathConstants.NODE);
            }
        }
        return new PathQuerierResult(result, nextParentNodePath);
    }

    private static String getParentNodePath(String parentNodePath) {
        String result = null;
        if (parentNodePath != null) {
            int lastSlashPosition = parentNodePath.lastIndexOf(Constants.SLASH);
            if (lastSlashPosition != -1) {
                result = parentNodePath.substring(lastSlashPosition + 1);
            }
        }
        return result;
    }


    private static String getQueryString(String currentParentNodePath, JsonPath xqf, boolean fromJSON, BeanType beanType) {
        String query = "";
        if (currentParentNodePath != null) {
            String nextParentNodePath = getParentNodePath(currentParentNodePath);
            if (nextParentNodePath != null) {
                query += nextParentNodePath;
            } else {
                query += currentParentNodePath;
            }
        }
        if (query != null && query.length() > 0) {
            query += Constants.SLASH;
        }
        if (beanType == BeanType.REQUEST) {
            if (xqf.requestPath() != null && xqf.requestPath().length() > 0) {
                query += xqf.requestPath();
            } else {
                query = null;
            }
        } else {
            if (xqf.responsePath() != null && xqf.responsePath().length() > 0) {
                query += xqf.responsePath();
            } else {
                query = null;
            }
        }
        return query;
    }
}
