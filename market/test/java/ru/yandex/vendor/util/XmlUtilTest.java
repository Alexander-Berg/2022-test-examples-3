package ru.yandex.vendor.util;

import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import ru.yandex.common.util.XmlUtils;

import java.util.List;
import java.util.stream.Stream;

import static java.util.Arrays.*;
import static java.util.stream.Collectors.*;

public class XmlUtilTest {

    @Test
    public void stream_node_list() throws Exception {
        NodeList nodeList = XmlUtils.parseSource(
                "<a>" +
                        "   <b>" +
                        "       <c id=\"1\"/>" +
                        "       <c id=\"2\"/>" +
                        "       <c id=\"3\">" +
                        "           <c id=\"4\"/>" +
                        "       </c>" +
                        "   </b>" +
                        "   <d>" +
                        "       <c id=\"5\"/>" +
                        "   </d>" +
                        "</a>"
        ).getDocumentElement().getElementsByTagName("c");
        Stream<Element> elements = XmlUtil.streamElements(nodeList);
        List<String> ids = elements.map(e -> e.getAttribute("id")).collect(toList());
        Assert.assertEquals(asList("1", "2", "3", "4", "5"), ids);
    }
}