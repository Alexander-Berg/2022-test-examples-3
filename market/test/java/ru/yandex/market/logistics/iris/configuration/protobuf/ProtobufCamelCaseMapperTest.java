package ru.yandex.market.logistics.iris.configuration.protobuf;

import java.io.IOException;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.ir.http.MdmIrisPayload;

public class ProtobufCamelCaseMapperTest {
    private ProtobufMapper instanceToTest;

    @Before
    public void setUp() {
        instanceToTest = new ProtobufCamelCaseMapper();
    }

    @Test
    public void whenReadSingleEscapedValueShouldParseItem() throws IOException {
        MdmIrisPayload.Item item = MdmIrisPayload.Item.newBuilder()
                .setItemId(MdmIrisPayload.MdmIdentifier.newBuilder().setSupplierId(1).setShopSku("asdggfdh"))
                .build();
        String reference = "{\"itemId\": {\"shopSku\": \"asdggfdh\", \"supplierId\": \"1\"}, \"information\": []}";

        MdmIrisPayload.Item.Builder builderToEnrich = MdmIrisPayload.Item.getDefaultInstance().toBuilder();
        instanceToTest.mergeJson(reference, builderToEnrich);
        MdmIrisPayload.Item parsedItem = builderToEnrich.build();

        Assertions.assertThat(parsedItem).isEqualTo(item);
    }

    @Test
    public void whenReadDoubleEscapedValueShouldParseItem() throws IOException {
        MdmIrisPayload.Item item = MdmIrisPayload.Item.newBuilder()
                .setItemId(MdmIrisPayload.MdmIdentifier.newBuilder().setSupplierId(1).setShopSku("asdggfdh"))
                .build();
        String reference = "\"{\\n   \\\"itemId\\\": {\\n" +
                "   \\\"shopSku\\\": \\\"asdggfdh\\\"," +
                " \\\"supplierId\\\": \\\"1\\\"}, \\\"information\\\": []}\"";

        MdmIrisPayload.Item.Builder builderToEnrich = MdmIrisPayload.Item.getDefaultInstance().toBuilder();
        instanceToTest.mergeJson(reference, builderToEnrich);
        MdmIrisPayload.Item parsedItem = builderToEnrich.build();

        Assertions.assertThat(parsedItem).isEqualTo(item);
    }

    @Test
    public void whenSerializeToJsonShouldParseToEqualItem() throws IOException {
        MdmIrisPayload.Item item = MdmIrisPayload.Item.newBuilder()
                .setItemId(MdmIrisPayload.MdmIdentifier.newBuilder().setSupplierId(1).setShopSku("asdggfdh"))
                .build();

        String message = instanceToTest.toJsonString(item);

        MdmIrisPayload.Item.Builder builderToEnrich = MdmIrisPayload.Item.getDefaultInstance().toBuilder();
        instanceToTest.mergeJson(message, builderToEnrich);
        MdmIrisPayload.Item parsedItem = builderToEnrich.build();

        Assertions.assertThat(parsedItem).isEqualTo(item);
    }

    @Test
    public void whenReadSingleEscapedValueShouldParseItemEvenIfItIsInSnakeCase() throws IOException {
        MdmIrisPayload.Item item = MdmIrisPayload.Item.newBuilder()
                .setItemId(MdmIrisPayload.MdmIdentifier.newBuilder().setSupplierId(1).setShopSku("asdggfdh"))
                .build();
        String reference = "{\"item_id\": {\"shop_sku\": \"asdggfdh\", \"supplier_id\": \"1\"}, \"information\": []}";

        MdmIrisPayload.Item.Builder builderToEnrich = MdmIrisPayload.Item.getDefaultInstance().toBuilder();
        instanceToTest.mergeJson(reference, builderToEnrich);
        MdmIrisPayload.Item parsedItem = builderToEnrich.build();

        Assertions.assertThat(parsedItem).isEqualTo(item);
    }

    @Test
    public void whenReadSingleEscapedValueShouldParseCompleteItem() throws IOException {
        MdmIrisPayload.CompleteItem item = MdmIrisPayload.CompleteItem.newBuilder()
                .setItemId(MdmIrisPayload.MdmIdentifier.newBuilder().setSupplierId(1).setShopSku("asdggfdh"))
                .build();
        String reference = "{\"itemId\": {\"shopSku\": \"asdggfdh\", \"supplierId\": \"1\"}, \"information\": []}";

        MdmIrisPayload.CompleteItem.Builder builderToEnrich = MdmIrisPayload.CompleteItem.getDefaultInstance().toBuilder();
        instanceToTest.mergeJson(reference, builderToEnrich);
        MdmIrisPayload.CompleteItem parsedItem = builderToEnrich.build();

        Assertions.assertThat(parsedItem).isEqualTo(item);
    }

    @Test
    public void whenReadDoubleEscapedValueShouldParseCompleteItem() throws IOException {
        MdmIrisPayload.CompleteItem item = MdmIrisPayload.CompleteItem.newBuilder()
                .setItemId(MdmIrisPayload.MdmIdentifier.newBuilder().setSupplierId(1).setShopSku("asdggfdh"))
                .build();
        String reference = "\"{\\n   \\\"itemId\\\": {\\n" +
                "   \\\"shopSku\\\": \\\"asdggfdh\\\"," +
                " \\\"supplierId\\\": \\\"1\\\"}, \\\"information\\\": []}\"";

        MdmIrisPayload.CompleteItem.Builder builderToEnrich = MdmIrisPayload.CompleteItem.getDefaultInstance().toBuilder();
        instanceToTest.mergeJson(reference, builderToEnrich);
        MdmIrisPayload.CompleteItem parsedItem = builderToEnrich.build();

        Assertions.assertThat(parsedItem).isEqualTo(item);
    }

    @Test
    public void whenSerializeToJsonShouldParseToEqualCompleteItem() throws IOException {
        MdmIrisPayload.CompleteItem item = MdmIrisPayload.CompleteItem.newBuilder()
                .setItemId(MdmIrisPayload.MdmIdentifier.newBuilder().setSupplierId(1).setShopSku("asdggfdh"))
                .build();

        String message = instanceToTest.toJsonString(item);

        MdmIrisPayload.CompleteItem.Builder builderToEnrich = MdmIrisPayload.CompleteItem.getDefaultInstance().toBuilder();
        instanceToTest.mergeJson(message, builderToEnrich);
        MdmIrisPayload.CompleteItem parsedCompleteItem = builderToEnrich.build();

        Assertions.assertThat(parsedCompleteItem).isEqualTo(item);
    }

    @Test
    public void whenReadSingleEscapedValueShouldParseCompleteItemEvenIfItIsInSnakeCase() throws IOException {
        MdmIrisPayload.CompleteItem item = MdmIrisPayload.CompleteItem.newBuilder()
                .setItemId(MdmIrisPayload.MdmIdentifier.newBuilder().setSupplierId(1).setShopSku("asdggfdh"))
                .build();
        String reference = "{\"item_id\": {\"shop_sku\": \"asdggfdh\", \"supplier_id\": \"1\"}, \"information\": []}";

        MdmIrisPayload.CompleteItem.Builder builderToEnrich = MdmIrisPayload.CompleteItem.getDefaultInstance().toBuilder();
        instanceToTest.mergeJson(reference, builderToEnrich);
        MdmIrisPayload.CompleteItem parsedItem = builderToEnrich.build();

        Assertions.assertThat(parsedItem).isEqualTo(item);
    }
}