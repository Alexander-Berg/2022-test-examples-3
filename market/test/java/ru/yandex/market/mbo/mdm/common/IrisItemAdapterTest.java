package ru.yandex.market.mbo.mdm.common;

import java.time.Instant;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.ir.http.MdmIrisPayload;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.FromIrisItemWrapper;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ItemWrapperTestUtil;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;

public class IrisItemAdapterTest {
    private static final Gson GSON = new GsonBuilder()
        .registerTypeAdapter(MdmIrisPayload.Item.class, new IrisItemAdapter())
        .create();

    @Test
    public void testIncludingSupplierSourceType() {
        //given
        MdmIrisPayload.ShippingUnit.Builder shippingUnit = ItemWrapperTestUtil.generateShippingUnit(
            10.0,
            11.0,
            12.0,
            1.0,
            0.8,
            0.2,
            1640070000000L
        );
        MdmIrisPayload.Item item = ItemWrapperTestUtil.createItem(
            new ShopSkuKey(10, "11"),
            MdmIrisPayload.MasterDataSource.SUPPLIER,
            "Откосы и отвесы",
            shippingUnit
        );
        FromIrisItemWrapper fromIrisItemWrapper = new FromIrisItemWrapper(item)
            .setReceivedTs(Instant.parse("2021-12-21T10:15:30.00Z"));

        //when
        String fromIrisItemAsJson = GSON.toJson(fromIrisItemWrapper);

        //then
        Assertions.assertThat(fromIrisItemAsJson)
            .isEqualTo(
                "{\"receivedTs\":{\"seconds\":1640081730,\"nanos\":0},\"item\":{\n" +
                    "  \"itemId\": {\n" +
                    "    \"supplierId\": \"10\",\n" +
                    "    \"shopSku\": \"11\"\n" +
                    "  },\n" +
                    "  \"information\": [{\n" +
                    "    \"source\": {\n" +
                    "      \"id\": \"Откосы и отвесы\",\n" +
                    "      \"type\": \"SUPPLIER\"\n" +
                    "    },\n" +
                    "    \"itemShippingUnit\": {\n" +
                    "      \"weightGrossMg\": {\n" +
                    "        \"value\": \"1000000\",\n" +
                    "        \"updatedTs\": \"1640070000000\"\n" +
                    "      },\n" +
                    "      \"weightNetMg\": {\n" +
                    "        \"value\": \"800000\",\n" +
                    "        \"updatedTs\": \"1640070000000\"\n" +
                    "      },\n" +
                    "      \"weightTareMg\": {\n" +
                    "        \"value\": \"200000\",\n" +
                    "        \"updatedTs\": \"1640070000000\"\n" +
                    "      },\n" +
                    "      \"widthMicrometer\": {\n" +
                    "        \"value\": \"110000\",\n" +
                    "        \"updatedTs\": \"1640070000000\"\n" +
                    "      },\n" +
                    "      \"heightMicrometer\": {\n" +
                    "        \"value\": \"120000\",\n" +
                    "        \"updatedTs\": \"1640070000000\"\n" +
                    "      },\n" +
                    "      \"lengthMicrometer\": {\n" +
                    "        \"value\": \"100000\",\n" +
                    "        \"updatedTs\": \"1640070000000\"\n" +
                    "      }\n" +
                    "    }\n" +
                    "  }]\n" +
                    "},\"isProcessed\":false}"
            );
    }

    @Test
    public void testToAndBack() {
        //given
        MdmIrisPayload.ShippingUnit.Builder shippingUnit = ItemWrapperTestUtil.generateShippingUnit(
            10.0,
            11.0,
            12.0,
            1.0,
            0.8,
            0.2,
            1640070000000L
        );
        MdmIrisPayload.Item item = ItemWrapperTestUtil.createItem(
            new ShopSkuKey(10, "11"),
            MdmIrisPayload.MasterDataSource.SUPPLIER,
            "Откосы и отвесы",
            shippingUnit
        );
        FromIrisItemWrapper fromIrisItemWrapper = new FromIrisItemWrapper(item)
            .setReceivedTs(Instant.parse("2021-12-21T10:15:30.00Z"));

        //when
        String fromIrisItemAsJson = GSON.toJson(fromIrisItemWrapper);
        FromIrisItemWrapper fromIrisItemFromJson = GSON.fromJson(fromIrisItemAsJson, FromIrisItemWrapper.class);

        //then
        Assertions.assertThat(fromIrisItemFromJson).isEqualTo(fromIrisItemWrapper);
    }
}
