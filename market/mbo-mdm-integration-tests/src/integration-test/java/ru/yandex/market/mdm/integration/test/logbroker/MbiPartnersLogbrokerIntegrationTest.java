package ru.yandex.market.mdm.integration.test.logbroker;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;

import ru.yandex.market.mbo.mdm.common.infrastructure.MdmLogbrokerEvent;
import ru.yandex.market.mbo.mdm.common.infrastructure.MdmLogbrokerService;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplier;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierType;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmSupplierRepository;
import ru.yandex.market.mboc.common.MdmBaseIntegrationTestWithLogbrokerClass;
import ru.yandex.market.partner.event.PartnerInfo;

@DirtiesContext
public class MbiPartnersLogbrokerIntegrationTest extends MdmBaseIntegrationTestWithLogbrokerClass {
    private static final int LB_TIMEOUT_SEC = 10;
    private static final int BUSINESS_A = 1;
    private static final int SERVICE_A1 = 11;
    private static final int SERVICE_A2 = 12;
    private static final int BUSINESS_B = 2;
    private static final int SERVICE_B1 = 21;
    private static final int SERVICE_B2 = 22;

    @Autowired
    private MdmSupplierRepository mdmSupplierRepository;
    @Autowired
    @Qualifier("testingMbiToMdmPartnersProducer")
    private MdmLogbrokerService mbiToMdmLogbrokerService;
    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Before
    public void setup() {
        mdmSupplierRepository.deleteAll();
    }

    @Test
    public void testNewSuppliersReceived() {
        commit();
        messages(
            businessEvent(BUSINESS_A), serviceEvent(SERVICE_A1, BUSINESS_A), serviceEvent(SERVICE_A2, BUSINESS_A)
        ).forEach(mbiToMdmLogbrokerService::publishEvent);

        MdmSupplier expectedBusinessA = business(BUSINESS_A);
        MdmSupplier expectedServiceA1 = service(SERVICE_A1, BUSINESS_A);
        MdmSupplier expectedServiceA2 = service(SERVICE_A2, BUSINESS_A);

        Awaitility.await().atMost(LB_TIMEOUT_SEC, TimeUnit.SECONDS).untilAsserted(() -> {
            Assertions.assertThat(mdmSupplierRepository.findAll())
                .usingElementComparatorIgnoringFields(
                    "businessStateUpdatedTs", "updatedTs", "businessSwitchTransports")
                .containsExactlyInAnyOrder(
                    expectedBusinessA, expectedServiceA1, expectedServiceA2
                );
        });
    }

    @Test
    public void testOldSuppliersUpdated() {
        MdmSupplier existingA1 = service(SERVICE_A1).setDeleted(true);
        MdmSupplier existingA2 = service(SERVICE_A2).setName("oldname2");
        mdmSupplierRepository.insertBatch(existingA1, existingA2);
        commit();

        messages(
            serviceEvent(SERVICE_A1).toBuilder().setName("newname1").build(),
            serviceEvent(SERVICE_A2).toBuilder().setName("newname2").build()
        ).forEach(mbiToMdmLogbrokerService::publishEvent);

        MdmSupplier expectedServiceA1 = service(SERVICE_A1).setName("newname1");
        MdmSupplier expectedServiceA2 = service(SERVICE_A2).setName("newname2");
        Awaitility.await().atMost(LB_TIMEOUT_SEC, TimeUnit.SECONDS).untilAsserted(() -> {
            Assertions.assertThat(mdmSupplierRepository.findAll())
                .usingElementComparatorIgnoringFields(
                    "businessStateUpdatedTs", "updatedTs", "businessSwitchTransports")
                .containsExactlyInAnyOrder(
                    expectedServiceA1, expectedServiceA2
                );
        });
    }

    @Test
    public void testStageChanged() {
        MdmSupplier existingBusinessA = business(BUSINESS_A);
        MdmSupplier existingServiceA1 = service(SERVICE_A1, BUSINESS_A);
        MdmSupplier existingServiceA2 = service(SERVICE_A2, BUSINESS_A).setBusinessEnabled(true);
        mdmSupplierRepository.insertBatch(existingBusinessA, existingServiceA1, existingServiceA2);
        commit();

        messages(
            stageChangeEvent(SERVICE_A1, BUSINESS_A, true),
            stageChangeEvent(SERVICE_A2, BUSINESS_A, false)
        ).forEach(mbiToMdmLogbrokerService::publishEvent);

        MdmSupplier expectedBusinessA = existingBusinessA;
        MdmSupplier expectedServiceA1 = service(SERVICE_A1, BUSINESS_A).setBusinessEnabled(true);
        MdmSupplier expectedServiceA2 = service(SERVICE_A2, BUSINESS_A);
        Awaitility.await().atMost(LB_TIMEOUT_SEC, TimeUnit.SECONDS).untilAsserted(() -> {
            Assertions.assertThat(mdmSupplierRepository.findAll())
                .usingElementComparatorIgnoringFields(
                    "businessStateUpdatedTs", "updatedTs", "businessSwitchTransports")
                .containsExactlyInAnyOrder(
                    expectedBusinessA, expectedServiceA1, expectedServiceA2
                );
        });
    }

    @Test
    public void testBusinessChanged() {
        MdmSupplier existingBusinessA = business(BUSINESS_A);
        MdmSupplier existingServiceA1 = service(SERVICE_A1, BUSINESS_A);
        MdmSupplier existingServiceA2 = service(SERVICE_A2, BUSINESS_A);
        MdmSupplier existingBusinessB = business(BUSINESS_B);
        MdmSupplier existingServiceB1 = service(SERVICE_B1, BUSINESS_B);
        MdmSupplier existingServiceB2 = service(SERVICE_B2, BUSINESS_B);
        mdmSupplierRepository.insertBatch(
            existingBusinessA, existingServiceA1, existingServiceA2,
            existingBusinessB, existingServiceB1, existingServiceB2);
        commit();

        messages(
            businessChangeEvent(SERVICE_A1, BUSINESS_B),
            businessChangeEvent(SERVICE_B1, BUSINESS_A)
        ).forEach(mbiToMdmLogbrokerService::publishEvent);

        MdmSupplier expectedBusinessA = existingBusinessA;
        MdmSupplier expectedServiceA1 = service(SERVICE_A1, BUSINESS_B);
        MdmSupplier expectedServiceA2 = existingServiceA2;
        MdmSupplier expectedBusinessB = existingBusinessB;
        MdmSupplier expectedServiceB1 = service(SERVICE_B1, BUSINESS_A);
        MdmSupplier expectedServiceB2 = existingServiceB2;
        Awaitility.await().atMost(LB_TIMEOUT_SEC, TimeUnit.SECONDS).untilAsserted(() -> {
            Assertions.assertThat(mdmSupplierRepository.findAll())
                .usingElementComparatorIgnoringFields(
                    "businessStateUpdatedTs", "updatedTs", "businessSwitchTransports")
                .containsExactlyInAnyOrder(
                    expectedBusinessA, expectedServiceA1, expectedServiceA2,
                    expectedBusinessB, expectedServiceB1, expectedServiceB2
                );
        });
    }

    @Test
    public void testStageChangedOnNewSupplier() {
        MdmSupplier existingBusinessA = business(BUSINESS_A);
        mdmSupplierRepository.insertBatch(existingBusinessA);
        commit();

        messages(
            stageChangeEvent(SERVICE_A1, BUSINESS_A, true),
            stageChangeEvent(SERVICE_A2, BUSINESS_A, false)
        ).forEach(mbiToMdmLogbrokerService::publishEvent);

        MdmSupplier expectedBusinessA = existingBusinessA;
        MdmSupplier expectedServiceA1 = service(SERVICE_A1, BUSINESS_A).setBusinessEnabled(true);
        MdmSupplier expectedServiceA2 = service(SERVICE_A2, BUSINESS_A);
        Awaitility.await().atMost(LB_TIMEOUT_SEC, TimeUnit.SECONDS).untilAsserted(() -> {
            Assertions.assertThat(mdmSupplierRepository.findAll())
                .usingElementComparatorIgnoringFields(
                    "businessStateUpdatedTs", "updatedTs", "businessSwitchTransports")
                .containsExactlyInAnyOrder(
                    expectedBusinessA, expectedServiceA1, expectedServiceA2
                );
        });
    }

    @After
    public void cleanup() {
        mdmSupplierRepository.deleteAll();
        commit();
    }

    private MdmSupplier service(int id) {
        return new MdmSupplier()
            .setId(id)
            .setType(MdmSupplierType.THIRD_PARTY);
    }

    private MdmSupplier service(int id, int businessId) {
        return new MdmSupplier()
            .setId(id)
            .setBusinessId(businessId)
            .setType(MdmSupplierType.THIRD_PARTY);
    }

    private MdmSupplier business(int id) {
        return new MdmSupplier()
            .setId(id)
            .setType(MdmSupplierType.BUSINESS);
    }

    private List<MdmLogbrokerEvent<PartnerInfo.PartnerInfoEvent>> messages(PartnerInfo.PartnerInfoEvent... events) {
        List<MdmLogbrokerEvent<PartnerInfo.PartnerInfoEvent>> messages = new ArrayList<>();
        for (PartnerInfo.PartnerInfoEvent event : events) {
            messages.add(new MdmLogbrokerEvent<>(event));
        }
        return messages;
    }

    private PartnerInfo.PartnerInfoEvent serviceEvent(int id) {
        return PartnerInfo.PartnerInfoEvent.newBuilder()
            .setId(id)
            .setType(PartnerInfo.MbiPartnerType.SUPPLIER)
            .setSupplierType(PartnerInfo.SupplierType.THREE_P)
            .setUpdateType(PartnerInfo.UpdateType.IS_PUSH) // в протобуфке нет дефолта, возьмём рандомный ненужный
            .build();
    }

    private PartnerInfo.PartnerInfoEvent serviceEvent(int id, int businessId) {
        return PartnerInfo.PartnerInfoEvent.newBuilder()
            .setId(id)
            .setBusinessId(businessId)
            .setType(PartnerInfo.MbiPartnerType.SUPPLIER)
            .setSupplierType(PartnerInfo.SupplierType.THREE_P)
            .setUpdateType(PartnerInfo.UpdateType.IS_PUSH) // в протобуфке нет дефолта, возьмём рандомный ненужный
            .build();
    }

    private PartnerInfo.PartnerInfoEvent businessEvent(int id) {
        return PartnerInfo.PartnerInfoEvent.newBuilder()
            .setId(id)
            .setType(PartnerInfo.MbiPartnerType.BUSINESS)
            .setUpdateType(PartnerInfo.UpdateType.IS_PUSH) // в протобуфке нет дефолта, возьмём рандомный ненужный
            .build();
    }

    private PartnerInfo.PartnerInfoEvent businessChangeEvent(int id, int businessId) {
        return PartnerInfo.PartnerInfoEvent.newBuilder()
            .setId(id)
            .setBusinessId(businessId)
            .setType(PartnerInfo.MbiPartnerType.SUPPLIER)
            .setSupplierType(PartnerInfo.SupplierType.THREE_P)
            .setUpdateType(PartnerInfo.UpdateType.SERVICE_LINK)
            .build();
    }

    private PartnerInfo.PartnerInfoEvent stageChangeEvent(int id, int businessId, boolean isStage3) {
        return PartnerInfo.PartnerInfoEvent.newBuilder()
            .setId(id)
            .setBusinessId(businessId)
            .setType(PartnerInfo.MbiPartnerType.SUPPLIER)
            .setSupplierType(PartnerInfo.SupplierType.THREE_P)
            .setIsUnitedCatalog(isStage3)
            .setUpdateType(PartnerInfo.UpdateType.UNITED_CATALOG)
            .build();
    }

    private void commit() {
        jdbcTemplate.getJdbcTemplate().execute("commit");
    }
}
