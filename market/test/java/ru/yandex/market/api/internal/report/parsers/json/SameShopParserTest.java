package ru.yandex.market.api.internal.report.parsers.json;

import org.junit.Assert;
import org.junit.Test;
import javax.inject.Inject;
import ru.yandex.market.api.domain.Field;
import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.internal.report.ReportRequestContext;
import ru.yandex.market.api.internal.report.parsers.ReportParserFactory;
import ru.yandex.market.api.internal.report.parsers.SameShopResponseParser;
import ru.yandex.market.api.internal.report.sameshop.report.OfferPack;
import ru.yandex.market.api.internal.report.sameshop.report.OfferPackItem;
import ru.yandex.market.api.internal.report.sameshop.report.OfferPackItemType;
import ru.yandex.market.api.internal.report.sameshop.report.SameShopResponse;
import ru.yandex.market.api.util.ResourceHelpers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SameShopParserTest extends BaseTest {

    @Inject
    private ReportParserFactory reportParserFactory;


    @Test
    public void sample() {
        SameShopResponse result = getParser().parse(ResourceHelpers.getResource("same-shop-sample.json"));
        Assert.assertEquals(7, result.getFilters().size());

        Assert.assertNotNull(result.getSorts());
        Assert.assertTrue(result.getSorts().isEmpty());

        List<OfferPack> packs = result.getPacks();
        Assert.assertEquals(1, packs.size());

        OfferPack firstPack = packs.get(0);
        List<OfferPackItem> offers = firstPack.getOffers();
        Assert.assertEquals(2, offers.size());
        OfferPackItem offerPackItem = offers.get(0);
        Assert.assertEquals(774, (long) offerPackItem.getShopId());
        Assert.assertEquals(OfferPackItemType.OFFER, offerPackItem.getType());
        Assert.assertEquals("0lYHRYIWGMcO2vrKvdE8sA", offerPackItem.getWareMd5());
        Assert.assertEquals("testyxshop1offer1chngd1 bk", offerPackItem.getName());
        Assert.assertEquals("DGzdsCjtjKNywLiMI8-tTmKo-AsZIFncUpHpa3NgCd9cjPg5QsXKBMmQcU0xXPkmHFn6h82zQG0,", offerPackItem.getFeeShow());
        Assert.assertEquals(offerPackItem.getFeeShow(), offerPackItem.getId().getFeeShow());
        Assert.assertEquals(offerPackItem.getWareMd5(), offerPackItem.getId().getWareMd5());
        Assert.assertEquals(true, offerPackItem.getOnStock());
        Assert.assertNull(offerPackItem.getPrice().getBase());
        Assert.assertNull(offerPackItem.getPrice().getDiscount());
        Assert.assertNull(offerPackItem.getPrice().getShopMax());
        Assert.assertNull(offerPackItem.getPrice().getShopMin());
        Assert.assertEquals("101", offerPackItem.getPrice().getValue());

        OfferPackItem offerPackItem2 = offers.get(1);
        Assert.assertEquals(OfferPackItemType.OFFER, offerPackItem2.getType());
        Assert.assertEquals(774, (long) offerPackItem2.getShopId());
        Assert.assertEquals("5KvudlNJvCXxzqOpmrqxOQ", offerPackItem2.getWareMd5());
        Assert.assertEquals("testyxshop1book2changed1forEdit", offerPackItem2.getName());
        Assert.assertEquals("DGzdsCjtjKNywLiMI8-tTmKo-AsZIFncUpHpa3NgCd_2dDRomafn6HfMGO1fgsYd80O5OgXKxP0,", offerPackItem2.getFeeShow());
        Assert.assertEquals(offerPackItem2.getFeeShow(), offerPackItem2.getId().getFeeShow());
        Assert.assertEquals(offerPackItem2.getWareMd5(), offerPackItem2.getId().getWareMd5());
        Assert.assertEquals(true, offerPackItem2.getOnStock());
        Assert.assertNull(offerPackItem2.getPrice().getBase());
        Assert.assertNull(offerPackItem2.getPrice().getDiscount());
        Assert.assertNull(offerPackItem2.getPrice().getShopMax());
        Assert.assertNull(offerPackItem2.getPrice().getShopMin());
        Assert.assertEquals("8", offerPackItem2.getPrice().getValue());
    }

    @Test
    public void offerAlternative() {
        SameShopResponse result = getParser().parse(ResourceHelpers.getResource("same-shop-offer-alternate-sample.json"));
        Assert.assertNull(result.getFilters());

        Assert.assertNull(result.getSorts());

        List<OfferPack> packs = result.getPacks();
        Assert.assertEquals(4, packs.size());

        assertOfferTypesForPack(packs.get(0), OfferPackItemType.OFFER, OfferPackItemType.OFFER, OfferPackItemType.OFFER_ALTERNATIVE, OfferPackItemType.OFFER);
        assertOfferTypesForPack(packs.get(1), OfferPackItemType.OFFER, OfferPackItemType.OFFER, OfferPackItemType.OFFER_ALTERNATIVE, OfferPackItemType.OFFER);
        assertOfferTypesForPack(packs.get(2), OfferPackItemType.OFFER_ALTERNATIVE, OfferPackItemType.OFFER_ALTERNATIVE, OfferPackItemType.OFFER, OfferPackItemType.OFFER);
        assertOfferTypesForPack(packs.get(3), OfferPackItemType.OFFER_ALTERNATIVE, OfferPackItemType.OFFER_ALTERNATIVE, OfferPackItemType.OFFER, OfferPackItemType.OFFER);
    }

    private void assertOfferTypesForPack(OfferPack offerPack, OfferPackItemType... types) {
        Assert.assertEquals(types.length, offerPack.getOffers().size());
        for (int i = 0; i < types.length; ++i) {
            Assert.assertEquals(types[i], offerPack.getOffers().get(i).getType());
        }
    }

    private SameShopResponseParser getParser() {
        ReportRequestContext reportContext = new ReportRequestContext();
        Collection<Field> fields = new ArrayList<>();
        reportContext.setFields(fields);
        return reportParserFactory.getSameShopParser(reportContext);
    }
}
