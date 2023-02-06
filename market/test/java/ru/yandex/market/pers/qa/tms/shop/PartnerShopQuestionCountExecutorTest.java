package ru.yandex.market.pers.qa.tms.shop;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.application.monitoring.ComplexMonitoring;
import ru.yandex.market.application.monitoring.MonitoringStatus;
import ru.yandex.market.pers.qa.PersQaTmsTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static ru.yandex.market.pers.qa.tms.shop.PartnerShopQuestionService.PARTNER_SHOP_WITH_MODELS_DEFAULT_LOWER_VALUE;
import static ru.yandex.market.pers.qa.tms.shop.PartnerShopQuestionService.PARTNER_SHOP_WITH_MODELS_DEFAULT_UPPER_VALUE;
import static ru.yandex.market.pers.qa.tms.shop.PartnerShopQuestionService.PARTNER_SHOP_WITH_PREPARED_QUESTION_DEFAULT_LOWER_VALUE;
import static ru.yandex.market.pers.qa.tms.shop.PartnerShopQuestionService.PARTNER_SHOP_WITH_PREPARED_QUESTION_DEFAULT_UPPER_VALUE;
import static ru.yandex.market.pers.qa.tms.shop.PartnerShopQuestionService.QUESTION_FOR_PARTNER_SHOP_DEFAULT_MEDIAN_VALUE;

public class PartnerShopQuestionCountExecutorTest extends PersQaTmsTest {

    @Autowired
    PartnerShopQuestionCountExecutor executor;

    @Autowired
    @Qualifier("yqlJdbcTemplate")
    JdbcTemplate yqlJdbcTemplate;

    @Autowired
    private ComplexMonitoring complicatedMonitoring;

    @Test
    void testCountPartnerShopWithModelsLowerBound() throws Exception {
        assertMonitoring(
                "CRIT {countPartnerShopWithModels: Shop count: <14999> is less than the lower bound: <15000>}",
                PARTNER_SHOP_WITH_MODELS_DEFAULT_LOWER_VALUE - 1,
                PARTNER_SHOP_WITH_MODELS_DEFAULT_LOWER_VALUE,
                false
        );
    }

    @Test
    void testCountPartnerShopWithPreparedQuestionsLowerBound() throws Exception {
        assertMonitoring(
                "CRIT {countPartnerShopWithPreparedQuestions: Shop count: <14999> is less than the lower bound: <15000>}",
                PARTNER_SHOP_WITH_PREPARED_QUESTION_DEFAULT_LOWER_VALUE - 1,
                PARTNER_SHOP_WITH_PREPARED_QUESTION_DEFAULT_LOWER_VALUE,
                true
        );
    }

    @Test
    void testCountPartnerShopWithModelsUpperBound() throws Exception {
        assertMonitoring(
                "CRIT {countPartnerShopWithModels: Shop count: <20001> is more than the upper bound: <20000>}",
                PARTNER_SHOP_WITH_MODELS_DEFAULT_UPPER_VALUE + 1,
                PARTNER_SHOP_WITH_MODELS_DEFAULT_UPPER_VALUE,
                false
        );
    }

    @Test
    void testCountPartnerShopWithPreparedQuestionsUpperBound() throws Exception {
        assertMonitoring(
                "CRIT {countPartnerShopWithPreparedQuestions: Shop count: <20001> is more than the upper bound: <20000>}",
                PARTNER_SHOP_WITH_PREPARED_QUESTION_DEFAULT_UPPER_VALUE + 1,
                PARTNER_SHOP_WITH_PREPARED_QUESTION_DEFAULT_UPPER_VALUE,
                true
        );
    }

    @Test
    void testSuccessCheckFreshShopQuestions() {
        when(yqlJdbcTemplate.queryForObject(anyString(), any(Class.class))).thenReturn(10L);
        executor.freshnessPartnerShopPreparedQuestions();
        assertEquals(MonitoringStatus.OK, complicatedMonitoring.getResult().getStatus());
    }

    @Test
    void testUnsuccessCheckFreshShopQuestions() {
        when(yqlJdbcTemplate.queryForObject(anyString(), any(Class.class))).thenReturn(0L);
        executor.freshnessPartnerShopPreparedQuestions();
        assertEquals(MonitoringStatus.CRITICAL, complicatedMonitoring.getResult().getStatus());
        assertEquals("CRIT {freshShopQuestions: 0 fresh questions in shop_questions table}", complicatedMonitoring.getResult().getMessage());
    }

    private void assertMonitoring(String message, long critBound, long validBound, boolean withPreparedQuestion) throws Exception {
        when(yqlJdbcTemplate.queryForObject(anyString(), any(Class.class))).thenReturn(critBound);
        if (withPreparedQuestion) {
            executor.countPartnerShopWithPreparedQuestions();
        } else {
            executor.countPartnerShopWithModels();
        }
        assertEquals(MonitoringStatus.CRITICAL, complicatedMonitoring.getResult().getStatus());
        assertEquals(message, complicatedMonitoring.getResult().getMessage());

        resetMonitoring();
        when(yqlJdbcTemplate.queryForObject(anyString(), any(Class.class))).thenReturn(validBound);
        executor.countPartnerShopWithPreparedQuestions();
        assertEquals(MonitoringStatus.OK, complicatedMonitoring.getResult().getStatus());
    }

    @Test
    void testQuestionMedianForPartnerShop() throws Exception {
        when(yqlJdbcTemplate.queryForObject(anyString(), any(Class.class)))
                .thenReturn(QUESTION_FOR_PARTNER_SHOP_DEFAULT_MEDIAN_VALUE.subtract(BigDecimal.ONE));
        executor.countQuestionForPartnerShop();
        assertEquals(MonitoringStatus.CRITICAL, complicatedMonitoring.getResult().getStatus());
        assertEquals("CRIT {countQuestionForPartnerShop: Question median for shop:" +
                " <59> is less than the median bound: <60>}", complicatedMonitoring.getResult().getMessage());

        resetMonitoring();
        when(yqlJdbcTemplate.queryForObject(anyString(), any(Class.class))).thenReturn(QUESTION_FOR_PARTNER_SHOP_DEFAULT_MEDIAN_VALUE);
        executor.countQuestionForPartnerShop();
        assertEquals(MonitoringStatus.OK, complicatedMonitoring.getResult().getStatus());
    }

    @Test
    public void testExportPartnerShopQuestionStats() {
        when(yqlJdbcTemplate.queryForList(anyString())).thenReturn(Collections.singletonList(
                new HashMap<String, Object>() {{
                    put("q_min", 123);
                    put("q_max", 124);
                    put("q_median", 125);
                    put("q_percentile_40", 126);
                    put("q_percentile_80", 127);
                }}
        ));
        when(yqlJdbcTemplate.queryForObject(anyString(), any(Class.class))).thenReturn(123L);

        executor.exportPartnerShopQuestionStats();
    }

    @Test
    public void testExportPartnerShopQuestionWithEmptyStat() {
        when(yqlJdbcTemplate.queryForList(anyString())).thenReturn(Collections.emptyList());
        when(yqlJdbcTemplate.queryForObject(anyString(), any(Class.class))).thenReturn(123L);

        executor.exportPartnerShopQuestionStats();
    }
}
