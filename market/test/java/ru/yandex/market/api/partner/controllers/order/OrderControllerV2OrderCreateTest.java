package ru.yandex.market.api.partner.controllers.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import Market.DataCamp.SyncAPI.OffersBatch;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import ru.yandex.market.api.partner.client.orderservice.PapiOrderServiceClient;
import ru.yandex.market.api.partner.context.FunctionalTest;
import ru.yandex.market.api.partner.context.FunctionalTestHelper;
import ru.yandex.market.common.retrofit.CommonRetrofitHttpExecutionException;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.ProtoTestUtil;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.mbi.util.MbiAsserts;
import ru.yandex.market.orderservice.client.model.BuyerDto;
import ru.yandex.market.orderservice.client.model.CreateExternalOrderResponse;
import ru.yandex.market.orderservice.client.model.CreateExternalOrderResponseResult;
import ru.yandex.market.orderservice.client.model.CurrencyValue;
import ru.yandex.market.orderservice.client.model.DeliveryOptionDto;
import ru.yandex.market.orderservice.client.model.DeliveryOptionsDto;
import ru.yandex.market.orderservice.client.model.DeliveryType;
import ru.yandex.market.orderservice.client.model.GetDeliveryOptionsResponse;
import ru.yandex.market.orderservice.client.model.GetPartnerOrderPiResponse;
import ru.yandex.market.orderservice.client.model.OrderAddressDto;
import ru.yandex.market.orderservice.client.model.OrderStatus;
import ru.yandex.market.orderservice.client.model.OrderSubStatus;
import ru.yandex.market.orderservice.client.model.PartnerDetailedOrderPiDto;
import ru.yandex.market.orderservice.client.model.PartnerOrderDeliveryPiDto;
import ru.yandex.market.orderservice.client.model.PaymentMethod;
import ru.yandex.market.orderservice.client.model.PaymentType;
import ru.yandex.market.personal_market.PersonalAddress;
import ru.yandex.market.personal_market.PersonalMarketService;
import ru.yandex.market.personal_market.PersonalRetrieveRequestBuilder;
import ru.yandex.market.personal_market.PersonalRetrieveResponse;
import ru.yandex.market.personal_market.PersonalStoreRequestBuilder;
import ru.yandex.market.personal_market.PersonalStoreResponse;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static ru.yandex.market.api.partner.controllers.order.config.OrderControllerV2Config.ENV_USE_PERSONAL_CREATE_ORDER;
import static ru.yandex.market.api.partner.controllers.order.config.OrderControllerV2Config.ENV_USE_PERSONAL_GET_ORDER;

@DbUnitDataSet(before = "create/OrderControllerV2OrderCreateTest.before.csv")
class OrderControllerV2OrderCreateTest extends FunctionalTest implements ResourceUtilitiesMixin {
    private static final Logger LOG = LoggerFactory.getLogger(OrderControllerV2OrderCreateTest.class);

    private static final long BUSINESS_ID = 100L;
    private static final long PARTNER_ID = 668L;
    private static final long ORDER_ID = 123456L;

    @Autowired
    private PapiOrderServiceClient papiOrderServiceClient;

    @Autowired
    @Qualifier("dataCampShopClient")
    private DataCampClient dataCampClient;

    @Autowired
    private PersonalMarketService personalMarketService;

    @Autowired
    private EnvironmentService environmentService;

    @ParameterizedTest(name = "use personal = " + ParameterizedTest.ARGUMENTS_PLACEHOLDER)
    @ValueSource(booleans = {true, false})
    void createOrderJson(boolean usePersonal) {
        environmentService.setValue(ENV_USE_PERSONAL_CREATE_ORDER, Boolean.toString(usePersonal));
        environmentService.setValue(ENV_USE_PERSONAL_GET_ORDER, Boolean.toString(usePersonal));
        prepareCreateOrderMocks();

        String orderCreateRequestJson = resourceAsString(
                "create/OrderControllerV2OrderCreateTest.orderCreateRequest.json"
        );

        try {
            ResponseEntity<String> response = FunctionalTestHelper.makeRequestWithContentType(
                    urlBasePrefix + "/v2/campaigns/10668/orders.json",
                    HttpMethod.POST,
                    orderCreateRequestJson,
                    String.class,
                    MediaType.APPLICATION_JSON);

            MbiAsserts.assertJsonEquals(
                    resourceAsString("create/OrderControllerV2OrderCreateTest.orderCreateResponse.json"),
                    response.getBody()
            );

        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            LOG.error(ex + ", response: " + ex.getResponseBodyAsString());
            throw (ex);
        }
    }

    @ParameterizedTest(name = "use personal = {0}")
    @ValueSource(booleans = {true, false})
    void createOrderXml(boolean usePersonal) {
        environmentService.setValue(ENV_USE_PERSONAL_CREATE_ORDER, Boolean.toString(usePersonal));
        prepareCreateOrderMocks();

        String orderCreateRequestXml = resourceAsString(
                "create/OrderControllerV2OrderCreateTest.orderCreateRequest.xml"
        );

        try {
            ResponseEntity<String> response = FunctionalTestHelper.makeRequestWithContentType(
                    urlBasePrefix + "/v2/campaigns/10668/orders.xml",
                    HttpMethod.POST,
                    orderCreateRequestXml,
                    String.class,
                    MediaType.APPLICATION_XML);

            MbiAsserts.assertXmlEquals(
                    resourceAsString("create/OrderControllerV2OrderCreateTest.orderCreateResponse.xml"),
                    response.getBody()
            );
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            LOG.error(ex + ", response: " + ex.getResponseBodyAsString());
            throw (ex);
        }
    }

    @Test
    void createOrderGetDeliveryOptionsError() {
        mockGetOffersResponse();
        when(papiOrderServiceClient.getDeliveryOptions(eq(PARTNER_ID), any()))
                .thenReturn(CompletableFuture.failedFuture(
                        createOsException(
                                HttpStatus.NOT_FOUND,
                                "create/OrderControllerV2OrderCreateTest.addressNotFoundError.json"
                        )
                ));
        mockCreateOderResponse();
        mockGetOderResponse();

        String orderCreateRequestJson = resourceAsString(
                "create/OrderControllerV2OrderCreateTest.orderCreateRequest.json"
        );

        var ex = assertThrows(HttpClientErrorException.class, () ->
                FunctionalTestHelper.makeRequestWithContentType(
                        urlBasePrefix + "/v2/campaigns/10668/orders.json",
                        HttpMethod.POST,
                        orderCreateRequestJson,
                        String.class,
                        MediaType.APPLICATION_JSON)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @ParameterizedTest(name = "use personal = " + ParameterizedTest.ARGUMENTS_PLACEHOLDER)
    @ValueSource(booleans = {true, false})
    void createOrderWithNullDelivery(boolean usePersonal) {
        environmentService.setValue(ENV_USE_PERSONAL_CREATE_ORDER, Boolean.toString(usePersonal));
        environmentService.setValue(ENV_USE_PERSONAL_GET_ORDER, Boolean.toString(usePersonal));
        prepareCreateOrderMocks();

        String orderCreateRequestJson = resourceAsString(
                "create/OrderControllerV2OrderCreateTest.createOrderWithNullDelivery.json"
        );

        var error = assertThrows(HttpClientErrorException.class, () ->
                FunctionalTestHelper.makeRequestWithContentType(
                        urlBasePrefix + "/v2/campaigns/10668/orders.json",
                        HttpMethod.POST,
                        orderCreateRequestJson,
                        String.class,
                        MediaType.APPLICATION_JSON
                )
        );

        assertThat(error.getStatusCode(), equalTo(HttpStatus.BAD_REQUEST));
    }

    private Throwable createOsException(HttpStatus status, String errorBodyResource) {
        return new CommonRetrofitHttpExecutionException(
                status.getReasonPhrase(),
                status.value(),
                null,
                resourceAsString(errorBodyResource)
        );
    }

    private void prepareCreateOrderMocks() {
        mockGetOffersResponse();
        mockDeliveryOptionsResponse();
        mockStorePersonalData();
        mockCreateOderResponse();
        mockGetOderResponse();
    }

    private void mockStorePersonalData() {
        when(personalMarketService.store(new PersonalStoreRequestBuilder()
                .fullName("Иван", "Иванов", "Иванович")
                .phone("+71234567890")
                .email("ivanov.ivan@yandex.ru")
                .address(PersonalAddress.builder()
                        .withCountry("Россия")
                        .withCity("Москва")
                        .withSubway("Проспект Вернадского")
                        .withStreet("Ленинский проспект")
                        .withHouse("90")
                        .withBlock("2")
                        .withFloor("6")
                        .withApartment("289")
                        .withEntrance("10")
                        .withPostcode("119313")
                        .build())
        )).thenReturn(CompletableFuture.completedFuture(
                PersonalStoreResponse.builder()
                        .fullNameId("some_fullname_id")
                        .phoneId("some_phone_id")
                        .emailId("some_email_id")
                        .addressId("some_address_id")
                        .build()
        ));
    }

    private void mockGetOderResponse() {
        PartnerDetailedOrderPiDto osOrder = (PartnerDetailedOrderPiDto) new PartnerDetailedOrderPiDto()
                .buyer(new BuyerDto()
                        .firstName("Иван")
                        .middleName("Иванович")
                        .lastName("Иванов")
                        .personalFullNameId("some_fullname_id")
                        .email("ivanov.ivan@yandex.ru")
                        .personalEmailId("some_email_id")
                        .phone("+71234567890")
                        .personalPhoneId("some_phone_id"))
                .orderId(ORDER_ID)
                .status(OrderStatus.DELIVERY)
                .substatus(OrderSubStatus.SHIPPED)
                .createdAt(LocalDateTime.parse("2011-12-03T10:15:30")
                        .atZone(ZoneId.systemDefault())
                        .toOffsetDateTime())
                .itemsTotal(new CurrencyValue().value(BigDecimal.valueOf(1500L)))
                .subsidyTotal(new CurrencyValue().value(BigDecimal.ZERO))
                .paymentType(PaymentType.POSTPAID)
                .paymentMethod(PaymentMethod.YANDEX)
                .lines(List.of())
                .deliveryInfo(new PartnerOrderDeliveryPiDto()
                        .address(new OrderAddressDto()
                                .country("Россия")
                                .postcode("119313")
                                .city("Москва")
                                .subway("Проспект Вернадского")
                                .street("Ленинский проспект")
                                .house("90")
                                .floor("6")
                                .personalAddressId("some_address_id")
                        )
                        .deliveryType(DeliveryType.DELIVERY));

        when(papiOrderServiceClient.getPartnerOrderPI(eq(PARTNER_ID), eq(ORDER_ID)))
                .thenReturn(CompletableFuture.completedFuture(
                        new GetPartnerOrderPiResponse()
                                .result(osOrder)
                ));

        when(personalMarketService.retrieve(new PersonalRetrieveRequestBuilder()
                .fullName("some_fullname_id")
                .phone("some_phone_id")
                .email("some_email_id")
                .address("some_address_id")
        )).thenReturn(CompletableFuture.completedFuture(
                PersonalRetrieveResponse.builder()
                        .fullName("some_fullname_id", "Иван", "Иванов", "Иванович")
                        .phone("some_phone_id", "+71234567890")
                        .email("some_email_id", "ivanov.ivan@yandex.ru")
                        .address("some_address_id", PersonalAddress.builder()
                                .withCountry("Россия")
                                .withCity("Москва")
                                .withSubway("Проспект Вернадского")
                                .withStreet("Ленинский проспект")
                                .withHouse("90")
                                .withBlock("2")
                                .withFloor("6")
                                .withApartment("289")
                                .withEntrance("10")
                                .withPostcode("119313")
                                .build())
                        .build()
        ));
    }

    private void mockDeliveryOptionsResponse() {
        DeliveryOptionDto deliveryOption = new DeliveryOptionDto();
        when(papiOrderServiceClient.getDeliveryOptions(eq(PARTNER_ID), any()))
                .thenReturn(CompletableFuture.completedFuture(
                        new GetDeliveryOptionsResponse()
                                .result(new DeliveryOptionsDto().addOptionsItem(deliveryOption))
                ));
    }

    @SuppressWarnings("ConstantConditions")
    private void mockCreateOderResponse() {
        when(papiOrderServiceClient.createExternalOrder(eq(PARTNER_ID), any()))
                .thenReturn(CompletableFuture.completedFuture(
                        new CreateExternalOrderResponse()
                                .result(new CreateExternalOrderResponseResult().orderId(ORDER_ID))
                ));
    }

    private void mockGetOffersResponse() {
        var offersResponse = ProtoTestUtil.getProtoMessageByJson(
                OffersBatch.UnitedOffersBatchResponse.class,
                "create/OrderControllerV2OrderCreateTest.UnitedOffersBatchResponse.json",
                getClass()
        );
        when(dataCampClient.getBusinessUnitedOffers(eq(BUSINESS_ID), eq(Set.of("0516465165")), eq(PARTNER_ID)))
                .thenReturn(offersResponse);
    }
}
