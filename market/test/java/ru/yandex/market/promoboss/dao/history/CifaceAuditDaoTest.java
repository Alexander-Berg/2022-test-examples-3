package ru.yandex.market.promoboss.dao.history;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.promoboss.dao.AbstractDaoTest;
import ru.yandex.market.promoboss.model.CifacePromotion;
import ru.yandex.market.promoboss.model.history.Audit;
import ru.yandex.market.promoboss.model.history.OperationType;

@ContextConfiguration(classes = CifaceAuditDao.class)
@DbUnitDataSet(before = "AuditTestData.csv")
class CifaceAuditDaoTest extends AbstractDaoTest {

    @Autowired
    private CifaceAuditDao cifaceAuditDao;

    @Test
    void getCifaceChanges_emptyResult() {
        List<Audit<Map<String, String>>> actualResult =
                cifaceAuditDao.getCifaceChanges(List.of(BigDecimal.valueOf(777L)));
        Assertions.assertEquals(Collections.emptyList(), actualResult);
    }

    @Test
    void getCifaceChanges_success() {
        List<Audit<Map<String, String>>> actualResult =
                cifaceAuditDao.getCifaceChanges(List.of(BigDecimal.valueOf(123L), BigDecimal.valueOf(9999L)));
        List<Audit<Map<String, String>>> expectedResult = List.of(
                Audit.<Map<String, String>>builder()
                        .dbUser("postgres9")
                        .operationTime(4073587200000L)
                        .operationType(OperationType.INSERT)
                        .transactionId(BigDecimal.valueOf(9999L))
                        .entity(new HashMap<>() {
                            {
                                put("promo_purpose", "promoPurpose9");
                                put("compensation_source", "compensationSource9");
                                put("trade_manager", "tradeManager9");
                                put("cat_manager", "catManager9");
                                put("promo_kind", "promoKind9");
                                put("supplier_type", "supplierType9");
                                put("author", "author9");
                                put("budget_owner", "TRADE_MARKETING9");
                                put("media_plan_s3_key", "mediaPlanS3Key9");
                                put("media_plan_s3_file_name", "mediaPlanS3FileName9");
                                put("final_budget", "true");
                                put("auto_compensation", "false");
                                put("compensation_ticket", "compensationTicket9");
                                put("assortment_load_method", "assortmentLoadMethod9");
                                put("pi_published_at", "999999");
                            }
                        })
                        .build(),
                Audit.<Map<String, String>>builder()
                        .dbUser("postgres")
                        .operationTime(4070908800000L)
                        .operationType(OperationType.INSERT)
                        .transactionId(BigDecimal.valueOf(123L))
                        .entity(new HashMap<>() {
                            {
                                put("promo_purpose", "promoPurpose");
                                put("compensation_source", "compensationSource");
                                put("trade_manager", "tradeManager");
                                put("cat_manager", "catManager");
                                put("promo_kind", "promoKind");
                                put("supplier_type", "supplierType");
                                put("author", "author");
                                put("budget_owner", "TRADE_MARKETING");
                                put("media_plan_s3_key", "mediaPlanS3Key");
                                put("media_plan_s3_file_name", "mediaPlanS3FileName");
                                put("final_budget", "true");
                                put("auto_compensation", "false");
                                put("compensation_ticket", "compensationTicket");
                                put("assortment_load_method", "assortmentLoadMethod");
                                put("pi_published_at", "1640998861");
                            }
                        })
                        .build()
        );
        Assertions.assertEquals(expectedResult, actualResult);
    }

    @Test
    void getCifaceMultiPropsChanges() {
        List<Audit<Map<String, String>>> actualResult =
                cifaceAuditDao.getCifaceMultiPropsChanges(List.of(
                        BigDecimal.valueOf(123L),
                        BigDecimal.valueOf(9999L),
                        BigDecimal.valueOf(77L)));

        List<Audit<Map<String, String>>> expectedResult = List.of(
                Audit.<Map<String, String>>builder()
                        .dbUser("postgres")
                        .operationTime(4070908800000L)
                        .operationType(OperationType.INSERT)
                        .transactionId(BigDecimal.valueOf(123L))
                        .entity(new HashMap<>() {
                            {
                                put("string_value", "method1");
                                put("id", "1");
                                put("property_name", "COMPENSATION_RECEIVE_METHOD");
                            }
                        })
                        .build(),
                Audit.<Map<String, String>>builder()
                        .dbUser("postgres")
                        .operationTime(4070908800000L)
                        .operationType(OperationType.INSERT)
                        .transactionId(BigDecimal.valueOf(123L))
                        .entity(new HashMap<>() {
                            {
                                put("string_value", "method2");
                                put("id", "2");
                                put("property_name", "COMPENSATION_RECEIVE_METHOD");
                            }
                        })
                        .build(),
                Audit.<Map<String, String>>builder()
                        .dbUser("postgres")
                        .operationTime(4070908800000L)
                        .operationType(OperationType.INSERT)
                        .transactionId(BigDecimal.valueOf(123L))
                        .entity(new HashMap<>() {
                            {
                                put("string_value", "stream");
                                put("id", "3");
                                put("property_name", "CATEGORY_STREAM");
                            }
                        })
                        .build(),
                Audit.<Map<String, String>>builder()
                        .dbUser("postgres")
                        .operationTime(4070908800000L)
                        .operationType(OperationType.INSERT)
                        .transactionId(BigDecimal.valueOf(123L))
                        .entity(new HashMap<>() {
                            {
                                put("string_value", "dept");
                                put("id", "4");
                                put("property_name", "CATEGORY_DEPARTMENT");
                            }
                        })
                        .build(),
                Audit.<Map<String, String>>builder()
                        .dbUser("postgres")
                        .operationTime(4039372800000L)
                        .operationType(OperationType.INSERT)
                        .transactionId(BigDecimal.valueOf(9999L))
                        .entity(new HashMap<>() {
                            {
                                put("string_value", "dept99");
                                put("id", "5");
                                put("property_name", "CATEGORY_DEPARTMENT");
                            }
                        })
                        .build()
        );
        Assertions.assertEquals(expectedResult.size(), actualResult.size());
        Assertions.assertEquals(expectedResult, actualResult);
    }

    @Test
    void getCifacePromotionChanges() {
        List<Audit<CifacePromotion>> actualResult =
                cifaceAuditDao.getCifacePromotionChanges(List.of(BigDecimal.valueOf(123L), BigDecimal.valueOf(124L)));
        List<Audit<CifacePromotion>> expectedResult = List.of(Audit.<CifacePromotion>builder().dbUser("postgres")
                .operationTime(4070908800000L)
                .operationType(OperationType.INSERT)
                .transactionId(BigDecimal.valueOf(123L))
                .entity(CifacePromotion.builder()
                        .promoId(1L)
                        .budgetFact(140000L)
                        .budgetPlan(150000L)
                        .isCustomBudgetPlan(false)
                        .category("Медийное размещение Главная")
                        .catteam("DiY")
                        .channel("Главная страница. Растяжка 500 тыс. показов")
                        .count(1L)
                        .countUnit("нед")
                        .comment(null).build())
                .build());
        Assertions.assertEquals(expectedResult, actualResult);
    }
}
