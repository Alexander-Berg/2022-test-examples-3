package ru.yandex.market.tpl.core.service.user;

import java.time.Clock;
import java.time.Instant;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.core.domain.partner.SortingCenterRepository;
import ru.yandex.market.tpl.core.domain.sc.SortingCenterProperties;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.core.domain.partner.SortingCenter.DEFAULT_SC_ID;

@RequiredArgsConstructor
class SortingCenterPropertyServiceTest extends TplAbstractTest {

    private final SortingCenterPropertyService sortingCenterPropertyService;
    private final SortingCenterRepository sortingCenterRepository;
    private final Clock clock;

    @Test
    void getSortingCenterPropertiesMap() {
        //given
        var sortingCenter = sortingCenterRepository.findByIdOrThrow(DEFAULT_SC_ID);
        sortingCenterPropertyService.upsertPropertyToSortingCenter(sortingCenter,
                SortingCenterProperties.ONE_DAY_SLOTS_SCHEDULE_ENABLED, true);

        //when
        var result = sortingCenterPropertyService.getSortingCenterPropertiesMap();

        //then
        assertThat(result).isNotNull();
        assertThat(result.getPropertiesBySc()).hasSize(1);
        var scPropertyEntry = result.getPropertiesBySc().entrySet().iterator().next();
        assertThat(scPropertyEntry.getKey()).isEqualTo(DEFAULT_SC_ID);
        assertThat(scPropertyEntry.getValue()).hasSize(1);
        var propertyEntry = scPropertyEntry.getValue().entrySet().iterator().next();
        assertThat(propertyEntry.getKey()).isEqualTo(SortingCenterProperties.ONE_DAY_SLOTS_SCHEDULE_ENABLED.getName());
        assertThat(propertyEntry.getValue().getValue()).isEqualTo(true);
        assertThat(result.getDefaultValues()).isNotNull();
        assertThat(result.getDefaultValues()).hasSize(SortingCenterProperties.getDefinitions().size());
    }

    @Test
    void getSortingCenterPropertiesMap_notActiveProperty() {
        //given
        var sortingCenter = sortingCenterRepository.findByIdOrThrow(DEFAULT_SC_ID);
        sortingCenterPropertyService.upsertPropertyToSortingCenter(
                sortingCenter,
                SortingCenterProperties.ONE_DAY_SLOTS_SCHEDULE_ENABLED,
                true,
                Instant.now(clock).minusSeconds(120),
                Instant.now(clock).minusSeconds(60)
        );

        //when
        var result = sortingCenterPropertyService.getSortingCenterPropertiesMap();

        //then
        assertThat(result).isNotNull();
        assertThat(result.getPropertiesBySc()).hasSize(0);
    }

}
