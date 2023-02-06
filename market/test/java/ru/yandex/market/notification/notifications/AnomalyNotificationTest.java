package ru.yandex.market.notification.notifications;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Date;
import java.text.ParseException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.joda.time.DateTimeComparator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.campaign.CampaignService;
import ru.yandex.market.core.notification.service.NotificationSendContext;
import ru.yandex.market.core.xml.impl.NamedContainer;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.notification.PeriodicNotificationDao;
import ru.yandex.market.shop.FunctionalTest;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;


class AnomalyNotificationTest extends FunctionalTest {

    @Autowired
    private CampaignService campaignService;

    @Autowired
    private EnvironmentService env;

    @Autowired
    private PeriodicNotificationDao periodicNotificationDao;

    private final CronNotificationSchedule cron = Mockito.mock(CronNotificationSchedule.class);
    private final NamedParameterJdbcTemplate yql = Mockito.mock(NamedParameterJdbcTemplate.class);

    private final DateTimeComparator DATE_ONLY_COMPARATOR = DateTimeComparator.getDateOnlyInstance();
    private final Instant today = LocalDate.of(2022, Month.JULY, 18).atStartOfDay().toInstant(ZoneOffset.UTC);
    private final Clock clock = Clock.fixed(today , ZoneOffset.UTC);

    private String expectedQueryPath = "ru/yandex/market/notification/" +
            "notifications/AnomalyNotificationTest.todayQuery.txt";

    private AnomalyNotification anomalyNotification;
    private Method getPartnerIds;
    private Method selectDates;
    private Method prepareQuery;
    private Method getPartnerNotification;

    List<Long> realIds = List.of(1L, 2L, 3L, 4L);

    @BeforeEach
    void setUp() throws NoSuchMethodException, ParseException {
        when(yql.query(Mockito.anyString(), any(RowMapper.class))).thenReturn(realIds);
        anomalyNotification = new AnomalyNotification(
                yql, env, clock, campaignService, periodicNotificationDao)
                .setYtTablePath("cluster_name.`home/market/production/mstat/dictionaries/wms/anomaly/latest`");
        getPartnerIds = openPrivateMethod("getPartnerIds");
        selectDates = openPrivateMethod("selectDates");
        prepareQuery = openPrivateMethod("prepareQuery", String.class, String.class);
        getPartnerNotification = openPrivateMethod("getPartnerNotification", long.class);
    }

    @Test
    @DbUnitDataSet(before = "AnomalyNotificationTest.before.csv")
    void shouldSkipIdIfTheyNotTestIdAndTestIdEnabled() throws Exception {
        env.setValue("anomaly.notification.test.partners.only", "true");
        Assertions.assertEquals(new ArrayList<>(List.of(1L, 3L)), getPartnerIds.invoke(anomalyNotification));
    }

    @Test
    @DbUnitDataSet(before = "AnomalyNotificationTest.before.csv")
    void shouldReturnRealIdsIfTestIdsDisabled() throws Exception {
        env.setValue("anomaly.notification.test.partners.only", "false");
        Assertions.assertEquals(new ArrayList<>(List.of(1L, 2L, 3L, 4L)), getPartnerIds.invoke(anomalyNotification));
    }

    @Test
    @DbUnitDataSet(before = "AnomalyNotificationTest.before.csv")
    void shouldReturnEmptyListIfNotificationDisabled() throws Exception {
        env.setValue("anomaly.notification.test.partners.only", "false");
        env.setValue("anomaly.notification.enabled", "false");
        Assertions.assertEquals(List.of(), getPartnerIds.invoke(anomalyNotification));
    }

    @Test
    @DbUnitDataSet(before = "AnomalyNotificationTest.before.csv")
    void shouldSelectOnlyTodayIfNextNotificationTimeToday() throws InvocationTargetException, IllegalAccessException {
        var ans = Date.from(today);
        var result = ((ArrayList<java.util.Date>) selectDates.invoke(anomalyNotification)).get(0);
        Assertions.assertTrue(datesEqualsIgnoreTime(ans, result));
    }

    @Test
    @DbUnitDataSet(before = "AnomalyNotificationTest.before.setYesterday.csv")
    void shouldReturnDateRangeIfSentTimeHaveDivergentFromToday() throws Exception {
        var yesterday = Date.from(today.minusSeconds(60 * 60 * 24));
        var ans =  List.of(yesterday, Date.from(today));
        var result = (ArrayList<java.util.Date>) selectDates.invoke(anomalyNotification);
        for (int i = 0; i < ans.size(); ++i) {
            Assertions.assertTrue(datesEqualsIgnoreTime(ans.get(i), result.get(i)));
        }
    }

    @Test
    @DbUnitDataSet(before = "AnomalyNotificationTest.before.csv")
    void testReturnTodayIdsOnly() throws Exception {
        var todayIds = realIds;
        when(yql.query(anyString(), any(RowMapper.class)))
                .thenReturn(todayIds);
        Assertions.assertEquals(todayIds, getPartnerIds.invoke(anomalyNotification));
    }

    @Test
    @DbUnitDataSet(before = "AnomalyNotificationTest.before.setYesterday.csv")
    void checkCaseWhenJobFailedYesterday() throws Exception {
        var todayIds = realIds;
        var yesterdayIds = List.of(18L, 19L, 20L);
        var queryToday = (String) prepareQuery.invoke(anomalyNotification, "hahn", "2022-06-18");
        var queryYesterday = (String) prepareQuery.invoke(anomalyNotification, "hahn", "2022-06-17");
        when(yql.query(eq(queryToday), any(RowMapper.class))).thenReturn(todayIds);
        when(yql.query(eq(queryYesterday), any(RowMapper.class))).thenReturn(yesterdayIds);
        var expect = new ArrayList<>(todayIds);
        expect.addAll(yesterdayIds);
        var result = (List<Long>)getPartnerIds.invoke(anomalyNotification);
        Collections.sort(expect);
        Collections.sort(result);
        Assertions.assertEquals(expect, result);
    }

    @Test
    @DbUnitDataSet(before = "AnomalyNotificationTest.before.csv")
    void shouldReturnTodayIdsIfNextNotificationToday() throws Exception {
        var todayIds = realIds;
        var params = new MapSqlParameterSource().addValue("date", "2022-07-18");
        when(yql.query(any(), any(RowMapper.class)))
                .thenReturn(todayIds);
        var result = (List<Long>)getPartnerIds.invoke(anomalyNotification);
        Assertions.assertEquals(realIds, result);
    }

    @Test
    void checkQuery() throws Exception {
        var expectedQuery = readText(expectedQueryPath);
        var result = (String) prepareQuery.invoke(anomalyNotification, "hahn", "2022-07-18");
        Assertions.assertEquals(expectedQuery, result);
    }

    @Test
    @DbUnitDataSet(before = "AnomalyNotificationTest.mapToCampaign.csv")
    void checkNotificationContext() throws Exception {
        getPartnerIds.invoke(anomalyNotification);
        var opt = (Optional<NotificationSendContext>)getPartnerNotification.invoke(anomalyNotification, 4L);
        var ctx = opt.get();

        var expectedData = List.of(
                new NamedContainer("removeDate", "17 августа 2022 г."),
                new NamedContainer("campaignId", 321L)
        );

        Assertions.assertEquals(4L, ctx.getShopId());
        Assertions.assertEquals(1656418929, ctx.getTypeId());
        Assertions.assertEquals(expectedData, ctx.getData());
    }

    private String readText(String fileName) throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource(fileName).getFile());
        return FileUtils.readFileToString(file, "UTF-8");
    }

    private boolean datesEqualsIgnoreTime(java.util.Date left, java.util.Date right) {
        return DATE_ONLY_COMPARATOR.compare(left, right) == 0;
    }

    private Method openPrivateMethod(String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {
        var method = AnomalyNotification.class.getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return method;
    }

    private Method openPrivateMethod(String methodName) throws NoSuchMethodException {
        var method = AnomalyNotification.class.getDeclaredMethod(methodName);
        method.setAccessible(true);
        return method;
    }
}
