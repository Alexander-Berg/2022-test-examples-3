package ru.yandex.market.logistics.management.repository.combinator;

import java.util.Optional;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.entity.combinator.SegmentMetainfoKey;

import static ru.yandex.market.logistics.management.service.graph.SegmentMetaInfoService.RETURN_SORTING_CENTER_ID_FIELD;

class SegmentMetaInfoKeyRepositoryTest extends AbstractContextualTest {
    @Autowired
    private SegmentMetaInfoKeyRepository segmentMetaInfoKeyRepository;

    @Test
    @DatabaseSetup({
        "/data/controller/admin/logisticSegments/prepare_data.xml",
        "/data/controller/admin/logisticSegments/prepare_meta_info.xml",
    })
    void getByKey() {
        Optional<SegmentMetainfoKey> key = segmentMetaInfoKeyRepository.getByKey(RETURN_SORTING_CENTER_ID_FIELD);
        softly.assertThat(key).contains(new SegmentMetainfoKey().setId(401L).setKey(RETURN_SORTING_CENTER_ID_FIELD));
    }
}
