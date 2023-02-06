package ru.yandex.common.report;

import junit.framework.TestCase;
import org.jdom2.Element;
import org.jdom2.output.XMLOutputter;

import ru.yandex.common.report.tabular.converter.JDOMUtil;

/**
 * @author Vladimir Gorovoy vgorovoy@yandex-team.ru
 */
public class JdomTest extends TestCase {
    public void testJDomToXml() throws Exception {

        Element root = new Element("report-query-info");
        root.setAttribute("recipient-list", "yes");
        JDOMUtil.addSimpleElement(root, "name", "yopta");
        Element paramsElement = new Element("params");
        Element paramElement = new Element("param");
        JDOMUtil.addSimpleElement(paramElement, "name", "key");
        JDOMUtil.addSimpleElement(paramElement, "description", "desc");
        paramsElement.addContent(paramElement);
        root.addContent(paramsElement);

        System.out.println(root);
        XMLOutputter xmlOutputter = new XMLOutputter();
        System.out.println(xmlOutputter.outputString(root));
    }

    public void testQuerySubstr() throws Exception {
        String query = "select * from dddd ORDER BY ooops";
        int orderByIndex = query.toLowerCase().indexOf("order by");
        if (orderByIndex > 0) {
            System.out.println(query.substring(0, orderByIndex) + ";");
        }
    }

}
