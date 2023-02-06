package ru.yandex.market.tpl.core.service.lms.deliveryservice;

import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import ru.yandex.market.logistics.front.library.dto.grid.GridData;
import ru.yandex.market.logistics.front.library.dto.grid.GridItem;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.core.domain.lms.deliveryservice.LmsDeliveryServiceCreateDto;
import ru.yandex.market.tpl.core.domain.lms.deliveryservice.LmsDeliveryServiceFilterDto;
import ru.yandex.market.tpl.core.domain.lms.deliveryservice.LmsDeliveryServiceUpdateDto;
import ru.yandex.market.tpl.core.domain.partner.DeliveryService;
import ru.yandex.market.tpl.core.domain.partner.PartnerRepository;
import ru.yandex.market.tpl.core.domain.partner.SortingCenter;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RequiredArgsConstructor
class LmsDeliveryServiceFacadeTest extends TplAbstractTest {

    private static final long EXPECTED_DELIVERY_AREA_MARGIN_WIDTH = 5000L;
    private final PartnerRepository<DeliveryService> partnerRepository;
    private final DbQueueTestUtil dbQueueTestUtil;

    private final LmsDeliveryServiceFacade service;

    private final LMSClient lmsClient;

    @Test
    void getDeliveryServices() {
        DeliveryService deliveryService1 = partnerRepository.findByIdOrThrow(DeliveryService.DEFAULT_DS_ID);
        DeliveryService deliveryService2 = partnerRepository.findByIdOrThrow(DeliveryService.FAKE_DS_ID);

        GridData unfiltered = service.getDeliveryServices(null, Pageable.unpaged());

        // в ликвибейзе есть миграции автоматически добавляющие СД...
        assertThat(unfiltered.getTotalCount()).isEqualTo(6);

        GridData filteredById = service.getDeliveryServices(
                LmsDeliveryServiceFilterDto.builder()
                        .deliveryServiceId(deliveryService1.getId())
                        .build(),
                Pageable.unpaged()
        );

        assertThat(filteredById.getItems())
                .extracting(GridItem::getId)
                .containsExactly(deliveryService1.getId());

        GridData filteredByName = service.getDeliveryServices(
                LmsDeliveryServiceFilterDto.builder()
                        .name("" + deliveryService2.getName())
                        .build(),
                Pageable.unpaged()
        );

        assertThat(filteredByName.getItems())
                .extracting(GridItem::getId)
                .containsExactly(deliveryService2.getId());

        GridData filteredBySortingCenterId = service.getDeliveryServices(
                LmsDeliveryServiceFilterDto.builder()
                        .sortingCenterId(SortingCenter.DEFAULT_SC_ID)
                        .build(),
                Pageable.unpaged()
        );

        assertThat(filteredBySortingCenterId.getItems())
                .hasSize(2);
    }

    @Test
    void updateDeliveryService() {
        //given
        String expectedToken = "test_token";
        String expectedName = "SuperDS";
        List<String> expectedKkt = List.of("12345678901234", "12345678901234");

        DeliveryService deliveryService = partnerRepository.findByIdOrThrow(DeliveryService.DEFAULT_DS_ID);

        assertThat(deliveryService.getToken()).isNotEqualTo(expectedToken);
        assertThat(deliveryService.getName()).isNotEqualTo(expectedName);
        assertThat(deliveryService.getKkt()).isNullOrEmpty();
        assertThat(deliveryService.getDeliveryAreaMarginWidth()).isEqualTo(0);

        //when
        service.updateDeliveryService(
                deliveryService.getId(),
                LmsDeliveryServiceUpdateDto.builder()
                        .token(expectedToken)
                        .name(expectedName)
                        .kkt(String.join(",", expectedKkt))
                        .deliveryAreaMarginWidth(EXPECTED_DELIVERY_AREA_MARGIN_WIDTH)
                        .build()
        );

        //then
        deliveryService = partnerRepository.findByIdOrThrow(DeliveryService.DEFAULT_DS_ID);
        assertThat(deliveryService.getToken()).isEqualTo(expectedToken);
        assertThat(deliveryService.getName()).isEqualTo(expectedName);
        assertThat(deliveryService.getDeliveryAreaMarginWidth()).isEqualTo(EXPECTED_DELIVERY_AREA_MARGIN_WIDTH);
        assertThat(deliveryService.getKkt()).isNotEmpty().containsAll(expectedKkt);
    }

    @Test
    void createDeliveryService() {
        //given
        long deliveryServiceId = 123123;

        when(lmsClient.getPartner(deliveryServiceId))
                .thenReturn(Optional.of(PartnerResponse.newBuilder().partnerType(PartnerType.DELIVERY).build()));

        dbQueueTestUtil.assertQueueHasSize(QueueType.CREATE_API_SETTINGS, 0);

        Optional<DeliveryService> optional = partnerRepository.findById(deliveryServiceId);

        assertThat(optional).isEmpty();

        String expectedName = "SuperDS";
        List<String> expectedKkt = List.of("12345678901234", "12345678901234");

        //when
        service.createDeliveryService(
                LmsDeliveryServiceCreateDto.builder()
                        .deliveryServiceId(deliveryServiceId)
                        .name(expectedName)
                        .kkt(String.join(",", expectedKkt))
                        .deliveryAreaMarginWidth(EXPECTED_DELIVERY_AREA_MARGIN_WIDTH)
                        .build()
        );

        //then
        DeliveryService deliveryService = partnerRepository.findByIdOrThrow(deliveryServiceId);

        assertThat(deliveryService.getToken()).isNotEmpty();
        assertThat(deliveryService.getName()).isEqualTo(expectedName);
        assertThat(deliveryService.getKkt()).isNotEmpty().containsAll(expectedKkt);
        assertThat(deliveryService.getDeliveryAreaMarginWidth()).isEqualTo(EXPECTED_DELIVERY_AREA_MARGIN_WIDTH);

        dbQueueTestUtil.assertQueueHasSize(QueueType.CREATE_API_SETTINGS, 1);
    }
}
