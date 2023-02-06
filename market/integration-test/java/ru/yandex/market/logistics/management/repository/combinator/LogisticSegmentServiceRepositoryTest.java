package ru.yandex.market.logistics.management.repository.combinator;

import java.util.List;
import java.util.stream.Collectors;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.entity.combinator.LogisticSegmentService;

class LogisticSegmentServiceRepositoryTest extends AbstractContextualTest {
    @Autowired
    private LogisticSegmentServiceRepository logisticSegmentServiceRepository;

    @Test
    @DatabaseSetup({
        "/data/controller/admin/logisticSegments/prepare_data.xml",
        "/data/controller/admin/logisticSegments/prepare_meta_info.xml",
    })
    void getBySegmentId() {
        List<LogisticSegmentService> services = logisticSegmentServiceRepository.getBySegmentId(10001L);
        softly.assertThat(services.stream().map(LogisticSegmentService::getId).collect(Collectors.toList()))
            .containsOnly(301L, 302L);
    }
}
