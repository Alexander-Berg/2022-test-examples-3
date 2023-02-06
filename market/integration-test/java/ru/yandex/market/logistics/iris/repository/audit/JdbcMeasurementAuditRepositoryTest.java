package ru.yandex.market.logistics.iris.repository.audit;

import java.time.LocalDate;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.iris.configuration.AbstractContextualTest;
import ru.yandex.market.logistics.iris.core.domain.event.EventType;
import ru.yandex.market.logistics.iris.core.domain.item.ItemIdentifier;

public class JdbcMeasurementAuditRepositoryTest extends AbstractContextualTest {

    @Autowired
    private JdbcMeasurementAuditRepository jdbcMeasurementAuditRepository;

    @Test
    @DatabaseSetup(value = "classpath:fixtures/setup/audit/1.xml")
    public void shouldReturnOneRecord() {
        final LocalDate fromDate = LocalDate.of(2020, 9, 13);

        List<ItemIdentifier> identifierList = jdbcMeasurementAuditRepository.findByItemIdentifierAndEventTypeDailyEvent(
                ImmutableList.of(ItemIdentifier.of("1", "sku1")),
                EventType.PUT_VERDICT,
                fromDate);

        assertions().assertThat(identifierList).hasSize(1);
    }

    @Test
    @DatabaseSetup(value = "classpath:fixtures/setup/audit/1.xml")
    public void shouldNotReturnAnyRecord() {
        final LocalDate fromDate = LocalDate.of(2020, 9, 14);

        List<ItemIdentifier> identifierList = jdbcMeasurementAuditRepository.findByItemIdentifierAndEventTypeDailyEvent(
                ImmutableList.of(ItemIdentifier.of("1", "sku1")),
                EventType.PUT_VERDICT,
                fromDate);

        assertions().assertThat(identifierList).hasSize(0);
    }

    @Test
    @DatabaseSetup(value = "classpath:fixtures/setup/audit/2.xml")
    public void shouldReturnIfCreatedTimeInMidTime() {
        final LocalDate fromDate = LocalDate.of(2020, 9, 13);

        List<ItemIdentifier> identifierList = jdbcMeasurementAuditRepository.findByItemIdentifierAndEventTypeDailyEvent(
                ImmutableList.of(ItemIdentifier.of("1", "sku1")),
                EventType.PUT_VERDICT,
                fromDate);

        assertions().assertThat(identifierList).hasSize(1);
    }

    @Test
    @DatabaseSetup(value = "classpath:fixtures/setup/audit/3.xml")
    public void shouldNotReturnIfCreatedLessMidTime() {
        final LocalDate fromDate = LocalDate.of(2020, 9, 13);

        List<ItemIdentifier> identifierList = jdbcMeasurementAuditRepository.findByItemIdentifierAndEventTypeDailyEvent(
                ImmutableList.of(ItemIdentifier.of("1", "sku1")),
                EventType.PUT_VERDICT,
                fromDate);

        assertions().assertThat(identifierList).hasSize(0);
    }
}
