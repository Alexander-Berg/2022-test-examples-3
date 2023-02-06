package ru.yandex.market.logistics.management.controller;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.repository.DeliveryIntervalRepository;
import ru.yandex.market.logistics.management.repository.PartnerCapacityRepository;
import ru.yandex.market.logistics.management.repository.PartnerMarketIdStatusRepository;
import ru.yandex.market.logistics.management.repository.PartnerRelationRepository;
import ru.yandex.market.logistics.management.repository.PartnerRepository;
import ru.yandex.market.logistics.management.repository.PartnerRouteRepository;
import ru.yandex.market.logistics.management.repository.PartnerTariffRepository;
import ru.yandex.market.logistics.management.repository.PlatformClientPartnerRepository;
import ru.yandex.market.logistics.management.repository.PutReferenceWarehouseInDeliveryStatusRepository;
import ru.yandex.market.logistics.management.repository.combinator.LogisticSegmentRepository;
import ru.yandex.market.logistics.management.repository.combinator.LogisticSegmentServiceRepository;

import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DatabaseSetup(
    value = "/data/controller/partner/partnerCleaner/prepare_data.xml",
    connection = "dbUnitQualifiedDatabaseConnection"
)
class PartnerCleanerControllerTest extends AbstractContextualTest {

    @Autowired
    LogisticSegmentRepository logisticSegmentRepository;
    @Autowired
    PartnerRepository partnerRepository;
    @Autowired
    DeliveryIntervalRepository deliveryIntervalRepository;
    @Autowired
    LogisticSegmentServiceRepository logisticSegmentServiceRepository;
    @Autowired
    PartnerMarketIdStatusRepository partnerMarketIdStatusRepository;
    @Autowired
    PartnerCapacityRepository partnerCapacityRepository;
    @Autowired
    PartnerRelationRepository partnerRelationRepository;
    @Autowired
    PartnerRouteRepository partnerRouteRepository;
    @Autowired
    PlatformClientPartnerRepository platformClientPartnerRepository;
    @Autowired
    PartnerTariffRepository partnerTariffRepository;
    @Autowired
    PutReferenceWarehouseInDeliveryStatusRepository putReferenceWarehouseInDeliveryStatusRepository;

    @Test
    @DisplayName("Удаление партнеров")
    void delete() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/deletePartners")
            .contentType(MediaType.APPLICATION_JSON)
            .content("[1]"))
            .andExpect(status().isOk());

        softly.assertThat(partnerRepository.findById(1L)).isEmpty();
        softly.assertThat(partnerMarketIdStatusRepository.findAll().size()).isEqualTo(2);
        softly.assertThat(partnerRelationRepository.findAll().size()).isEqualTo(1);
        softly.assertThat(putReferenceWarehouseInDeliveryStatusRepository.findAll().size()).isEqualTo(1);

        //check cascade deletions
        softly.assertThat(logisticSegmentRepository.findAll().size()).isEqualTo(1);
        softly.assertThat(logisticSegmentServiceRepository.findAll().size()).isEqualTo(1);
        softly.assertThat(partnerCapacityRepository.findAll().size()).isEqualTo(2);
        softly.assertThat(partnerRouteRepository.findAll().size()).isEqualTo(1);
        softly.assertThat(platformClientPartnerRepository.findAll().size()).isEqualTo(3);
    }
}
