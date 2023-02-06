package ru.yandex.market.pricelabs.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.inside.yt.kosher.impl.ytree.object.serializers.YTreeObjectSerializer;
import ru.yandex.inside.yt.kosher.impl.ytree.object.serializers.YTreeObjectSerializerFactory;
import ru.yandex.inside.yt.kosher.impl.ytree.serialization.YTreeDeepCopier;
import ru.yandex.market.pricelabs.model.types.OfferType;
import ru.yandex.market.pricelabs.model.types.ShopStatus;
import ru.yandex.market.pricelabs.model.types.Status;
import ru.yandex.market.pricelabs.model.types.StrategyFormType;
import ru.yandex.market.pricelabs.model.types.StrategyType;
import ru.yandex.yt.ytclient.object.MappedRowSerializer;
import ru.yandex.yt.ytclient.object.MappedRowsetDeserializer;
import ru.yandex.yt.ytclient.object.UnversionedRowsetDeserializer;
import ru.yandex.yt.ytclient.tables.ColumnValueType;
import ru.yandex.yt.ytclient.wire.UnversionedRow;
import ru.yandex.yt.ytclient.wire.UnversionedValue;
import ru.yandex.yt.ytclient.wire.WireProtocolReader;
import ru.yandex.yt.ytclient.wire.WireProtocolWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SerializationTest {

    private List<Offer> list;
    private Offer offer;

    private MappedRowsetDeserializer<Offer> rowsetDeserializer;
    private MappedRowSerializer<Offer> rowSerializer;

    @BeforeEach
    void init() {

        list = new ArrayList<>();
        YTreeObjectSerializer<Offer> serializer =
                (YTreeObjectSerializer<Offer>) YTreeObjectSerializerFactory.forClass(Offer.class);

        rowSerializer = MappedRowSerializer.forClass(serializer);
        rowsetDeserializer = MappedRowsetDeserializer.forClass(rowSerializer.getSchema(), serializer, list::add);

        Instant zero = Instant.ofEpochMilli(0);

        offer = new Offer();
        offer.setShop_id(9612);
        offer.setFeed_id(8859);
        offer.setOffer_id("test");
        offer.setModel_id(1717188835);
        offer.setType(OfferType.UNSUPPORTED);
        offer.setCategory_id(7);
        offer.setMarket_category_id(10682501);
        offer.setCurrency_id("");
        offer.setPrice(545000);
        offer.setName("Chicco Подвеска \"Активная Гимнастика\" Deluxe 65408.20");
        offer.setVendor_code("");
        offer.setCreated_at(Instant.parse("2015-03-05T07:19:50.000Z"));
        offer.setUpdated_at(Instant.parse("2019-05-20T14:15:02.598Z"));
        offer.setStrategy_updated_at(zero);
        offer.setStrategy_modified_at(zero);
        offer.setStrategy_applied_at(zero);
        offer.setRecommendations_updated_at(zero);
        offer.setVendor_strategy_updated_at(zero);
        offer.setVendor_strategy_modified_at(zero);
        offer.setIn_stock_count(1);
        offer.setStatus(Status.DELETED);
        offer.setIn_model_count(1);
        offer.setQuery("");
        offer.setVendor_name("");
        offer.setWare_md5("");
        offer.setParams_map(Object2ObjectMaps.emptyMap());
        offer.setMin_price_offer_id("");
        offer.setApp_strategy_id(1L);
        offer.setStrategy_type(StrategyType.NONE);
        offer.setShop_sku("");
        offer.normalizeFields();
        offer.setMax_bid(0L);
    }

    @Test
    void testSerializeDeserialize() {
        List<byte[]> bytes = this.write();

        WireProtocolReader reader = new WireProtocolReader(bytes);
        reader.readUnversionedRowset(rowsetDeserializer);

        Offer actual = list.remove(0);
        assertEquals(offer, actual);
    }

    @Test
    void testSerializeOnlyChanged() {
        offer.saveYTreeObjectState();
        offer.setPrice(445);

        var strategy = new Strategy();
        strategy.setStrategy_id(1);
        strategy.setType(StrategyFormType.MAIN);
        offer.setApp_strategy_id(StrategyPair.fromMain(strategy).getStrategy_id()); // Стратегия не изменилась уже 1)
        offer.setStrategy_updated_at(Instant.now()); // форсировано меняем дату

        List<byte[]> bytes = this.write();
        WireProtocolReader reader = new WireProtocolReader(bytes);

        UnversionedRowsetDeserializer deserializer = new UnversionedRowsetDeserializer(rowSerializer.getSchema());
        List<UnversionedRow> rows = reader.readUnversionedRowset(deserializer).getRowset().getRows();

        assertEquals(1, rows.size());

        UnversionedRow expectRow = new UnversionedRow(List.of(
                new UnversionedValue(0, ColumnValueType.INT64, false, (long) offer.getShop_id()),
                new UnversionedValue(1, ColumnValueType.INT64, false, (long) offer.getFeed_id()),
                new UnversionedValue(2, ColumnValueType.STRING, false, offer.getOffer_id().getBytes()),
                new UnversionedValue(12, ColumnValueType.INT64, false, offer.getPrice()),
                new UnversionedValue(22, ColumnValueType.INT64, false, offer.getUpdated_at().toEpochMilli()),
                new UnversionedValue(32, ColumnValueType.INT64, false, offer.getStrategy_updated_at().toEpochMilli())));
        assertEquals(expectRow, rows.get(0));
    }

    @Test
    void testDeepCopy() {
        var shop = new Shop();
        shop.setShop_id(1);
        shop.setStatus(ShopStatus.ACTIVE);
        shop.setDomain("domain");
        var shop2 = YTreeDeepCopier.deepCopyOf(shop);
        assertEquals(shop, shop2);
    }

    private List<byte[]> write() {
        WireProtocolWriter writer = new WireProtocolWriter();
        writer.writeUnversionedRowset(List.of(offer), rowSerializer);
        return writer.finish();
    }
}
