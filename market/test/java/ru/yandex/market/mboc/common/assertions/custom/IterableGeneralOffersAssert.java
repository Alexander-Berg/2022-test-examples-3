package ru.yandex.market.mboc.common.assertions.custom;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.assertj.core.api.FactoryBasedNavigableIterableAssert;

import ru.yandex.market.mboc.common.offers.model.BusinessSkuKey;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.utils.MbocComparators;

/**
 * Отдельный класс необходим, чтобы упростить проверку равенства списков, когда надо сравнивать элементы по логике
 * {@link ru.yandex.market.mboc.common.utils.MbocComparators#equalsGeneral(Offer, Offer)}.
 *
 * @author s-ermakov
 */
public class IterableGeneralOffersAssert extends FactoryBasedNavigableIterableAssert<IterableGeneralOffersAssert,
    Iterable<? extends Offer>, Offer, OfferAssertions> implements OffersAssertions<IterableGeneralOffersAssert> {

    private IterableGeneralOffersAssert(Iterable<? extends Offer> offers) {
        super(offers, IterableGeneralOffersAssert.class, OfferAssertions::assertThat);
    }

    public static IterableGeneralOffersAssert assertThat(Iterable<? extends Offer> actual) {
        return new IterableGeneralOffersAssert(actual)
            .usingElementComparatorIgnoringFields("lastVersion")
            .usingComparatorForType(MbocComparators.LOCAL_DATE_TIME_COMPARATOR, LocalDateTime.class)
            .usingComparatorForType(MbocComparators.OFFERS_MAPPING_COMPARATOR, Offer.Mapping.class);
    }

    @Override
    public IterableGeneralOffersAssert containsExactlyIds(int... ids) {
        List<Long> actualIds = StreamSupport.stream(actual.spliterator(), false)
            .map(Offer::getId)
            .collect(Collectors.toList());
        Object[] expectedIds = Arrays.stream(ids).mapToLong(value -> (long) value).boxed().toArray();

        iterables.assertContainsExactly(info, actualIds, expectedIds);

        return myself;
    }

    @Override
    public IterableGeneralOffersAssert containsExactlyInAnyOrderIds(int... ids) {
        List<Long> actualIds = StreamSupport.stream(actual.spliterator(), false)
            .map(Offer::getId)
            .collect(Collectors.toList());
        Object[] expectedIds = Arrays.stream(ids).mapToLong(value -> (long) value).boxed().toArray();

        iterables.assertContainsExactlyInAnyOrder(info, actualIds, expectedIds);

        return myself;
    }

    @Override
    public IterableGeneralOffersAssert containsExactlyInAnyOrderBusinessSkuKeys(BusinessSkuKey... keys) {
        List<BusinessSkuKey> actualKeys = StreamSupport.stream(actual.spliterator(), false)
            .map(Offer::getBusinessSkuKey)
            .collect(Collectors.toList());

        iterables.assertContainsExactlyInAnyOrder(info, actualKeys, keys);

        return myself;
    }
}
