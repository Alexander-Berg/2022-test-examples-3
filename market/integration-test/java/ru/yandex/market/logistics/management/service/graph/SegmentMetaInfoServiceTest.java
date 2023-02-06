package ru.yandex.market.logistics.management.service.graph;

import java.util.List;
import java.util.Optional;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.entity.combinator.SegmentMetainfoKey;
import ru.yandex.market.logistics.management.domain.entity.combinator.SegmentMetainfoValue;
import ru.yandex.market.logistics.management.domain.entity.type.PartnerType;
import ru.yandex.market.logistics.management.exception.BadRequestException;
import ru.yandex.market.logistics.management.exception.SegmentMetaInfoManipulationException;
import ru.yandex.market.logistics.management.repository.combinator.SegmentMetaInfoValueRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.market.logistics.management.service.graph.SegmentMetaInfoService.RETURN_SORTING_CENTER_ID_FIELD;

class SegmentMetaInfoServiceTest extends AbstractContextualTest {
    @Autowired
    private SegmentMetaInfoValueRepository segmentMetaInfoValueRepository;

    @Autowired
    private SegmentMetaInfoService segmentMetaInfoService;

    @Test
    @DatabaseSetup({
        "/data/controller/admin/logisticSegments/prepare_data.xml",
        "/data/controller/admin/logisticSegments/prepare_meta_info.xml",
    })
    void updateReturnSortingCenterId_createsMetaInfoValueObject() {
        Long newReturnSortingCenterId = 2L;

        Optional<SegmentMetainfoValue> value = segmentMetaInfoService.updateReturnSortingCenterId(
            10001L,
            newReturnSortingCenterId
        );

        assertThat(value).isNotEmpty();
        softly.assertThat(value).map(SegmentMetainfoValue::getValue).contains(newReturnSortingCenterId.toString());

        List<SegmentMetainfoValue> values = segmentMetaInfoValueRepository.findByServiceSegmentId(10001L);
        softly.assertThat(values).hasSize(1).contains(value.get());
    }

    @Test
    @DatabaseSetup({
        "/data/controller/admin/logisticSegments/prepare_data.xml",
        "/data/controller/admin/logisticSegments/prepare_meta_info.xml",
        "/data/controller/admin/logisticSegments/prepare_meta_info_value.xml",
    })
    void updateReturnSortingCenterId_updatesExistingMetaInfoValueObject() {
        Long newReturnSortingCenterId = 2L;

        Optional<SegmentMetainfoValue> value = segmentMetaInfoService.updateReturnSortingCenterId(
            10001L,
            newReturnSortingCenterId
        );

        assertThat(value).isNotEmpty();
        softly.assertThat(value).map(SegmentMetainfoValue::getValue).contains(newReturnSortingCenterId.toString());

        List<SegmentMetainfoValue> values = segmentMetaInfoValueRepository.findByServiceSegmentId(10001L);
        softly.assertThat(values).hasSize(2).contains(value.get());
    }

    @Test
    @DatabaseSetup({
        "/data/controller/admin/logisticSegments/prepare_data.xml",
        "/data/controller/admin/logisticSegments/prepare_meta_info.xml",
        "/data/controller/admin/logisticSegments/prepare_meta_info_value.xml",
    })
    void updateReturnSortingCenterId_deletesExistingMetaInfoValueObject() {
        Optional<SegmentMetainfoValue> value = segmentMetaInfoService.updateReturnSortingCenterId(
            10001L,
            null
        );

        softly.assertThat(value).isEmpty();

        List<SegmentMetainfoValue> values = segmentMetaInfoValueRepository.findByServiceSegmentId(10001L);
        softly.assertThat(values)
            .hasSize(1)
            .extracting(SegmentMetainfoValue::getParamType)
            .extracting(SegmentMetainfoKey::getKey)
            .doesNotContain(RETURN_SORTING_CENTER_ID_FIELD);
    }

    @Test
    void updateReturnSortingCenterId_throwsWhenReturnPartnerDoesNotExist() {
        Long returnSortingCenterId = 2000L;
        assertThatThrownBy(() -> segmentMetaInfoService.updateReturnSortingCenterId(10001L, returnSortingCenterId))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("Партнер с ID %s не найден", returnSortingCenterId);
    }

    @Test
    @DatabaseSetup("/data/controller/admin/logisticSegments/prepare_data.xml")
    void updateReturnSortingCenterId_throwsWhenReturnPartnerIsNotSortingCenter() {
        assertThatThrownBy(() -> segmentMetaInfoService.updateReturnSortingCenterId(10001L, 1L))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining(
                "Возвратный партнер должен быть типа %s, а не %s",
                PartnerType.SORTING_CENTER,
                PartnerType.DROPSHIP
            );
    }

    @Test
    @DatabaseSetup("/data/controller/admin/logisticSegments/prepare_data.xml")
    void updateReturnSortingCenterId_throwsWhenNoServices() {
        assertThatThrownBy(() -> segmentMetaInfoService.updateReturnSortingCenterId(10001L, 2L))
            .isInstanceOf(SegmentMetaInfoManipulationException.class)
            .hasMessageContaining("на сегменте отсутствуют сервисы");
    }

    @Test
    @DatabaseSetup({
        "/data/controller/admin/logisticSegments/prepare_data.xml",
        "/data/controller/admin/logisticSegments/prepare_meta_info.xml",
    })
    @DatabaseSetup(
        value = "/data/controller/admin/logisticSegments/prepare_meta_info_keys_only.xml",
        type = DatabaseOperation.DELETE
    )
    void updateReturnSortingCenterId_throwsWhenNoReturnSortingCenterKey() {
        assertThatThrownBy(() -> segmentMetaInfoService.updateReturnSortingCenterId(10001L, 2L))
            .isInstanceOf(SegmentMetaInfoManipulationException.class)
            .hasMessageContaining("отсутствует MetaInfo ключ для поля " + RETURN_SORTING_CENTER_ID_FIELD);
    }

    @Test
    @DatabaseSetup({
        "/data/controller/admin/logisticSegments/prepare_data.xml",
        "/data/controller/admin/logisticSegments/prepare_meta_info.xml",
        "/data/controller/admin/logisticSegments/prepare_meta_info_value.xml",
        "/data/controller/admin/logisticSegments/prepare_meta_info_value_duplicate_return_sorting_center_id_value.xml",
    })
    void updateReturnSortingCenterId_throwsWhenDuplicateReturnCenterIdValuesPresent() {
        assertThatThrownBy(() -> segmentMetaInfoService.updateReturnSortingCenterId(10001L, 2L))
            .isInstanceOf(SegmentMetaInfoManipulationException.class)
            .hasMessageContaining(
                "у этого сегмента больше одного MetaValue для поля %s",
                RETURN_SORTING_CENTER_ID_FIELD
            );
    }
}
