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

@ContextConfiguration(classes = ConstraintAuditDao.class)
@DbUnitDataSet(before = "AuditTestData.csv")
class ConstraintAuditDaoTest extends AbstractDaoTest {

    @Autowired
    private ConstraintAuditDao constraintAuditDao;

    @Test
    void getCategoryConstrainsChanges() {
        List<Audit<Map<String, String>>> actualResult =
                constraintAuditDao.getCategoryConstrainsChanges(
                        List.of(BigDecimal.valueOf(1236L), BigDecimal.valueOf(1240L)));

        List<Audit<Map<String, String>>> expectedResult = List.of(
                Audit.<Map<String, String>>builder()
                        .dbUser("postgres")
                        .operationTime(4070926800000L) //2099-01-01 05:00:00.0000
                        .operationType(OperationType.INSERT)
                        .transactionId(BigDecimal.valueOf(1236L))
                        .entity(new HashMap<>() {
                            {
                                put("category_id", "61");
                                put("percent", "55");
                                put("exclude", "false");
                                put("id", "2");
                            }
                        }).build(),
                Audit.<Map<String, String>>builder()
                        .dbUser("postgres")
                        .operationTime(4070923200000L) //2099-01-01 04:00:00.0000
                        .operationType(OperationType.DELETE)
                        .transactionId(BigDecimal.valueOf(1236L))
                        .entity(new HashMap<>() {
                            {
                                put("category_id", "71");
                                put("percent", "55");
                                put("exclude", "false");
                                put("id", "1");
                            }
                        }).build());

        Assertions.assertEquals(expectedResult, actualResult);
    }

    @Test
    void getWarehouseConstraintsChanges() {
        List<Audit<Map<String, String>>> actualResult =
                constraintAuditDao.getWarehouseConstraintsChanges(
                        List.of(BigDecimal.valueOf(1236L), BigDecimal.valueOf(1240L)));

        List<Audit<Map<String, String>>> expectedResult = List.of(
                Audit.<Map<String, String>>builder()
                        .dbUser("postgres")
                        .operationTime(4070926800000L) //2099-01-01 05:00:00.0000
                        .operationType(OperationType.INSERT)
                        .transactionId(BigDecimal.valueOf(1236L))
                        .entity(new HashMap<>() {
                            {
                                put("warehouse_id", "91");
                                put("exclude", "false");
                                put("id", "2");
                            }
                        }).build(),
                Audit.<Map<String, String>>builder()
                        .dbUser("postgres")
                        .operationTime(4070923200000L) //2099-01-01 04:00:00.0000
                        .operationType(OperationType.DELETE)
                        .transactionId(BigDecimal.valueOf(1236L))
                        .entity(new HashMap<>() {
                            {
                                put("warehouse_id", "81");
                                put("exclude", "false");
                                put("id", "1");
                            }
                        }).build());

        Assertions.assertEquals(expectedResult, actualResult);
    }

    @Test
    void getVendorConstraintsChanges() {
        List<Audit<Map<String, String>>> actualResult =
                constraintAuditDao.getVendorConstraintsChanges(
                        List.of(BigDecimal.valueOf(1235L), BigDecimal.valueOf(1240L)));

        List<Audit<Map<String, String>>> expectedResult = List.of(
                Audit.<Map<String, String>>builder()
                        .dbUser("postgres")
                        .operationTime(4070916000000L) //2099-01-01 02:00:00.0000
                        .operationType(OperationType.INSERT)
                        .transactionId(BigDecimal.valueOf(1235L))
                        .entity(new HashMap<>() {
                            {
                                put("vendor_id", "41");
                                put("exclude", "false");
                                put("id", "2");
                            }
                        }).build(),
                Audit.<Map<String, String>>builder()
                        .dbUser("postgres")
                        .operationTime(4070912400000L) //2099-01-01 01:00:00.0000
                        .operationType(OperationType.DELETE)
                        .transactionId(BigDecimal.valueOf(1235L))
                        .entity(new HashMap<>() {
                            {
                                put("vendor_id", "31");
                                put("exclude", "false");
                                put("id", "1");
                            }
                        }).build());

        Assertions.assertEquals(expectedResult, actualResult);
    }

    @Test
    void getMskuConstraintsChanges() {
        List<Audit<Map<String, String>>> actualResult =
                constraintAuditDao.getMskuConstraintsChanges(
                        List.of(BigDecimal.valueOf(1234L), BigDecimal.valueOf(1240L)));

        List<Audit<Map<String, String>>> expectedResult = List.of(
                Audit.<Map<String, String>>builder()
                        .dbUser("postgres")
                        .operationTime(4070912460000L) //2099-01-01 01:01:00.0000
                        .operationType(OperationType.INSERT)
                        .transactionId(BigDecimal.valueOf(1234L))
                        .entity(new HashMap<>() {
                            {
                                put("msku_id", "21");
                                put("exclude", "false");
                                put("id", "2");
                            }
                        }).build(),
                Audit.<Map<String, String>>builder()
                        .dbUser("postgres")
                        .operationTime(4070912400000L) //2099-01-01 01:00:00.0000
                        .operationType(OperationType.DELETE)
                        .transactionId(BigDecimal.valueOf(1234L))
                        .entity(new HashMap<>() {
                            {
                                put("msku_id", "11");
                                put("exclude", "false");
                                put("id", "1");
                            }
                        }).build());

        Assertions.assertEquals(expectedResult, actualResult);
    }

    @Test
    void getRegionConstraintsChanges() {
        List<Audit<Map<String, String>>> actualResult =
                constraintAuditDao.getRegionConstraintsChanges(
                        List.of(BigDecimal.valueOf(1236L), BigDecimal.valueOf(1240L)));

        List<Audit<Map<String, String>>> expectedResult = List.of(
                Audit.<Map<String, String>>builder()
                        .dbUser("postgres")
                        .operationTime(4070923260000L) //2099-01-01 04:01:00.0000
                        .operationType(OperationType.INSERT)
                        .transactionId(BigDecimal.valueOf(1236L))
                        .entity(new HashMap<>() {
                            {
                                put("region_id", "61");
                                put("exclude", "false");
                                put("id", "2");
                            }
                        }).build(),
                Audit.<Map<String, String>>builder()
                        .dbUser("postgres")
                        .operationTime(4070923200000L) //2099-01-01 04:00:00.0000
                        .operationType(OperationType.DELETE)
                        .transactionId(BigDecimal.valueOf(1236L))
                        .entity(new HashMap<>() {
                            {
                                put("region_id", "51");
                                put("exclude", "false");
                                put("id", "1");
                            }
                        }).build());

        Assertions.assertEquals(expectedResult, actualResult);
    }

    @Test
    void getSupplierConstraintsChanges() {
        List<Audit<Map<String, String>>> actualResult =
                constraintAuditDao.getSupplierConstraintsChanges(
                        List.of(BigDecimal.valueOf(1236L), BigDecimal.valueOf(1240L)));

        List<Audit<Map<String, String>>> expectedResult = List.of(
                Audit.<Map<String, String>>builder()
                        .dbUser("postgres")
                        .operationTime(4070919660000L) //2099-01-01 03:01:00.0000
                        .operationType(OperationType.INSERT)
                        .transactionId(BigDecimal.valueOf(1236L))
                        .entity(new HashMap<>() {
                            {
                                put("supplier_id", "41");
                                put("exclude", "false");
                                put("id", "2");
                            }
                        }).build(),
                Audit.<Map<String, String>>builder()
                        .dbUser("postgres")
                        .operationTime(4070919600000L) //2099-01-01 03:00:00.0000
                        .operationType(OperationType.DELETE)
                        .transactionId(BigDecimal.valueOf(1236L))
                        .entity(new HashMap<>() {
                            {
                                put("supplier_id", "31");
                                put("exclude", "false");
                                put("id", "1");
                            }
                        }).build());

        Assertions.assertEquals(expectedResult, actualResult);
    }
}
