package ru.yandex.market.tpl.carrier.lms.service.deliveryservice;

import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.jdbc.Sql;

import ru.yandex.market.logistics.front.library.dto.grid.GridData;
import ru.yandex.market.logistics.front.library.dto.grid.GridItem;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.tpl.carrier.core.audit.CarrierAuditTracer;
import ru.yandex.market.tpl.carrier.core.audit.CarrierSource;
import ru.yandex.market.tpl.carrier.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.carrier.core.domain.partner.DeliveryService;
import ru.yandex.market.tpl.carrier.core.domain.partner.PartnerRepository;
import ru.yandex.market.tpl.carrier.lms.service.LmsServiceTest;
import ru.yandex.market.tpl.carrier.planner.lms.deliveryservice.LmsDeliveryServiceCreateDto;
import ru.yandex.market.tpl.carrier.planner.lms.deliveryservice.LmsDeliveryServiceFilterDto;
import ru.yandex.market.tpl.carrier.planner.lms.deliveryservice.LmsDeliveryServiceUpdateDto;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@Sql(value = "classpath:mockPartner/defaultDeliveryServices.sql")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class LmsDeliveryServiceFacadeTest extends LmsServiceTest {

    static {
        CarrierAuditTracer.putSource(CarrierSource.SYSTEM);
    }

    private static final long EXPECTED_DELIVERY_AREA_MARGIN_WIDTH = 5000L;
    private final PartnerRepository<DeliveryService> partnerRepository;
    private final DbQueueTestUtil dbQueueTestUtil;

    private final LmsDeliveryServiceFacade service;

    private final LMSClient lmsClient;

    @Test
    void getDeliveryServices() {
        DeliveryService deliveryService1 = partnerRepository.findByIdOrThrow(DeliveryService.DEFAULT_DS_ID);
        DeliveryService deliveryService2 = partnerRepository.findByIdOrThrow(239L);

        GridData unfiltered = service.getDeliveryServices(null, Pageable.unpaged());

        // в ликвибейзе есть миграции автоматически добавляющие СД...
        assertThat(unfiltered.getTotalCount()).isEqualTo(2);

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

        //when
        service.updateDeliveryService(
                deliveryService.getId(),
                LmsDeliveryServiceUpdateDto.builder()
                        .token(expectedToken)
                        .name(expectedName)
                        .build()
        );

        //then
        deliveryService = partnerRepository.findByIdOrThrow(DeliveryService.DEFAULT_DS_ID);
        assertThat(deliveryService.getToken()).isEqualTo(expectedToken);
        assertThat(deliveryService.getName()).isEqualTo(expectedName);
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
                        .build()
        );

        //then
        DeliveryService deliveryService = partnerRepository.findByIdOrThrow(deliveryServiceId);

        assertThat(deliveryService.getToken()).isNotEmpty();
        assertThat(deliveryService.getName()).isEqualTo(expectedName);

        dbQueueTestUtil.assertQueueHasSize(QueueType.CREATE_API_SETTINGS, 1);
    }
}
