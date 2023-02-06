package ru.yandex.market.sc.core.domain.courier.repository;

import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.sc.core.domain.delivery_service.repository.DeliveryService;
import ru.yandex.market.sc.core.domain.delivery_service.repository.DeliveryServicePropertySource;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.tpl.common.web.config.TplProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.sc.core.domain.util.LocalCacheName.COURIER_IDS_BY_SORTING_CENTER;
import static ru.yandex.market.sc.core.test.TestFactory.order;

/**
 * @author valter
 */
@ActiveProfiles({TplProfiles.TESTS, "cache"})
@EmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class CourierRepositoryTest {
    private final CourierRepository courierRepository;
    private final CacheManager cacheManager;
    private final TestFactory testFactory;
    private final DeliveryServicePropertySource deliveryServicePropertySource;

    SortingCenter sortingCenter;
    DeliveryService deliveryService;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        deliveryService = testFactory.storedDeliveryService("123", true);
        deliveryServicePropertySource.refreshForce();
    }

    @Test
    void save() {
        var expected = testFactory.courier();
        assertThat(courierRepository.save(expected)).isEqualTo(expected);
    }

    @Test
    void testCache() {
        var courier = courierRepository.save(testFactory.courier());
        testFactory.createForToday(order(sortingCenter).deliveryService(deliveryService).build())
                .updateCourier(courier)
                .get();

        var expected = courierRepository.findAllCourierIdsBySortingCenter(sortingCenter.getId());
        var cachedCourierIds = getCachedCourierList(sortingCenter.getId());

        assertThat(cachedCourierIds).isPresent();
        assertThat(cachedCourierIds.get()).isEqualTo(expected);
    }

    @SuppressWarnings("unchecked")
    private Optional<List<Long>> getCachedCourierList(long sortingCenterId) {
        return Optional.ofNullable(cacheManager.getCache(COURIER_IDS_BY_SORTING_CENTER))
                .map(c -> c.get(sortingCenterId, List.class));
    }

}
