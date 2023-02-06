package ru.yandex.market.ff.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;

/**
 * Интеграционный тест для {@link LogbrokerReaderOffsetRepository}.
 */
public class LogbrokerReaderOffsetRepositoryTest extends IntegrationTest {

    private static final LocalDateTime FIRST_PARTITION_UPDATED = LocalDateTime.of(2019, 9, 11, 9, 54, 38, 575643000);
    private static final LocalDateTime SECOND_PARTITION_UPDATED = LocalDateTime.of(2019, 9, 11, 10, 54, 38, 575643000);

    @Autowired
    private LogbrokerReaderOffsetRepository logbrokerReaderOffsetRepository;

    @Test
    @DatabaseSetup("classpath:repository/logbroker-reader-offset/empty.xml")
    public void getLastUpdateTimeFromEmptyTable() {
        Optional<LocalDateTime> lastUpdateTime = logbrokerReaderOffsetRepository.getLastUpdateTime("checkouter_event");
        assertions.assertThat(lastUpdateTime).isEmpty();
    }

    @Test
    @DatabaseSetup("classpath:repository/logbroker-reader-offset/before-not-checkouter-event.xml")
    public void getEmptyLastUpdateTimeFromTableWithoutCheckouterOffsets() {
        Optional<LocalDateTime> lastUpdateTime = logbrokerReaderOffsetRepository.getLastUpdateTime("checkouter_event");
        assertions.assertThat(lastUpdateTime).isEmpty();
    }

    @Test
    @DatabaseSetup("classpath:repository/logbroker-reader-offset/before-single-partition.xml")
    public void getLastUpdateTimeForOnePartition() {
        Optional<LocalDateTime> lastUpdateTime = logbrokerReaderOffsetRepository.getLastUpdateTime("checkouter_event");
        assertions.assertThat(lastUpdateTime).isPresent();
        assertions.assertThat(lastUpdateTime.get()).isEqualTo(FIRST_PARTITION_UPDATED);
    }

    @Test
    @DatabaseSetup("classpath:repository/logbroker-reader-offset/before-multiple-partitions.xml")
    public void getLastUpdateTimeForMultiplePartitions() {
        Optional<LocalDateTime> lastUpdateTime = logbrokerReaderOffsetRepository.getLastUpdateTime("checkouter_event");
        assertions.assertThat(lastUpdateTime).isPresent();
        assertions.assertThat(lastUpdateTime.get()).isEqualTo(SECOND_PARTITION_UPDATED);
    }
}
