package ru.yandex.market.rg.crm;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Sets;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.api.cpa.yam.entity.SignatoryDocType;
import ru.yandex.market.common.export.xml.ReportXML;
import ru.yandex.market.common.spring.UploadHandler;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.environment.ActiveParamService;
import ru.yandex.market.core.partner.placement.PartnerPlacementProgramStatus;
import ru.yandex.market.core.partner.placement.PartnerPlacementProgramType;
import ru.yandex.market.core.program.partner.model.Status;
import ru.yandex.market.core.wizard.model.WizardStepType;
import ru.yandex.market.mbi.util.MbiAsserts;
import ru.yandex.market.rg.config.FunctionalTest;
import ru.yandex.market.rg.crm.executor.TestCRMExportResourceSender;
import ru.yandex.market.rg.util.ExportExecutor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.rg.util.ExportExecutor.COMPONENT_NAME;

/**
 * @author otedikova
 */
@DbUnitDataSet(before = "CRMRepositorySupplierExportTest.before.csv")
class CRMRepositorySupplierExportTest extends FunctionalTest {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    @Qualifier("importedStocksActiveParamService")
    private ActiveParamService activeParamService;

    @Autowired
    Clock clock;

    private CRMRepository crmRepository;

    @BeforeEach
    void setUp() {
        crmRepository = new CRMRepository(jdbcTemplate, clock, activeParamService);
    }

    @Test
    void testSupplierRepositoryResult() {
        Map<Long, SupplierExportDto> resultMap = new HashMap<>();
        crmRepository.provideSupplierIteratorTo(iterator -> {
            while (iterator.hasNext()) {
                SupplierExportDto supplier = iterator.next();
                assertFalse(resultMap.containsKey(supplier.getId()), "CRM не ожидает дублей в выгрузке (см. " +
                        "MBI-68767)");
                resultMap.put(supplier.getId(), supplier);
            }
        });
        assertEquals(2, resultMap.size());
        SupplierExportDto supplier1 = resultMap.get(12345L);
        assertEquals("supplier_1_name", supplier1.getName());
        assertEquals("sup1.ru", supplier1.getDomain());
        assertEquals(54321L, supplier1.getCampaignId());
        assertEquals(10L, supplier1.getBusinessId().longValue());
        assertEquals("business", supplier1.getBusinessName());
        assertEquals(13007L, supplier1.getClientId().longValue());
        assertEquals(223434, supplier1.getBalanceClientId().longValue());
        assertEquals("supplier_1_representative", supplier1.getRepresentativeLogin());
        assertEquals(1234, supplier1.getRepresentativeUid());
        assertEquals(261411L, supplier1.getSalesSum().longValue());
        assertEquals(2, supplier1.getOrdersCount());
        assertEquals(5, supplier1.getStockRemainsCount());
        assertEquals(2, supplier1.getStockRemainsSKUCounts());
        List<SupplierContractExportDto> contracts = supplier1.getContracts();
        assertEquals(2, contracts.size());
        assertEquals(12321, contracts.get(0).getContractId());
        assertEquals(12322, contracts.get(1).getContractId());
        assertEquals(230L, supplier1.getTotalOffersCount().longValue());
        assertThat(supplier1.getOnboardingState()).usingRecursiveFieldByFieldElementComparator()
                .containsExactlyElementsOf(List.of(
                        new CRMOnboardingState(WizardStepType.COMMON_INFO, Status.FULL, "2020-10-27 17:03:57"),
                        new CRMOnboardingState(WizardStepType.SUPPLIER_INFO, Status.FULL, "2020-10-27 17:03:57"),
                        new CRMOnboardingState(WizardStepType.PLACEMENT_MODEL, Status.FULL, "2020-12-28 21:34:15"),
                        new CRMOnboardingState(WizardStepType.ASSORTMENT, Status.FILLED, "2020-12-08 11:40:21"),
                        new CRMOnboardingState(WizardStepType.SUPPLIER_FEED, Status.NONE, "2020-12-08 11:40:21")
                ));
        assertNull(supplier1.getYaManager());
        assertThat(supplier1.getPrograms())
                .usingRecursiveFieldByFieldElementComparator()
                .containsOnly(
                        new CRMPlacementProgram(PartnerPlacementProgramType.FULFILLMENT,
                                PartnerPlacementProgramStatus.SUCCESS),
                        new CRMPlacementProgram(PartnerPlacementProgramType.CROSSDOCK,
                                PartnerPlacementProgramStatus.CONFIGURE));
        assertEquals(Instant.parse("2018-01-01T10:30:11.00Z"), supplier1.getFirstRegistrationDate());
        assertTrue(supplier1.isExpress());
        assertEquals("90764", supplier1.getPartnerLeadCategory());
        assertTrue(supplier1.isSelfEmployed());

        SupplierContact returnContact = supplier1.getReturnContact();
        assertEquals("Гордеева Елена", returnContact.getName());
        assertEquals("+7 906 898 54 55", returnContact.getPhoneNumber());
        assertEquals("egord@mail.ru", returnContact.getEmail());

        SupplierContact paymentContact = supplier1.getPaymentContact();
        assertEquals("Евгений Давыдов", paymentContact.getName());
        assertEquals("edavy@yandex.ru", paymentContact.getEmail());
        assertEquals("+7(989) 667 54-34", paymentContact.getPhoneNumber());

        Signatory signatory = supplier1.getSignatory();
        assertEquals("Павел Павлов", signatory.getName());
        assertEquals("директора", signatory.getPosition());
        assertEquals(SignatoryDocType.AOA_OR_ENTREPRENEUR, signatory.getDocType());

        SupplierExportDto supplier2 = resultMap.get(12346L);
        assertEquals(20L, supplier2.getBusinessId());
        assertEquals(0, supplier2.getStockRemainsCount());
        assertEquals(0, supplier2.getStockRemainsSKUCounts());
        assertNull(supplier2.getBalanceClientId());
        assertNull(supplier2.getTotalOffersCount());
        User supplier2YaManager = supplier2.getYaManager();
        assertEquals(1, supplier2YaManager.id());
        assertEquals("Иванов Иван", supplier2YaManager.name().orElse(null));
        assertEquals("yndx-iivanov", supplier2YaManager.login().orElse(null));
        assertEquals("iivanov", supplier2YaManager.staffLogin().orElse(null));
        assertNull(supplier2.getReturnContact());
        assertFalse(supplier2.isExpress());
        assertNull(supplier2.getOnboardingState());
        assertNull(supplier2.getFirstRegistrationDate());
        assertNull(supplier2.getPartnerLeadCategory());
        assertFalse(supplier2.isSelfEmployed());
    }

    @Test
    void testCRMRepositoryExecutor() throws IOException {
        ReportXML.DataWriterFactory<SupplierExportDto> writerFactory =
                ReportXML.<SupplierExportDto, XMLMarshallableSupplier>jaxb(XMLMarshallableSupplier.class,
                                XMLMarshallableSupplier::new)
                        .withChrome(ReportXML.standardChrome("suppliers"));
        TestCRMExportResourceSender resourceSender = new TestCRMExportResourceSender();
        ExportExecutor<SupplierExportDto> executor = new ExportExecutor<>(crmRepository::provideSupplierIteratorTo,
                writerFactory,
                () -> new UploadHandler(resourceSender),
                COMPONENT_NAME);
        executor.doJob(null);
        String actualXMLData = resourceSender.getUploadedData();
        String expectedXMLData = IOUtils.toString(this.getClass().getResourceAsStream(
                        "executor/expected_crm_export.xml"),
                Charset.defaultCharset());
        MbiAsserts.assertXmlEquals(expectedXMLData, actualXMLData, Sets.newHashSet("timestamp"));
    }
}
