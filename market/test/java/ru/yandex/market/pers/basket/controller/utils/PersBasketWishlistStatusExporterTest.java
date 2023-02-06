package ru.yandex.market.pers.basket.controller.utils;

import java.time.Instant;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.pers.basket.model.BasketItemAction;
import ru.yandex.market.pers.basket.model.BasketReferenceItem;
import ru.yandex.market.pers.basket.utils.PersBasketWishlistStatusExporter;
import ru.yandex.market.pers.list.model.BasketOwner;
import ru.yandex.market.pers.list.model.v2.enums.MarketplaceColor;
import ru.yandex.market.pers.list.model.v2.enums.ReferenceType;

import static ru.yandex.market.pers.list.model.v2.enums.SecondaryReferenceType.HID;

public class PersBasketWishlistStatusExporterTest {

    @Test
    public void convert2tskvBlue() {
        final Instant timestamp = Instant.parse("2020-01-22T11:27:43.859Z");
        BasketReferenceItem item = buildStubRef(MarketplaceColor.BLUE, ReferenceType.SKU, "95483");
        Assert.assertEquals("tskv\tUID=32\tmsku=95483\tregionId=23\tstatus=ADD\ttimestamp=1579692463",
            PersBasketWishlistStatusExporter.buildLegacyItemTskv(
                BasketOwner.fromUid(32),
                item,
                23,
                BasketItemAction.ADD,
                timestamp));

        // check region defaults
        Assert.assertEquals("tskv\tYANDEXUID=5625624\tmsku=95483\tregionId=42\tstatus=ADD\ttimestamp=1579692463",
            PersBasketWishlistStatusExporter.buildLegacyItemTskv(
                BasketOwner.fromYandexUid("5625624"),
                item,
                null,
                BasketItemAction.ADD,
                timestamp));

        // check empty region
        item.setRegionId(null);
        Assert.assertEquals("tskv\tYANDEXUID=5625624\tmsku=95483\tstatus=ADD\ttimestamp=1579692463",
            PersBasketWishlistStatusExporter.buildLegacyItemTskv(
                BasketOwner.fromYandexUid("5625624"),
                item,
                null,
                BasketItemAction.ADD,
                timestamp));

        // try other action
        Assert.assertEquals("tskv\tUUID=234234234\tmsku=95483\tregionId=23\tstatus=DELETE\ttimestamp=1579692463",
            PersBasketWishlistStatusExporter.buildLegacyItemTskv(
                BasketOwner.fromUuid("234234234"),
                item,
                23,
                BasketItemAction.DELETE,
                timestamp));

        // try invalid items
        item = buildStubRef(MarketplaceColor.BLUE, ReferenceType.OFFER, "95483");
        Assert.assertNull(PersBasketWishlistStatusExporter.buildLegacyItemTskv(
            BasketOwner.fromUuid("234234234"),
            item,
            23,
            BasketItemAction.DELETE,
            timestamp));

        item = buildStubRef(MarketplaceColor.BLUE, ReferenceType.PRODUCT, "95483");
        Assert.assertNull(PersBasketWishlistStatusExporter.buildLegacyItemTskv(
            BasketOwner.fromUuid("234234234"),
            item,
            23,
            BasketItemAction.DELETE,
            timestamp));
    }

    @Test
    public void convert2tskvWhite() {
        Instant timestamp = Instant.parse("2020-01-22T01:27:43.859Z");
        BasketReferenceItem item = buildStubRef(MarketplaceColor.WHITE, ReferenceType.PRODUCT, "95483");
        Assert.assertEquals("tskv\tbasket_item_id=1\tpuid=32\tgroup_id=95483\tmodel_id=95483\tcluster_id=95483\t" +
                "region_id=23\taction=1\tchange_date=2020-01-22\tunixtime=1579656463",
            PersBasketWishlistStatusExporter.buildLegacyItemTskv(
                BasketOwner.fromUid(32),
                item,
                23,
                BasketItemAction.ADD,
                timestamp));

        // check region defaults
        item.setRegionId(42);
        Assert.assertEquals("tskv\tbasket_item_id=1\tpuid=32\tgroup_id=95483\tmodel_id=95483\tcluster_id=95483\t" +
                "region_id=42\taction=1\tchange_date=2020-01-22\tunixtime=1579656463",
            PersBasketWishlistStatusExporter.buildLegacyItemTskv(
                BasketOwner.fromUid(32),
                item,
                null,
                BasketItemAction.ADD,
                timestamp));

        // add hid
        item.setData(Map.of(HID.getName(), "21"));
        Assert.assertEquals("tskv\tbasket_item_id=1\tpuid=32\tgroup_id=95483\tmodel_id=95483\tcluster_id=95483\t" +
                "region_id=23\thid=21\taction=1\tchange_date=2020-01-22\tunixtime=1579656463",
            PersBasketWishlistStatusExporter.buildLegacyItemTskv(
                BasketOwner.fromUid(32),
                item,
                23,
                BasketItemAction.ADD,
                timestamp));

        // invalid hid
        item.setData(Map.of(HID.getName(), "21qqqq"));
        Assert.assertEquals("tskv\tbasket_item_id=1\tpuid=32\tgroup_id=95483\tmodel_id=95483\tcluster_id=95483\t" +
                "region_id=23\taction=1\tchange_date=2020-01-22\tunixtime=1579656463",
            PersBasketWishlistStatusExporter.buildLegacyItemTskv(
                BasketOwner.fromUid(32),
                item,
                23,
                BasketItemAction.ADD,
                timestamp));

        // another action + chance timestamp
        timestamp = Instant.parse("2020-01-21T23:27:43.859Z");
        Assert.assertEquals("tskv\tbasket_item_id=1\tpuid=32\tgroup_id=95483\tmodel_id=95483\tcluster_id=95483\t" +
                "region_id=23\taction=2\tchange_date=2020-01-21\tunixtime=1579649263",
            PersBasketWishlistStatusExporter.buildLegacyItemTskv(
                BasketOwner.fromUid(32),
                item,
                23,
                BasketItemAction.DELETE,
                timestamp));

        //invalid
        item = buildStubRef(MarketplaceColor.WHITE, ReferenceType.SKU, "95483");
        Assert.assertNull(PersBasketWishlistStatusExporter.buildLegacyItemTskv(
            BasketOwner.fromUuid("234234234"),
            item,
            23,
            BasketItemAction.ADD,
            timestamp));

        item = buildStubRef(MarketplaceColor.WHITE, ReferenceType.OFFER, "95483");
        Assert.assertNull(PersBasketWishlistStatusExporter.buildLegacyItemTskv(
            BasketOwner.fromUuid("234234234"),
            item,
            23,
            BasketItemAction.ADD,
            timestamp));
    }

    private BasketReferenceItem buildStubRef(MarketplaceColor color, ReferenceType type, String refId) {
        BasketReferenceItem result = new BasketReferenceItem();
        result.setId(1L);
        result.setOwnerId(2L);
        result.setTitle("stub");
        result.setRegionId(42);
        result.setColor(color);
        result.setReferenceType(type);
        result.setReferenceId(refId);
        return result;
    }
}
