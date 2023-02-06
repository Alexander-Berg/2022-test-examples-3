package ru.yandex.market.tpl.core.external.routing.api;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturn;
import ru.yandex.market.tpl.core.domain.logistic_request.LogisticRequestPoint;
import ru.yandex.market.tpl.core.domain.usershift.value.RoutePointAddress;
import ru.yandex.market.tpl.core.test.factory.TestClientReturnFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.core.test.factory.TestClientReturnFactory.DEFAULT_TEST_DIMENSION_CLASS;


class MultiClientReturnTest {

    public static final String BUILDING = "building";
    public static final String HOUSE = "house";
    public static final BigDecimal LATITUDE = BigDecimal.ONE;
    public static final BigDecimal LONGITUDE = BigDecimal.TEN;
    public static final String HOUSING = "housing";

    @Test
    void getRoutingTaskId_whenSeveralItems() {
        //given
        ClientReturn clientReturn1 = TestClientReturnFactory.buildMock(1L);
        ClientReturn clientReturn2 = TestClientReturnFactory.buildMock(10L);
        ClientReturn clientReturn3 = TestClientReturnFactory.buildMock(5L);


        //then
        assertThat(MultiClientReturn.builder()
                .items(List.of(clientReturn1, clientReturn2, clientReturn3))
                .build()
                .getRoutingTaskId()
        ).isEqualTo("cr_1_5_10");
    }

    @Test
    void getRoutingTaskId_whenOneItem() {
        //given
        ClientReturn clientReturn1 = TestClientReturnFactory.buildMock(1L);

        //then
        assertThat(MultiClientReturn.builder()
                .items(List.of(clientReturn1))
                .build()
                .getRoutingTaskId()
        ).isEqualTo("cr_1");
    }

    @Test
    void getRoutingRef() {
        //given
        ClientReturn clientReturn1 = TestClientReturnFactory.buildMock(1L, "external1");
        ClientReturn clientReturn2 = TestClientReturnFactory.buildMock(2L, "external2");

        //then
        assertThat(MultiClientReturn.builder()
                .items(List.of(clientReturn1, clientReturn2))
                .build()
                .getRoutingRef()
        ).isEqualTo("cr_external1_external2");
    }

    @Test
    void getVolumeInCubicMeters() {
        //given
        ClientReturn clientReturn1 = TestClientReturnFactory.buildMock(List.of(BigDecimal.ONE, BigDecimal.TEN));
        ClientReturn clientReturn2 = TestClientReturnFactory.buildMock(List.of(BigDecimal.TEN, BigDecimal.TEN));
        ClientReturn clientReturn3 = TestClientReturnFactory.buildMock(List.of(BigDecimal.ONE, BigDecimal.ONE,
                BigDecimal.ONE));

        //then
        assertThat(MultiClientReturn.builder()
                .items(List.of(clientReturn1, clientReturn2, clientReturn3))
                .build()
                .getVolumeInCubicMeters()
        ).isEqualTo(BigDecimal.valueOf(34));
    }

    @Test
    void getDimensionsClass() {
        //given
        ClientReturn clientReturn1 = TestClientReturnFactory.buildMock(List.of(BigDecimal.ONE, BigDecimal.TEN));
        ClientReturn clientReturn2 = TestClientReturnFactory.buildMock(List.of(BigDecimal.TEN, BigDecimal.TEN));


        //then
        assertThat(MultiClientReturn.builder()
                .items(List.of(clientReturn1, clientReturn2))
                .build()
                .getDimensionsClass()
        ).isEqualTo(DEFAULT_TEST_DIMENSION_CLASS);
    }

    @Test
    void getAddress() {
        //given
        var logisticPoint = LogisticRequestPoint.builder()
                .house(HOUSE)
                .building(BUILDING)
                .housing(HOUSING)
                .originalLatitude(LATITUDE)
                .originalLongitude(LONGITUDE)
                .buildWithAdress();

        ClientReturn clientReturn = TestClientReturnFactory.buildMock(logisticPoint);

        //when
        RoutePointAddress address = MultiClientReturn.builder()
                .items(List.of(clientReturn))
                .build()
                .getAddress();

        //then
        assertThat(address).isNotNull();
        assertThat(address.getBuilding()).isEqualTo(BUILDING);
        assertThat(address.getHouse()).isEqualTo(HOUSE);
        assertThat(address.getHousing()).isEqualTo(HOUSING);
        assertThat(address.getLatitude()).isEqualTo(LATITUDE);
        assertThat(address.getLongitude()).isEqualTo(LONGITUDE);
    }
}
