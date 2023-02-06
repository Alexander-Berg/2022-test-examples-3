package ru.yandex.market.tpl.tms.service.clientretrun.address;

import java.math.BigDecimal;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturn;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnGenerator;
import ru.yandex.market.tpl.core.domain.clientreturn.dbqueue.address.ClarifyClientReturnAddressPayload;
import ru.yandex.market.tpl.core.domain.clientreturn.repository.ClientReturnRepository;
import ru.yandex.market.tpl.core.domain.order.Dimensions;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.address.AddressGenerator;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPoint;
import ru.yandex.market.tpl.tms.service.clientreturn.address.ClarifyClientReturnAddressProcessingService;
import ru.yandex.market.tpl.tms.test.TplTmsAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;


@RequiredArgsConstructor
public class ClarifyClientReturnAddressProcessingServiceTest extends TplTmsAbstractTest {

    private static final Long buyerYandexUid = 123L;

    private final ClientReturnGenerator clientReturnGenerator;

    private final ClarifyClientReturnAddressProcessingService clarifyClientReturnAddressProcessingService;

    private final DbQueueTestUtil dbQueueTestUtil;

    private final ClientReturnRepository clientReturnRepository;

    private final OrderGenerateService orderGenerateService;

    @Test
    void callTrackerWhenZeroCoordinates() {

        //given
        ClientReturn clientReturn =
                clientReturnGenerator.createClientReturn(
                        ClientReturnGenerator.ClientReturnGenerateParam
                                .builder()
                                .itemCount(10L)
                                .dimensions(new Dimensions(BigDecimal.valueOf(10), 10, 10, 10))
                                .addressGenerateParam(
                                        AddressGenerator.AddressGenerateParam.builder().geoPoint(
                                                GeoPoint.ofLatLon(BigDecimal.ZERO, BigDecimal.ZERO)
                                        ).build())
                                .build());

        //when
        clarifyClientReturnAddressProcessingService.processPayload(
                new ClarifyClientReturnAddressPayload(
                        "requestId",
                        clientReturn.getId()
                )
        );

        //then
        dbQueueTestUtil.assertQueueHasSize(QueueType.CLIENT_RETURN_ZERO_COORDINATES, 1);

    }

    @Test
    void tryUpdateZeroCoordinatesByAddress() {

        //given
        AddressGenerator.AddressGenerateParam.AddressGenerateParamBuilder equalsAddress =
                AddressGenerator.AddressGenerateParam.builder()
                        .city("Балашиха")
                        .street("Мира")
                        .house("18");

        double latitude = 55.806786;
        double longitude = 37.464592;

        var orderWithCoordinates = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .addressGenerateParam(equalsAddress.geoPoint(GeoPoint.ofLatLon(latitude, longitude)).build())
                        .buyerYandexUid(buyerYandexUid)
                        .build()
        );

        ClientReturn clientReturn =
                clientReturnGenerator.createClientReturn(
                        ClientReturnGenerator.ClientReturnGenerateParam
                                .builder()
                                .itemCount(10L)
                                .dimensions(new Dimensions(BigDecimal.valueOf(10), 10, 10, 10))
                                .addressGenerateParam(
                                        equalsAddress.geoPoint(GeoPoint.ofLatLon(BigDecimal.ZERO, BigDecimal.ZERO))
                                                .build())
                                .build());

        //when
        clarifyClientReturnAddressProcessingService.processPayload(
                new ClarifyClientReturnAddressPayload(
                        "requestId",
                        clientReturn.getId()
                )
        );

        //then
        ClientReturn updatedClientReturn = clientReturnRepository.findByIdOrThrow(clientReturn.getId());

        assertThat(updatedClientReturn.getLogisticRequestPointFrom().getGeoPoint())
                .isEqualTo(orderWithCoordinates.getDelivery().getDeliveryAddress().getGeoPoint());
    }


}
