package ru.yandex.market.rg.crm.executor;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.export.xml.ReportXML;
import ru.yandex.market.common.spring.UploadHandler;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.db.DbUnitRefreshMatViews;
import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.mbi.util.MbiAsserts;
import ru.yandex.market.rg.config.FunctionalTest;
import ru.yandex.market.rg.crm.CRMRepository;
import ru.yandex.market.rg.crm.ClientData;
import ru.yandex.market.rg.crm.XMLMarshallableClientData;
import ru.yandex.market.rg.util.ExportExecutor;

import static ru.yandex.market.rg.util.ExportExecutor.COMPONENT_NAME;

/**
 * @author otedikova
 */
class CRMClientsExportExecutorTest extends FunctionalTest {
    @Autowired
    private CRMRepository crmRepository;

    private ExportExecutor<ClientData> executor;
    private TestCRMExportResourceSender resourceSender;

    @BeforeEach
    void setUp() {
        ReportXML.DataWriterFactory<ClientData> writerFactory =
                ReportXML.jaxb(XMLMarshallableClientData.class, XMLMarshallableClientData::new)
                        .withChrome(ReportXML.standardChrome("clients"));
        resourceSender = new TestCRMExportResourceSender();
        executor = new ExportExecutor<>(crmRepository::provideClientIteratorTo, writerFactory,
                () -> new UploadHandler(resourceSender),
                COMPONENT_NAME
        );
    }

    @Test
    @DbUnitDataSet(before = "CRMShopDataExportTest.before.csv")
    @DbUnitRefreshMatViews
    void testShopsExport() {
        executor.doJob(null);
        String actualXMLData = resourceSender.getUploadedData();
        String expectedXMLData = StringTestUtil.getString(getClass(), "expected_clients_export.xml");
        MbiAsserts.assertXmlEquals(expectedXMLData, actualXMLData, Sets.newHashSet("timestamp"));
    }
}
