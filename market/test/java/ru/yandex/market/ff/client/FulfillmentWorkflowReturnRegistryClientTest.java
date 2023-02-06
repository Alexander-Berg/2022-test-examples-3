package ru.yandex.market.ff.client;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseCreator;

import ru.yandex.market.ff.client.dto.CarDTO;
import ru.yandex.market.ff.client.dto.ClientResourceIdDTO;
import ru.yandex.market.ff.client.dto.CourierDTO;
import ru.yandex.market.ff.client.dto.LegalEntityDTO;
import ru.yandex.market.ff.client.dto.LocationDTO;
import ru.yandex.market.ff.client.dto.PersonDTO;
import ru.yandex.market.ff.client.dto.PhoneDTO;
import ru.yandex.market.ff.client.dto.PutSupplyRequestWithInboundRegisterDTO;
import ru.yandex.market.ff.client.dto.ShopRequestDTO;
import ru.yandex.market.ff.client.enums.LegalFormType;
import ru.yandex.market.ff.client.enums.RequestStatus;
import ru.yandex.market.ff.client.enums.RequestType;
import ru.yandex.market.ff.client.enums.StockType;
import ru.yandex.market.logistic.gateway.common.model.common.CompositeId;
import ru.yandex.market.logistic.gateway.common.model.common.PartialId;
import ru.yandex.market.logistic.gateway.common.model.common.PartialIdType;
import ru.yandex.market.logistic.gateway.common.model.common.RegistryBox;
import ru.yandex.market.logistic.gateway.common.model.common.ResourceId;
import ru.yandex.market.logistic.gateway.common.model.common.UnitCount;
import ru.yandex.market.logistic.gateway.common.model.common.UnitInfo;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.InboundRegistry;

import static java.lang.ClassLoader.getSystemResourceAsStream;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static ru.yandex.market.logistic.gateway.common.model.common.RegistryType.PLANNED_RETURNS;

/**
 * Функциональные тесты для  {@link FulfillmentWorkflowReturnRegistryClient}.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = Config.class)
class FulfillmentWorkflowReturnRegistryClientTest {

    @Autowired
    private FulfillmentWorkflowReturnRegistryClientApi clientApi;

    @Autowired
    @Qualifier("secondHttpTemplate")
    private MockRestServiceServer mockServer;

    @Value("${fulfillment.workflow.api.host}")
    private String host;

    @AfterEach
    void resetMocks() {
        mockServer.reset();
    }

    @Test
    void createSupplyRequest() throws IOException {
        ResponseCreator returnResponseCreator = withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(IOUtils.toString(Objects.requireNonNull(
                        getSystemResourceAsStream("create_supply_with_registry.json")), StandardCharsets.UTF_8));

        mockServer.expect(requestTo(host + "/supplies-with-registry"))
                .andExpect(content().json(IOUtils.toString(Objects.requireNonNull(getSystemResourceAsStream(
                        "valid-supply-with-courier.json")), StandardCharsets.UTF_8), true))
                .andRespond(returnResponseCreator);

        ShopRequestDTO supplyRequest = clientApi.createRequestAndPutRegistry(createSupplyWithRegistryDTO());
        assertThat(supplyRequest, notNullValue());
        assertThat(supplyRequest.getId(), equalTo(1L));
        assertThat(supplyRequest.getRequestedDate(), equalTo(LocalDateTime.of(2018, 11, 10, 10, 10, 10)));
        assertThat(supplyRequest.getCreatedAt(), equalTo(LocalDateTime.of(2018, 10, 10, 10, 10, 10)));
        assertThat(supplyRequest.getUpdatedAt(), equalTo(LocalDateTime.of(2018, 10, 10, 11, 10, 10)));
        assertThat(supplyRequest.getShopId(), equalTo(15L));
        assertThat(supplyRequest.getShopName(), equalTo("some name"));
        assertThat(supplyRequest.getType(), equalTo(RequestType.ORDERS_SUPPLY.getId()));
        assertThat(supplyRequest.getStockType(), equalTo(StockType.DEFECT));
        assertThat(supplyRequest.getStatus(), equalTo(RequestStatus.CREATED));
        assertThat(supplyRequest.getItemsTotalCount(), equalTo(0L));
        assertCourierIsDeserializedCorrectly(supplyRequest.getCourier(), "Олег");
    }

    static void assertCourierIsDeserializedCorrectly(CourierDTO courier, String name) {
        assertNotNull(courier);
        assertThat(courier.getPartnerId().getYandexId(), equalTo("106"));
        assertThat(courier.getPartnerId().getPartnerId(), equalTo("107"));
        List<PersonDTO> persons = courier.getPersons();
        assertThat(persons, notNullValue());
        assertThat(persons.size(), equalTo(1));
        PersonDTO personDTO = persons.get(0);
        assertThat(personDTO.getName(), equalTo(name));
        assertThat(personDTO.getSurname(), equalTo("Егоров"));
        assertThat(personDTO.getPatronymic(), equalTo("Васильевич"));
        CarDTO car = courier.getCar();
        assertThat(car, notNullValue());
        assertThat(car.getNumber(), equalTo("О123НО790"));
        assertThat(car.getDescription(), equalTo("Белый форд транзит"));
        PhoneDTO phone = courier.getPhone();
        assertThat(phone, notNullValue());
        assertThat(phone.getPhoneNumber(), equalTo("+78005553535"));
        assertThat(phone.getAdditional(), equalTo("88005553535"));
        assertThat(courier.getMarketId(), equalTo(123L));
        LegalEntityDTO legalEntity = courier.getLegalEntity();
        assertThat(legalEntity, notNullValue());
        assertThat(legalEntity.getName(), equalTo("ООО Синтез РУС"));
        assertThat(legalEntity.getLegalForm(), equalTo(LegalFormType.OOO));
        LocationDTO address = legalEntity.getAddress();
        assertThat(address, notNullValue());
        assertThat(address.getCountry(), equalTo("Россия"));
        assertThat(address.getLat(), equalTo(BigDecimal.valueOf(55.733957)));
        assertThat(address.getLocationId(), equalTo(213L));
    }

    static PutSupplyRequestWithInboundRegisterDTO createSupplyWithRegistryDTO() {
        PutSupplyRequestWithInboundRegisterDTO supply = new PutSupplyRequestWithInboundRegisterDTO();
        supply.setDate(OffsetDateTime.parse("2018-01-06T10:10:10+03:00"));
        supply.setSupplierId(1L);
        supply.setComment("some comment");
        supply.setType(RequestType.ORDERS_SUPPLY.getId());
        supply.setStockType(StockType.DEFECT);
        supply.setExternalRequestId("2341431");
        supply.setLogisticsPointId(12341L);
        supply.setCourier(createCourierDTO());
        supply.setInboundRegistry(InboundRegistry.builder(ResourceId.builder().build(),
            ResourceId.builder().build(),
            PLANNED_RETURNS)
                .setDocumentId("49781-3079882-3")
                .setBoxes(List.of(getRegisryBox()))
            .build());
        return supply;
    }

    private static RegistryBox getRegisryBox() {
        UnitCount.UnitCountBuilder unitCountBuilder = new UnitCount.UnitCountBuilder();
        UnitCount unitCount = unitCountBuilder
            .setCountType(ru.yandex.market.logistic.gateway.common.model.common.UnitCountType.FIT)
            .setQuantity(1)
            .build();
        UnitInfo unitInfo = UnitInfo.builder()
            .setRelations(null)
            .setCompositeId(new CompositeId(List.of(new PartialId(PartialIdType.BOX_ID, "box1"))))
            .setCounts(Collections.singletonList(unitCount))
            .build();
        return new RegistryBox(unitInfo);
    }

    static CourierDTO createCourierDTO() {
        CourierDTO courier = new CourierDTO();
        courier.setPartnerId(createResourceIdDTO());
        courier.setPersons(Collections.singletonList(createPersonDTO()));
        courier.setCar(createCarDTO());
        courier.setPhone(createPhoneDTO());
        courier.setMarketId(123L);
        courier.setLegalEntity(createLegalEntityDTO());
        return courier;
    }

    private static ClientResourceIdDTO createResourceIdDTO() {
        ClientResourceIdDTO partnerId = new ClientResourceIdDTO();
        partnerId.setYandexId("106");
        partnerId.setPartnerId("107");
        return partnerId;
    }

    private static PersonDTO createPersonDTO() {
        PersonDTO personDTO = new PersonDTO();
        personDTO.setName("Олег");
        personDTO.setSurname("Егоров");
        personDTO.setPatronymic("Васильевич");
        return personDTO;
    }

    private static CarDTO createCarDTO() {
        CarDTO car = new CarDTO();
        car.setNumber("О123НО790");
        car.setDescription("Белый форд транзит");
        return car;
    }

    private static PhoneDTO createPhoneDTO() {
        PhoneDTO phone = new PhoneDTO();
        phone.setPhoneNumber("+78005553535");
        phone.setAdditional("88005553535");
        return phone;
    }

    private static LegalEntityDTO createLegalEntityDTO() {
        LegalEntityDTO legalEntity = new LegalEntityDTO();
        legalEntity.setName("ООО Синтез РУС");
        legalEntity.setLegalName("ООО Синтез РУС");
        legalEntity.setLegalForm(LegalFormType.OOO);
        legalEntity.setOgrn("1000000000000");
        legalEntity.setInn("7777777777");
        legalEntity.setKpp("555555555");
        legalEntity.setAddress(createLocationDTO());
        legalEntity.setBank("Сбербанк");
        legalEntity.setAccount("1234000005678");
        legalEntity.setBik("4444444");
        legalEntity.setCorrespondentAccount("987600001111");
        return legalEntity;
    }

    private static LocationDTO createLocationDTO() {
        LocationDTO address = new LocationDTO();
        address.setCountry("Россия");
        address.setFederalDistrict("Центральный федеральный округ");
        address.setRegion("Москва и Московская область");
        address.setSubRegion("Городской округ");
        address.setLocality("Москва");
        address.setSettlement("Поселение");
        address.setStreet("9-я Северная линия");
        address.setHouse("23");
        address.setBuilding("1");
        address.setHousing("2");
        address.setRoom("98");
        address.setZipCode("123456");
        address.setPorch("1");
        address.setFloor(-1);
        address.setMetro("Дмитровская");
        address.setLat(BigDecimal.valueOf(55.733957));
        address.setLng(BigDecimal.valueOf(37.588274));
        address.setLocationId(213L);
        address.setIntercom("B98");
        return address;
    }
}
