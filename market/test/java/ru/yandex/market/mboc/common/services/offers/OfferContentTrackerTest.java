package ru.yandex.market.mboc.common.services.offers;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.junit.Test;

import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.model.OfferContent;
import ru.yandex.market.mboc.common.utils.DateTimeUtils;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.mboc.common.offers.model.Offer.MappingConfidence.CONTENT;

/**
 * @author yuramalinov
 * @created 06.08.18
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class OfferContentTrackerTest {
    private EnhancedRandom random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
        .seed(16667)
        .build();

    @Test
    public void testChangeTracking() {
        for (int i = 0; i < 42; i++) {
            Offer offer = random.nextObject(Offer.class)
                .setId(42)
                .setBusinessId(1234)
                .updateApprovedSkuMapping(null, null)
                .setSupplierSkuMapping(null)
                .setContentSkuMapping(null)
                .setMappedCategoryId(null)
                .setMappedModelId(null)
                .setContentChangedTs(DateTimeUtils.dateTimeNow().minusDays(1))
                .setServiceOffers(List.of(new Offer.ServiceOffer(1234)))
                .setIsOfferContentPresent(true);

            Offer approved = offer.copy()
                .updateApprovedSkuMapping(OfferTestUtils.mapping(1), CONTENT);

            check("Nothing changed", false, approved, o -> {
            });
            check("title", true, approved, o -> o.setTitle("other"));
            check("barCode", true, approved, o -> o.setBarCode("other"));
            check("description", true, approved,
                o -> o.storeOfferContent(OfferContent.copyToBuilder(offer.extractOfferContent()).description("other").build()));
            check("vendor", true, approved, o -> o.setVendor("other"));
            check("vendorCode", true, approved, o -> o.setVendorCode("other"));
            check("shopCategoryName", false, approved, o -> o.setShopCategoryName("other"));
            check("no upload if just url", true, approved,
                o -> o.storeOfferContent(OfferContent.copyToBuilder(
                    offer.extractOfferContent()).urls(Collections.singletonList("http://ya.ru/some-url")).build()));
            check("no content if urls change order", false,
                approved.copy().storeOfferContent(OfferContent.copyToBuilder(
                    offer.extractOfferContent()).urls(Arrays.asList("a", "b")).build()),
                o -> o.storeOfferContent(OfferContent.copyToBuilder(
                    offer.extractOfferContent()).urls(Arrays.asList("b", "a")).build()));

            check("picUrls", true, approved,
                    o -> o.storeOfferContent(OfferContent.copyToBuilder(offer.extractOfferContent()).picUrls("other")
                            .build()));

            check("no content if pic urls change order", false,
                    approved.copy().storeOfferContent(OfferContent.copyToBuilder(
                            offer.extractOfferContent()).picUrls("a\nb").build()),
                    o -> o.storeOfferContent(OfferContent.copyToBuilder(
                            offer.extractOfferContent()).picUrls("b\na").build()));


            check("No change if not approved", true, offer, o -> o.setTitle("other"));
            check("No upload if was not approved and now approved", true, offer,
                o -> o.setTitle("other").updateApprovedSkuMapping(OfferTestUtils.mapping(1), CONTENT));
            check(offer.copy()
                    .setContentSkuMapping(new Offer.Mapping(123, DateTimeUtils.dateTimeNow()))
                    .approve(Offer.MappingType.CONTENT, Offer.MappingConfidence.CONTENT),
                o -> o.setTitle("Other"),
                (before, after) -> {
                    assertTrue("Must upload previous source",
                        Offer.Mapping.mappingEqual(after.getContentSkuMapping(), after.getApprovedSkuMapping()));
                    assertNotEquals(before.getContentChangedTs(), after.getContentChangedTs());
                }
            );
        }
    }

    @Test
    public void testAlmostEquals() {
        assertTrue(OfferContentTracker.almostEquals("aa", " aa"));
        assertTrue(OfferContentTracker.almostEquals("aa", " aA"));
        assertTrue(OfferContentTracker.almostEquals(null, null));
        assertTrue(OfferContentTracker.almostEquals("ЯЯ", "яя"));
        assertTrue(OfferContentTracker.almostEquals("Яь", "яЬ"));
        assertTrue(OfferContentTracker.almostEquals("ДА?!\"[]&", "Да"));

        assertFalse(OfferContentTracker.almostEquals(null, "a"));
        assertFalse(OfferContentTracker.almostEquals("a", null));
        assertFalse(OfferContentTracker.almostEquals("Да", "Нет"));
    }

    private void check(String message, boolean contentChanged, Offer offer, Consumer<Offer> mutator) {
        check(offer, mutator, (before, after) -> {
            if (contentChanged) {
                assertNotEquals(message + ": content must be changed",
                    before.getContentChangedTs(), after.getContentChangedTs());
            } else {
                assertEquals(message + ": content must not be changed",
                    before.getContentChangedTs(), after.getContentChangedTs());
            }
        });
    }

    private void check(Offer before, Consumer<Offer> mutator, BiConsumer<Offer, Offer> validator) {
        OfferContentTracker dump = OfferContentTracker.createContentDump(Collections.singletonList(before));
        Offer after = before.copy();
        mutator.accept(after);
        dump.markChanges(Collections.singletonList(after));
        validator.accept(before, after);
    }
}
