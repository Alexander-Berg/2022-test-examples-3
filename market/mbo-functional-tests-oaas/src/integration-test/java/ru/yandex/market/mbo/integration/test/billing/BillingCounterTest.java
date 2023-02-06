package ru.yandex.market.mbo.integration.test.billing;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.RowMapper;
import ru.yandex.market.mbo.billing.BillingCounter;
import ru.yandex.market.mbo.billing.BillingLogCleaner;
import ru.yandex.market.mbo.billing.BillingSessionManager;
import ru.yandex.market.mbo.billing.PaidAction;
import ru.yandex.market.mbo.billing.counter.info.BillingOperationInfoBase;
import ru.yandex.market.mbo.billing.counter.info.BillingOperationInfoWithPrice;
import ru.yandex.market.mbo.billing.counter.info.BillingOperationInfoWithoutPrice;
import ru.yandex.market.mbo.billing.tarif.TarifManager;
import ru.yandex.market.mbo.integration.test.BaseIntegrationTest;
import ru.yandex.market.mbo.statistic.model.TaskType;
import ru.yandex.market.mbo.utils.RandomTestUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("checkstyle:magicnumber")
public class BillingCounterTest extends BaseIntegrationTest {

    private static final PaidAction PAID_ACTION = PaidAction.ADD_MODEL;
    private static final long CATEGORY_ID_1 = 100500L;
    private static final long CATEGORY_ID_2 = 100600L;
    private static final BigDecimal PRICE = new BigDecimal(1.5);

    @Autowired
    BillingCounter billingCounter;

    @Autowired
    BillingLogCleaner billingLogCleaner;

    @Autowired
    TestOperationCounter testOperationCounter;

    @Autowired
    JdbcOperations siteCatalogJdbcTemplate;

    @Autowired
    BillingSessionManager billingSessionManager;

    @Autowired
    TarifManager tarifManager;

    private final BillingLogEntryRowMapper paidOperationRowMapper = new BillingLogEntryRowMapper(true);
    private final BillingLogEntryRowMapper suspendedOperationRowMapper = new BillingLogEntryRowMapper(false);

    @Test
    public void countSessionWithSuspended() {
        LocalDateTime billingStart = LocalDateTime.now()
            .minusDays(3)
            .toLocalDate().atStartOfDay();

        setBillingStartDate(billingStart);

        List<BillingOperationInfoWithPrice> day1Operations =  ImmutableList.of(
            createOperationWithPrice(billingStart.plusMinutes(1)),
            createOperationWithPrice(billingStart.plusMinutes(2)),
            createOperationWithPrice(billingStart.plusMinutes(3))
        );
        List<BillingOperationInfoWithPrice> day2Operations =  ImmutableList.of(
            createOperationWithPrice(billingStart.plusDays(1).plusMinutes(4)),
            createOperationWithPrice(billingStart.plusDays(1).plusMinutes(5))
        );
        List<BillingOperationInfoWithoutPrice> suspendedOperations =  ImmutableList.of(
            createOperationWithoutPrice(billingStart.plusMinutes(1)),
            createOperationWithoutPrice(billingStart.plusMinutes(2))
        );
        List<BillingOperationInfoWithPrice> allOperations = Stream.of(day1Operations, day2Operations)
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
        testOperationCounter.setOperations(allOperations, suspendedOperations);

        billingCounter.loadBilling(billingSessionManager);

        List<BillingLogEntry> countedOperations = getLogEntries();
        List<BillingLogEntry> expectedOperations = day1Operations.stream()
            .map(this::from)
            .collect(Collectors.toList());

        assertThat(countedOperations).containsOnlyElementsOf(expectedOperations);

        tarifManager.updateCategoryOperationTarif(
            PAID_ACTION.getId(), CATEGORY_ID_1, toCalendar(billingStart.plusHours(1)), PRICE);

        billingCounter.loadBilling(billingSessionManager);
        countedOperations = getLogEntries();
        expectedOperations = Stream.concat(
            allOperations.stream().map(this::from),
            suspendedOperations.stream().map(o -> from(o, billingStart.plusDays(1), PRICE))
        ).collect(Collectors.toList());

        assertThat(countedOperations).containsOnlyElementsOf(expectedOperations);
    }

    @Test
    public void cleanAndCountSessionWithSuspended() {
        LocalDateTime billingStart = LocalDateTime.now()
            .minusDays(3)
            .toLocalDate().atStartOfDay();

        setBillingStartDate(billingStart);

        List<BillingOperationInfoWithPrice> day1Operations =  ImmutableList.of(
            createOperationWithPrice(billingStart.plusMinutes(1)),
            createOperationWithPrice(billingStart.plusMinutes(2)),
            createOperationWithPrice(billingStart.plusMinutes(3))
        );
        List<BillingOperationInfoWithPrice> day2Operations =  ImmutableList.of(
            createOperationWithPrice(billingStart.plusDays(1).plusMinutes(4)),
            createOperationWithPrice(billingStart.plusDays(1).plusMinutes(5))
        );
        List<BillingOperationInfoWithoutPrice> suspendedDay1 =  ImmutableList.of(
            createOperationWithoutPrice(billingStart.plusMinutes(1)),
            createOperationWithoutPrice(billingStart.plusMinutes(2))
        );
        List<BillingOperationInfoWithoutPrice> suspendedDay2 =  ImmutableList.of(
            createOperationWithoutPrice(billingStart.plusDays(1).plusMinutes(1), CATEGORY_ID_2),
            createOperationWithoutPrice(billingStart.plusDays(1).plusMinutes(2), CATEGORY_ID_2)
        );
        List<BillingOperationInfoWithPrice> allOperations = Stream.of(day1Operations, day2Operations)
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
        List<BillingOperationInfoWithoutPrice> allSuspended = Stream.of(suspendedDay1, suspendedDay2)
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
        testOperationCounter.setOperations(allOperations, allSuspended);

        billingCounter.loadBilling(billingSessionManager);

        tarifManager.updateCategoryOperationTarif(
            PAID_ACTION.getId(), CATEGORY_ID_1, toCalendar(billingStart.plusDays(1).plusHours(1)), PRICE);
        billingCounter.loadBilling(billingSessionManager);

        tarifManager.updateCategoryOperationTarif(
            PAID_ACTION.getId(), CATEGORY_ID_2, toCalendar(billingStart.plusDays(2).plusHours(1)), PRICE);
        billingCounter.loadBilling(billingSessionManager);

        List<BillingLogEntry> countedOperations = getLogEntries();
        List<BillingLogEntry> expectedOperations = Stream.of(
            allOperations.stream().map(this::from),
            suspendedDay1.stream().map(o -> from(o, billingStart.plusDays(1), PRICE)),
            suspendedDay2.stream().map(o -> from(o, billingStart.plusDays(2), PRICE))
        ).flatMap(Function.identity()).collect(Collectors.toList());

        assertThat(countedOperations).containsOnlyElementsOf(expectedOperations);

        billingLogCleaner.clean();
        billingLogCleaner.clean();
        billingCounter.loadBilling(billingSessionManager);
        billingCounter.loadBilling(billingSessionManager);

        countedOperations = getLogEntries();
        assertThat(countedOperations).containsOnlyElementsOf(expectedOperations);
    }

    @Test
    public void testPriceMultiplierOnSuspended() {
        LocalDateTime billingStart = LocalDateTime.now()
            .minusDays(3)
            .toLocalDate().atStartOfDay();

        setBillingStartDate(billingStart);

        BigDecimal priceMultiplicator = new BigDecimal("3");
        BillingOperationInfoWithoutPrice multiplicator = createOperationWithoutPrice(billingStart.plusMinutes(1))
            .setPriceMultiplicator(priceMultiplicator);
        BillingOperationInfoWithoutPrice simple = createOperationWithoutPrice(billingStart.plusMinutes(2))
            .setPriceMultiplicator(null);

        testOperationCounter.setOperations(Collections.emptyList(), Arrays.asList(multiplicator, simple));
        billingCounter.loadBilling(billingSessionManager);

        List<BillingLogEntry> countedOperations = getLogEntries();
        assertThat(countedOperations).isEmpty();

        tarifManager.updateCategoryOperationTarif(
            PAID_ACTION.getId(), CATEGORY_ID_1, toCalendar(billingStart.plusHours(1)), PRICE);

        billingCounter.loadBilling(billingSessionManager);
        countedOperations = getLogEntries();

        assertThat(countedOperations).containsExactlyInAnyOrder(
            fromBase(multiplicator).setPrice(PRICE.multiply(priceMultiplicator)).setTime(billingStart.plusDays(1)),
            fromBase(simple).setPrice(PRICE).setTime(billingStart.plusDays(1))
        );
    }

    @Test
    public void testParameterNameOnSuspended() {
        LocalDateTime billingStart = LocalDateTime.now()
            .minusDays(3)
            .toLocalDate().atStartOfDay();

        setBillingStartDate(billingStart);

        BillingOperationInfoWithoutPrice paramName = createOperationWithoutPrice(billingStart.plusMinutes(1));
        paramName.setParameterName("hello world");
        BillingOperationInfoWithoutPrice simple = createOperationWithoutPrice(billingStart.plusMinutes(2));
        simple.setParameterName(null);

        testOperationCounter.setOperations(Collections.emptyList(), Arrays.asList(paramName, simple));
        billingCounter.loadBilling(billingSessionManager);

        List<BillingLogEntry> countedOperations = getLogEntries();
        assertThat(countedOperations).isEmpty();

        tarifManager.updateCategoryOperationTarif(
            PAID_ACTION.getId(), CATEGORY_ID_1, toCalendar(billingStart.plusHours(1)), PRICE);

        billingCounter.loadBilling(billingSessionManager);
        countedOperations = getLogEntries();

        assertThat(countedOperations).containsExactlyInAnyOrder(
            from(paramName, billingStart.plusDays(1), PRICE).setParameterName("hello world"),
            from(simple, billingStart.plusDays(1), PRICE).setParameterName(null)
        );
    }

    private BillingOperationInfoWithPrice createOperationWithPrice(LocalDateTime time) {
        BillingOperationInfoWithPrice result = RandomTestUtils.randomObject(BillingOperationInfoWithPrice.class);
        fillOperationData(result, time, CATEGORY_ID_1);
        result.setPrice(result.getPrice().setScale(2, RoundingMode.HALF_UP).stripTrailingZeros());
        return result;
    }

    private BillingOperationInfoWithoutPrice createOperationWithoutPrice(LocalDateTime time) {
        BillingOperationInfoWithoutPrice result = RandomTestUtils.randomObject(BillingOperationInfoWithoutPrice.class);
        fillOperationData(result, time, CATEGORY_ID_1);
        return result;
    }

    private BillingOperationInfoWithoutPrice createOperationWithoutPrice(LocalDateTime time, Long categoryId) {
        BillingOperationInfoWithoutPrice result = RandomTestUtils.randomObject(BillingOperationInfoWithoutPrice.class);
        fillOperationData(result, time, categoryId);
        return result;
    }

    private void fillOperationData(BillingOperationInfoBase operation, LocalDateTime time, long categoryId) {
        operation.setTime(toCalendar(time));
        operation.setOperation(PAID_ACTION);
        operation.setGuruCategoryId(categoryId);
    }

    private Calendar toCalendar(LocalDateTime time) {
        ZoneId zoneId = ZoneId.systemDefault();
        ZonedDateTime zdt = time.atZone(zoneId);
        return GregorianCalendar.from(zdt);
    }

    private void setBillingStartDate(LocalDateTime dateTime) {
        siteCatalogJdbcTemplate.update(
            "INSERT INTO ng_daily_balance (id, day, balance) VALUES (ng_daily_balance_id_seq.NEXTVAL, ?, ?)",
            new Date(toCalendar(dateTime).getTimeInMillis()),
            0
        );
    }

    private List<BillingLogEntry> getLogEntries() {
        return siteCatalogJdbcTemplate.query("select * from ng_paid_operation_log",
            paidOperationRowMapper);
    }

    static class BillingLogEntryRowMapper implements RowMapper<BillingLogEntry> {

        private final boolean hasPrice;

        BillingLogEntryRowMapper(boolean hasPrice) {
            this.hasPrice = hasPrice;
        }

        @Override
        public BillingLogEntry mapRow(ResultSet rs, int rowNum) throws SQLException {
            BillingLogEntry result = new BillingLogEntry();
            result.setUserId(rs.getLong("user_id"));
            result.setAuditActionId(rs.getLong("audit_action_id"));
            result.setCount(rs.getDouble("count"));
            result.setGuruCategoryId(rs.getLong("category_id"));
            result.setOperation(PaidAction.getPaidAction(rs.getInt("operation_id")));
            result.setTime(rs.getTimestamp("time").toLocalDateTime());
            result.setSourceId(rs.getLong("source_id"));
            if (hasPrice) {
                result.setPrice(rs.getBigDecimal("price"));
            }
            result.setExternalSource(rs.getString("external_source") == null ?
                null :
                TaskType.valueOf(rs.getString("external_source")));
            result.setExternalSourceId(rs.getString("external_source_id"));
            result.setParameterName(rs.getString("parameter_name"));
            return result;
        }
    }

    private BillingLogEntry from(BillingOperationInfoWithPrice operation) {
        BillingLogEntry result = fromBase(operation);
        result.setPrice(operation.getPrice());
        return result;
    }

    private BillingLogEntry from(BillingOperationInfoWithoutPrice operation,
                                 LocalDateTime processedDate,
                                 BigDecimal price) {
        BigDecimal priceMultiplicator = operation.getPriceMultiplicator();
        if (priceMultiplicator != null) {
            price = price.multiply(priceMultiplicator);
        }
        BillingLogEntry result = fromBase(operation);
        result.setPrice(price);
        result.setTime(processedDate);
        return result;
    }

    private BillingLogEntry fromBase(BillingOperationInfoBase operation) {
        BillingLogEntry result = new BillingLogEntry();
        result.setUserId(operation.getUserId());
        result.setAuditActionId(operation.getAuditActionId());
        result.setCount(operation.getCount());
        result.setGuruCategoryId(operation.getGuruCategoryId());
        result.setOperation(operation.getOperation());
        result.setTime(LocalDateTime.ofInstant(operation.getTime().toInstant(), ZoneId.systemDefault()));
        result.setSourceId(operation.getSourceId());
        result.setExternalSource(operation.getExternalSource());
        result.setExternalSourceId(operation.getExternalSourceId());
        result.setParameterName(operation.getParameterName());
        return result;
    }

}
