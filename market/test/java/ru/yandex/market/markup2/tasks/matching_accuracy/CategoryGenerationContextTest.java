package ru.yandex.market.markup2.tasks.matching_accuracy;

import com.google.common.collect.Lists;
import org.junit.Test;
import ru.yandex.market.markup2.entries.group.ModelTypeValue;
import ru.yandex.market.markup2.entries.group.PublishingValue;
import ru.yandex.market.markup2.utils.OfferTestUtils;
import ru.yandex.market.markup2.utils.cards.Card;
import ru.yandex.market.markup2.utils.cards.CardType;
import ru.yandex.market.markup2.utils.offer.Offer;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author inenakhov
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class CategoryGenerationContextTest {
    private CategoryGenerationContext categoryGenerationContext;
    private int categoryId = 1;

    private PublishingValue publishingValue = PublishingValue.PUBLISHED;
    private ModelTypeValue modelTypeValue = ModelTypeValue.ALL;

    private Card createCard(long id) {
        return new Card(id, "", "", "", categoryId, CardType.CLUSTER);
    }

    private Offer createOffer(String wareMd5) {
        return new Offer(OfferTestUtils.createOffer(wareMd5, wareMd5, 1L, 1L, wareMd5));
    }

    @Test
    public void putForChecking() throws Exception {
        categoryGenerationContext = new CategoryGenerationContext(categoryId, 100, modelTypeValue,
            publishingValue, 10, 10);
        Offer someOffer = createOffer("12345");

        List<Long> someProcessedCardsIds = Lists.newArrayList(1L, 2L, 3L);
        List<Card> someFoundCards = Lists.newArrayList(createCard(2L));

        categoryGenerationContext.addCards(someFoundCards);
        categoryGenerationContext.saveProcessedCardsIds(someProcessedCardsIds);

        categoryGenerationContext.putForChecking(1L, someOffer);
        categoryGenerationContext.putForChecking(2L, someOffer);
        categoryGenerationContext.putForChecking(4L, someOffer);

        assertFalse(categoryGenerationContext.getUncheckedModelsIds().contains(1L));
        assertTrue(categoryGenerationContext.getUncheckedModelsIds().contains(2L));
        assertTrue(categoryGenerationContext.getUncheckedModelsIds().contains(4L));
    }

    @Test
    public void addOfferToExistingCard() {
        int offersLimit = 100;
        int modelsLimit = 10;
        int offersPerModelLimit = 10;

        categoryGenerationContext = new CategoryGenerationContext(categoryId,
                                                                  offersLimit,
                                                                  modelTypeValue,
                                                                  publishingValue,
                                                                  offersPerModelLimit,
                                                                  modelsLimit);
        String somePicUrl = "http://url";
        Long cardIdToAddOffers = 1L;
        categoryGenerationContext.addCards(Lists.newArrayList(createCard(cardIdToAddOffers)));
        categoryGenerationContext.addOfferToCard(cardIdToAddOffers, createOffer("0"), somePicUrl);

        for (int i = 0; i < 2 * modelsLimit; i++) {
            Offer offer = createOffer(String.valueOf(offersLimit + i));
            categoryGenerationContext.addOfferToCard(cardIdToAddOffers, offer, somePicUrl);
        }

        assertEquals(modelsLimit, categoryGenerationContext.getAllProcessedData().get(cardIdToAddOffers).size());
        assertEquals(modelsLimit, categoryGenerationContext.processedOffersSize());

        try {
            categoryGenerationContext.addOfferToCard(-1L, createOffer("0"), somePicUrl);
            assertTrue(false);
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }
}
