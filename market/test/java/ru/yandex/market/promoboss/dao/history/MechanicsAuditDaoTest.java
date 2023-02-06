package ru.yandex.market.promoboss.dao.history;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.promoboss.dao.AbstractDaoTest;
import ru.yandex.market.promoboss.model.history.Audit;
import ru.yandex.market.promoboss.model.history.OperationType;

@ContextConfiguration(classes = MechanicsAuditDao.class)
@DbUnitDataSet(before = "AuditTestData.csv")
class MechanicsAuditDaoTest extends AbstractDaoTest {

    @Autowired
    private MechanicsAuditDao mechanicsAuditDao;

    @Test
    void getPromocodeChanges() {
        List<Audit<Map<String, String>>> actualResult =
                mechanicsAuditDao.getPromocodeChanges(
                        List.of(BigDecimal.valueOf(800L), BigDecimal.valueOf(801L), BigDecimal.valueOf(802L)));

        List<Audit<Map<String, String>>> expectedResult = List.of(
                Audit.<Map<String, String>>builder()
                        .dbUser("postgres")
                        .operationTime(4070995200000L)//2099-01-02 00:00:00.000000
                        .operationType(OperationType.INSERT)
                        .transactionId(BigDecimal.valueOf(801L))
                        .entity(new HashMap<>() {
                            {
                                put("id", "2");
                                put("code_type", "FIXED_DISCOUNT");
                                put("value", "123");
                                put("code", "code");
                                put("min_cart_price", "22");
                                put("max_cart_price", "2222");
                                put("apply_multiple_times", "true");
                                put("budget", "1234567");
                                put("additional_conditions", "additional_conditions1");
                            }
                        }).build(),
                Audit.<Map<String, String>>builder()
                        .dbUser("postgres")
                        .operationTime(4070908800000L)//2099-01-01 00:00:00.000000
                        .operationType(OperationType.DELETE)
                        .transactionId(BigDecimal.valueOf(800L))
                        .entity(new HashMap<>() {
                            {
                                put("id", "1");
                                put("code_type", "FIXED_DISCOUNT");
                                put("value", "11");
                                put("code", "code1");
                                put("min_cart_price", "11");
                                put("max_cart_price", "1111");
                                put("apply_multiple_times", "true");
                                put("budget", "1234567");
                                put("additional_conditions", "additional_conditions1");
                            }
                        }).build());

        Assertions.assertEquals(expectedResult, actualResult);
    }

    @Test
    void getCheapestAsGiftChanges() {
        List<Audit<Map<String, String>>> actualResult =
                mechanicsAuditDao.getCheapestAsGiftChanges(
                        List.of(BigDecimal.valueOf(123L), BigDecimal.valueOf(124L), BigDecimal.valueOf(802L)));

        List<Audit<Map<String, String>>> expectedResult = List.of(
                Audit.<Map<String, String>>builder()
                        .dbUser("postgres")
                        .operationTime(4070912340100L)//2099-01-01 00:59:00.1000
                        .operationType(OperationType.UPDATE_AFTER)
                        .transactionId(BigDecimal.valueOf(124L))
                        .entity(new HashMap<>() {
                            {
                                put("count", "13");
                                put("id", "3");
                            }
                        }).build(),
                Audit.<Map<String, String>>builder()
                        .dbUser("postgres")
                        .operationTime(4070912340000L)//2099-01-01 00:59:00.0000
                        .operationType(OperationType.UPDATE_BEFORE)
                        .transactionId(BigDecimal.valueOf(124L))
                        .entity(new HashMap<>() {
                            {
                                put("count", "11");
                                put("id", "2");
                            }
                        }).build(),
                Audit.<Map<String, String>>builder()
                        .dbUser("postgres")
                        .operationTime(4070911800000L)//2099-01-01 00:50:00.0000
                        .operationType(OperationType.INSERT)
                        .transactionId(BigDecimal.valueOf(123L))
                        .entity(new HashMap<>() {
                            {
                                put("count", "11");
                                put("id", "1");
                            }
                        }).build());

        Assertions.assertEquals(expectedResult, actualResult);
    }
}
