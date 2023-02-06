package ru.yandex.market.rg.crm.executor;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.Instant;

import com.google.common.collect.Sets;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.common.export.xml.ReportXML;
import ru.yandex.market.common.spring.UploadHandler;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.mbi.util.MbiAsserts;
import ru.yandex.market.rg.config.FunctionalTest;
import ru.yandex.market.rg.crm.CRMUpdatedContactsExportRepository;
import ru.yandex.market.rg.crm.Contact;
import ru.yandex.market.rg.crm.XMLMarshallableContact;
import ru.yandex.market.rg.util.VersionedExportExecutor;

/**
 * @author otedikova
 */
class CRMUpdatedContactsExportExecutorTest extends FunctionalTest {
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private EnvironmentService environmentService;

    private VersionedExportExecutor<Contact, Instant> exportExecutor;

    private TestCRMExportResourceSender resourceSender;

    @BeforeEach
    void setUp() {
        ReportXML.DataWriterFactory<Contact> writerFactory =
                ReportXML.jaxb(XMLMarshallableContact.class, XMLMarshallableContact::new)
                        .withChrome(ReportXML.standardChrome("contacts"));
        CRMUpdatedContactsExportRepository contactsExportRepository = new CRMUpdatedContactsExportRepository(
                jdbcTemplate,
                environmentService
        );

        resourceSender = new TestCRMExportResourceSender();
        exportExecutor = new VersionedExportExecutor<>(contactsExportRepository,
                writerFactory,
                () -> new UploadHandler(resourceSender));
    }

    @DbUnitDataSet(before = "CRMUpdatedContactsExportExecutorTest.before.csv")
    @Test
    void testContactsExport() throws IOException {
        exportExecutor.doJob(null);
        String actualXMLData = resourceSender.getUploadedData();
        String expectedXMLData = IOUtils.toString(
                getClass().getResourceAsStream("expected_contacts_export.xml"),
                Charset.defaultCharset()
        );
        MbiAsserts.assertXmlEquals(expectedXMLData, actualXMLData, Sets.newHashSet("timestamp"));
    }
}
