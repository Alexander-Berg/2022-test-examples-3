package ru.yandex.market.wms.api.controller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.logistic.api.model.fulfillment.Barcode;
import ru.yandex.market.logistic.api.model.fulfillment.CargoType;
import ru.yandex.market.logistic.api.model.fulfillment.CargoTypes;
import ru.yandex.market.logistic.api.model.fulfillment.Contractor;
import ru.yandex.market.logistic.api.model.fulfillment.Item;
import ru.yandex.market.logistic.api.model.fulfillment.Korobyte;
import ru.yandex.market.logistic.api.model.fulfillment.Service;
import ru.yandex.market.logistic.api.model.fulfillment.ServiceType;
import ru.yandex.market.logistic.api.model.fulfillment.UnitId;
import ru.yandex.market.wms.api.config.ServiceBusConfiguration;
import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.servicebus.model.dto.SkuIdDto;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static java.util.List.of;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistic.api.model.fulfillment.CargoType.CIS_REQUIRED;
import static ru.yandex.market.logistic.api.model.fulfillment.CargoType.MEDICAL_SUPPLIES;
import static ru.yandex.market.logistic.api.model.fulfillment.CargoType.UNKNOWN;

@ContextConfiguration(classes = ServiceBusConfiguration.class)
public class ItemControllerTest extends IntegrationTest {

    private final ObjectMapper mapper = new ObjectMapper().disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    private final Random random = new Random();

    @Test
    @DatabaseSetup("/items/db/void.xml")
    @ExpectedDatabase(value = "/items/db/void.xml", assertionMode = NON_STRICT)
    public void putEmptyBody() throws Exception {
        mockMvc.perform(
                        put("/ENTERPRISE/items/itembatch")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DatabaseSetup("/items/db/void.xml")
    @ExpectedDatabase(value = "/items/db/void.xml", assertionMode = NON_STRICT)
    public void putIncorrectBody() throws Exception {
        mockMvc.perform(
                        put("/ENTERPRISE/items/itembatch")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DatabaseSetup("/items/db/void.xml")
    @ExpectedDatabase(value = "/items/db/void.xml", assertionMode = NON_STRICT)
    public void putEmptyList() throws Exception {
        final String body = mockMvc.perform(
                        put("/ENTERPRISE/items/itembatch")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("[]"))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertions.assertThat(body).contains("items: must not be empty");
    }

    @Test
    @DatabaseSetup("/items/db/void.xml")
    @ExpectedDatabase(value = "/items/db/void.xml", assertionMode = NON_STRICT)
    public void putItemBatchWithoutVendor() throws Exception {
        List<Item> items = Collections.singletonList(new Item
                .ItemBuilder("Test item", 1, BigDecimal.TEN)
                .setUnitId(new UnitId.UnitIdBuilder(null, UUID.randomUUID().toString()).build())
                .build()
        );
        final String body = mockMvc.perform(
                        put("/BLAT/items/itembatch")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(items)))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertions.assertThat(body).contains("vendorId: must not be null");
    }

    @Test
    @DatabaseSetup("/items/db/void.xml")
    @ExpectedDatabase(value = "/items/db/void.xml", assertionMode = NON_STRICT)
    public void putItemBatchWithIncorrectScheme() throws Exception {
        List<Item> items = Collections.singletonList(new Item
                .ItemBuilder("Test item", 1, BigDecimal.TEN)
                .setUnitId(new UnitId.UnitIdBuilder(random.nextLong(), UUID.randomUUID().toString()).build())
                .build()
        );
        String body = mockMvc.perform(
                        put("/BLAT/items/itembatch")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(items)))
                .andExpect(status().isInternalServerError())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertions.assertThat(body).contains("Incorrect scheme type");
    }

    @Test
    @DatabaseSetup(value = "/items/db/initial.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/items/db/initial.xml", connection = "enterpriseConnection")
    @ExpectedDatabase(value = "/items/db/5/storer-sku-pack-altsku.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT_UNORDERED)
    @ExpectedDatabase(value = "/items/db/5/storer-sku-pack.xml",
            connection = "enterpriseConnection", assertionMode = NON_STRICT_UNORDERED)
    @ExpectedDatabase(value = "/items/db/1/storer-partner-facility-control.xml",
            connection = "enterpriseConnection", assertionMode = NON_STRICT_UNORDERED)
    public void putNewItemBatch() throws Exception {
        List<Item> items = Collections.singletonList(genericItemWithArticle("IPC647BOOKCCRYWHIOSG")
                .setHasLifeTime(true)
                .build()
        );
        final MockHttpServletResponse response = mockMvc.perform(
                        put("/ENTERPRISE/items/itembatch?updateExisting=true&loadTrustworthy=true")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(items)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();

        assertEquals(MediaType.APPLICATION_JSON_UTF8_VALUE, response.getContentType());

        final List<SkuIdDto> skuDTOList = mapper.readValue(response.getContentAsString(), new TypeReference<>() {
        });

        assertThat(skuDTOList, hasItems(new SkuIdDto(603674L, "ROV603674IPC647BOOKCCRYWHIOSG")));
    }

    @Test
    @DatabaseSetup(value = "/items/db/nonuit/before.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/items/db/nonuit/before.xml", connection = "enterpriseConnection")
    @ExpectedDatabase(value = "/items/db/nonuit/storer-sku-pack-altsku-disabled-nonuit.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT_UNORDERED)
    @ExpectedDatabase(value = "/items/db/nonuit/storer-sku-pack-disabled-nonuit.xml",
            connection = "enterpriseConnection", assertionMode = NON_STRICT_UNORDERED)
    @ExpectedDatabase(value = "/items/db/nonuit/storer-partner-facility-control.xml",
            connection = "enterpriseConnection", assertionMode = NON_STRICT_UNORDERED)
    public void putNewItemBatchDisabledNonUit() throws Exception {
        List<Item> items = Collections.singletonList(genericNonUITItem()
                .setHasLifeTime(true)
                .build()
        );
        final MockHttpServletResponse response = mockMvc.perform(
                        put("/ENTERPRISE/items/itembatch?updateExisting=true&loadTrustworthy=true")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(items)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();

        assertEquals(MediaType.APPLICATION_JSON_UTF8_VALUE, response.getContentType());

        final List<SkuIdDto> skuDTOList = mapper.readValue(response.getContentAsString(), new TypeReference<>() {
        });

        assertThat(skuDTOList, hasItems(new SkuIdDto(465852L, "ROV465852IPC647BOOKCCRYWHI")));
    }

    @Test
    @DatabaseSetup(value = "/items/db/nonuit/before.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/items/db/nonuit/enable-nonuit.xml",
            type = DatabaseOperation.INSERT, connection = "wmwhseConnection")
    @DatabaseSetup(value = "/items/db/nonuit/before.xml", connection = "enterpriseConnection")
    @ExpectedDatabase(value = "/items/db/nonuit/storer-sku-pack-altsku-enabled-nonuit.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT_UNORDERED)
    @ExpectedDatabase(value = "/items/db/nonuit/storer-sku-pack-enabled-nonuit.xml",
            connection = "enterpriseConnection", assertionMode = NON_STRICT_UNORDERED)
    @ExpectedDatabase(value = "/items/db/nonuit/storer-partner-facility-control.xml",
            connection = "enterpriseConnection", assertionMode = NON_STRICT_UNORDERED)
    public void putNewItemBatchEnabledNonUit() throws Exception {
        List<Item> items = Collections.singletonList(genericNonUITItem()
                .setHasLifeTime(true)
                .build()
        );
        final MockHttpServletResponse response = mockMvc.perform(
                        put("/ENTERPRISE/items/itembatch?updateExisting=true&loadTrustworthy=true")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(items)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();

        assertEquals(MediaType.APPLICATION_JSON_UTF8_VALUE, response.getContentType());

        final List<SkuIdDto> skuDTOList = mapper.readValue(response.getContentAsString(), new TypeReference<>() {
        });

        assertThat(skuDTOList, hasItems(new SkuIdDto(465852L, "ROV465852IPC647BOOKCCRYWHI")));
    }

    @Test
    @DatabaseSetup(value = "/items/db/initial.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/items/db/initial.xml", connection = "enterpriseConnection")
    @ExpectedDatabase(value = "/items/db/3/storer-sku-pack-altsku.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT_UNORDERED)
    @ExpectedDatabase(value = "/items/db/3/storer-sku-pack.xml",
            connection = "enterpriseConnection", assertionMode = NON_STRICT_UNORDERED)
    @ExpectedDatabase(value = "/items/db/1/storer-partner-facility-control.xml",
            connection = "enterpriseConnection", assertionMode = NON_STRICT_UNORDERED)
    public void putNewItemBatchWithoutLifetime() throws Exception {
        List<Item> items = Collections.singletonList(genericItem()
                .setHasLifeTime(false)
                .build()
        );
        final MockHttpServletResponse response = mockMvc.perform(
                        put("/ENTERPRISE/items/itembatch?updateExisting=true&loadTrustworthy=true")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(items)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();

        assertEquals(MediaType.APPLICATION_JSON_UTF8_VALUE, response.getContentType());

        final List<SkuIdDto> skuDTOList = mapper.readValue(response.getContentAsString(), new TypeReference<>() {
        });

        assertThat(skuDTOList, hasItems(new SkuIdDto(603674L, "ROV603674IPC647BOOKCCRYWHI")));
    }

    @Test
    @DatabaseSetup(value = "/items/db/initial.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/items/db/initial.xml", connection = "enterpriseConnection")
    @ExpectedDatabase(value = "/items/db/6/storer-sku-pack-altsku.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT_UNORDERED)
    @ExpectedDatabase(value = "/items/db/3/storer-sku-pack.xml",
            connection = "enterpriseConnection", assertionMode = NON_STRICT_UNORDERED)
    @ExpectedDatabase(value = "/items/db/1/storer-partner-facility-control.xml",
            connection = "enterpriseConnection", assertionMode = NON_STRICT_UNORDERED)
    public void putAdditionalGtin() throws Exception {
        List<Item> items = Collections.singletonList(genericItemWithBarcode("0112345678000001")
                .setHasLifeTime(false)
                .build()
        );
        final MockHttpServletResponse response = mockMvc.perform(
                        put("/ENTERPRISE/items/itembatch?updateExisting=true&loadTrustworthy=true")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(items)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();

        assertEquals(MediaType.APPLICATION_JSON_UTF8_VALUE, response.getContentType());

        final List<SkuIdDto> skuDTOList = mapper.readValue(response.getContentAsString(), new TypeReference<>() {
        });

        assertThat(skuDTOList, hasItems(new SkuIdDto(603674L, "ROV603674IPC647BOOKCCRYWHI")));
    }

    @Test
    @DatabaseSetup(value = "/items/db/1/storer-sku-pack-altsku.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/items/db/1/storer-sku-pack-altsku.xml", connection = "enterpriseConnection")
    @ExpectedDatabase(value = "/items/db/4/storer-sku-pack-altsku.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT_UNORDERED)
    @ExpectedDatabase(value = "/items/db/4/storer-sku-pack.xml",
            connection = "enterpriseConnection", assertionMode = NON_STRICT_UNORDERED)
    public void putItemBatchClearLifetime() throws Exception {
        List<Item> items = Collections.singletonList(genericItem()
                .setHasLifeTime(false)
                .build()
        );
        final MockHttpServletResponse response = mockMvc.perform(
                        put("/WMWHSE1/items/itembatch?updateExisting=true&loadTrustworthy=true")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(items)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();

        assertEquals(MediaType.APPLICATION_JSON_UTF8_VALUE, response.getContentType());

        final List<SkuIdDto> skuDTOList = mapper.readValue(response.getContentAsString(), new TypeReference<>() {
        });

        assertThat(skuDTOList, hasItems(new SkuIdDto(603674L, "ROV603674IPC647BOOKCCRYWHI")));
    }

    @Test
    @DatabaseSetup(value = "/items/db/initial.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/items/db/initial.xml", connection = "enterpriseConnection")
    @ExpectedDatabase(value = "/items/db/2/storer-sku-fulfillment-pack-altsku.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT_UNORDERED)
    @ExpectedDatabase(value = "/items/db/2/storer-sku-fulfillment-pack.xml",
            connection = "enterpriseConnection", assertionMode = NON_STRICT_UNORDERED)
    @ExpectedDatabase(value = "/items/db/1/storer-partner-facility-control.xml",
            connection = "enterpriseConnection", assertionMode = NON_STRICT_UNORDERED)
    public void putNewItemBatchWithoutLoadKorobyteFromIris() throws Exception {
        List<Item> items = Collections.singletonList(genericItem()
                .setHasLifeTime(true)
                .setKorobyte(new Korobyte.KorobyteBuiler(5, 10, 15, BigDecimal.valueOf(3.5))
                        .setWeightNet(BigDecimal.valueOf(3.4))
                        .setWeightTare(BigDecimal.valueOf(0.1))
                        .build()
                )
                .build()
        );
        final MockHttpServletResponse response = mockMvc.perform(
                        put("/ENTERPRISE/items/itembatch?updateExisting=true&loadTrustworthy=false")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(items)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();

        assertEquals(MediaType.APPLICATION_JSON_UTF8_VALUE, response.getContentType());

        final List<SkuIdDto> skuDTOList = mapper.readValue(response.getContentAsString(), new TypeReference<>() {
        });

        assertThat(skuDTOList, hasItems(new SkuIdDto(603674L, "ROV603674IPC647BOOKCCRYWHI")));
    }

    @Test
    @DatabaseSetup(value = "/items/db/1/storer-sku-pack.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/items/db/cargotypes/one.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/items/db/1/storer-sku-pack.xml", connection = "enterpriseConnection")
    @ExpectedDatabase(value = "/items/db/cargotypes/many-and-identity.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT)
    public void putItemBatchWithCargoTypesToUpdate() throws Exception {
        List<CargoType> cargoTypes = Arrays.asList(MEDICAL_SUPPLIES, CIS_REQUIRED);
        List<Item> items = Collections.singletonList(genericItem()
                .setHasLifeTime(false)
                .setCargoTypes(new CargoTypes(cargoTypes))
                .build());

        mockMvc.perform(
                        put("/ENTERPRISE/items/itembatch?updateExisting=true&loadTrustworthy=false")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(items)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();
    }

    @Test
    @DatabaseSetup(value = "/items/db/1/storer-sku-pack.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/items/db/cargotypes/one.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/items/db/1/storer-sku-pack.xml", connection = "enterpriseConnection")
    @ExpectedDatabase(value = "/items/db/cargotypes/many-and-identity.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT)
    public void putItemBatchWithUnknownCargoType() throws Exception {
        List<Item> items = Collections.singletonList(genericItem()
                .setHasLifeTime(false)
                .setCargoTypes(new CargoTypes(of(MEDICAL_SUPPLIES, UNKNOWN, CIS_REQUIRED)))
                .build());

        mockMvc.perform(
                        put("/ENTERPRISE/items/itembatch?updateExisting=true&loadTrustworthy=false")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(items)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();
    }

    @Test
    @DatabaseSetup(value = "/items/db/1/storer-sku-pack.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/items/db/cargotypes/one.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/items/db/1/storer-sku-pack.xml", connection = "enterpriseConnection")
    @ExpectedDatabase(value = "/items/db/cargotypes/one.xml", connection = "wmwhseConnection",
            assertionMode = NON_STRICT)
    public void putItemBatchWithEmptyCargoTypes() throws Exception {
        List<Item> items = Collections.singletonList(genericItem()
                .setHasLifeTime(false)
                .setCargoType(null)
                .setCargoTypes(null)
                .build());

        mockMvc.perform(
                        put("/ENTERPRISE/items/itembatch?updateExisting=true&loadTrustworthy=false")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(items)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();
    }

    @Test
    @DatabaseSetup(value = "/items/db/1/storer-sku-pack.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/items/db/cargotypes/many.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/items/db/1/storer-sku-pack.xml", connection = "enterpriseConnection")
    @ExpectedDatabase(value = "/items/db/cargotypes/one.xml", connection = "wmwhseConnection",
            assertionMode = NON_STRICT)
    public void putItemBatchWithSubListCargoTypes() throws Exception {
        List<Item> items = Collections.singletonList(genericItem()
                .setHasLifeTime(false)
                .setCargoType(null)
                .setCargoTypes(new CargoTypes(of(CargoType.ADULT)))
                .build());

        mockMvc.perform(
                        put("/ENTERPRISE/items/itembatch?updateExisting=true&loadTrustworthy=false")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(items)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();
    }

    @Test
    @DatabaseSetup(value = "/items/db/initial.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/items/db/initial.xml", connection = "enterpriseConnection")
    @ExpectedDatabase(value = "/items/db/3/storer-sku-pack-altsku.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT_UNORDERED)
    @ExpectedDatabase(value = "/items/db/3/storer-sku-pack.xml",
            connection = "enterpriseConnection", assertionMode = NON_STRICT_UNORDERED)
    @ExpectedDatabase(value = "/items/db/1/storer-partner-facility-control.xml",
            connection = "enterpriseConnection", assertionMode = NON_STRICT_UNORDERED)
    public void duplicatedItemBatch() throws Exception {
        final Item item = genericItem()
                .setHasLifeTime(false)
                .build();
        List<Item> items = new ArrayList<>();
        items.add(item);
        items.add(item);
        mockMvc.perform(
                        put("/ENTERPRISE/items/itembatch?updateExisting=true&loadTrustworthy=true")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(items)))
                .andExpect(status().isOk())
                .andReturn();
    }

    @Test
    @DatabaseSetup(value = "/items/db/initial.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/items/db/initial.xml", connection = "enterpriseConnection")
    @ExpectedDatabase(value = "/items/db/5/storer-sku-pack-altsku.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT_UNORDERED)
    @ExpectedDatabase(value = "/items/db/5/storer-sku-pack.xml",
            connection = "enterpriseConnection", assertionMode = NON_STRICT_UNORDERED)
    @ExpectedDatabase(value = "/items/db/1/storer-partner-facility-control.xml",
            connection = "enterpriseConnection", assertionMode = NON_STRICT_UNORDERED)
    public void mergeTrustworthyBarcodes() throws Exception {
        List<Item> items = Collections.singletonList(genericItemWithArticle("IPC647BOOKCCRYWHIOSG")
                .setBarcodes(List.of(
                        new Barcode("EAN-IPC647BOOKCCRYWHIOSG", "EAN"),
                        new Barcode("D8033830112492", "EAN")
                ))
                .setHasLifeTime(true)
                .build()
        );
        final MockHttpServletResponse response = mockMvc.perform(
                        put("/ENTERPRISE/items/itembatch?updateExisting=true&loadTrustworthy=true")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(items)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();

        assertEquals(MediaType.APPLICATION_JSON_UTF8_VALUE, response.getContentType());

        final List<SkuIdDto> skuDTOList = mapper.readValue(response.getContentAsString(), new TypeReference<>() {
        });

        assertThat(skuDTOList, hasItems(new SkuIdDto(603674L, "ROV603674IPC647BOOKCCRYWHIOSG")));
    }

    private Item.ItemBuilder genericItem() {
        return new Item
                .ItemBuilder("Test Item", 1, BigDecimal.TEN)
                .setUnitId(new UnitId.UnitIdBuilder(603674L, "IPC647BOOKCCRYWHI").build())
                .setCargoType(UNKNOWN)
                .setBarcodes(Collections.singletonList(new Barcode.BarcodeBuilder("D8033830112492").build()))
                .setBoxCapacity(1)
                .setUrls(Arrays.asList("urls1", "urls2"))
                .setContractor(new Contractor("contractor-id", "contractor-name"));
    }

    private Item.ItemBuilder genericItemWithArticle(String article) {
        return new Item
                .ItemBuilder("Test Item", 1, BigDecimal.TEN)
                .setUnitId(new UnitId.UnitIdBuilder(603674L, article).build())
                .setCargoType(UNKNOWN)
                .setBarcodes(Collections.singletonList(new Barcode.BarcodeBuilder("D8033830112492").build()))
                .setBoxCapacity(1)
                .setUrls(Arrays.asList("urls1", "urls2"))
                .setContractor(new Contractor("contractor-id", "contractor-name"))
                .setInboundServices(
                        Collections.singletonList(
                                new Service(ServiceType.NO_MEASURE_ITEM, null, null, true)
                        )
                );
    }

    private Item.ItemBuilder genericItemWithBarcode(String barcode) {
        return new Item
                .ItemBuilder("Test Item", 1, BigDecimal.TEN)
                .setUnitId(new UnitId.UnitIdBuilder(603674L, "IPC647BOOKCCRYWHI").build())
                .setCargoType(UNKNOWN)
                .setBarcodes(Collections.singletonList(new Barcode.BarcodeBuilder(barcode).build()))
                .setBoxCapacity(1)
                .setUrls(Arrays.asList("urls1", "urls2"))
                .setContractor(new Contractor("contractor-id", "contractor-name"));
    }

    private Item.ItemBuilder genericNonUITItem() {
        return new Item
                .ItemBuilder("Test Item", 1, BigDecimal.TEN)
                .setUnitId(new UnitId.UnitIdBuilder(465852L, "IPC647BOOKCCRYWHI").build())
                .setCargoType(UNKNOWN)
                .setBarcodes(Collections.singletonList(new Barcode.BarcodeBuilder("D8033830112492").build()))
                .setBoxCapacity(1)
                .setUrls(Arrays.asList("urls1", "urls2"))
                .setContractor(new Contractor("contractor-id", "contractor-name"));
    }
}
