package ru.yandex.market.mboc.common.utils;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.ir.http.MdmIrisPayload;
import ru.yandex.market.mbo.lightmapper.ProtobufMapper;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;

public class ProtobufMapperTest extends MdmBaseDbTestClass {

    private static final long SEED = 5375632L;

    private final ProtobufMapper<MdmIrisPayload.Item> protobufMapper = new ProtobufMapper<>(
        MdmIrisPayload.Item::getDefaultInstance
    );

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private EnhancedRandom defaultRandom;

    @Before
    public void setUp() throws Exception {
        defaultRandom = TestDataUtils.defaultRandom(SEED);
    }

    @Test
    public void whenSerializeToJsonShouldParseToEqualItem() throws IOException {
        MdmIrisPayload.Item item = defaultRandom.nextObject(MdmIrisPayload.Item.class);

        String message = protobufMapper.serializeMessage(item);
        MdmIrisPayload.Item parsedItem = protobufMapper.parseMessage(message);

        Assertions.assertThat(parsedItem).isEqualTo(item);
    }

    @Test
    public void whenSaveToPgShouldSelectEqualItem() {
        String tableName = generateTableName();
        jdbcTemplate.execute("create table " + tableName + " (item jsonb)");

        MdmIrisPayload.Item item = defaultRandom.nextObject(MdmIrisPayload.Item.class);

        jdbcTemplate.update(
            "insert into " + tableName + " (item) values (?)",
            protobufMapper.toPreparedStatementSetter(item)
        );

        List<MdmIrisPayload.Item> result = jdbcTemplate.query(
            "select * from " + tableName,
            protobufMapper.toRowMapper()
        );

        Assertions.assertThat(result).containsExactly(item);
    }

    @Test
    public void whenSaveToPgShouldSelectValidJson() {
        String tableName = generateTableName();
        jdbcTemplate.execute("create table " + tableName + " (item jsonb)");

        MdmIrisPayload.Item item = MdmIrisPayload.Item.newBuilder()
            .setItemId(MdmIrisPayload.MdmIdentifier.newBuilder().setSupplierId(1).setShopSku("asdggfdh"))
            .build();

        jdbcTemplate.update(
            "insert into " + tableName + " (item) values (?)",
            protobufMapper.toPreparedStatementSetter(item)
        );

        List<String> result = jdbcTemplate.query(
            "select * from " + tableName, (rs, rowNum) -> rs.getString(1)
        );
        String reference = "{\"itemId\": {\"shopSku\": \"asdggfdh\", \"supplierId\": \"1\"}, \"information\": []}";
        Assertions.assertThat(result).containsExactly(reference);
    }

    @Test
    public void whenReadSingleEscapedValueShouldParse() throws IOException {
        MdmIrisPayload.Item item = MdmIrisPayload.Item.newBuilder()
            .setItemId(MdmIrisPayload.MdmIdentifier.newBuilder().setSupplierId(1).setShopSku("asdggfdh"))
            .build();
        String reference = "{\"itemId\": {\"shopSku\": \"asdggfdh\", \"supplierId\": \"1\"}, \"information\": []}";
        MdmIrisPayload.Item parsedItem = protobufMapper.parseMessage(reference);
        Assertions.assertThat(parsedItem).isEqualTo(item);
    }

    @Test
    public void whenReadDoubleEscapedValueShouldParse() throws IOException {
        MdmIrisPayload.Item item = MdmIrisPayload.Item.newBuilder()
            .setItemId(MdmIrisPayload.MdmIdentifier.newBuilder().setSupplierId(1).setShopSku("asdggfdh"))
            .build();
        String reference = "\"{\\n   \\\"itemId\\\": {\\n" +
            "   \\\"shopSku\\\": \\\"asdggfdh\\\"," +
            " \\\"supplierId\\\": \\\"1\\\"}, \\\"information\\\": []}\"";
        MdmIrisPayload.Item parsedItem = protobufMapper.parseMessage(reference);
        Assertions.assertThat(parsedItem).isEqualTo(item);
    }

    public String generateTableName() {
        UUID uuid = UUID.randomUUID();
        return "mdm.protobuf_mapper_test_" +
            Math.abs(uuid.getMostSignificantBits()) + "_" + Math.abs(uuid.getLeastSignificantBits());
    }

}
