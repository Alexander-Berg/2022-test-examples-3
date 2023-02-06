package ru.yandex.market.logistics.nesu.controller.settings;

import java.util.List;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.nesu.jobs.producer.ModifierUploadTaskProducer;
import ru.yandex.market.logistics.nesu.model.LmsFactory;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static ru.yandex.market.logistics.nesu.model.LmsFactory.createPartner;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Создание задач на загрузку модификаторов в КД после обновления настроек")
@DatabaseSetup({
    "/repository/settings/sender/before/logistics_point_availabilities_dataset.xml",
    "/repository/settings/sender/before/common_sender.xml"
})
class SettingsControllerUpdateModifierUploadTest extends AbstractSettingsControllerTest {

    @Autowired
    private ModifierUploadTaskProducer producer;

    @BeforeEach
    void setup() {
        doNothing().when(producer).produceTask(anyLong());
        mockAvailableWarehousesAndPartners();
        mockSortingCenterRelation();
    }

    @Test
    @DisplayName("Задача при изменении активности СД для сендера")
    void produceDeliveryServiceActivityChange() throws Exception {
        verifyProducerOnUpdate("controller/settings/sender/add_new_delivery_to_region.json", 1);
    }

    @Test
    @DisplayName("Добавление неактивной СД")
    void notProduceAddDisabledDelivery() throws Exception {
        verifyProducerOnUpdate("controller/settings/sender/add_disabled_delivery.json", 0);
    }

    @Test
    @DisplayName("Отсутствие задачи при обновлении настроек, если активность СД для сендера не изменилась")
    @DatabaseSetup("/repository/settings/sender/before/common_region_settings.xml")
    void notProduceDeliveryServiceActivityNotChanged() throws Exception {
        verifyProducerOnUpdate("controller/settings/sender/add_new_delivery_to_region.json", 0);
    }

    @Test
    @DisplayName("Деактивация СД при существующих настройках")
    @DatabaseSetup("/repository/settings/sender/before/common_region_settings.xml")
    void produceOnDeactivateExistingDelivery() throws Exception {
        verifyProducerOnUpdate("controller/settings/sender/add_disabled_delivery.json", 1);
    }

    @Test
    @DisplayName("Добавление способов доставки")
    void produceOnDeliveryTypesUpdate() throws Exception {
        verifyProducerOnUpdate("controller/settings/sender/set_delivery_types.json", 1);
    }

    @Test
    @DisplayName("Изменение набора способов доставки, активность не изменилась")
    @DatabaseSetup("/repository/settings/sender/before/common_region_settings.xml")
    void dontProduceOnDeliveryTypesPartialUpdate() throws Exception {
        verifyProducerOnUpdate("controller/settings/sender/set_delivery_types.json", 0);
    }

    @Test
    @DisplayName("Деактивация СД через набор способов доставки")
    @DatabaseSetup("/repository/settings/sender/before/common_region_settings.xml")
    void produceOnDeliveryTypesRemove() throws Exception {
        verifyProducerOnUpdate("controller/settings/sender/remove_delivery_types.json", 1);
    }

    private void verifyProducerOnUpdate(String requestPath, int times) throws Exception {
        mockGetDeliveries();
        mockMvc.perform(
            post("/back-office/settings/sender/delivery")
                .param("userId", "1")
                .param("shopId", "1")
                .param("senderId", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent(requestPath))
        )
            .andExpect(status().isOk());

        verify(producer, times(times)).produceTask(1L);
    }

    private void mockGetDeliveries() {
        SearchPartnerFilter filter = LmsFactory.createPartnerFilter(
            null,
            Set.of(PartnerStatus.ACTIVE),
            Set.of(PartnerType.DELIVERY, PartnerType.OWN_DELIVERY)
        );
        PartnerResponse firstValid = createPartner(1L, PartnerType.DELIVERY);
        PartnerResponse secondValid = createPartner(2L, PartnerType.DELIVERY);
        PartnerResponse thirdValid = createPartner(8L, PartnerType.DELIVERY);
        when(lmsClient.searchPartners(filter))
            .thenAnswer(invocation -> List.of(firstValid, secondValid, thirdValid));
    }

}
