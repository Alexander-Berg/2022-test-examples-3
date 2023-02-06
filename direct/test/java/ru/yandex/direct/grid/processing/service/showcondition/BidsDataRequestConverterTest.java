package ru.yandex.direct.grid.processing.service.showcondition;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import ru.yandex.direct.core.entity.bids.container.SetBidItem;
import ru.yandex.direct.core.entity.bids.container.ShowConditionType;
import ru.yandex.direct.grid.processing.model.showcondition.mutation.GdSetBids;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.grid.processing.service.showcondition.converter.BidsDataRequestConverter.convertRequestToSetBidItems;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

public class BidsDataRequestConverterTest {
    private static final long SHOW_CONDITION_ID = 1L;
    private static final BigDecimal EXACT_PRICE_CONTEXT = BigDecimal.valueOf(17.1D);
    private static final BigDecimal EXACT_PRICE_SEARCH = BigDecimal.valueOf(11.1D);

    @Test
    public void convertRequestToSetBidItems_AllPricesEmpty() {
        GdSetBids gdSetBids = new GdSetBids()
                .withShowConditionIds(Collections.singletonList(SHOW_CONDITION_ID));

        List<SetBidItem> expectedGdSetBidItems = Collections.singletonList(new SetBidItem()
                .withId(SHOW_CONDITION_ID)
                .withShowConditionType(ShowConditionType.KEYWORD)
        );

        List<SetBidItem> setBidItems = convertRequestToSetBidItems(gdSetBids, id -> ShowConditionType.KEYWORD);

        assertThat(setBidItems).is(matchedBy(beanDiffer(expectedGdSetBidItems)));
    }

    @Test
    public void convertRequestToSetBidItems_KeywordsAllPricesFilled() {
        GdSetBids gdSetBids = new GdSetBids()
                .withShowConditionIds(Collections.singletonList(SHOW_CONDITION_ID))
                .withExactPriceSearch(EXACT_PRICE_SEARCH)
                .withExactPriceContext(EXACT_PRICE_CONTEXT);

        List<SetBidItem> expectedGdSetBidItems = Collections.singletonList(new SetBidItem()
                .withId(SHOW_CONDITION_ID)
                .withShowConditionType(ShowConditionType.KEYWORD)
                .withPriceContext(EXACT_PRICE_CONTEXT)
                .withPriceSearch(EXACT_PRICE_SEARCH));

        List<SetBidItem> setBidItems = convertRequestToSetBidItems(gdSetBids, id -> ShowConditionType.KEYWORD);

        assertThat(setBidItems).is(matchedBy(beanDiffer(expectedGdSetBidItems)));
    }

    @Test
    public void convertRequestToSetBidItems_ShowConditionIdsAllPricesFilled() {
        GdSetBids gdSetBids = new GdSetBids()
                .withShowConditionIds(Collections.singletonList(SHOW_CONDITION_ID))
                .withExactPriceSearch(EXACT_PRICE_SEARCH)
                .withExactPriceContext(EXACT_PRICE_CONTEXT);

        List<SetBidItem> expectedGdSetBidItems = Collections.singletonList(new SetBidItem()
                .withId(SHOW_CONDITION_ID)
                .withShowConditionType(ShowConditionType.KEYWORD)
                .withPriceContext(EXACT_PRICE_CONTEXT)
                .withPriceSearch(EXACT_PRICE_SEARCH));

        List<SetBidItem> setBidItems = convertRequestToSetBidItems(gdSetBids, id -> ShowConditionType.KEYWORD);

        assertThat(setBidItems).is(matchedBy(beanDiffer(expectedGdSetBidItems)));
    }

    @Test
    public void convertRequestToSetBidItems_RetargetingsAllPricesFilled() {
        GdSetBids gdSetBids = new GdSetBids()
                .withShowConditionIds(Collections.singletonList(SHOW_CONDITION_ID))
                .withExactPriceSearch(EXACT_PRICE_SEARCH)
                .withExactPriceContext(EXACT_PRICE_CONTEXT);

        List<SetBidItem> expectedGdSetBidItems = Collections.singletonList(new SetBidItem()
                .withId(SHOW_CONDITION_ID)
                .withShowConditionType(ShowConditionType.AUDIENCE_TARGET)
                .withPriceContext(EXACT_PRICE_CONTEXT)
                .withPriceSearch(EXACT_PRICE_SEARCH));

        List<SetBidItem> setBidItems = convertRequestToSetBidItems(gdSetBids, id -> ShowConditionType.AUDIENCE_TARGET);

        assertThat(setBidItems).is(matchedBy(beanDiffer(expectedGdSetBidItems)));
    }
}
