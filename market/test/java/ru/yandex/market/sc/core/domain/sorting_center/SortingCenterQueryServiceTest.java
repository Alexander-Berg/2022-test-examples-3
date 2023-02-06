package ru.yandex.market.sc.core.domain.sorting_center;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.sc.core.domain.sorting_center.model.ApiSortingCenterDto;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author hardlight
 */
@EmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class SortingCenterQueryServiceTest {
    private final SortingCenterQueryService sortingCenterQueryService;
    private final TestFactory testFactory;

    SortingCenter sortingCenter;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
    }

    @Test
    void getSortingCenterListUser() {
        User user = testFactory.getOrCreateStoredUser(sortingCenter);
        var sortingCenterList = sortingCenterQueryService.getSortingCenterList(user);
        assertThat(sortingCenterList.size()).isEqualTo(1);
        assertThat(sortingCenterList.get(0).getId()).isEqualTo(user.getSortingCenter().getId());
    }

    @Test
    void getSortingCenterListSupportUser() {
        User user = testFactory.getOrCreateSupportStoredUser(sortingCenter);
        var sortingCenterList = sortingCenterQueryService.getSortingCenterList(user);
        assertThat(sortingCenterList)
                .containsOnly(new ApiSortingCenterDto(sortingCenter.getId(), sortingCenter.getScName()));
    }

    @Test
    void getSortingCenterListSupportUserFilterByIdentifier() {
        User user = testFactory.getOrCreateSupportStoredUser(sortingCenter);
        var scByName = sortingCenterQueryService.getSortingCenterList(sortingCenter.getScName(), List.of(), user);
        var scById = sortingCenterQueryService.getSortingCenterList(sortingCenter.getId().toString(), List.of(), user);
        var scNotFound = sortingCenterQueryService.getSortingCenterList("wrong string", List.of(), user);
        assertThat(scByName.size()).isEqualTo(1);
        assertThat(scById.size()).isEqualTo(1);
        assertThat(scNotFound.size()).isEqualTo(0);
    }

    @Test
    void getSortingCenterListSupportUserFilterByProperty() {
        User user = testFactory.getOrCreateSupportStoredUser(sortingCenter);
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.IS_DROPOFF, true);
        var xdocSc = sortingCenterQueryService.getSortingCenterList("", List.of(SortingCenterPropertiesKey.XDOC_ENABLED.getTitle()), user);
        var dropoffSc = sortingCenterQueryService.getSortingCenterList("", List.of(SortingCenterPropertiesKey.IS_DROPOFF.getTitle()), user);
        assertThat(xdocSc.size()).isEqualTo(0);
        assertThat(dropoffSc.size()).isEqualTo(1);
    }

    @Test
    void getSortingCenterProperties() {
        User user = testFactory.getOrCreateStoredUser(sortingCenter);
        var properties = sortingCenterQueryService.getSortingCenterProperties(user);
        assertThat(properties.size()).isEqualTo(0);
    }

    @Test
    void getSortingCenterPropertiesSupportUser() {
        User user = testFactory.getOrCreateSupportStoredUser(sortingCenter);
        var properties = sortingCenterQueryService.getSortingCenterProperties(user);
        assertThat(properties.size()).isGreaterThan(0);
    }
}
