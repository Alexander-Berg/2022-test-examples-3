package ru.yandex.market.notification.notifications.question;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.AbstractYqlTest;

public class QuestionReportNotificationDaoTest extends AbstractYqlTest {
    private QuestionReportNotificationDao questionReportNotificationDao;

    @DisplayName("Проверяем, что запрос выполняется корректно")
    @Test
    @Disabled("Для локального тестирования запроса")
    public void testValidYql() {
        NamedParameterJdbcTemplate template = yqlNamedParameterJdbcTemplate();
        QuestionReportNotificationSettings settings = new QuestionReportNotificationSettings(null,
                "//home/market/testing/pers-qa/partner/shop_questions/current",
                "//home/market/prestable/mstat/dictionaries/partner_types/latest");
        questionReportNotificationDao = new QuestionReportNotificationDao(template, settings);

        String day = "2021-10-28";
        List<QuestionReportNotificationData.Record> records = questionReportNotificationDao.readQuestions(day);

        Assertions.assertEquals(29, records.size());
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
