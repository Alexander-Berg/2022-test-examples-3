package ru.yandex.market.sc.core.domain.sorting_center.repository;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.util.CheckAnnotations;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author valter
 */
@EmbeddedDbTest
class SortingCenterPropertySourceTest {

    @Autowired
    SortingCenterPropertySource sortingCenterPropertySource;

    @Test
    void supportedCellTypesDefault() {
        assertThat(sortingCenterPropertySource.supportedCellTypes())
                .containsOnlyOnce(
                        CellType.COURIER, CellType.BUFFER, CellType.RETURN
                );
    }

    @Test
    void checkCacheable() {
        CheckAnnotations.checkCacheableAnnotations(SortingCenterPropertySource.class, Set.of());
    }

}
