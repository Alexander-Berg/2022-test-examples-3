package ru.yandex.market.mboc.common.masterdata.repository;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.NumericNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.mboc.common.MdmBaseIntegrationTestClass;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.masterdata.model.MasterData;
import ru.yandex.market.mboc.common.masterdata.model.TimeInUnits;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;

import static org.assertj.core.api.Java6Assertions.assertThat;

/**
 * @author jkt on 02.10.18.
 */
@SuppressWarnings("checkstyle:magicNumber")
public class MasterDataSerializationTest extends MdmBaseIntegrationTestClass {

    private static final String MANUFACTURER_COUNTRY_SINGLE = "Китай";
    private static final int SEED = 123749;
    private static final List<String> MANUFACTURER_COUNTRIES_MULTIPLE = Arrays.asList("Китай", "Россия");
    private static final int SUPPLIER_ID = 1;
    @Autowired
    MasterDataRepository masterDataRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;
    private EnhancedRandom random = TestDataUtils.defaultRandom(SEED);
    private ShopSkuKey shopSkuKey;

    @Before
    public void insertOffersAndSuppliers() {
        shopSkuKey = new ShopSkuKey(SUPPLIER_ID, "3");
    }

    @Test
    public void whenDeserializeManufacturerCountryFromStringShouldTransformToArray() {
        TextNode textNode = JsonNodeFactory.instance.textNode(MANUFACTURER_COUNTRY_SINGLE);
        jdbcTemplate.execute(insertMasterDataSql(generateMasterDataJsonWithRandomData(
            json -> json.set(MasterData.MANUFACTURER_COUNTRY_FIELD, textNode))));

        MasterData masterDataFromDb = masterDataRepository.findById(shopSkuKey);

        assertThat(masterDataFromDb.getManufacturerCountries()).containsExactlyInAnyOrder(MANUFACTURER_COUNTRY_SINGLE);
    }

    @Test
    public void whenDeserializeManufacturerCountryFromNullShouldTransformToEmptyArray() {
        NullNode nullNode = JsonNodeFactory.instance.nullNode();
        jdbcTemplate.execute(insertMasterDataSql(generateMasterDataJsonWithRandomData(
            json -> json.set(MasterData.MANUFACTURER_COUNTRY_FIELD, nullNode))));

        MasterData masterDataFromDb = masterDataRepository.findById(shopSkuKey);

        assertThat(masterDataFromDb.getManufacturerCountries()).isEmpty();
    }

    @Test
    public void deserializeShouldBeBackwardCompatible() {
        // don't change test data! fix serializer
        jdbcTemplate.execute(insertMasterDataSql("{\n" +
            "  \"vat\": null,\n" +
            "  \"boxCount\": null,\n" +
            "  \"lifeTime\": null,\n" +
            "  \"heavyGood\": false,\n" +
            "  \"shelfLife\": null,\n" +
            "  \"minShipment\": 0,\n" +
            "  \"deliveryTime\": 0,\n" +
            "  \"manufacturer\": null,\n" +
            "  \"dangerousGood\": null,\n" +
            "  \"supplySchedule\": [],\n" +
            "  \"guaranteePeriod\": null,\n" +
            "  \"lifeTimeComment\": null,\n" +
            "  \"quantumOfSupply\": 0,\n" +
            "  \"itemShippingUnit\": null,\n" +
            "  \"shelfLifeComment\": null,\n" +
            "  \"shelfLifeRequired\": false,\n" +
            "  \"transportUnitSize\": 0,\n" +
            "  \"manufacturerCountry\": [\n" +
            "    \"Россия\"\n" +
            "  ],\n" +
            "  \"customsCommodityCode\": null,\n" +
            "  \"nonItemShippingUnits\": [],\n" +
            "  \"guaranteePeriodComment\": null\n" +
            "}"));

        MasterData masterDataFromDb = masterDataRepository.findById(shopSkuKey);
    }

    @Test
    public void whenDeserializeManufacturerCountryFromArrayShouldUseAsArray() {
        ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
        MANUFACTURER_COUNTRIES_MULTIPLE.forEach(arrayNode::add);

        jdbcTemplate.execute(insertMasterDataSql(generateMasterDataJsonWithRandomData(
            json -> json.set(MasterData.MANUFACTURER_COUNTRY_FIELD, arrayNode)))
        );

        MasterData masterDataFromDb = masterDataRepository.findById(shopSkuKey);

        assertThat(masterDataFromDb.getManufacturerCountries())
            .containsExactlyInAnyOrderElementsOf(MANUFACTURER_COUNTRIES_MULTIPLE);
    }

    @Test
    public void deserializeShelfLifeAsTimeInUnits() {
        TimeInUnits shelfLife = new TimeInUnits(140, TimeInUnits.TimeUnit.DAY);
        ValueNode node = JsonNodeFactory.instance.pojoNode(shelfLife);

        jdbcTemplate.execute(insertMasterDataSql(generateMasterDataJsonWithRandomData(
            json -> json.set("shelfLife", node)))
        );

        MasterData masterDataFromDb = masterDataRepository.findById(shopSkuKey);

        assertThat(masterDataFromDb.getShelfLife()).isEqualTo(shelfLife);
    }

    @Test
    public void deserializeShelfLifeAsInteger() {
        TimeInUnits shelfLife = new TimeInUnits(140, TimeInUnits.TimeUnit.DAY);
        NumericNode node = JsonNodeFactory.instance.numberNode(140);

        jdbcTemplate.execute(insertMasterDataSql(generateMasterDataJsonWithRandomData(
            json -> json.set("shelfLife", node)))
        );

        MasterData masterDataFromDb = masterDataRepository.findById(shopSkuKey);

        assertThat(masterDataFromDb.getShelfLife()).isEqualTo(shelfLife);
    }

    @Test
    public void deserializeShelfLifeAsNull() {
        NullNode node = JsonNodeFactory.instance.nullNode();

        jdbcTemplate.execute(insertMasterDataSql(generateMasterDataJsonWithRandomData(
            json -> json.set("shelfLife", node)))
        );

        MasterData masterDataFromDb = masterDataRepository.findById(shopSkuKey);

        assertThat(masterDataFromDb.getShelfLife()).isEqualTo(null);
    }

    private String insertMasterDataSql(String masterDataJson) {
        return "INSERT INTO mdm.master_data\n" +
            "VALUES ('" + shopSkuKey.getShopSku() + "', " + shopSkuKey.getSupplierId() + ", '" + masterDataJson + "')";
    }

    @SuppressWarnings("checkstyle:linelength")
    private String generateMasterDataJsonWithRandomData(Consumer<ObjectNode> jsonFormatter) {
        try {
            ObjectMapper mapper = new ObjectMapper();

            MasterData masterData = TestDataUtils.generateMasterData("shop_sku", 1, random);
            ObjectNode jsonNodes = mapper.valueToTree(masterData);
            jsonFormatter.accept(jsonNodes);

            return mapper.writeValueAsString(jsonNodes);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Error generating json", ex);
        }
    }
}
