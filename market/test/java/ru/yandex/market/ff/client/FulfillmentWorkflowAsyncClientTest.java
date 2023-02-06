package ru.yandex.market.ff.client;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.MockRestServiceServer;

import ru.yandex.market.ff.client.dto.ResourceIdDto;
import ru.yandex.market.logistic.gateway.common.model.common.CompositeId;
import ru.yandex.market.logistic.gateway.common.model.common.DateTimeInterval;
import ru.yandex.market.logistic.gateway.common.model.common.Inbound;
import ru.yandex.market.logistic.gateway.common.model.common.InboundType;
import ru.yandex.market.logistic.gateway.common.model.common.Location;
import ru.yandex.market.logistic.gateway.common.model.common.LogisticPoint;
import ru.yandex.market.logistic.gateway.common.model.common.Outbound;
import ru.yandex.market.logistic.gateway.common.model.common.PartialId;
import ru.yandex.market.logistic.gateway.common.model.common.PartialIdType;
import ru.yandex.market.logistic.gateway.common.model.common.RegistryBox;
import ru.yandex.market.logistic.gateway.common.model.common.RegistryPallet;
import ru.yandex.market.logistic.gateway.common.model.common.RegistryType;
import ru.yandex.market.logistic.gateway.common.model.common.ResourceId;
import ru.yandex.market.logistic.gateway.common.model.common.UnitInfo;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Contractor;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.InboundRegistry;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.OutboundRegistry;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.RegistryItem;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.RemainingLifetimes;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ShelfLife;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ShelfLives;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Tax;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.TaxType;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.UnitOperationType;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.VatValue;

import static java.lang.ClassLoader.getSystemResourceAsStream;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

/**
 * Функциональные тесты для  {@link FulfillmentWorkflowAsyncClient}.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = Config.class)
class FulfillmentWorkflowAsyncClientTest {

    public static final String REQUEST_ID = "123456";
    public static final String REGISTRY_ID = "789";
    private static final ResourceIdDto RESOURCE_ID = ResourceIdDto.builder().yandexId("1").build();

    @Autowired
    private FulfillmentWorkflowAsyncClient clientApi;

    @Autowired
    private MockRestServiceServer mockServer;

    @Value("${fulfillment.workflow.api.host}")
    private String host;

    @AfterEach
    void resetMocks() {
        mockServer.reset();
    }

    @Test
    void successfulFfGetInbound() throws IOException {
        mockServer.expect(requestTo(host + "/requests/" + REQUEST_ID + "/accept-successful-ff-get-inbound"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(content().json(getJsonFromFile("successful_ff_inbound_request.json")))
                .andRespond(withStatus(OK));

        clientApi.setFfGetInboundSuccess(getInbound(), List.of(InboundRegistry.builder(
                ResourceId.builder().setYandexId(REGISTRY_ID).build(),
                ResourceId.builder().setYandexId(REQUEST_ID).build(),
                RegistryType.FACTUAL)
                .setBoxes(getBoxes())
                .setPallets(getPallets())
                .setItems(getItems())
                .build()), 1L, null);
    }

    @Test
    void successfulFfGetOutbound() throws IOException {
        mockServer.expect(requestTo(host + "/requests/" + REQUEST_ID + "/accept-successful-ff-get-outbound"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(content().json(getJsonFromFile("successful_ff_outbound_request.json")))
                .andRespond(withStatus(OK));

        clientApi.setFfGetOutboundSuccess(getOutbound(), List.of(OutboundRegistry.builder(
                ResourceId.builder().setYandexId(REGISTRY_ID).build(),
                ResourceId.builder().setYandexId(REQUEST_ID).build(),
                RegistryType.FACTUAL)
                .setBoxes(getBoxes())
                .setPallets(getPallets())
                .setItems(getItems())
                .build()), 1L, null);
    }

    @Test
    void setFfGetInboundError() throws IOException {
        mockServer.expect(requestTo(host + "/requests/" + REQUEST_ID + "/accept-error-ff-get-inbound"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(content().json(getJsonFromFile("error_inbound_request.json")))
                .andRespond(withStatus(OK));

        clientApi.setFfGetInboundError(REQUEST_ID, "1", 1L, null,
                "error");
    }

    @Test
    void setFfGetOutboundError() throws IOException {
        mockServer.expect(requestTo(host + "/requests/" + REQUEST_ID + "/accept-error-ff-get-outbound"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(content().json(getJsonFromFile("error_inbound_request.json")))
                .andRespond(withStatus(OK));

        clientApi.setFfGetOutboundError(REQUEST_ID, "1", 1L, null, "error");
    }

    @Test
    void successfulFfInboundRegister() throws IOException {
        mockServer.expect(requestTo(host + "/requests/ff-inbound-register"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(content().json(getJsonFromFile("successful_inbound_register.json")))
                .andRespond(withStatus(OK)
                        .contentType(APPLICATION_JSON)
                        .body(IOUtils.toString(Objects.requireNonNull(
                                getSystemResourceAsStream("resource_id.json")), StandardCharsets.UTF_8)));

        ResourceIdDto resourceId = clientApi.putFfInboundRegistry(InboundRegistry.builder(
                ResourceId.builder().setYandexId(REGISTRY_ID).build(),
                ResourceId.builder().setYandexId(REQUEST_ID).build(),
                RegistryType.PLANNED)
                .setBoxes(getBoxes())
                .setPallets(getPallets())
                .setItems(getItems())
                .build());

        assertEquals(RESOURCE_ID, resourceId);
    }

    @Test
    void successfulFfOutboundRegister() throws IOException {
        mockServer.expect(requestTo(host + "/requests/ff-outbound-register"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(content().json(getJsonFromFile("successful_outbound_register.json")))
                .andRespond(withStatus(OK)
                        .contentType(APPLICATION_JSON)
                        .body(IOUtils.toString(Objects.requireNonNull(
                                getSystemResourceAsStream("resource_id.json")), StandardCharsets.UTF_8)));

        ResourceIdDto resourceId = clientApi.putFfOutboundRegistry(OutboundRegistry.builder(
                ResourceId.builder().setYandexId(REGISTRY_ID).build(),
                ResourceId.builder().setYandexId(REQUEST_ID).build(),
                RegistryType.PLANNED)
                .setBoxes(getBoxes())
                .setPallets(getPallets())
                .setItems(getItems())
                .build());

        assertEquals(RESOURCE_ID, resourceId);
    }

    @Test
    void successfulAcceptRegistry() throws IOException {
        mockServer.expect(requestTo(host + "/requests/accept-successful-registry"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(content().json(getJsonFromFile("successful_accept_registry.json")))
                .andRespond(withStatus(OK));

        ResourceId registryId = ResourceId.builder().setYandexId("1").setPartnerId("partnerId").build();

        clientApi.acceptSuccessfulPutRegistry(registryId);
    }

    @Test
    void successfulAcceptRegistryError() throws IOException {
        mockServer.expect(requestTo(host + "/requests/accept-error-registry"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(content().json(getJsonFromFile("successful_accept_registry_error.json")))
                .andRespond(withStatus(OK));


        clientApi.acceptErrorPutRegistry(100L, "error");
    }

    private Outbound getOutbound() {
        var outboundBuilder = Outbound.builder(
                ResourceId.builder().setYandexId(REQUEST_ID).build(),
                DateTimeInterval.fromFormattedValue("2018-01-05T10:00:00/2018-01-05T10:00:00")
        );
        outboundBuilder.setComment("Comment");
        outboundBuilder.setLogisticPoint(getLogisticPoint());
        return outboundBuilder.build();
    }

    private List<RegistryBox> getBoxes() {
        return List.of(new RegistryBox(UnitInfo.builder()
                .setCompositeId(CompositeId.builder(List.of(new PartialId(PartialIdType.BOX_ID, "box")))
                        .build())
                .build()));
    }

    private List<RegistryPallet> getPallets() {
        return List.of(new RegistryPallet(UnitInfo.builder()
                .setCompositeId(CompositeId.builder(List.of(new PartialId(PartialIdType.PALLET_ID, "pallet")))
                        .build())
                .build()));
    }

    private List<RegistryItem> getItems() {
        return List.of(
            RegistryItem
                .builder(
                    UnitInfo.builder()
                        .setCompositeId(
                            CompositeId.builder(
                                List.of(
                                    new PartialId(PartialIdType.ARTICLE, "article"),
                                    new PartialId(PartialIdType.VENDOR_ID, "vendor")
                                )
                            )
                            .build()
                        )
                        .build()
                )
                .setVendorCodes(Collections.emptyList())
                .setBarcodes(Collections.emptyList())
                .setName("item")
                .setPrice(BigDecimal.ONE)
                .setTax(new Tax(TaxType.VAT, VatValue.EIGHTEEN))
                .setUntaxedPrice(BigDecimal.ONE)
                .setCargoTypes(Collections.emptyList())
                .setInboundServices(Collections.emptyList())
                .setHasLifeTime(false)
                .setLifeTime(0)
                .setBoxCapacity(0)
                .setBoxCount(0)
                .setComment("comment")
                .setContractor(new Contractor("id", "name"))
                .setRemainingLifetimes(
                    new RemainingLifetimes(
                        new ShelfLives(new ShelfLife(0), new ShelfLife(0)),
                        new ShelfLives(new ShelfLife(0), new ShelfLife(0))
                    )
                )
                .setUpdated(null)
                .setCategoryId(1L)
                .setRemovableIfAbsent(false)
                .setUnitOperationType(UnitOperationType.CROSSDOCK)
                .setUrls(Collections.emptyList())
                .build()
        );
    }

    private Inbound getInbound() {
        var inboundBuilder = Inbound.builder(
                ResourceId.builder().setYandexId(REQUEST_ID).build(),
                InboundType.DS_SC,
                DateTimeInterval.fromFormattedValue("2018-01-05T10:00:00/2018-01-05T10:00:00")
        );
        inboundBuilder.setComment("Comment");
        inboundBuilder.setLogisticPoint(getLogisticPoint());
        return inboundBuilder.build();
    }

    private LogisticPoint getLogisticPoint() {
        return LogisticPoint.builder(ResourceId.builder().setYandexId("5125425").setPartnerId("550-1234").build())
                .setLocation(getLocation())
                .build();
    }

    private Location getLocation() {
        return Location.builder("Россия", "Королев", "Московская обл.")
                .setLocationId(123L)
                .setSettlement("Королев")
                .setStreet("Проспект Космонавтов")
                .setHouse("47")
                .setHousing("17")
                .setBuilding("1")
                .setRoom("1")
                .setZipCode("141080")
                .setLat(BigDecimal.valueOf(50.4151))
                .setLng(BigDecimal.valueOf(31.0341))
                .build();
    }

    private String getJsonFromFile(final String name) throws IOException {
        return IOUtils.toString(
                Objects.requireNonNull(
                        getSystemResourceAsStream(name)),
                StandardCharsets.UTF_8).trim();
    }
}
