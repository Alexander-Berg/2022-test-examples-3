package ru.yandex.market.adv.shop.integration.checkouter.logbroker.service;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.adv.shop.integration.AbstractShopIntegrationTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.logbroker.consumer.LogbrokerException;
import ru.yandex.market.logbroker.db.LogbrokerMonitorExceptionsService;

/**
 * Date: 25.05.2022
 * Project: adv-shop-integration
 *
 * @author alexminakov
 */
class LogbrokerMonitorExceptionsServiceImplTest extends AbstractShopIntegrationTest {

    @Autowired
    private LogbrokerMonitorExceptionsService logbrokerMonitorExceptionsService;

    @DisplayName("Обновили текущую ошибку по топику")
    @DbUnitDataSet(
            before = "LogbrokerMonitorExceptionsServiceImpl/csv/addException_topicUpdate_one.before.csv",
            after = "LogbrokerMonitorExceptionsServiceImpl/csv/addException_topicUpdate_one.after.csv"
    )
    @Test
    void addException_topicUpdate_one() {
        logbrokerMonitorExceptionsService.addException(
                new LogbrokerException(
                        "checkouter_order",
                        "vla",
                        new RuntimeException()
                )
        );
    }

    @DisplayName("Создали новую ошибку по топику")
    @DbUnitDataSet(
            before = "LogbrokerMonitorExceptionsServiceImpl/csv/addException_topicNew_one.before.csv",
            after = "LogbrokerMonitorExceptionsServiceImpl/csv/addException_topicNew_one.after.csv"
    )
    @Test
    void addException_topicNew_one() {
        logbrokerMonitorExceptionsService.addException(
                new LogbrokerException(
                        "checkouter_order",
                        "iva",
                        new RuntimeException()
                )
        );
    }

    @DisplayName("Удалили ошибку по топику")
    @DbUnitDataSet(
            before = "LogbrokerMonitorExceptionsServiceImpl/csv/deleteAllExceptions_byExistTopic_two.before.csv",
            after = "LogbrokerMonitorExceptionsServiceImpl/csv/deleteAllExceptions_byExistTopic_two.after.csv"
    )
    @Test
    void deleteAllExceptions_byExistTopic_two() {
        Assertions.assertThat(logbrokerMonitorExceptionsService.deleteException("checkouter_order"))
                .isEqualTo(2);
    }

    @DisplayName("Ничего не удалили, т.к. не было ошибок по топику")
    @DbUnitDataSet(
            before = "LogbrokerMonitorExceptionsServiceImpl/csv/deleteAllExceptions_byEmptyTopic_zero.before.csv",
            after = "LogbrokerMonitorExceptionsServiceImpl/csv/deleteAllExceptions_byEmptyTopic_zero.after.csv"
    )
    @Test
    void deleteAllExceptions_byEmptyTopic_zero() {
        Assertions.assertThat(logbrokerMonitorExceptionsService.deleteException("topic"))
                .isEqualTo(0);
    }

    @DisplayName("Удаление всех ошибок произошло успешно")
    @DbUnitDataSet(
            before = "LogbrokerMonitorExceptionsServiceImpl/csv/deleteAllExceptions_all_three.before.csv",
            after = "LogbrokerMonitorExceptionsServiceImpl/csv/deleteAllExceptions_all_three.after.csv"
    )
    @Test
    void deleteAllExceptions_all_three() {
        Assertions.assertThat(logbrokerMonitorExceptionsService.deleteAllExceptions())
                .isEqualTo(3);
    }
}
