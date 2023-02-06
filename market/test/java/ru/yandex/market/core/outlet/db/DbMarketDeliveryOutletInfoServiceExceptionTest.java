package ru.yandex.market.core.outlet.db;

import java.math.BigDecimal;
import java.sql.BatchUpdateException;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.common.util.phone.PhoneType;
import ru.yandex.market.common.spring.jdbc.LogBatchExceptionHandler;
import ru.yandex.market.core.history.HistoryService;
import ru.yandex.market.core.outlet.OutletInfo;
import ru.yandex.market.core.outlet.OutletSource;
import ru.yandex.market.core.schedule.ScheduleDao;
import ru.yandex.market.core.schedule.ScheduleLine;
import ru.yandex.market.core.xml.Marshaller;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.core.outlet.db.DbMarketDeliveryOutletInfoProvider.DELIVERY_SERVICE_ID;
import static ru.yandex.market.core.outlet.db.DbMarketDeliveryOutletInfoProvider.DELIVERY_SERVICE_OUTLET_CODE1;
import static ru.yandex.market.core.outlet.db.DbMarketDeliveryOutletInfoProvider.DELIVERY_SERVICE_OUTLET_CODE2;
import static ru.yandex.market.core.outlet.db.DbMarketDeliveryOutletInfoProvider.IS_INLET;
import static ru.yandex.market.core.outlet.db.DbMarketDeliveryOutletInfoProvider.POINT_TYPE;
import static ru.yandex.market.core.outlet.db.DbMarketDeliveryOutletInfoService.DELIVERY_SERVICE_OUTLET_CODE_QUERY;
import static ru.yandex.market.core.outlet.db.DbMarketDeliveryOutletInfoService.INSERT_DELIVERY_SERVICE_DELIVERY_RULE_SQL;
import static ru.yandex.market.core.outlet.db.DbMarketDeliveryOutletInfoService.INSERT_DELIVERY_SERVICE_OUTLET_CODES_SQL;
import static ru.yandex.market.core.outlet.db.DbMarketDeliveryOutletInfoService.INSERT_DELIVERY_SERVICE_PHONE_SQL;
import static ru.yandex.market.core.outlet.db.DbOutletInfoService.MERGE_OUTLET_INFO_SQL;

/**
 * Тест для проверки обработки ошибок в
 * {@link DbMarketDeliveryOutletInfoService#refreshMarketDeliveryOutletInfo}.
 *
 * @author ivmelnik
 * @since 06.07.18
 */
@RunWith(MockitoJUnitRunner.class)
public class DbMarketDeliveryOutletInfoServiceExceptionTest {

    // Это означает, что первую запись сохранили, а на второй упали -> проверяем логи всех вторых записей
    private static final BatchUpdateException CAUSE = new BatchUpdateException(new int[]{1});
    private static final DataIntegrityViolationException EXCEPTION = new DataIntegrityViolationException("ooops", CAUSE);

    private static final List<OutletInfo> POINTS = asList(
            DbMarketDeliveryOutletInfoProvider.getOutletWithChildData(DELIVERY_SERVICE_OUTLET_CODE1),
            DbMarketDeliveryOutletInfoProvider.getOutletWithChildData(DELIVERY_SERVICE_OUTLET_CODE2)
    );

    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    @Mock
    private HistoryService historyService;
    @Mock
    private TransactionTemplate transactionTemplate;
    @Mock
    private Marshaller marshaller;
    @Mock
    private ScheduleDao outletScheduleDao;

    private DbMarketDeliveryOutletInfoService spiedService;

    private LogBatchExceptionHandler spiedLogHandler;

    @Before
    public void setUp() {
        // mock batchUpdate
        when(jdbcTemplate.execute(any(PreparedStatementCreator.class), any())).thenReturn(new int[0][0]);

        LogBatchExceptionHandler logBatchExceptionHandler = new LogBatchExceptionHandler();
        spiedLogHandler = spy(logBatchExceptionHandler);

        DbMarketDeliveryOutletInfoService service = new DbMarketDeliveryOutletInfoService();
        service.setJdbcTemplate(jdbcTemplate);
        service.setNamedParameterJdbcTemplate(namedParameterJdbcTemplate);
        service.setHistoryService(historyService);
        service.setTransactionTemplate(transactionTemplate);
        service.setMarshaller(marshaller);
        service.setOutletScheduleDao(outletScheduleDao);
        service.setLogBatchExceptionHandler(spiedLogHandler);
        spiedService = spy(service);
    }

    private void callRefreshAndCheckLog(String expectedLog) {
        try {
            spiedService.innerRefreshMarketDeliveryOutletInfo(DELIVERY_SERVICE_ID, POINT_TYPE, POINTS,
                    OutletSource.MARKET_DELIVERY);
        } catch (DataIntegrityViolationException e) {
            ArgumentCaptor<String> logCaptor = ArgumentCaptor.forClass(String.class);
            verify(spiedLogHandler).logError(logCaptor.capture(), eq(CAUSE));
            assertThat(logCaptor.getValue(), is(expectedLog));
            throw e;
        }
        fail();
    }

    @SafeVarargs
    private static String asLog(Pair<String, Object>... pairs) {
        return Stream.of(pairs)
                .collect(Collectors.toMap(Pair::getKey, Pair::getValue, (k1, k2) -> k1, TreeMap::new))
                .toString();
    }

    @Test(expected = DataIntegrityViolationException.class)
    public void exceptionOnInsertOutletCodes() {
        doThrow(EXCEPTION)
                .when(jdbcTemplate)
                .execute(eq(INSERT_DELIVERY_SERVICE_OUTLET_CODES_SQL), any());
        callRefreshAndCheckLog("DeliveryServiceCodes -> " + asLog(
                Pair.of("delivery_service_id", DELIVERY_SERVICE_ID),
                Pair.of("delivery_service_outlet_code", DELIVERY_SERVICE_OUTLET_CODE2),
                Pair.of("is_inlet", IS_INLET ? 1 : 0)
        ));
    }

    @Test(expected = DataIntegrityViolationException.class)
    public void exceptionOnMergeOutlets() {
        doThrow(EXCEPTION)
                .when(jdbcTemplate)
                .execute(eq(MERGE_OUTLET_INFO_SQL), any());
        callRefreshAndCheckLog("OutletInfo -> " + asLog(
                Pair.of("delivery_service_id", DELIVERY_SERVICE_ID),
                Pair.of("delivery_service_outlet_code", DELIVERY_SERVICE_OUTLET_CODE2),
                Pair.of("is_inlet", IS_INLET ? 1 : 0)
        ));
    }

    @Test(expected = DataIntegrityViolationException.class)
    public void exceptionOnInsertChildPhones() {
        doThrow(EXCEPTION).when(namedParameterJdbcTemplate)
                .batchUpdate(eq(INSERT_DELIVERY_SERVICE_PHONE_SQL), any(MapSqlParameterSource[].class));
        callRefreshAndCheckLog("Phone -> " + asLog(
                Pair.of("delivery_service_id", DELIVERY_SERVICE_ID),
                Pair.of("delivery_service_outlet_code", DELIVERY_SERVICE_OUTLET_CODE1),
                Pair.of("is_inlet", IS_INLET ? 1 : 0),
                Pair.of("country", "7"),
                Pair.of("city", "345"),
                Pair.of("num", "322-4567"),
                Pair.of("extension_str", "55"),
                Pair.of("comments", "blablaphone"),
                Pair.of("phone_type", PhoneType.FAX.ordinal())
        ));
    }

    @Test(expected = DataIntegrityViolationException.class)
    public void exceptionOnInsertChildRules() {
        doThrow(EXCEPTION).when(namedParameterJdbcTemplate)
                .batchUpdate(eq(INSERT_DELIVERY_SERVICE_DELIVERY_RULE_SQL), any(MapSqlParameterSource[].class));
        callRefreshAndCheckLog("DeliveryRule -> " + asLog(
                Pair.of("delivery_service_id", DELIVERY_SERVICE_ID),
                Pair.of("delivery_service_outlet_code", DELIVERY_SERVICE_OUTLET_CODE1),
                Pair.of("is_inlet", IS_INLET ? 1 : 0),
                Pair.of("price_from", BigDecimal.valueOf(1)),
                Pair.of("price_to", BigDecimal.valueOf(1000)),
                Pair.of("cost", BigDecimal.valueOf(200L)),
                Pair.of("min_delivery_days", 0),
                Pair.of("max_delivery_days", 30),
                Pair.of("work_in_holiday", 0),
                Pair.of("date_switch_hour", 19),
                Pair.of("unspecified_delivery_interval", 1),
                Pair.of("shipper_id", DELIVERY_SERVICE_ID),
                Pair.of("cost_currency", Currency.RUR.name())
        ));
    }

    @Test(expected = DataIntegrityViolationException.class)
    public void exceptionOnInsertChildSchedules() {
        doThrow(EXCEPTION).when(outletScheduleDao)
                .insertScheduleWithIdValueQuery(eq(DELIVERY_SERVICE_OUTLET_CODE_QUERY), any());
        callRefreshAndCheckLog("Schedule -> " + asLog(
                Pair.of("delivery_service_id", DELIVERY_SERVICE_ID),
                Pair.of("delivery_service_outlet_code", DELIVERY_SERVICE_OUTLET_CODE1),
                Pair.of("is_inlet", IS_INLET ? 1 : 0),
                Pair.of("start_day", ScheduleLine.DayOfWeek.THURSDAY.ordinal()),
                Pair.of("days", 1),
                Pair.of("start_minute", 540),
                Pair.of("minutes", 600)
        ));
    }

}
