package ru.yandex.market.logistics.management.repository.combinator;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.entity.combinator.SegmentMetainfoValue;

import static ru.yandex.market.logistics.management.service.graph.SegmentMetaInfoService.RETURN_SORTING_CENTER_ID_FIELD;

class SegmentMetaInfoValueRepositoryTest extends AbstractContextualTest {
    @Autowired
    private SegmentMetaInfoValueRepository segmentMetaInfoValueRepository;

    @Test
    @DatabaseSetup({
        "/data/controller/admin/logisticSegments/prepare_data.xml",
        "/data/controller/admin/logisticSegments/prepare_meta_info.xml",
        "/data/controller/admin/logisticSegments/prepare_meta_info_value.xml",
    })
    void findByServiceSegmentId() {
        List<SegmentMetainfoValue> values = segmentMetaInfoValueRepository.findByServiceSegmentId(10001L);

        softly.assertThat(values).extracting(SegmentMetainfoValue::getId).containsOnly(501L, 503L);
    }

    @Test
    @DatabaseSetup({
        "/data/controller/admin/logisticSegments/prepare_data.xml",
        "/data/controller/admin/logisticSegments/prepare_meta_info.xml",
        "/data/controller/admin/logisticSegments/prepare_meta_info_value.xml",
    })
    void deleteReturnSortingCenterIdValuesBySegmentId() {
        segmentMetaInfoValueRepository.deleteBySegmentIdAndMetaValueKey(10001L, RETURN_SORTING_CENTER_ID_FIELD);

        List<SegmentMetainfoValue> values = segmentMetaInfoValueRepository.findAll();
        softly.assertThat(values).extracting(SegmentMetainfoValue::getId)
            .containsOnly(
                503L, // key of value is not RETURN_SORTING_CENTER_ID
                504L  // segment of value is not 10001L
            );
    }
}
