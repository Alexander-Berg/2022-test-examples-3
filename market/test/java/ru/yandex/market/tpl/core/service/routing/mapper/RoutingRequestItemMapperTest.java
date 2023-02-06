package ru.yandex.market.tpl.core.service.routing.mapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturn;
import ru.yandex.market.tpl.core.external.routing.api.AdditionalTag;
import ru.yandex.market.tpl.core.external.routing.api.MultiClientReturn;
import ru.yandex.market.tpl.core.external.routing.api.RoutingRequestItem;
import ru.yandex.market.tpl.core.external.routing.api.RoutingRequestItemType;
import ru.yandex.market.tpl.core.service.routing.MultiClientReturnPackagerService;
import ru.yandex.market.tpl.core.test.TplAbstractTest;
import ru.yandex.market.tpl.core.test.factory.TestClientReturnFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.core.domain.order.TplOrderGenerateConstants.DEFAULT_DS_ID;
import static ru.yandex.market.tpl.core.service.routing.mapper.RoutingRequestItemMapper.DEFAULT_CLIENT_RETURN_ADDITIONAL_TIME_FOR_SURVEY;
import static ru.yandex.market.tpl.core.service.routing.mapper.RoutingRequestItemMapper.DEFAULT_CLIENT_RETURN_FASHION_ORDERS_COUNT;
import static ru.yandex.market.tpl.core.service.routing.mapper.RoutingRequestItemMapper.DEFAULT_CLIENT_RETURN_REGION_ID;

@RequiredArgsConstructor
class RoutingRequestItemMapperTest extends TplAbstractTest {
    public static final int DEPOT_ID = 123;
    private final RoutingRequestItemMapper routingRequestItemMapper;
    private final TestClientReturnFactory clientReturnFactory;
    private final MultiClientReturnPackagerService multiClientReturnPackagerService;

    private static final LocalDateTime ARRIVE_FROM = LocalDate.now().atStartOfDay();
    private static final LocalDateTime ARRIVE_TO = LocalDate.now().atStartOfDay().plusHours(4);
    private static final String PHONE = "+7999999999";
    private static final BigDecimal LAT = BigDecimal.valueOf(55.5);
    private static final BigDecimal LON = BigDecimal.valueOf(33.5);


    @Test
    void mapToClientReturnRoutingItems() {
        //given
        var mapperContext = RoutingRequestItemMapper.MultiClientReturnMapperContext.builder()
                .isUserShiftRoutingRequest(false)
                .depotId(String.valueOf(DEPOT_ID))
                .build();
        MultiClientReturn multiClientReturn = buildMultiClientReturn();

        //when
        RoutingRequestItem requestItem = routingRequestItemMapper.map(multiClientReturn, mapperContext);

        //then
        assertThat(requestItem).isNotNull();
        assertThat(requestItem.getDepotId()).isEqualTo(String.valueOf(mapperContext.getDepotId()));
        assertThat(requestItem.getType()).isEqualTo(RoutingRequestItemType.CLIENT_RETURN);
        assertThat(requestItem.getTaskId()).isNotBlank();
        assertThat(requestItem.getSubTaskCount()).isEqualTo(2);
        assertThat(requestItem.getAddress()).isNotNull();
        assertThat(requestItem.getAddress().getLatitude()).isEqualTo(LAT);
        assertThat(requestItem.getAddress().getLongitude()).isEqualTo(LON);
        assertThat(requestItem.getInterval()).isNotNull();
        assertThat(requestItem.getRef()).isNotNull();
        assertThat(requestItem.getTags()).isEmpty();
        assertThat(requestItem.getAdditionalTags()).contains(AdditionalTag.CLIENT_RETURN.getCode());
        assertThat(requestItem.getVolume()).isEqualTo(BigDecimal.valueOf(33));
        assertThat(requestItem.isExcludedFromLocationGroups()).isEqualTo(multiClientReturn.isPartOfHugeMultiItem());
        assertThat(requestItem.getDimensionsClass()).isNotNull();
        assertThat(requestItem.getRegionId()).isEqualTo(DEFAULT_CLIENT_RETURN_REGION_ID);
        assertThat(requestItem.getAdditionalTimeForSurvey())
                .isEqualTo(DEFAULT_CLIENT_RETURN_ADDITIONAL_TIME_FOR_SURVEY);
        assertThat(requestItem.getFashionOrdersCount()).isEqualTo(DEFAULT_CLIENT_RETURN_FASHION_ORDERS_COUNT);
        assertThat(requestItem.isUserShiftRoutingRequest()).isEqualTo(mapperContext.isUserShiftRoutingRequest());
    }

    private MultiClientReturn buildMultiClientReturn() {
        List<TestClientReturnFactory.ItemDimensions> itemDimensions1 = List.of(
                TestClientReturnFactory.ItemDimensions.of(BigDecimal.ONE, 1, 2, 3));
        List<TestClientReturnFactory.ItemDimensions> itemDimensions2 = List.of(
                TestClientReturnFactory.ItemDimensions.of(BigDecimal.ONE, 3, 3, 3));

        ClientReturn clientReturnAlreadyOnCourier = clientReturnFactory.buildAndSave(DEFAULT_DS_ID, ARRIVE_FROM,
                ARRIVE_TO, PHONE, LAT, LON, itemDimensions1);
        ClientReturn clientReturnNotAssigned = clientReturnFactory.buildAndSave(DEFAULT_DS_ID, ARRIVE_FROM,
                ARRIVE_TO, PHONE, LAT, LON, itemDimensions2);


        var multiList = multiClientReturnPackagerService.pack(List.of(clientReturnAlreadyOnCourier,
                clientReturnNotAssigned));
        assertThat(multiList).hasSize(1);
        return multiList.iterator().next();
    }


}
