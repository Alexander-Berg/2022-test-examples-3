package ru.yandex.market.rg.crm.executor;

import java.time.Instant;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.common.export.xml.ReportXML;
import ru.yandex.market.common.spring.UploadHandler;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.db.DbUnitRefreshMatViews;
import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.mbi.util.MbiAsserts;
import ru.yandex.market.rg.config.FunctionalTest;
import ru.yandex.market.rg.crm.CRMUpdatedShopsExportRepository;
import ru.yandex.market.rg.crm.ShopFullData;
import ru.yandex.market.rg.crm.VersionedExportRepository;
import ru.yandex.market.rg.crm.XMLMarshallableFullShopData;
import ru.yandex.market.rg.util.VersionedExportExecutor;

/**
 * @author otedikova
 */
class CRMUpdatedShopsExportExecutorTest extends FunctionalTest {
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private EnvironmentService environmentService;
    private VersionedExportExecutor<ShopFullData, Instant> executor;
    private TestCRMExportResourceSender resourceSender;

    @BeforeEach
    void setUp() {
        ReportXML.DataWriterFactory<ShopFullData> writerFactory =
                ReportXML.jaxb(XMLMarshallableFullShopData.class, XMLMarshallableFullShopData::new)
                        .withChrome(ReportXML.standardChrome("shops"));
        resourceSender = new TestCRMExportResourceSender();

        VersionedExportRepository<ShopFullData, Instant> crmUpdatedShopsExportRepository =
                new CRMUpdatedShopsExportRepository(jdbcTemplate, environmentService);
        executor = new VersionedExportExecutor<>(crmUpdatedShopsExportRepository,
                writerFactory,
                () -> new UploadHandler(resourceSender));
    }

    @Test
    @DbUnitDataSet(before = "CRMUpdatedShopsExportTest.before.csv")
    @DbUnitRefreshMatViews
    void testShopsExport() {
        executor.doJob(null);
        String actualXMLData = resourceSender.getUploadedData();
        String expectedXMLData = StringTestUtil.getString(getClass(), "expected_updated_shops_export.xml");
        MbiAsserts.assertXmlEquals(expectedXMLData, actualXMLData, Sets.newHashSet("timestamp"));
    }
}
