package ru.yandex.market.partner.mvc.controller.delivery;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import io.grpc.stub.StreamObserver;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.core.matchers.HttpClientErrorMatcher;
import ru.yandex.market.id.GetByRegistrationNumberRequest;
import ru.yandex.market.id.GetByRegistrationNumberResponse;
import ru.yandex.market.id.MarketIdServiceGrpc;
import ru.yandex.market.logistics.nesu.client.NesuClient;
import ru.yandex.market.logistics.nesu.client.model.NamedEntity;
import ru.yandex.market.logistics.nesu.client.model.ShopWithSendersDto;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@DbUnitDataSet(before = "csv/YaDeliverySettingsControllerFunctionalTest.before.csv")
class YaDeliverySettingsControllerFunctionalTest extends FunctionalTest {

    @Autowired
    private MarketIdServiceGrpc.MarketIdServiceImplBase marketIdServiceImplBase;

    @Autowired
    private NesuClient nesuClient;

    @BeforeEach
    void before() {
        doAnswer(invocation -> {
            StreamObserver<GetByRegistrationNumberResponse> responseObserver = invocation.getArgument(1);
            responseObserver.onNext(
                    GetByRegistrationNumberResponse
                            .newBuilder()
                            .setSuccess(true)
                            .setMarketId(
                                    Long.parseLong(
                                            ((GetByRegistrationNumberRequest) invocation.getArgument(0)).getRegistrationNumber()
                                    )
                            )
                            .build());
            responseObserver.onCompleted();
            return null;
        }).when(marketIdServiceImplBase).getByRegistrationNumber(any(), any());
    }

    /**
     * У клиента один магазин в МО на белом и один в доставке.
     * У магазина ранее не была ранее настроена Яндекс.Доставка.
     */
    @Test
    @DbUnitDataSet(before = "csv/getAvailableSendersTestOneShopCase.before.csv")
    void getAvailableSendersOneShopCaseTest() {
        when(nesuClient.getActiveShopsByMarketId(Collections.singleton(1000000000000L)))
                .thenReturn(Collections.singletonList(shopWithOneActiveSender()));

        ResponseEntity<String> response = getAvailableSendersRequest(2020);
        checkValidResponse(response, "json/available-senders-one-shop-case.json");
    }

    private ShopWithSendersDto shopWithOneActiveSender() {
        return ShopWithSendersDto.builder()
                .id(1L)
                .name("Shop with one sender")
                .senders(Collections.singletonList(
                        NamedEntity.builder()
                                .id(1L)
                                .name("First sender")
                                .build()
                ))
                .build();
    }

    /**
     * У клиента один магазин в ЛО на белом и три в доставке (в двух кабинетах).
     * У магазина ранее была включена и настроена Яндекс.Доставка.
     */
    @Test
    @DbUnitDataSet(before = "csv/getAvailableSendersTestManyShopsCase.before.csv")
    void getAvailableSendersManyShopsCaseTest() {
        when(nesuClient.getActiveShopsByMarketId(Collections.singleton(1000000000001L)))
                .thenReturn(twoShopWithThreeActiveSenders());

        ResponseEntity<String> response = getAvailableSendersRequest(2021);
        checkValidResponse(response, "json/available-senders-many-shops-case-with-saved-settings.json");
    }

    private List<ShopWithSendersDto> twoShopWithThreeActiveSenders() {
        return Arrays.asList(
                ShopWithSendersDto.builder()
                        .id(1L)
                        .name("First shop with one sender")
                        .senders(Collections.singletonList(
                                NamedEntity.builder()
                                        .id(1L)
                                        .name("First sender")
                                        .build()
                        ))
                        .build(),
                ShopWithSendersDto.builder()
                        .id(2L)
                        .name("Second shop with two senders")
                        .senders(Arrays.asList(
                                NamedEntity.builder()
                                        .id(2L)
                                        .name("Second sender")
                                        .build(),
                                NamedEntity.builder()
                                        .id(3L)
                                        .name("Third sender")
                                        .build()
                        ))
                        .build()
        );
    }

    /**
     * У клиента один магазин в ЛО на белом и нет магазинов в доставке.
     */
    @Test
    @DbUnitDataSet(before = "csv/getAvailableSendersTestZeroSendersCase.before.csv")
    void getAvailableSendersZeroSendersCaseTest() {
        when(nesuClient.getActiveShopsByMarketId(Collections.singleton(1000000000002L)))
                .thenReturn(Collections.emptyList());

        ResponseEntity<String> response = getAvailableSendersRequest(2022);
        checkValidResponse(response, "json/no-available-senders.json");
    }

    /**
     * У клиента один магазин не в МО/ЛО на белом.
     */
    @Test
    @DbUnitDataSet(before = "csv/getAvailableSendersTestNotValidRegionCase.before.csv")
    void getAvailableSendersNotValidRegionCaseTest() {
        ResponseEntity<String> response = getAvailableSendersRequest(2023);
        checkValidResponse(response, "json/no-available-senders.json");
    }

    /**
     * У клиента четыре магазина на белом и по их market-id есть магазины в доставке.
     */
    @Test
    @DbUnitDataSet(before = "csv/getAvailableSendersTestBusinessHasSeveralShopsCase.before.csv")
    void getAvailableSendersBusinessHasSeveralShopsWithShopsInYaDoCaseTest() {
        when(nesuClient.getActiveShopsByMarketId(new HashSet<>(Arrays.asList(1111000000004L, 1000000000005L, 1111000000006L))))
                .thenReturn(twoShopWithThreeActiveSenders());

        ResponseEntity<String> response = getAvailableSendersRequest(2024);
        checkValidResponse(response, "json/available-senders-many-shops-case.json");
    }

    /**
     * У клиента четыре магазина на белом и по их market-id нет магазинов в доставке.
     */
    @Test
    @DbUnitDataSet(before = "csv/getAvailableSendersTestBusinessHasSeveralShopsCase.before.csv")
    void getAvailableSendersClientHasSeveralShopsCaseTest() {
        when(nesuClient.getActiveShopsByMarketId(new HashSet<>(Arrays.asList(1111000000004L, 1000000000005L, 1111000000006L))))
                .thenReturn(Collections.emptyList());

        ResponseEntity<String> response = getAvailableSendersRequest(2024);
        checkValidResponse(response, "json/no-available-senders.json");
    }

    /**
     * Тест проверяет, что недонастроенные кабинеты (без сендеров) не возвращаются.
     */
    @Test
    @DbUnitDataSet(before = "csv/filterCabinetWithoutSendersTest.before.csv")
    void filterCabinetWithoutSendersTest() {
        when(nesuClient.getActiveShopsByMarketId(Collections.singleton(1000000000005L)))
                .thenReturn(shopsWithEmptySendersList());

        ResponseEntity<String> response = getAvailableSendersRequest(2025);
        checkValidResponse(response, "json/available-cabinets-without-empty-senders-list.json");
    }

    private List<ShopWithSendersDto> shopsWithEmptySendersList() {
        return Arrays.asList(
                ShopWithSendersDto.builder()
                        .id(1L)
                        .name("First shop with two senders")
                        .senders(Arrays.asList(
                                NamedEntity.builder()
                                        .id(1L)
                                        .name("First sender")
                                        .build(),
                                NamedEntity.builder()
                                        .id(2L)
                                        .name("Second sender")
                                        .build()
                        ))
                        .build(),
                ShopWithSendersDto.builder()
                        .id(2L)
                        .name("Second shop with empty senders list")
                        .senders(Collections.emptyList())
                        .build(),
                ShopWithSendersDto.builder()
                        .id(3L)
                        .name("Third shop with one sender")
                        .senders(Collections.singletonList(
                                NamedEntity.builder()
                                        .id(3L)
                                        .name("Third sender")
                                        .build()
                        ))
                        .build(),
                ShopWithSendersDto.builder()
                        .id(4L)
                        .name("Fourth shop with empty senders list")
                        .senders(Collections.emptyList())
                        .build()
        );
    }

    /**
     * Первое сохранение настроек оплаты магазина.
     */
    @Test
    @DbUnitDataSet(
            before = "csv/createNewYaDeliverySettingsWithNewRecordTest.before.csv",
            after = "csv/createNewYaDeliverySettingsWithNewRecordTest.after.csv"
    )
    void createNewYaDeliverySettingsWithNewRecordTest() {
        ResponseEntity<String> response = editYaDeliverySettingsRequest(2020L, "json/create-new-ya-delivery-settings.json");
        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    /**
     * Первое сохранение настроек Яндекс.Доставки магазина, но до этого уже были сохранены некоторые настройки оплаты.
     */
    @Test
    @DbUnitDataSet(
            before = "csv/createNewYaDeliverySettingsInExistingRecordTest.before.csv",
            after = "csv/createNewYaDeliverySettingsInExistingRecordTest.after.csv"
    )
    void createNewYaDeliverySettingsInExistingRecordTest() {
        ResponseEntity<String> response = editYaDeliverySettingsRequest(2021L, "json/create-new-ya-delivery-settings.json");
        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    /**
     * Обновление настроек Яндекс.Доставки магазина.
     */
    @Test
    @DbUnitDataSet(
            before = "csv/updateYaDeliverySettingsTest.before.csv",
            after = "csv/updateYaDeliverySettingsTest.after.csv"
    )
    void updateYaDeliverySettingsTest() {
        ResponseEntity<String> response = editYaDeliverySettingsRequest(2022L, "json/update-ya-delivery-settings.json");
        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    /**
     * Выключение настроек Яндекс.Доставки магазином.
     */
    @Test
    @DbUnitDataSet(
            before = "csv/turnOffYaDeliverySettingsTest.before.csv",
            after = "csv/turnOffYaDeliverySettingsTest.after.csv"
    )
    void turnOffYaDeliverySettingsTest() {
        ResponseEntity<String> response = editYaDeliverySettingsRequest(2023L, "json/turn-off-ya-delivery-settings.json");
        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    /**
     * Некорректные настройки - чекбокс включен, однако senderId отсутствует.
     */
    @Test
    @DbUnitDataSet(before = "csv/incorrectSettingsTest.before.csv")
    void incorrectSettingsTest() {
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> editYaDeliverySettingsRequest(2024L, "json/incorrect-yado-settings.json")
        );
        MatcherAssert.assertThat(exception, HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST));
    }

    private ResponseEntity<String> getAvailableSendersRequest(long campaignId) {
        return FunctionalTestHelper.get(baseUrl + "/shops/ya-delivery-settings?id=" + campaignId);
    }

    private ResponseEntity<String> editYaDeliverySettingsRequest(long campaignId, String requestFilename) {
        HttpEntity request = JsonTestUtil.getJsonHttpEntity(getClass(), requestFilename);
        return FunctionalTestHelper.put(baseUrl + "/shops/ya-delivery-settings?id=" + campaignId, request);
    }

    private void checkValidResponse(ResponseEntity<String> response, String expectedResponseFilename) {
        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonTestUtil.assertEquals(response, getClass(), expectedResponseFilename);
    }
}
