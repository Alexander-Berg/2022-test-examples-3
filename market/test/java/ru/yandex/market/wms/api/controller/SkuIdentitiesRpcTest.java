package ru.yandex.market.wms.api.controller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import ru.yandex.market.logistic.api.model.fulfillment.Barcode;
import ru.yandex.market.logistic.api.model.fulfillment.CargoType;
import ru.yandex.market.logistic.api.model.fulfillment.CargoTypes;
import ru.yandex.market.logistic.api.model.fulfillment.CisHandleMode;
import ru.yandex.market.logistic.api.model.fulfillment.Contractor;
import ru.yandex.market.logistic.api.model.fulfillment.Item;
import ru.yandex.market.logistic.api.model.fulfillment.UnitId;
import ru.yandex.market.logistic.api.utils.DateTime;
import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.enums.TypeOfIdentity;
import ru.yandex.market.wms.common.spring.servicebus.model.dto.SkuIdDto;
import ru.yandex.market.wms.common.spring.servicebus.model.dto.SkuIdentityDTO;
import ru.yandex.market.wms.common.spring.servicebus.model.dto.SkuIdentityPkDTO;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.List.of;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class SkuIdentitiesRpcTest extends IntegrationTest {

    private final ObjectMapper mapper = new ObjectMapper().disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

    @Test
    @DatabaseSetup(value = "/items/db/1/storer-sku-pack-altsku.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/items/db/identities/ssku-identity-default-regex.xml", connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    public void createSkuIdentity() throws Exception {
        Collection<SkuIdentityDTO> skuIdentities = new ArrayList<>();
        final SkuIdDto skuId = SkuIdDto.builder()
                .storerKey(603674L)
                .sku("ROV603674IPC647BOOKCCRYWHI")
                .build();
        skuIdentities.add(
                SkuIdentityDTO.builder()
                        .skuId(skuId)
                        .type("IMEI")
                        .requirements(2)
                        .regex(TypeOfIdentity.IMEI.getDefaultRegex())
                        .description("Международный идентификатор мобильного оборудования")
                        .build()
        );
        skuIdentities.add(
                SkuIdentityDTO.builder()
                        .skuId(skuId)
                        .type("SN")
                        .requirements(1)
                        .regex(TypeOfIdentity.SN.getDefaultRegex())
                        .description("Серийный номер")
                        .build()
        );
        mockMvc.perform(
                put("/ENTERPRISE/items/identity")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(skuIdentities)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();
    }

    @Test
    @DatabaseSetup(value = "/items/db/1/storer-sku-pack-altsku.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/items/db/identities/ssku-identity.xml", connection = "wmwhseConnection",
            type = DatabaseOperation.REFRESH)
    @ExpectedDatabase(value = "/items/db/identities/ssku-identity-updated.xml", connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    public void updateSkuIdentity() throws Exception {
        Collection<SkuIdentityDTO> skuIdentities = new ArrayList<>();
        final SkuIdDto skuId = SkuIdDto.builder()
                .storerKey(603674L)
                .sku("ROV603674IPC647BOOKCCRYWHI")
                .build();
        skuIdentities.add(
                SkuIdentityDTO.builder()
                        .skuId(skuId)
                        .type("SN")
                        .requirements(1)
                        .regex("xyz")
                        .description("Серийный номер")
                        .build()
        );
        mockMvc.perform(
                put("/ENTERPRISE/items/identity")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(skuIdentities)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();
    }

    @Test
    @DatabaseSetup(value = "/items/db/1/storer-sku-pack-altsku.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/items/db/identities/ssku-identity.xml", connection = "wmwhseConnection",
            type = DatabaseOperation.REFRESH)
    @ExpectedDatabase(value = "/items/db/identities/ssku-identity-deleted.xml", connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    public void deleteSkuIdentity() throws Exception {
        Collection<SkuIdentityPkDTO> skuIdentityPkDTOS = new ArrayList<>();
        final SkuIdDto skuId = SkuIdDto.builder()
                .storerKey(603674L)
                .sku("ROV603674IPC647BOOKCCRYWHI")
                .build();
        skuIdentityPkDTOS.add(
                SkuIdentityPkDTO.builder()
                        .skuId(skuId)
                        .type("SN")
                        .build()
        );
        mockMvc.perform(
                delete("/ENTERPRISE/items/identity")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(skuIdentityPkDTOS)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();
    }

    @Test
    @DatabaseSetup(value = "/items/db/1/storer-sku-pack-altsku.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/items/db/identities/ssku-identity.xml", connection = "wmwhseConnection",
            type = DatabaseOperation.REFRESH)
    @ExpectedDatabase(value = "/items/db/identities/ssku-identity.xml", connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    public void deleteEmptyListOfSkuIdentity() throws Exception {
        mockMvc.perform(
                delete("/ENTERPRISE/items/identity")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Collections.emptyList())))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();
    }

    @Test
    @DatabaseSetup(value = "/items/db/1/storer-sku-pack-altsku.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/items/db/identities/ssku-identity.xml", connection = "wmwhseConnection",
            type = DatabaseOperation.REFRESH)
    @ExpectedDatabase(value = "/items/db/identities/ssku-identity.xml", connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    public void deleteUnknownSkuIdentity() throws Exception {
        Collection<SkuIdentityPkDTO> skuIdentityPkDTOS = new ArrayList<>();
        final SkuIdDto skuId = SkuIdDto.builder()
                .storerKey(603674L)
                .sku("ROV603674IPC647BOOKCCRYWHI")
                .build();
        skuIdentityPkDTOS.add(
                SkuIdentityPkDTO.builder()
                        .skuId(skuId)
                        .type(UUID.randomUUID().toString())
                        .build()
        );
        mockMvc.perform(
                delete("/ENTERPRISE/items/identity")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(skuIdentityPkDTOS)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();
    }

    @Test
    @DatabaseSetup(value = "/items/db/1/storer-sku-pack.xml", connection = "enterpriseConnection")
    @DatabaseSetup(value = "/items/db/1/storer-sku-pack-altsku.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/items/db/identities/ssku-identity.xml", connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    public void createImeiSn() throws Exception {
        List<Item> items = singletonList(defaultItemBuilder()
                .setCheckSn(1)
                .setSnMask("abc")
                .setCheckImei(2)
                .setImeiMask("123")
                .build());

        mockMvc.perform(
                put("/ENTERPRISE/items/itembatch?updateExisting=true&loadTrustworthy=false")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(items)))
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(value = "/items/db/1/storer-sku-pack.xml", connection = "enterpriseConnection")
    @DatabaseSetup(value = "/items/db/1/storer-sku-pack-altsku.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/items/db/goldsku/goldsku-create.xml", connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    public void createGoldSku() throws Exception {
        List<Item> items = singletonList(defaultItemBuilder()
                .setLifeTime(111)
                .setUpdatedDateTime(new DateTime("2022-01-01T00:00:00Z"))
                .build());

        mockMvc.perform(
                        put("/ENTERPRISE/items/itembatch?updateExisting=true&loadTrustworthy=false")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(items)))
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(value = "/items/db/1/storer-sku-pack.xml", connection = "enterpriseConnection")
    @DatabaseSetup(value = "/items/db/1/storer-sku-pack-altsku.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/items/db/identities/ssku-identity.xml", connection = "wmwhseConnection",
            type = DatabaseOperation.REFRESH)
    @ExpectedDatabase(value = "/items/db/identities/ssku-identity-deleted.xml", connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    public void deleteSnKeepImei() throws Exception {
        List<Item> items = singletonList(defaultItemBuilder()
                .setCheckSn(0)
                .setCheckImei(2)
                .setImeiMask("123")
                .build());

        mockMvc.perform(
                put("/ENTERPRISE/items/itembatch?updateExisting=true&loadTrustworthy=false")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(items)))
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(value = "/items/db/identities/cis/storer-sku-pack.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/items/db/identities/cis/storer-sku-pack.xml", connection = "enterpriseConnection")
    @ExpectedDatabase(value = "/items/db/identities/cis/sku-identity-cis-required-added.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT)
    public void putItemBatchWithNewlyAddedCisCargoType() throws Exception {
        List<CargoType> cargoTypes = Arrays.asList(CargoType.MEDICAL_SUPPLIES, CargoType.CIS_REQUIRED);
        List<Item> items = singletonList(defaultItemBuilder()
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
    @DatabaseSetup(value = "/items/db/identities/cis/storer-sku-pack-sku-identity-cis.xml", connection =
            "wmwhseConnection")
    @DatabaseSetup(value = "/items/db/identities/cis/storer-sku-pack.xml", connection = "enterpriseConnection")
    @ExpectedDatabase(value = "/items/db/identities/cis/sku-identity-cis-optional-updated.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT)
    public void putItemBatchWithUpdatedCisCargoType() throws Exception {
        List<CargoType> cargoTypes = Arrays.asList(CargoType.MEDICAL_SUPPLIES, CargoType.CIS_OPTIONAL);
        List<Item> items = singletonList(defaultItemBuilder()
                .setCargoTypes(new CargoTypes(cargoTypes))
                .setCisHandleMode(CisHandleMode.NO_RESTRICTION)
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
    @DatabaseSetup(value = "/items/db/identities/cis/storer-sku-pack-sku-identity-cis.xml", connection =
            "wmwhseConnection")
    @DatabaseSetup(value = "/items/db/identities/cis/storer-sku-pack.xml", connection = "enterpriseConnection")
    @ExpectedDatabase(value = "/items/db/identities/cis/sku-identity-cis-optional-updated.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT)
    public void putItemBatchWithCisDistinctCargoType() throws Exception {
        List<CargoType> cargoTypes = singletonList(CargoType.CIS_DISTINCT);
        List<Item> items = singletonList(defaultItemBuilder()
                .setCargoTypes(new CargoTypes(cargoTypes))
                .setCisHandleMode(CisHandleMode.NO_RESTRICTION)
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
    @DatabaseSetup(value = "/items/db/identities/cis/storer-sku-pack-sku-identity-cis.xml", connection =
            "wmwhseConnection")
    @DatabaseSetup(value = "/items/db/identities/cis/storer-sku-pack.xml", connection = "enterpriseConnection")
    @ExpectedDatabase(value = "/items/db/identities/cis/sku-identity-cis-deleted.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT)
    public void putItemBatchWithDeletedCisCargoType() throws Exception {
        List<Item> items = singletonList(defaultItemBuilder()
                .setCargoTypes(new CargoTypes(emptyList()))
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
    @DatabaseSetup(value = "/items/db/identities/cis/sku-identity-cis-cargotype.xml", connection =
            "wmwhseConnection")
    @DatabaseSetup(value = "/items/db/identities/cis/storer-sku-pack.xml", connection = "enterpriseConnection")
    @ExpectedDatabase(value = "/items/db/identities/cis/sku-identity-cis-deleted.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT)
    public void putItemBatchWithExistingCargoTypeAndNewCargoTypeUnknown() throws Exception {
        List<Item> items = singletonList(defaultItemBuilder().build());

        mockMvc.perform(
                put("/ENTERPRISE/items/itembatch?updateExisting=true&loadTrustworthy=false")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(items)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();
    }

    @Test
    @DatabaseSetup(value = "/items/db/identities/cis/storer-sku-pack.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/items/db/identities/cis/storer-sku-pack.xml", connection = "enterpriseConnection")
    @ExpectedDatabase(value = "/items/db/identities/cis/sku-identity-cis-required-added.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT)
    public void putItemBatchWithMultipleCisCargoType() throws Exception {
        List<CargoType> cargoTypes = Arrays.asList(
                CargoType.MEDICAL_SUPPLIES,
                CargoType.CIS_OPTIONAL,
                CargoType.CIS_REQUIRED,
                CargoType.CIS_DISTINCT,
                CargoType.CIS_OPTIONAL,
                CargoType.CIS_OPTIONAL
        );
        List<Item> items = singletonList(defaultItemBuilder()
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
    @DatabaseSetup(value = "/items/db/identities/cis/storer-sku-pack.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/items/db/identities/cis/storer-sku-pack.xml", connection = "enterpriseConnection")
    @ExpectedDatabase(value = "/items/db/identities/cis/storer-sku-pack-sku-identity-cis.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT)
    public void putItemBatchWithNewMarkHandleMode() throws Exception {
        List<CargoType> cargoTypes = Arrays.asList(CargoType.MEDICAL_SUPPLIES, CargoType.CIS_REQUIRED);
        List<Item> items = singletonList(defaultItemBuilder()
                .setCargoTypes(new CargoTypes(cargoTypes))
                .setCisHandleMode(CisHandleMode.ACCEPT_ONLY_DECLARED)
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
    @DatabaseSetup(value = "/items/db/identities/cis/storer-sku-pack-sku-identity-cis.xml", connection =
            "wmwhseConnection")
    @DatabaseSetup(value = "/items/db/identities/cis/storer-sku-pack.xml", connection = "enterpriseConnection")
    @ExpectedDatabase(value = "/items/db/identities/cis/storer-sku-pack-sku-identity-cis.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT)
    public void putItemBatchWithNoDegradeMarkHandleMode() throws Exception {
        List<CargoType> cargoTypes = Arrays.asList(CargoType.MEDICAL_SUPPLIES, CargoType.CIS_REQUIRED);
        List<Item> items = singletonList(defaultItemBuilder()
                .setCargoTypes(new CargoTypes(cargoTypes))
                .setCisHandleMode(CisHandleMode.NOT_DEFINED)
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
    @DatabaseSetup(value = "/items/db/identities/cis/storer-sku-pack-1P.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/items/db/identities/cis/storer-sku-pack-1P.xml", connection = "enterpriseConnection")
    @ExpectedDatabase(value = "/items/db/identities/cis/storer-sku-pack-sku-identity-cis-1P.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT)
    public void putItemBatchWithDistinct1P() throws Exception {
        List<Item> items = singletonList(
                new Item
                        .ItemBuilder("Test Item", 1, BigDecimal.TEN)
                        .setUnitId(new UnitId("12345", 465852L, "IPC647BOOKCCRYWHI"))
                        .setCargoType(CargoType.UNKNOWN)
                        .setBarcodes(singletonList(new Barcode.BarcodeBuilder("D8033830112492").build()))
                        .setBoxCapacity(1)
                        .setContractor(new Contractor("contractor-id", "contractor-name"))
                        .setCargoTypes(new CargoTypes(singletonList(CargoType.CIS_DISTINCT)))
                        .setCisHandleMode(CisHandleMode.ACCEPT_ONLY_DECLARED)
                        .build()
        );
        mockMvc.perform(
                put("/ENTERPRISE/items/itembatch?updateExisting=true&loadTrustworthy=false")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(items)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();
    }

    private Item.ItemBuilder defaultItemBuilder() {
        return new Item
                .ItemBuilder("Test Item", 1, BigDecimal.TEN)
                .setUnitId(new UnitId("12345", 603674L, "IPC647BOOKCCRYWHI"))
                .setCargoType(CargoType.UNKNOWN)
                .setCargoTypes(new CargoTypes(of(CargoType.UNKNOWN)))
                .setBarcodes(singletonList(new Barcode.BarcodeBuilder("D8033830112492").build()))
                .setBoxCapacity(1)
                .setContractor(new Contractor("contractor-id", "contractor-name"));
    }
}
