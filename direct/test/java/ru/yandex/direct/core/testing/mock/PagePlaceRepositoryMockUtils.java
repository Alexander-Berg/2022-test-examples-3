package ru.yandex.direct.core.testing.mock;

import java.util.List;
import java.util.Set;

import ru.yandex.direct.core.entity.internalads.model.PagePlace;
import ru.yandex.direct.core.entity.internalads.repository.PagePlaceRepository;
import ru.yandex.direct.core.entity.internalads.repository.PagePlaceYtRepository;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PagePlaceRepositoryMockUtils {
    public static final long PAGE_1 = 1L;
    public static final long PAGE_2 = 2L;

    public static final Set<Long> ALL_PAGE_IDS = Set.of(PAGE_1, PAGE_2);

    private static final long PAGE_1_PLACE_1 = 11L;
    private static final long PAGE_1_PLACE_2 = 12L;
    private static final long PAGE_2_PLACE_1 = 13L;

    private static final PagePlace PAGE_PLACE_1 = new PagePlace().withPageId(PAGE_1).withPlaceId(PAGE_1_PLACE_1);
    private static final PagePlace PAGE_PLACE_2 = new PagePlace().withPageId(PAGE_1).withPlaceId(PAGE_1_PLACE_2);
    private static final PagePlace PAGE_PLACE_3 = new PagePlace().withPageId(PAGE_2).withPlaceId(PAGE_2_PLACE_1);

    private static final List<PagePlace> ALL_PAGE_PLACE = List.of(PAGE_PLACE_1, PAGE_PLACE_2, PAGE_PLACE_3);

    public static PagePlaceYtRepository createYtRepositoryMock() {
        var mockRepository = mock(PagePlaceYtRepository.class);
        when(mockRepository.getPagePlaces(Set.of(PAGE_1, PAGE_2))).thenReturn(ALL_PAGE_PLACE);
        return mockRepository;
    }

    public static PagePlaceRepository createMySqlRepositoryMock() {
        var mockRepository = mock(PagePlaceRepository.class);
        when(mockRepository.getAll()).thenReturn(ALL_PAGE_PLACE);
        return mockRepository;
    }
}
