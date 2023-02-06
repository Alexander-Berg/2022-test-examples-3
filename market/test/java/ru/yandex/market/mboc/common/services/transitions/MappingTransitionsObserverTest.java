package ru.yandex.market.mboc.common.services.transitions;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.jooq.repo.notification.EntityChangeEvent;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.enums.MappingSkuType;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.MappingTransition;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.repository.MappingTransitionsRepository;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;

public class MappingTransitionsObserverTest extends BaseDbTestClass {

    private static final int BUSINESS_ID = 10;

    private MappingTransitionsObserver observer;

    @Autowired
    private MappingTransitionsRepository repository;

    @Before
    public void setUp() {
        observer = new MappingTransitionsObserver(repository);
    }

    @Test
    public void testOfferCreating() {
        observer.onAfterSave(List.of(
                new EntityChangeEvent<>(
                        null,
                        offer(1L, Offer.SkuType.MARKET)
                )
        ));
        Assertions.assertThat(repository.findAll()).isEmpty();
    }

    @Test
    public void testOfferDeleting() {
        observer.onAfterSave(List.of(
                new EntityChangeEvent<>(
                        offer(1L, Offer.SkuType.MARKET),
                        null
                )
        ));
        Assertions.assertThat(repository.findAll()).isEmpty();
    }

    @Test
    public void testMappingCreation() {
        observer.onAfterSave(List.of(
                new EntityChangeEvent<>(
                        offer(null, null),
                        offer(1L, Offer.SkuType.MARKET)
                )
        ));
        Assertions.assertThat(repository.findAll()).isEmpty();
    }

    @Test
    public void testMappingDeleting() {
        observer.onAfterSave(List.of(
                new EntityChangeEvent<>(
                        offer(1L, Offer.SkuType.MARKET),
                        offer(null, null)
                ),
            new EntityChangeEvent<>(
                offer(2L, Offer.SkuType.MARKET),
                offer(0L, Offer.SkuType.MARKET)
            )
        ));

        Map<Long, MappingTransition> transitionMap = repository.findAll().stream()
            .collect(Collectors.toMap(t -> t.getOldMskuId(), t -> t));

        Assertions.assertThat(transitionMap.get(1L))
            .extracting(MappingTransition::getNewMskuId,
                MappingTransition::getNewMskuType,
                MappingTransition::getOldMskuType,
                MappingTransition::getSupplierId)
            .containsExactly(
                null, null, MappingSkuType.MARKET, (long) BUSINESS_ID
            );

        Assertions.assertThat(transitionMap.get(2L))
            .extracting(MappingTransition::getNewMskuId,
                MappingTransition::getNewMskuType,
                MappingTransition::getOldMskuType,
                MappingTransition::getSupplierId)
            .containsExactly(
                0L, MappingSkuType.MARKET, MappingSkuType.MARKET, (long) BUSINESS_ID
            );
    }

    @Test
    public void testMappingModifying() {
        observer.onAfterSave(List.of(
                new EntityChangeEvent<>(
                        offer(1L, Offer.SkuType.MARKET),
                        offer(2L, Offer.SkuType.MARKET)
                ),
                new EntityChangeEvent<>(
                        offer(5L, Offer.SkuType.FAST_SKU),
                        offer(10L, Offer.SkuType.MARKET)
                )
        ));

        Map<Long, MappingTransition> transitionMap = repository.findAll().stream()
                .collect(Collectors.toMap(t -> t.getOldMskuId(), t -> t));

        Assertions.assertThat(transitionMap.get(1L))
                .extracting(MappingTransition::getNewMskuId,
                        MappingTransition::getNewMskuType,
                        MappingTransition::getOldMskuType,
                        MappingTransition::getSupplierId)
                .containsExactly(
                        2L, MappingSkuType.MARKET, MappingSkuType.MARKET, (long) BUSINESS_ID
                );

        Assertions.assertThat(transitionMap.get(5L))
                .extracting(MappingTransition::getNewMskuId,
                        MappingTransition::getNewMskuType,
                        MappingTransition::getOldMskuType,
                        MappingTransition::getSupplierId)
                .containsExactly(
                        10L, MappingSkuType.MARKET, MappingSkuType.FAST_SKU, (long) BUSINESS_ID
                );
    }

    @Test
    public void testMappingModifyingFastSkuToPartnerSku() {
        observer.onAfterSave(List.of(
                new EntityChangeEvent<>(
                        offer(1L, Offer.SkuType.FAST_SKU),
                        offer(1L, Offer.SkuType.PARTNER20)
                )
        ));

        Map<Long, MappingTransition> transitionMap = repository.findAll().stream()
                .collect(Collectors.toMap(t -> t.getOldMskuId(), t -> t));

        Assertions.assertThat(transitionMap.get(1L))
                .extracting(MappingTransition::getNewMskuId,
                    MappingTransition::getNewMskuType,
                    MappingTransition::getOldMskuId,
                    MappingTransition::getOldMskuType,
                    MappingTransition::getSupplierId)
                .containsExactly(
                        1L, MappingSkuType.PARTNER20, 1L, MappingSkuType.FAST_SKU, (long) BUSINESS_ID
                );
    }

    private Offer offer(@Nullable Long skuId, @Nullable Offer.SkuType skuType) {
        Offer result = new Offer();
        result.setBusinessId(BUSINESS_ID);
        result.setShopSku("shopSKu");
        if (skuId != null) {
            result.updateApprovedSkuMapping(
                    new Offer.Mapping(skuId, LocalDateTime.now(), skuType)
            );
        }
        return result;
    }

}
