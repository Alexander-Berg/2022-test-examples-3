package ru.yandex.market.checkout.checkouter.order;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.util.report.FoundOfferBuilder;
import ru.yandex.market.common.report.model.FoundOffer;
import ru.yandex.market.common.report.model.filter.Filter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.test.providers.OfferFilterProvider.getFilter;

public class OrderTypeUtilsTest {

    @Test
    public void shouldNotDetermineOfferIsResaleIfResaleFiltersAreEmpty() throws IOException {
        //arrange
        String filterId = "validId";
        FoundOffer offer = getFoundOffer(List.of(getFilter(filterId, null, null)));
        //act
        boolean isResale = OrderTypeUtils.isOfferResale(offer, Collections.emptyMap());
        //assert
        assertFalse(isResale);
    }

    @Test
    public void shouldDetermineOfferIsResaleByFilterId() throws IOException {
        //arrange
        String filterId = "validId";
        FoundOffer offer = getFoundOffer(List.of(getFilter(filterId, null, null)));
        Map<String, List<String>> resaleFiltersMap = Map.of(filterId, Collections.emptyList());
        //act
        boolean isResale = OrderTypeUtils.isOfferResale(offer, resaleFiltersMap);
        //assert
        assertTrue(isResale);
    }

    @Test
    public void shouldDetermineOfferIsResaleByFilterValueId() throws IOException {
        //arrange
        String filterId = "validId";
        String resaleFilterValueId = "validValueId";
        FoundOffer offer = getFoundOffer(List.of(getFilter(filterId, resaleFilterValueId, null)));
        Map<String, List<String>> resaleFiltersMap = Map.of(filterId, List.of(resaleFilterValueId, "someId"));
        //act
        boolean isResale = OrderTypeUtils.isOfferResale(offer, resaleFiltersMap);
        //assert
        assertTrue(isResale);
    }

    @Test
    public void shouldNotDetermineOfferIsResaleIfWrongId() throws IOException {
        //arrange
        FoundOffer offer = getFoundOffer(List.of(getFilter(null, null, null)));
        Map<String, List<String>> resaleFiltersMap = Map.of("someId", List.of(),
                "anotherId", List.of("anotherValueId"
                ));
        //act
        boolean isResale = OrderTypeUtils.isOfferResale(offer, resaleFiltersMap);
        //assert
        assertFalse(isResale);
    }

    @Test
    public void shouldNotDetermineOfferIsResaleIfWrongValueId() throws IOException {
        //arrange
        String filterId = "validId";
        FoundOffer offer = getFoundOffer(List.of(getFilter(filterId, null, null)));
        Map<String, List<String>> resaleFiltersMap = Map.of(filterId, List.of("someValueId"));
        //act
        boolean isResale = OrderTypeUtils.isOfferResale(offer, resaleFiltersMap);
        //assert
        assertFalse(isResale);
    }

    @Test
    public void shouldDetermineOrderIsResaleWhenAllItemsAreResale() throws IOException {
        //arrange
        String filterId = "validId";
        String anotherFilterId = "anotherValidId";

        String filterValueId = "validValueId";
        String anotherFilterValueId = "anotherValidValueId";
        FoundOffer firstOffer = getFoundOffer(List.of(getFilter(filterId, filterValueId, null)));
        FoundOffer secondOffer = getFoundOffer(List.of(getFilter(anotherFilterId, filterValueId,
                anotherFilterValueId)));
        Map<String, List<String>> resaleFiltersMap = Map.of(filterId, List.of(filterValueId),
                anotherFilterId, List.of(anotherFilterValueId));
        //act
        boolean isResale = OrderTypeUtils.isOrderResale(List.of(firstOffer, secondOffer), resaleFiltersMap);
        //assert
        assertTrue(isResale);
    }

    @Test
    public void shouldNotDetermineOrderIsResaleWhenNotAllItemsAreResale() throws IOException {
        //arrange
        String filterId = "validId";
        String wrongFilterId = "invalidId";
        FoundOffer firstOffer = getFoundOffer(List.of(getFilter(filterId, null, null)));
        FoundOffer secondOffer = getFoundOffer(List.of(getFilter(wrongFilterId, null, null)));
        Map<String, List<String>> resaleFiltersMap = Map.of(filterId, Collections.emptyList());
        //act
        boolean isResale = OrderTypeUtils.isOrderResale(List.of(firstOffer, secondOffer), resaleFiltersMap);
        //assert
        assertFalse(isResale);
    }

    @Test
    public void shouldNotDetermineOrderIsResaleWhenOffersAreEmpty() throws IOException {
        //arrange
        String filterId = "validId";
        String wrongFilterId = "invalidId";
        Map<String, List<String>> resaleFiltersMap = Map.of(filterId, Collections.emptyList());

        //act
        boolean isResale = OrderTypeUtils.isOrderResale(Collections.emptyList(), resaleFiltersMap);

        //assert
        assertFalse(isResale);
    }

    private FoundOffer getFoundOffer(List<Filter> filters) {
        FoundOffer offer = FoundOfferBuilder.create().build();
        offer.setFilters(filters);
        return offer;
    }
}
