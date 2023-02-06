package ru.yandex.market.mboc.common.assertions.custom;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.assertj.core.api.FactoryBasedNavigableListAssert;

import ru.yandex.market.mboc.common.offers.model.BusinessSkuKey;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.utils.MbocComparators;

/**
 * Отдельный класс необходим, чтобы упростить проверку равенства списков, когда надо сравнивать элементы по логике
 * {@link ru.yandex.market.mboc.common.utils.MbocComparators#equalsGeneral(Offer, Offer)}.
 *
 * @author s-ermakov
 */
public class ListGeneralOfferAssertions extends FactoryBasedNavigableListAssert<ListGeneralOfferAssertions,
    List<? extends Offer>, Offer, OfferAssertions> implements OffersAssertions<ListGeneralOfferAssertions> {

    private ListGeneralOfferAssertions(List<? extends Offer> actual) {
        super(actual, ListGeneralOfferAssertions.class, OfferAssertions::assertThat);
    }

    public static ListGeneralOfferAssertions assertThat(List<? extends Offer> actual) {
        return new ListGeneralOfferAssertions(actual)
            .usingElementComparatorIgnoringFields("lastVersion", "isOfferContentPresent", "contentProcessed")
            .usingComparatorForType(MbocComparators.LOCAL_DATE_TIME_COMPARATOR, LocalDateTime.class)
            .usingComparatorForType(MbocComparators.OFFERS_MAPPING_COMPARATOR, Offer.Mapping.class);
    }

    @Override
    public ListGeneralOfferAssertions containsExactlyIds(int... ids) {
        List<Long> actualIds = actual.stream()
            .map(Offer::getId)
            .collect(Collectors.toList());

        Object[] expectedIds = Arrays.stream(ids).mapToLong(value -> (long) value).boxed().toArray();

        iterables.assertContainsExactly(info, actualIds, expectedIds);

        return myself;
    }

    public ListGeneralOfferAssertions usingWithoutYtStampComparison() {
        return usingElementComparatorIgnoringFields(
                "lastVersion",
                "uploadToYtStamp",
                "isOfferContentPresent"
        );
    }

    @Override
    public ListGeneralOfferAssertions containsExactlyInAnyOrderIds(int... ids) {
        List<Long> actualIds = actual.stream()
            .map(Offer::getId)
            .collect(Collectors.toList());
        Object[] expectedIds = Arrays.stream(ids).mapToLong(value -> (long) value).boxed().toArray();

        iterables.assertContainsExactlyInAnyOrder(info, actualIds, expectedIds);

        return myself;
    }

    @Override
    public ListGeneralOfferAssertions containsExactlyInAnyOrderBusinessSkuKeys(BusinessSkuKey... keys) {
        List<BusinessSkuKey> actualKeys = actual.stream()
            .map(Offer::getBusinessSkuKey)
            .collect(Collectors.toList());

        iterables.assertContainsExactlyInAnyOrder(info, actualKeys, keys);

        return myself;
    }
}
