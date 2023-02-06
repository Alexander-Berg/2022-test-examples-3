package ru.yandex.market.ir.matcher2.matcher;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.ir.http.Matcher;
import ru.yandex.market.ir.http.Offer;
import ru.yandex.market.ir.matcher2.matcher.alternate.impl.tokenizers.StringTokenizationFactory;
import ru.yandex.market.ir.matcher2.matcher.alternate.impl.tokenizers.Tokenization;
import ru.yandex.market.ir.matcher2.matcher.alternate.load.protobuf.ProtoOfferUtils;
import ru.yandex.market.ir.matcher2.matcher.be.OfferCopy;

public class ExtentedVendorInYMLTest {
    private static final StringTokenizationFactory stringTokenizationFactory = new StringTokenizationFactory(
        true, true, true, true
    );

    @Test
    public void testBrandFind() {
        Matcher.Offer offer = OfferMatchingTo.brand();
        String vendor = findVendorInYML(offer);

        Assert.assertEquals(vendor, OfferMatchingTo.SOTHYS);

    }

    @Test
    public void testVendorPriority() {
        Matcher.Offer offer = OfferMatchingTo.vendor();
        String vendor = findVendorInYML(offer);

        Assert.assertEquals(vendor, OfferMatchingTo.APPLE);

    }

    private String findVendorInYML(Matcher.Offer offer) {
        OfferCopy offerCopy = ProtoOfferUtils.buildOfferCopy(offer, (int) offer.getHid(), (int) offer.getHid());
        Tokenization tokenization = stringTokenizationFactory.getTokenization(offerCopy);

        return tokenization.getVendorYmlTokenization().get(0).getTokens().get(0);
    }


    private static class OfferMatchingTo {
        static final int HID = 13314841;
        static final String SOTHYS = "sothys";
        static final String APPLE = "apple";
        private static final Offer.YmlParam YML_BREND_POISK = Offer.YmlParam.newBuilder()
            .setName("Бренд (поиск)")
            .setValue(SOTHYS)
            .build();
        private static final Offer.YmlParam YML_EMPTY_VENDOR = Offer.YmlParam.newBuilder()
            .setName("vendor")
            .setValue("")
            .build();
        private static final Offer.YmlParam YML_APPLE_VENDOR = Offer.YmlParam.newBuilder()
            .setName("vendor")
            .setValue(APPLE)
            .build();

        static Matcher.Offer brand() {
            return Matcher.Offer.newBuilder()
                .setHid(HID)
                .addYmlParam(YML_BREND_POISK)
                .addYmlParam(YML_EMPTY_VENDOR)
                .build();
        }

        static Matcher.Offer vendor() {
            return Matcher.Offer.newBuilder()
                .setHid(HID)
                .addYmlParam(YML_BREND_POISK)
                .addYmlParam(YML_APPLE_VENDOR)
                .build();
        }
    }
}

