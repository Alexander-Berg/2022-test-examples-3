package ru.yandex.market.notification.notifications.feedback;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import ru.yandex.market.AbstractYqlTest;

public class FeedbackReportNotificationDaoTest extends AbstractYqlTest {

    private FeedbackReportNotificationDao feedbackReportNotificationDao;

    @DisplayName("Проверяем, что запрос выполняется корректно")
    @Test
    @Disabled("Для локального тестирования запроса")
    public void testValidYql() {
        NamedParameterJdbcTemplate template = yqlNamedParameterJdbcTemplate();
        FeedbackReportNotificationSettings settings = new FeedbackReportNotificationSettings(null,
                "//home/market/testing/pers-grade/tables/pub_shop_grades/current", 1);
        feedbackReportNotificationDao = new FeedbackReportNotificationDao(settings, template);

        String day = "2015-11-21";
        List<FeedbackReportNotificationData.Record> records = feedbackReportNotificationDao.readGrades(day);

        Set<Long> partners = Set.of(307233L, 267545L);
        List<FeedbackReportNotificationData.Record> filteredRecords = records.stream()
                .filter(record -> partners.contains(record.partnerId))
                .collect(Collectors.toList());

        Assertions.assertEquals(2, filteredRecords.size());
    }

    @Override
    protected String getUser() {
        return "user";
    }

    @Override
    protected String getPassword() {
        return "";
    }

    @Override
    protected String getYqlUrl() {
        return "jdbc:yql://yql.yandex.net:443";
    }
}
