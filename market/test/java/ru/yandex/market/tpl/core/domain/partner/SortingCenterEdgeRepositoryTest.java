package ru.yandex.market.tpl.core.domain.partner;

import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;

import static org.assertj.core.api.Assertions.assertThat;

@CoreTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class SortingCenterEdgeRepositoryTest {

    private final TestUserHelper testHelper;
    private final SortingCenterEdgeRepository sortingCenterEdgeRepository;

    private SortingCenter parentSc;
    private SortingCenter childSc;

    @BeforeEach
    void setUp() {
        parentSc = testHelper.sortingCenter(1L);
        childSc = testHelper.sortingCenter(2L);
    }

    @Test
    void findSortingCenterEdgeByChildSortingCenterId() {
        SortingCenterEdge sortingCenterEdge = new SortingCenterEdge();
        sortingCenterEdge.init(parentSc, childSc, 1);

        Optional<SortingCenterEdge> scEdgeFoundO =
                sortingCenterEdgeRepository.findSortingCenterEdgeByChildSortingCenterId(childSc.getId());

        assertThat(scEdgeFoundO)
                .isNotEmpty()
                .contains(sortingCenterEdge);
    }
}
