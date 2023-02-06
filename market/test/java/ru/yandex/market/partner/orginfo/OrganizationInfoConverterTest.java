package ru.yandex.market.partner.orginfo;

import java.io.IOException;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.xml.sax.SAXException;
import servantlet.ServantletTestSerializationConfig;

import ru.yandex.market.common.test.SerializationChecker;
import ru.yandex.market.core.orginfo.model.OrganizationInfo;
import ru.yandex.market.core.orginfo.model.OrganizationType;

/**
 * Тест для {@link OrganizationInfoConverter}.
 *
 * @author fbokovikov
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = ServantletTestSerializationConfig.class)
public class OrganizationInfoConverterTest {

    private static final String EXPECTED_XML =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<organization-info>\n" +
                    "   <info-id>100</info-id>\n" +
                    "   <organization-type>OOO</organization-type>\n" +
                    "   <organization-type-code>1</organization-type-code>\n" +
                    "   <name />\n" +
                    "   <ogrn />\n" +
                    "   <juridical-address>Санкт-Петербург, Дворцовая площадь</juridical-address>\n" +
                    "   <info-source>0</info-source>\n" +
                    "   <registration-number>1011010111</registration-number>\n" +
                    "   <info-url>http://nowhere.su/org-info</info-url>\n" +
                    "</organization-info>";

    @Autowired
    private SerializationChecker serializationChecker;

    @Test
    public void testOrganizationInfoSerialization() throws IOException, SAXException, JSONException {
        OrganizationInfo orgInfo = new OrganizationInfo();
        orgInfo.setDatasourceId(774L);
        orgInfo.setId(100);
        orgInfo.setJuridicalAddress("Санкт-Петербург, Дворцовая площадь");
        orgInfo.setType(OrganizationType.OOO);
        orgInfo.setRegistrationNumber("1011010111");
        orgInfo.setInfoUrl("http://nowhere.su/org-info");
        serializationChecker.testXmlSerialization(orgInfo, EXPECTED_XML);
    }
}
