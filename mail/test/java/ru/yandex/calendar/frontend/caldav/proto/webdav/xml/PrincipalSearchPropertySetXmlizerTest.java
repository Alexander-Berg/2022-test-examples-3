package ru.yandex.calendar.frontend.caldav.proto.webdav.xml;

import org.dom4j.Element;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.calendar.frontend.caldav.proto.caldav.CaldavConstants;
import ru.yandex.calendar.frontend.caldav.proto.webdav.WebdavConstants;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.xml.dom4j.Dom4jUtils;

/**
 * @author Stepan Koltsov
 */
public class PrincipalSearchPropertySetXmlizerTest {

    @Test
    public void serialize() {
        PrincipalSearchPropertySet psps = new PrincipalSearchPropertySet(Cf.list(
                    new PrincipalSearchProperty(WebdavConstants.DAV_DISPLAYNAME_PROP, "Display Name"),
                    new PrincipalSearchProperty(CaldavConstants.CALENDARSERVER_EMAIL_ADDRESS_SET_PROP, "Email Addresses")
                ));
        Element xml = Dom4jUtils.readRootElement(PrincipalSearchPropertySetXmlizer.S.serializeXml(psps));

//        System.out.println(Dom4jUtils.I.writeElementToString(xml, XmlWriteFormat.defaultFormat().writeHeader()));

        Assert.A.hasSize(3, xml.content());
        Element psp1 = (Element) xml.content().get(1);
        Assert.A.equals("principal-search-property", psp1.getName());
        Assert.A.equals("DAV:", psp1.getNamespaceURI());

        Element prop = (Element) xml.selectSingleNode("*/*");
        Assert.A.equals("prop", prop.getName());
        Assert.A.equals("DAV:", prop.getNamespaceURI());
    }

} //~
