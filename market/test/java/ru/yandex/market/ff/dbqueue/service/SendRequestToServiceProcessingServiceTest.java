package ru.yandex.market.ff.dbqueue.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EntityNotFoundException;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseSetups;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.google.common.collect.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.api.cpa.yam.dto.OrganizationInfoDTO;
import ru.yandex.market.api.cpa.yam.dto.PrepayRequestDTO;
import ru.yandex.market.core.orginfo.model.OrganizationType;
import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.client.enums.RequestStatus;
import ru.yandex.market.ff.client.enums.RequestType;
import ru.yandex.market.ff.model.converter.BaseLgwClientConverter;
import ru.yandex.market.ff.model.dbqueue.SendRequestToServicePayload;
import ru.yandex.market.ff.model.entity.ShopRequest;
import ru.yandex.market.ff.service.RequestSubTypeService;
import ru.yandex.market.logistic.gateway.client.exceptions.GatewayApiException;
import ru.yandex.market.logistic.gateway.common.model.common.Car;
import ru.yandex.market.logistic.gateway.common.model.common.DateTimeInterval;
import ru.yandex.market.logistic.gateway.common.model.common.Location;
import ru.yandex.market.logistic.gateway.common.model.common.LogisticPoint;
import ru.yandex.market.logistic.gateway.common.model.common.OutboundType;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.common.Party;
import ru.yandex.market.logistic.gateway.common.model.common.request.restricted.PutInboundRestrictedData;
import ru.yandex.market.logistic.gateway.common.model.common.request.restricted.PutOutboundRestrictedData;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Address;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Barcode;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.CargoType;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.CisHandleMode;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Consignment;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Contractor;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Courier;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Inbound;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Item;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.LegalEntity;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.LegalForm;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Outbound;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Person;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Phone;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Service;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ServiceType;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.StockType;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Transfer;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.UnitId;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

public class SendRequestToServiceProcessingServiceTest extends IntegrationTest {

    private static final Long SUPPLIER_ID = 100L;
    private static final Long RETURN_SUPPLIER_ID = 200L;
    private static final String DATE_1 = "2017-10-10T09:09:09";
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private SendRequestToServiceProcessingService service;
    @Autowired
    private  RequestSubTypeService requestSubTypeService;
    private static final Long NOT_VALID_ID = 0L;

    @BeforeEach
    void init() {
        final PrepayRequestDTO prepayRequest = new PrepayRequestDTO();
        final OrganizationInfoDTO organizationInfo = createOrganizationInfo();

        prepayRequest.setOrganizationInfo(organizationInfo);
        when(mbiApiClient.getPrepayRequest(anyLong(), anyLong()))
                .thenReturn(prepayRequest);
    }

    @Test
    public void processPayloadWithNotValidId() throws EntityNotFoundException {
        SendRequestToServicePayload payload = new SendRequestToServicePayload(NOT_VALID_ID);
        Throwable thrown = assertThrows(EntityNotFoundException.class, () -> service.processPayload(payload));
        assertions.assertThat(thrown.getMessage())
                .contains("Request[id=" + NOT_VALID_ID + "] hasn't been found.");
    }

    @Test
    @DatabaseSetup("classpath:tms/send-requests-to-service/before.xml")
    public void processPayloadWithNotValidStatus() throws IllegalStateException {
        SendRequestToServicePayload payload = new SendRequestToServicePayload(3);
        Throwable thrown = assertThrows(IllegalStateException.class, () -> service.processPayload(payload));
        assertions.assertThat(thrown.getMessage())
                .contains("Request[id=3] is in a status other than VALIDATED.");
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:params/use-market-name.xml"),
            @DatabaseSetup("classpath:tms/send-requests-to-service/before.xml")})
    @ExpectedDatabase(value = "classpath:tms/send-requests-to-service/after.xml", assertionMode = NON_STRICT_UNORDERED)
    void testJobUseMarketNameModeOn() throws GatewayApiException {
        processAllRequests();
        verifyPutInbound();
        verifyOutbound();
        verifyTransfer();
        verifyExpendableMaterialsSupply(true);
    }

    @Test
    @DatabaseSetup("classpath:tms/send-requests-to-service/before.xml")
    @ExpectedDatabase(value = "classpath:tms/send-requests-to-service/after.xml", assertionMode = NON_STRICT_UNORDERED)
    void testJobUseMarketNameModeOff() throws GatewayApiException {
        processAllRequests();
        verifyPutInbound();
        verifyOutbound();
        verifyTransfer();
        verifyExpendableMaterialsSupply(false);
    }

    @Test
    @DatabaseSetup("classpath:tms/send-requests-to-service/before-utilization-transfer.xml")
    @ExpectedDatabase(value = "classpath:tms/send-requests-to-service/after-utilization-transfer.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void testJobSendingUtilizationTransfer() throws GatewayApiException {
        processAllRequests();
        verifyTransfer();
    }

    @Test
    @DatabaseSetup(value = {"classpath:tms/send-requests-to-service/before-orders-requests.xml",
            "classpath:tms/send-requests-to-service/couriers-before.xml"})
    @ExpectedDatabase(
            value = "classpath:tms/send-requests-to-service/after-orders-requests.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void testSendingOrdersRequestsToPartners() throws GatewayApiException {
        transactionTemplate.execute(status -> {
            processAllRequests();
            return null;
        });
        verifyOrdersSupplyToFF();
        verifyOrdersWithdrawToFF();
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:params/use-market-name.xml"),
            @DatabaseSetup("classpath:tms/send-requests-to-service/before.xml")})
    @ExpectedDatabase(value = "classpath:tms/send-requests-to-service/after-put-inbound.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void supplyPutInboundRegistry() throws GatewayApiException {
        environmentParamService.setParam("supply-put-inbound-date-param",
                List.of(LocalDateTime.MIN.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
        processAllRequests();
        Mockito.verify(fulfillmentClient, Mockito.times(3)).putInbound(
                Mockito.any(),
                Mockito.any(),
                Mockito.any(),
                Mockito.isNull()
        );
    }

    @Transactional
    @Test
    @DatabaseSetup("classpath:tms/withdraw-with-registry-to-service/before.xml")
    @ExpectedDatabase(value = "classpath:tms/withdraw-with-registry-to-service/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void withdrawWithRegistryPutOutbound() throws GatewayApiException {
        processAllRequests();
        Mockito.verify(fulfillmentClient, Mockito.times(1)).putOutbound(
                Mockito.any(),
                Mockito.any(),
                Mockito.any(),
                Mockito.any()
        );
        Mockito.verify(fulfillmentClient, Mockito.times(1)).createOutbound(
                Mockito.any(),
                Mockito.any()
        );
    }

    private void verifyOrdersSupplyToFF() throws GatewayApiException {
        var inbound = createInbound(
                "1",
                "10000904648",
                "5303-003",
                "10000925131",
                "1",
                "234124",
                "3"
        );
        Mockito.verify(fulfillmentClient).putInbound(
                inbound,
                new Partner(10L),
                PutInboundRestrictedData.builder().setTransportationId("TMT10000").build(),
                null
        );
    }

    private void verifyOrdersWithdrawToFF() throws GatewayApiException {
        var outbound = createOutbound("3", "10000904648", "5303-003");
        Mockito.verify(fulfillmentClient).putOutbound(
                outbound,
                new Partner(10L),
                PutOutboundRestrictedData.builder().setTransportationId("TMT12345").build(),
                null);
    }

    private ru.yandex.market.logistic.gateway.common.model.common.Courier createCourier() {
        return ru.yandex.market.logistic.gateway.common.model.common.Courier.builder()
                .setPartnerId(createResourceIdInNewFormat("106", "107"))
                .setPersons(Collections.singletonList(createPerson()))
                .setCar(createCar())
                .setPhone(createPhone())
                .setLegalEntity(createLegalEntity())
                .build();
    }

    private ru.yandex.market.logistic.gateway.common.model.common.Person createPerson() {
        return ru.yandex.market.logistic.gateway.common.model.common.Person
                .builder("Олег")
                .setSurname("Егоров")
                .setPatronymic("Васильевич")
                .build();
    }

    private Car createCar() {
        return Car.builder("О123НО790").setDescription("Белый форд транзит").build();
    }

    private ru.yandex.market.logistic.gateway.common.model.common.Phone createPhone() {
        return ru.yandex.market.logistic.gateway.common.model.common.Phone
                .builder("+78005553535")
                .setAdditional("88005553535").build();
    }

    private ru.yandex.market.logistic.gateway.common.model.common.LegalEntity createLegalEntity() {
        return ru.yandex.market.logistic.gateway.common.model.common.LegalEntity.builder()
                .setName("ООО Синтез РУС")
                .setLegalName("ООО Синтез РУС")
                .setLegalForm(ru.yandex.market.logistic.gateway.common.model.common.LegalForm.OOO)
                .setOgrn("1000000000000")
                .setInn("7777777777")
                .setKpp("555555555")
                .setAddress(createLocation())
                .setBank("Сбербанк")
                .setAccount("1234000005678")
                .setBik("4444444")
                .setCorrespondentAccount("987600001111")
                .build();
    }

    private ru.yandex.market.logistic.gateway.common.model.common.Address createLocation() {
        return ru.yandex.market.logistic.gateway.common.model.common.Address
                .builder("123456, Россия, Москва, Москва и Московская область, Центральный федеральный округ," +
                        " Городской округ, Поселение, 9-я Северная линия, 23, 1, 2, 98, Подъезд 1, Этаж -1," +
                        " Метро Дмитровская, Координаты 55.733957,37.588274, Домофон B98")
                .setStructuredAddress(Location.builder("Россия", "Москва и Московская область", "Москва")
                        .setFederalDistrict("Центральный федеральный округ")
                        .setSubRegion("Городской округ")
                        .setSettlement("Поселение")
                        .setStreet("9-я Северная линия")
                        .setHouse("23")
                        .setBuilding("1")
                        .setHousing("2")
                        .setRoom("98")
                        .setZipCode("123456")
                        .setPorch("1")
                        .setFloor(-1)
                        .setMetro("Дмитровская")
                        .setLat(BigDecimal.valueOf(55.733957))
                        .setLng(BigDecimal.valueOf(37.588274))
                        .setLocationId(213L)
                        .setIntercom("B98")
                        .build())
                .build();
    }

    private ru.yandex.market.logistic.gateway.common.model.common.Inbound createInbound(
            String inboundId,
            String yandexLogPointId,
            String partnerLogPointId,
            String shipperPointId,
            String shipperPartnerId,
            String shipperPointExternalId,
            String outboundId
    ) {

        Location location = Location.builder("Россия", "Москва", "Московский")
                .setLocationId(1111L)
                .setSettlement("Москва")
                .setStreet("Большая Лубянка")
                .setHouse("2")
                .setZipCode("101000")
                .setLat(BigDecimal.valueOf(55.4531))
                .setLng(BigDecimal.valueOf(37.3741))
                .build();

        var logisticsPointId = createResourceIdInNewFormat(yandexLogPointId, partnerLogPointId);
        LogisticPoint logisticPoint = LogisticPoint.builder(logisticsPointId)
                .setLocation(location)
                .build();

        var inboundBuilder = ru.yandex.market.logistic.gateway.common.model.common.Inbound.builder(
                        createResourceIdInNewFormat(inboundId, null),
                        ru.yandex.market.logistic.gateway.common.model.common.InboundType.DS_SC,
                        DateTimeInterval.fromFormattedValue("2018-01-05T10:00:00/2018-01-05T10:00:00")
                )
                .setComment("postavishe")
                .setLogisticPoint(logisticPoint)
                .setCourier(createCourier())
                .setShipper(createConsignor(shipperPartnerId))
                .setNextReceiver(createParty("10000925131", "1", "234124"))
                .setOutboundIds(List.of(createResourceIdInNewFormat(outboundId, null)));

        return inboundBuilder.build();
    }

    private Party createConsignor(String shipperPartnerId) {
        return Party.builder(LogisticPoint.builder(
                                ru.yandex.market.logistic.gateway.common.model.common.ResourceId.builder()
                                        .setYandexId("10000904648").setPartnerId("10").build())
                        .build())
                .setPartnerId(ru.yandex.market.logistic.gateway.common.model.common.ResourceId.builder()
                        .setYandexId(shipperPartnerId)
                        .build())
                .setLegalEntity(ru.yandex.market.logistic.gateway.common.model.common.LegalEntity.builder()
                        .setName("Legal Name")
                        .setLegalName("Legal Name")
                        .setAddress(ru.yandex.market.logistic.gateway.common.model.common.Address
                                .builder("Legal Address").build())
                        .build())
                .build();
    }


    private ru.yandex.market.logistic.gateway.common.model.common.ResourceId
    createResourceIdInNewFormat(String yandexId, String partnerId) {
        return ru.yandex.market.logistic.gateway.common.model.common.ResourceId.builder()
                .setYandexId(yandexId)
                .setPartnerId(partnerId)
                .build();
    }

    private ru.yandex.market.logistic.gateway.common.model.common.Outbound createOutbound(String inboundId,
                                                                                          String yandexLogPointId,
                                                                                          String partnerLogPointId) {

        Location location = Location.builder("Россия", "Москва", "Московский")
                .setLocationId(1111L)
                .setSettlement("Москва")
                .setStreet("Большая Лубянка")
                .setHouse("2")
                .setZipCode("101000")
                .setLat(BigDecimal.valueOf(55.4531))
                .setLng(BigDecimal.valueOf(37.3741))
                .build();

        var logisticsPointId = createResourceIdInNewFormat(yandexLogPointId, partnerLogPointId);
        LogisticPoint logisticPoint = LogisticPoint.builder(logisticsPointId)
                .setLocation(location)
                .build();

        var outboundBuilder = ru.yandex.market.logistic.gateway.common.model.common.Outbound.builder(
                createResourceIdInNewFormat(inboundId, null),
                DateTimeInterval.fromFormattedValue("2018-01-05T10:00:00/2018-01-05T10:00:00"));
        outboundBuilder.setOutboundType(OutboundType.DS_SC);
        outboundBuilder.setComment("postavishe");
        outboundBuilder.setLogisticPoint(logisticPoint);
        outboundBuilder.setCourier(createCourier());
        outboundBuilder.setReceiver(createParty("10000925131", "1", "234124"));

        return outboundBuilder.build();
    }
    private void verifyInbound(boolean useMarketName) throws GatewayApiException {
        // Проверка отправки первой поставки.
        Inbound inbound = inbound("1",
                ru.yandex.market.logistic.gateway.common.model.fulfillment.InboundType.DEFAULT, "2017-01-01",
                "2017-01-01", "test",
                getConsignments(
                        "1",
                        unitId("1", "SHOPSKU1"),
                        Collections.unmodifiableList(Lists.newArrayList(
                                new Barcode("11", null, null),
                                new Barcode("22", null, null))),
                        new BigDecimal("50.50"),
                        useMarketName ? "market_name1" : "offer_1",
                        3,
                        List.of(CargoType.ART),
                        1,
                        true,
                        useMarketName ? Collections.singletonList("marketVendorCode1")
                                : Collections.singletonList("vendorCode1"),
                        10,
                        Collections.emptyList(),
                        List.of(String.format(BaseLgwClientConverter.MARKET_URL_TEMPLATE, 1)),
                        true
                ));

        Mockito.verify(fulfillmentClient).createInbound(inbound, new Partner(10L));

        // Проверка отправки второй поставки.
        final List<Consignment> consignments = getConsignments(
                "2",
                unitId("2", "SHOPSKU2"),
                Collections.singletonList(new Barcode("barcode1", null, null)),
                new BigDecimal("99.99"),
                useMarketName ? "market_name2" : "offer_2",
                3,
                List.of(CargoType.ART),
                1,
                false,
                useMarketName
                        ? Arrays.asList("marketVendorCode1", "marketVendorCode2")
                        : Collections.singletonList("vendorCode2"),
                null,
                useMarketName ? Collections.singletonList(service()) : Collections.emptyList(),
                List.of(String.format(BaseLgwClientConverter.MARKET_URL_TEMPLATE, 2)),
                false
        );
        inbound = inbound("2", ru.yandex.market.logistic.gateway.common.model.fulfillment.InboundType.DEFAULT,
                DATE_1, DATE_1, null, consignments);
        Mockito.verify(fulfillmentClient).createInbound(inbound, new Partner(1L));
        // проверка того, что правильно проставится сервис для 3P поставщиков
        inbound = inbound("6", ru.yandex.market.logistic.gateway.common.model.fulfillment.InboundType.DEFAULT,
                "2017-01-01", "2017-01-01", "test",
                getConsignments(
                        "6",
                        unitId("6", RETURN_SUPPLIER_ID, "SHOPSKU6"),
                        Collections.unmodifiableList(Lists.newArrayList(
                                new Barcode("11", null, null),
                                new Barcode("22", null, null))),
                        new BigDecimal("50.50"),
                        useMarketName ? "market_name6" : "offer_6",
                        3,
                        List.of(CargoType.CONSUMPTION_GOODS),
                        1,
                        true,
                        useMarketName ?
                                Collections.singletonList("marketVendorCode6")
                                : Collections.singletonList("vendorCode6"),
                        10,
                        Collections.emptyList(),
                        List.of(String.format(BaseLgwClientConverter.MARKET_URL_TEMPLATE, 6)),
                        null
                ));
        Mockito.verify(fulfillmentClient).createInbound(inbound, new Partner(10L));

        // Проверка отправки перепоставки (возврата).
        inbound = inbound("4", ru.yandex.market.logistic.gateway.common.model.fulfillment.InboundType.RETURN,
                DATE_1, DATE_1, null,
                getConsignments(
                        "4",
                        unitId("1", "SHOPSKU1"),
                        Collections.unmodifiableList(Lists.newArrayList(
                                new Barcode("11", null, null),
                                new Barcode("22", null, null))),
                        new BigDecimal("50.50"),
                        useMarketName ? "market_name1" : "offer_1",
                        1,
                        List.of(CargoType.JEWELRY, CargoType.VALUABLE),
                        1,
                        true,
                        useMarketName ?
                                Collections.singletonList("marketVendorCode1")
                                : Collections.singletonList("vendorCode1"),
                        null,
                        Collections.emptyList(),
                        List.of(String.format(BaseLgwClientConverter.MARKET_URL_TEMPLATE, 1)),
                        null
                ));
        Mockito.verify(fulfillmentClient).createInbound(inbound, new Partner(1L));
        // Проверям, что STORE_DEFECTIVE_ITEMS_SEPARATELY не проставится первому консайменту
        inbound = inbound("7", ru.yandex.market.logistic.gateway.common.model.fulfillment.InboundType.RETURN,
                DATE_1, DATE_1, null,
                Collections.unmodifiableList(
                        Arrays.asList(
                                getConsignment(
                                        "7",
                                        unitId("1", RETURN_SUPPLIER_ID, "SHOPSKU1"),
                                        Collections.unmodifiableList(Lists.newArrayList(
                                                new Barcode("11", null, null),
                                                new Barcode("22", null, null))),
                                        new BigDecimal("50.50"),
                                        useMarketName ? "market_name1" : "offer_1",
                                        1,
                                        List.of(CargoType.SMALL_GOODS),
                                        1,
                                        true,
                                        useMarketName ?
                                                Collections.singletonList("marketVendorCode1")
                                                : Collections.singletonList("vendorCode1"),
                                        null,
                                        Collections.emptyList(),
                                        List.of(String.format(BaseLgwClientConverter.MARKET_URL_TEMPLATE, 1)),
                                        null
                                ),
                                getConsignment(
                                        "7",
                                        unitId("1", SUPPLIER_ID, "SHOPSKU1"),
                                        Collections.unmodifiableList(Lists.newArrayList(
                                                new Barcode("11", null, null),
                                                new Barcode("22", null, null))),
                                        new BigDecimal("50.50"),
                                        useMarketName ? "market_name1" : "offer_1",
                                        1,
                                        List.of(CargoType.SMALL_GOODS),
                                        1,
                                        true,
                                        useMarketName ?
                                                Collections.singletonList("marketVendorCode1")
                                                : Collections.singletonList("vendorCode1"),
                                        null,
                                        Collections.emptyList(),
                                        List.of(String.format(BaseLgwClientConverter.MARKET_URL_TEMPLATE, 1)),
                                        null
                                )
                        )
                ));
        Mockito.verify(fulfillmentClient).createInbound(inbound, new Partner(1L));
    }

    private void verifyPutInbound() throws GatewayApiException {
        Mockito.verify(fulfillmentClient).putInbound(Mockito.argThat(arg ->
                arg.getInboundType() == ru.yandex.market.logistic.gateway.common.model.common.InboundType.DEFAULT &&
                        arg.getInboundId().getYandexId().equals("1") &&
                        arg.getComment().equals("test") &&
                        arg.getInterval()
                                .equals(DateTimeInterval.fromFormattedValue("2017-01-01T00:00:00/2017-01-01T00:00:00"))
        ), Mockito.argThat(arg -> arg.getId() == 10L), any(), any());

        Mockito.verify(fulfillmentClient).putInbound(Mockito.argThat(arg ->
                arg.getInboundType() == ru.yandex.market.logistic.gateway.common.model.common.InboundType.DEFAULT &&
                        arg.getInboundId().getYandexId().equals("2") &&
                        arg.getComment() == null &&
                        arg.getInterval()
                                .equals(DateTimeInterval.fromFormattedValue("2017-10-10T09:09:09/2017-10-10T09:09:09"))
        ), Mockito.argThat(arg -> arg.getId() == 1L), any(), any());

        Mockito.verify(fulfillmentClient).putInbound(Mockito.argThat(arg ->
                arg.getInboundType() == ru.yandex.market.logistic.gateway.common.model.common.InboundType.DEFAULT &&
                        arg.getInboundId().getYandexId().equals("6") &&
                        arg.getComment().equals("test") &&
                        arg.getInterval()
                                .equals(DateTimeInterval.fromFormattedValue("2017-01-01T00:00:00/2017-01-01T00:00:00"))
        ), Mockito.argThat(arg -> arg.getId() == 10L), any(), any());
    }

    private void verifyOutbound() throws GatewayApiException {
        // Проверка отправки изъятия.
        final Outbound outbound = outbound("5", DATE_1, "SORT_ORDER_1",
                getConsignments(
                        "5",
                        unitId("1", "SHOPSKU1"),
                        Collections.unmodifiableList(Lists.newArrayList(
                                new Barcode("11", null, null),
                                new Barcode("22", null, null))),
                        new BigDecimal("50.50"),
                        "offer_1",
                        2,
                        List.of(CargoType.CONSUMPTION_GOODS),
                        2,
                        false,
                        null,
                        null,
                        Collections.emptyList(),
                        List.of(String.format(BaseLgwClientConverter.MARKET_URL_TEMPLATE, 1)),
                        null
                ));

        // Проверка того, что для изъятия не проставляется сервис STORE_DEFECTIVE_ITEMS_SEPARATELY,
        // даже если это 3P поставщик
        final Outbound anotherOutbound = outbound("8", DATE_1, "SORT_ORDER_2",
                getConsignments(
                        "8",
                        unitId("1", RETURN_SUPPLIER_ID, "SHOPSKU1"),
                        Collections.unmodifiableList(Lists.newArrayList(
                                new Barcode("11", null, null),
                                new Barcode("22", null, null))),
                        new BigDecimal("50.50"),
                        "offer_1",
                        2,
                        List.of(CargoType.TECH_AND_ELECTRONICS),
                        2,
                        false,
                        null,
                        null,
                        Collections.emptyList(),
                        List.of(String.format(BaseLgwClientConverter.MARKET_URL_TEMPLATE, 1)),
                        null
                ));

        //Если включены параметры ignoreItemsWithError и withdrawAllWithLimit,
        //при отправке отфильтруются айтемы с ошибками и нулевым количеством.
        final Outbound outboundWithEnabledFlags = outbound("10", DATE_1, "SORT_ORDER_1",
                getConsignments(
                        "10",
                        unitId("1", "SHOPSKU23"),
                        Collections.unmodifiableList(Lists.newArrayList(
                                new Barcode("11", null, null),
                                new Barcode("22", null, null))),
                        new BigDecimal("50.50"),
                        "offer_1",
                        2,
                        List.of(CargoType.VALUABLE),
                        2,
                        false,
                        null,
                        null,
                        Collections.emptyList(),
                        List.of(String.format(BaseLgwClientConverter.MARKET_URL_TEMPLATE, 1)),
                        null
                ));

        ArgumentCaptor<Outbound> captor = ArgumentCaptor.forClass(Outbound.class);

        Mockito.verify(fulfillmentClient, times(3)).createOutbound(captor.capture(), eq(new Partner(1L)));

        List<Outbound> outbounds = captor.getAllValues().stream()
                .sorted(Comparator.comparing(Outbound::getComment))
                .collect(Collectors.toList());
        assertThat(outbounds.get(0), samePropertyValuesAs(outbound));
        assertThat(outbounds.get(2), samePropertyValuesAs(anotherOutbound));
        assertThat(outbounds.get(1), samePropertyValuesAs(outboundWithEnabledFlags));
    }

    private void verifyOutboundWithEnabledFlags() throws GatewayApiException {
        // Проверка отправки изъятия.
        final Outbound outbound = outbound("5", DATE_1, "SORT_ORDER_1",
                getConsignments(
                        "5",
                        unitId("1", "SHOPSKU1"),
                        Collections.unmodifiableList(Lists.newArrayList(
                                new Barcode("11", null, null),
                                new Barcode("22", null, null))),
                        new BigDecimal("50.50"),
                        "offer_1",
                        2,
                        List.of(CargoType.CONSUMPTION_GOODS),
                        2,
                        false,
                        null,
                        null,
                        Collections.emptyList(),
                        List.of(String.format(BaseLgwClientConverter.MARKET_URL_TEMPLATE, 1)),
                        null
                ));

        ArgumentCaptor<Outbound> captor = ArgumentCaptor.forClass(Outbound.class);
        Mockito.verify(fulfillmentClient, times(1)).createOutbound(captor.capture(), eq(new Partner(1L)));

        List<Outbound> outbounds = captor.getAllValues().stream()
                .sorted(Comparator.comparing(Outbound::getComment))
                .collect(Collectors.toList());
        assertThat(outbounds.get(0), samePropertyValuesAs(outbound));
    }

    private void verifyExpendableMaterialsSupply(boolean useMarketName) throws GatewayApiException {
        Inbound inbound = inbound("11", ru.yandex.market.logistic.gateway.common.model.fulfillment.InboundType.DEFAULT,
                DateTimeInterval.fromFormattedValue("2018-01-05T10:00:00/2018-01-05T10:30:00"), "test",
                getConsignments(
                        "11",
                        unitId("11", "SHOPSKU11"),
                        Collections.unmodifiableList(Lists.newArrayList(
                                new Barcode("11", null, null),
                                new Barcode("22", null, null))),
                        new BigDecimal("50.50"),
                        useMarketName ? "market_name1" : "offer_11",
                        3,
                        List.of(CargoType.SMALL_GOODS),
                        1,
                        true,
                        useMarketName ? Collections.singletonList("marketVendorCode1")
                                : Collections.singletonList("vendorCode1"),
                        10,
                        Collections.emptyList(),
                        List.of(String.format(BaseLgwClientConverter.MARKET_URL_TEMPLATE, 11)),
                        null
                ));

        Mockito.verify(fulfillmentClient).createInbound(inbound, new Partner(10L));
    }

    private void verifyTransfer() throws GatewayApiException {
        // Проверка отправки трансфера.
        ArgumentCaptor<Transfer> captor = ArgumentCaptor.forClass(Transfer.class);
        Mockito.verify(fulfillmentClient).createTransfer(captor.capture(), eq(new Partner(10L)));
    }

    private static Outbound outbound(String id, String date, String comment, List<Consignment> consignments) {
        final LegalEntity legalEntity = legalEntity();
        final Courier courier = courier(legalEntity);
        final ResourceId resourceId = ResourceId.builder()
                .setYandexId(id)
                .setPartnerId(null)
                .build();

        final Outbound.OutboundBuilder builder = new Outbound.OutboundBuilder(
                resourceId,
                StockType.EXPIRED,
                consignments,
                courier,
                createOwner(),
                DateTimeInterval.fromFormattedValue(date + "/" + date)
        );
        builder.setComment(comment);
        return builder.build();
    }

    private static Courier courier(final LegalEntity legalEntity) {
        final Person person = new Person("Some name", "Some surname", null, null);
        final Phone phone = new Phone("79232435555", null);
        return new Courier.CourierBuilder(Collections.singletonList(person))
                .setPhone(phone)
                .setLegalEntity(legalEntity)
                .build();
    }

    private Party createParty(String logPointId, String partnerId, String partnerLogPointId) {
        Location location = Location.builder("Россия", "Москва", "Московский")
                .setLocationId(1111L)
                .setSettlement("Москва")
                .setStreet("Большая Лубянка")
                .setHouse("2")
                .setZipCode("101000")
                .setLat(BigDecimal.valueOf(55.4531))
                .setLng(BigDecimal.valueOf(37.3741))
                .build();
        var logisticsPointId = createResourceIdInNewFormat(logPointId, partnerLogPointId);
        LogisticPoint logisticPoint = LogisticPoint.builder(logisticsPointId)
                .setLocation(location)
                .build();
        return Party.builder(logisticPoint)
                .setLegalEntity(createLegalEntity())
                .setPartnerId(ru.yandex.market.logistic.gateway.common.model.common.ResourceId.builder()
                        .setYandexId(partnerId)
                        .build())
                .build();
    }

    private static LegalEntity legalEntity() {
        return new LegalEntity.LegalEntityBuilder("Some consignee").setLegalName("Some consignee").build();
    }

    private static Service service() {
        return new Service.ServiceBuilder(ServiceType.VERIFY_ITEM).setIsOptional(true).build();
    }

    private static Inbound inbound(String id,
                                   ru.yandex.market.logistic.gateway.common.model.fulfillment.InboundType inboundType,
                                   String date,
                                   String dateTo,
                                   String comment,
                                   List<Consignment> consignments) {
        return inbound(
                id, inboundType,
                DateTimeInterval.fromFormattedValue(date + "/" + dateTo),
                comment, consignments
        );
    }

    private static Inbound inbound(String id,
                                   ru.yandex.market.logistic.gateway.common.model.fulfillment.InboundType inboundType,
                                   DateTimeInterval interval,
                                   String comment,
                                   List<Consignment> consignments) {
        final ResourceId resourceId = ResourceId.builder()
                .setYandexId(id)
                .setPartnerId(null)
                .build();

        Inbound.InboundBuilder inbound = new Inbound.InboundBuilder(
                resourceId, inboundType, consignments, interval);
        inbound.setComment(comment);
        return inbound.build();
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    private static Consignment getConsignment(String yandexId, UnitId unitId, List<Barcode> barcodes, BigDecimal price,
                                              String name, int count, List<CargoType> cargoTypes, int boxCount,
                                              boolean hasLifeTime,
                                              List<String> vendorCodes, Integer boxCapacity,
                                              List<Service> inboundServices,
                                              List<String> urls, Boolean surplusAllowed) {

        final ResourceId resourceId = ResourceId.builder()
                .setYandexId(yandexId)
                .setPartnerId(null)
                .build();

        final Item.ItemBuilder builder = new Item.ItemBuilder(name, count, price, CargoType.UNKNOWN, cargoTypes);
        builder.setUnitId(unitId);
        builder.setArticle(unitId.getArticle());
        builder.setBarcodes(barcodes);
        builder.setBoxCount(boxCount);
        builder.setHasLifeTime(hasLifeTime);
        builder.setVendorCodes(vendorCodes);
        builder.setBoxCapacity(boxCapacity);
        builder.setInboundServices(inboundServices);
        builder.setUrls(urls);
        builder.setCisHandleMode(CisHandleMode.NOT_DEFINED);
        builder.setInstances(Collections.emptyList());
        builder.setContractor(new Contractor(String.valueOf(unitId.getVendorId()), "supplier" + unitId.getVendorId()));
        builder.setSurplusAllowed(surplusAllowed);
        return new Consignment(resourceId, builder.build(), null);
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    private static List<Consignment> getConsignments(String yandexId, UnitId unitId, List<Barcode> barcodes,
                                                     BigDecimal price, String name, int count,
                                                     List<CargoType> cargoTypes,
                                                     int boxCount, boolean hasLifeTime,
                                                     List<String> vendorCodes, Integer boxCapacity,
                                                     List<Service> inboundServices,
                                                     List<String> urls, Boolean surplusAllowed) {

        return Collections.singletonList(getConsignment(yandexId, unitId, barcodes,
                price, name, count, cargoTypes, boxCount,
                hasLifeTime, vendorCodes, boxCapacity, inboundServices, urls, surplusAllowed));
    }

    private static OrganizationInfoDTO createOrganizationInfo() {
        return OrganizationInfoDTO.builder()
                .type(OrganizationType.IP)
                .inn("someInn")
                .ogrn("someOgrn")
                .kpp("someKpp")
                .name("someOrgName")
                .accountNumber("accNumber")
                .corrAccountNumber("corrAccNumber")
                .bankName("bankName")
                .bik("someBik")
                .factAddress("factAddr")
                .juridicalAddress("jurAddr")
                .build();
    }

    private static LegalEntity createOwner() {
        return new LegalEntity.LegalEntityBuilder("someOrgName")
                .setLegalName("someOrgName")
                .setLegalForm(LegalForm.IP)
                .setKpp("someKpp")
                .setInn("someInn")
                .setOgrn("someOgrn")
                .setAccount("accNumber")
                .setCorrespondentAccount("corrAccNumber")
                .setBik("someBik")
                .setBank("bankName")
                .setAddress(new Address("jurAddr", null))
                .build();
    }

    private UnitId unitId(String marketSku, String shopSku) {
        return new UnitId(marketSku, SUPPLIER_ID, shopSku);
    }

    private UnitId unitId(String marketSku, Long vendorId, String shopSku) {
        return new UnitId(marketSku, vendorId, shopSku);
    }
    private Collection<ShopRequest> getRequestsToSendToService() {
        Collection<ShopRequest> requestsToSendToService = shopRequestFetchingService
                .getNotInternalRequestsByStatusAndTypesWithValidCalendaring(RequestStatus.VALIDATED,
                        Arrays.asList(RequestType.values()));

        return requestsToSendToService.stream()
                .filter(shopRequest -> shopRequest.getRequestedDate() != null)
                .collect(Collectors.toList());
    }
    private void processAllRequests() {
        for (ShopRequest shopRequest:getRequestsToSendToService()) {
            if (!RequestType.SHADOW_TYPES.contains(shopRequest.getType())) {
                SendRequestToServicePayload payload = new SendRequestToServicePayload(shopRequest.getId());
                service.processPayload(payload);
            }
        }
    }
}
