package ru.yandex.calendar.frontend.caldav.proto.webdav;

import org.dom4j.Element;
import org.junit.Test;

import ru.yandex.calendar.frontend.caldav.proto.webdav.xml.WebdavXmlizers;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.xml.QName;
import ru.yandex.misc.xml.dom.DomUtils;
import ru.yandex.misc.xml.dom4j.Dom4jUtils;

/**
 * @author Stepan Koltsov
 */
public class DavHrefXmlizerTest {

    @Test
    public void parse() {
        String xml = "<href xmlns='DAV:'>/home</href>";
        DavHref davHref = WebdavXmlizers.hrefXmlizer.getParser().parseXml(DomUtils.I.readRootElement(xml));
        Assert.A.equals("/home", davHref.getDecoded());
    }

    @Test
    public void serialize() {
        DavHref davHref = DavHref.fromDecoded("/fgfg");
        Element xml = Dom4jUtils.readRootElement(WebdavXmlizers.hrefXmlizer.getSerializer().serializeXml(davHref));
        Assert.A.equals("/fgfg", xml.getText());
        Assert.A.isTrue(Dom4jUtils.I.nameIs(xml, QName.qname(WebdavConstants.DAV_NS_URI, "href")));
    }

} //~
