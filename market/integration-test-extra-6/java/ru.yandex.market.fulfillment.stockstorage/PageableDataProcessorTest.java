package ru.yandex.market.fulfillment.stockstorage;

import java.util.Collection;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.fulfillment.stockstorage.domain.dto.PageableUnitId;
import ru.yandex.market.fulfillment.stockstorage.repository.JdbcSkuRepository;
import ru.yandex.market.fulfillment.stockstorage.util.PageableDataProcessor;

import static org.assertj.core.api.Assertions.assertThat;

@DatabaseSetup({
        "classpath:database/states/pageable_data_processor.xml"
})
public class PageableDataProcessorTest extends AbstractContextualTest {

    private int actualCount;
    private int consumerRequests;

    @Autowired
    private JdbcSkuRepository jdbcSkuRepository;

    @BeforeEach
    void setup() {
        actualCount = 0;
        consumerRequests = 0;
    }

    @Test
    public void numberOfDataEqualsPageSize() {
        PageableDataProcessor processor = new PageableDataProcessor(10);
        Long lastUsedOffset = processor.extractAndProcessData(jdbcSkuRepository::findAll, this::chunkConsumer);
        assertThat(actualCount).isEqualTo(10);
        assertThat(consumerRequests).isEqualTo(2);
        assertThat(lastUsedOffset).isEqualTo(10);
    }

    @Test
    public void numberOfDataNotDivisibleToPageSize() {
        PageableDataProcessor processor = new PageableDataProcessor(3);
        Long lastUsedOffset = processor.extractAndProcessData(jdbcSkuRepository::findAll, this::chunkConsumer);
        assertThat(actualCount).isEqualTo(10);
        assertThat(consumerRequests).isEqualTo(4);
        assertThat(lastUsedOffset).isEqualTo(10);
    }

    @Test
    public void numberOfDataDivisibleToPageSize() {
        PageableDataProcessor processor = new PageableDataProcessor(5);
        Long lastUsedOffset = processor.extractAndProcessData(jdbcSkuRepository::findAll, this::chunkConsumer);
        assertThat(actualCount).isEqualTo(10);
        assertThat(consumerRequests).isEqualTo(3);
        assertThat(lastUsedOffset).isEqualTo(10);
    }

    @Test
    public void offsetIsLongerThanMaxCount() {
        int expectedCount = 0;
        PageableDataProcessor processor = new PageableDataProcessor(5, 11, 10);
        Long lastUsedOffset = processor.extractAndProcessData(jdbcSkuRepository::findAll, this::chunkConsumer);
        assertThat(actualCount).isEqualTo(expectedCount);
        assertThat(consumerRequests).isEqualTo(1);
        assertThat(lastUsedOffset).isEqualTo(11);
    }

    @Test
    public void offsetIsHalfOfMaxCount() {
        PageableDataProcessor processor = new PageableDataProcessor(5, 8, 10);
        Long lastUsedOffset = processor.extractAndProcessData(jdbcSkuRepository::findAll, this::chunkConsumer);
        assertThat(actualCount).isEqualTo(2);
        assertThat(consumerRequests).isEqualTo(1);
        assertThat(lastUsedOffset).isEqualTo(10);
    }

    @Test
    public void limitIsLessThenMaxCount() {
        PageableDataProcessor processor = new PageableDataProcessor(3, 3, 3);
        Long lastUsedOffset = processor.extractAndProcessData(jdbcSkuRepository::findAll, this::chunkConsumer);
        assertThat(actualCount).isEqualTo(3);
        assertThat(consumerRequests).isEqualTo(1);
        assertThat(lastUsedOffset).isEqualTo(6);
    }

    @Test
    public void limitIsEqualsMaxCount() {
        PageableDataProcessor processor = new PageableDataProcessor(3, 0, 10);
        Long lastUsedOffset = processor.extractAndProcessData(jdbcSkuRepository::findAll, this::chunkConsumer);
        assertThat(actualCount).isEqualTo(10);
        assertThat(consumerRequests).isEqualTo(4);
        assertThat(lastUsedOffset).isEqualTo(10);
    }

    private void chunkConsumer(Collection<PageableUnitId> pageables) {
        consumerRequests++;
        actualCount += pageables.size();
    }
}
