package ru.yandex.market.promoboss.integration.v2;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.promoboss.integration.AbstractIntegrationTest;
import ru.yandex.market.promoboss.integration.IntegrationPromoUtils;

public class PromoHistoryIntegrationTest extends AbstractIntegrationTest {
    public static final String URL = "/api/v2/promos/history";

    @Test
    void getPromoHistory_promoNotFound() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(URL).param("id", IntegrationPromoUtils.PROMO_ID))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    @DbUnitDataSet(before = "PromoHistoryIntegrationTest.getPromoHistory_promoInsert_success.csv")
    void getPromoHistory_promoInsert_success() throws Exception {
        Map<String, String> changes = new HashMap<>();

        changes.put("$.changes[0].fields[0].field", "active");
        changes.put("$.changes[0].fields[0].operationType", "INSERT");
        changes.put("$.changes[0].fields[0].values", "true");

        changes.put("$.changes[0].fields[1].field", "createdAt");
        changes.put("$.changes[0].fields[1].operationType", "INSERT");
        changes.put("$.changes[0].fields[1].values", "123");

        changes.put("$.changes[0].fields[2].field", "endAt");
        changes.put("$.changes[0].fields[2].operationType", "INSERT");
        changes.put("$.changes[0].fields[2].values", "451");

        changes.put("$.changes[0].fields[3].field", "hidden");
        changes.put("$.changes[0].fields[3].operationType", "INSERT");
        changes.put("$.changes[0].fields[3].values", "false");

        changes.put("$.changes[0].fields[4].field", "landingUrl");
        changes.put("$.changes[0].fields[4].operationType", "INSERT");
        changes.put("$.changes[0].fields[4].values", "https://landing.url");

        changes.put("$.changes[0].fields[5].field", "landingUrlAuto");
        changes.put("$.changes[0].fields[5].operationType", "INSERT");
        changes.put("$.changes[0].fields[5].values", "false");

        changes.put("$.changes[0].fields[6].field", "mechanicsType");
        changes.put("$.changes[0].fields[6].operationType", "INSERT");
        changes.put("$.changes[0].fields[6].values", "cheapest_as_gift");

        changes.put("$.changes[0].fields[7].field", "name");
        changes.put("$.changes[0].fields[7].operationType", "INSERT");
        changes.put("$.changes[0].fields[7].values", "name");

        changes.put("$.changes[0].fields[8].field", "parentPromoId");
        changes.put("$.changes[0].fields[8].operationType", "INSERT");
        changes.put("$.changes[0].fields[8].values", "parent_promo_id");

        changes.put("$.changes[0].fields[9].field", "promoKey");
        changes.put("$.changes[0].fields[9].operationType", "INSERT");
        changes.put("$.changes[0].fields[9].values", "promo_key");

        changes.put("$.changes[0].fields[10].field", "rulesUrl");
        changes.put("$.changes[0].fields[10].operationType", "INSERT");
        changes.put("$.changes[0].fields[10].values", "https://rules.url");

        changes.put("$.changes[0].fields[11].field", "rulesUrlAuto");
        changes.put("$.changes[0].fields[11].operationType", "INSERT");
        changes.put("$.changes[0].fields[11].values", "false");

        changes.put("$.changes[0].fields[12].field", "sourceType");
        changes.put("$.changes[0].fields[12].operationType", "INSERT");
        changes.put("$.changes[0].fields[12].values", "CATEGORYIFACE");

        changes.put("$.changes[0].fields[13].field", "startAt");
        changes.put("$.changes[0].fields[13].operationType", "INSERT");
        changes.put("$.changes[0].fields[13].values", "765");

        changes.put("$.changes[0].fields[14].field", "status");
        changes.put("$.changes[0].fields[14].operationType", "INSERT");
        changes.put("$.changes[0].fields[14].values", "NEW");

        ResultActions resultActions =
                mockMvc.perform(MockMvcRequestBuilders.get(URL).param("id", IntegrationPromoUtils.PROMO_ID))
                        .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes.length()").value(1))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.totalCount").value(1))

                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes[0].updatedAt").isNotEmpty())
                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes[0].updatedBy").isEmpty())
                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes[0].dbUser").value("postgres"))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes[0].requestId").isEmpty())
                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes[0].source").isEmpty())
                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes[0].transactionId").isNotEmpty())
                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes[0].fields.length()").value(15));

        for (Map.Entry<String, String> entry : changes.entrySet()) {
            resultActions.andExpect(MockMvcResultMatchers.jsonPath(entry.getKey()).value(entry.getValue()));
        }
    }

    @Test
    @DbUnitDataSet(before = "PromoHistoryIntegrationTest.getPromoHistory_promocodeInsert_success.csv")
    void getPromoHistory_promocodeInsert_success() throws Exception {
        Map<String, String> changes = new HashMap<>();

        changes.put("$.changes[0].fields[0].field", "promocodeAdditionalConditions");
        changes.put("$.changes[0].fields[0].operationType", "INSERT");
        changes.put("$.changes[0].fields[0].values", "additional_conditions1");

        changes.put("$.changes[0].fields[1].field", "promocodeApplyMultipleTimes");
        changes.put("$.changes[0].fields[1].operationType", "INSERT");
        changes.put("$.changes[0].fields[1].values", "true");

        changes.put("$.changes[0].fields[2].field", "promocodeBudget");
        changes.put("$.changes[0].fields[2].operationType", "INSERT");
        changes.put("$.changes[0].fields[2].values", "1234567");

        changes.put("$.changes[0].fields[3].field", "promocodeCode");
        changes.put("$.changes[0].fields[3].operationType", "INSERT");
        changes.put("$.changes[0].fields[3].values", "code1");

        changes.put("$.changes[0].fields[4].field", "promocodeCodeType");
        changes.put("$.changes[0].fields[4].operationType", "INSERT");
        changes.put("$.changes[0].fields[4].values", "FIXED_DISCOUNT");

        changes.put("$.changes[0].fields[5].field", "promocodeMaxCartPrice");
        changes.put("$.changes[0].fields[5].operationType", "INSERT");
        changes.put("$.changes[0].fields[5].values", "222");

        changes.put("$.changes[0].fields[6].field", "promocodeMinCartPrice");
        changes.put("$.changes[0].fields[6].operationType", "INSERT");
        changes.put("$.changes[0].fields[6].values", "111");

        changes.put("$.changes[0].fields[7].field", "promocodeValue");
        changes.put("$.changes[0].fields[7].operationType", "INSERT");
        changes.put("$.changes[0].fields[7].values", "11");

        ResultActions resultActions =
                mockMvc.perform(MockMvcRequestBuilders.get(URL).param("id", IntegrationPromoUtils.PROMO_ID))
                        .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes.length()").value(2))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.totalCount").value(2))

                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes[0].updatedAt").isNotEmpty())
                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes[0].updatedBy").isEmpty())
                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes[0].dbUser").value("postgres"))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes[0].requestId").isEmpty())
                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes[0].source").isEmpty())
                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes[0].transactionId").value("123"))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes[0].fields.length()").value(8));

        for (Map.Entry<String, String> entry : changes.entrySet()) {
            resultActions.andExpect(MockMvcResultMatchers.jsonPath(entry.getKey()).value(entry.getValue()));
        }
    }

    @Test
    @DbUnitDataSet(before = "PromoHistoryIntegrationTest.getPromoHistory_cheapestAsGiftInsert_success.csv")
    void getPromoHistory_cheapestAsGiftInsert_success() throws Exception {
        Map<String, String> changes = new HashMap<>();

        changes.put("$.changes[0].fields[0].field", "cheapestAsGiftCount");
        changes.put("$.changes[0].fields[0].operationType", "INSERT");
        changes.put("$.changes[0].fields[0].values", "11");

        ResultActions resultActions =
                mockMvc.perform(MockMvcRequestBuilders.get(URL).param("id", IntegrationPromoUtils.PROMO_ID))
                        .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes.length()").value(2))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.totalCount").value(2))

                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes[0].updatedAt").isNotEmpty())
                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes[0].updatedBy").isEmpty())
                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes[0].dbUser").value("postgres"))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes[0].requestId").isEmpty())
                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes[0].source").isEmpty())
                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes[0].transactionId").value("123"))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes[0].fields.length()").value(1));

        for (Map.Entry<String, String> entry : changes.entrySet()) {
            resultActions.andExpect(MockMvcResultMatchers.jsonPath(entry.getKey()).value(entry.getValue()));
        }
    }

    @Test
    @DbUnitDataSet(before = "PromoHistoryIntegrationTest.getPromoHistory_cifaceInsert_success.csv")
    void getPromoHistory_cifaceInsert_success() throws Exception {
        Map<String, String> changes = new HashMap<>();

        changes.put("$.changes[0].srcFields.ciface.fields[0].field", "assortmentLoadMethod");
        changes.put("$.changes[0].srcFields.ciface.fields[0].operationType", "INSERT");
        changes.put("$.changes[0].srcFields.ciface.fields[0].values", "assortmentLoadMethod");

        changes.put("$.changes[0].srcFields.ciface.fields[1].field", "author");
        changes.put("$.changes[0].srcFields.ciface.fields[1].operationType", "INSERT");
        changes.put("$.changes[0].srcFields.ciface.fields[1].values", "author");

        changes.put("$.changes[0].srcFields.ciface.fields[2].field", "autoCompensation");
        changes.put("$.changes[0].srcFields.ciface.fields[2].operationType", "INSERT");
        changes.put("$.changes[0].srcFields.ciface.fields[2].values", "false");

        changes.put("$.changes[0].srcFields.ciface.fields[3].field", "budgetOwner");
        changes.put("$.changes[0].srcFields.ciface.fields[3].operationType", "INSERT");
        changes.put("$.changes[0].srcFields.ciface.fields[3].values", "TRADE_MARKETING");

        changes.put("$.changes[0].srcFields.ciface.fields[4].field", "markom");
        changes.put("$.changes[0].srcFields.ciface.fields[4].operationType", "INSERT");
        changes.put("$.changes[0].srcFields.ciface.fields[4].values", "catManager");

        changes.put("$.changes[0].srcFields.ciface.fields[5].field", "compensationSource");
        changes.put("$.changes[0].srcFields.ciface.fields[5].operationType", "INSERT");
        changes.put("$.changes[0].srcFields.ciface.fields[5].values", "compensationSource");

        changes.put("$.changes[0].srcFields.ciface.fields[6].field", "compensationTicket");
        changes.put("$.changes[0].srcFields.ciface.fields[6].operationType", "INSERT");
        changes.put("$.changes[0].srcFields.ciface.fields[6].values", "compensationTicket");

        changes.put("$.changes[0].srcFields.ciface.fields[7].field", "finalBudget");
        changes.put("$.changes[0].srcFields.ciface.fields[7].operationType", "INSERT");
        changes.put("$.changes[0].srcFields.ciface.fields[7].values", "true");

        changes.put("$.changes[0].srcFields.ciface.fields[8].field", "mediaPlanS3FileName");
        changes.put("$.changes[0].srcFields.ciface.fields[8].operationType", "INSERT");
        changes.put("$.changes[0].srcFields.ciface.fields[8].values", "mediaPlanS3FileName");

        changes.put("$.changes[0].srcFields.ciface.fields[9].field", "mediaPlanS3Key");
        changes.put("$.changes[0].srcFields.ciface.fields[9].operationType", "INSERT");
        changes.put("$.changes[0].srcFields.ciface.fields[9].values", "mediaPlanS3Key");

        changes.put("$.changes[0].srcFields.ciface.fields[10].field", "piPublishedAt");
        changes.put("$.changes[0].srcFields.ciface.fields[10].operationType", "INSERT");
        changes.put("$.changes[0].srcFields.ciface.fields[10].values", "1640998861");

        changes.put("$.changes[0].srcFields.ciface.fields[11].field", "promoKind");
        changes.put("$.changes[0].srcFields.ciface.fields[11].operationType", "INSERT");
        changes.put("$.changes[0].srcFields.ciface.fields[11].values", "promoKind");

        changes.put("$.changes[0].srcFields.ciface.fields[12].field", "purpose");
        changes.put("$.changes[0].srcFields.ciface.fields[12].operationType", "INSERT");
        changes.put("$.changes[0].srcFields.ciface.fields[12].values", "promoPurpose");


        changes.put("$.changes[0].srcFields.ciface.fields[13].field", "supplierType");
        changes.put("$.changes[0].srcFields.ciface.fields[13].operationType", "INSERT");
        changes.put("$.changes[0].srcFields.ciface.fields[13].values", "supplierType");

        changes.put("$.changes[0].srcFields.ciface.fields[14].field", "tradeManager");
        changes.put("$.changes[0].srcFields.ciface.fields[14].operationType", "INSERT");
        changes.put("$.changes[0].srcFields.ciface.fields[14].values", "tradeManager");

        changes.put("$.changes[0].srcFields.ciface.fields[15].field", "compensationReceiveMethods");
        changes.put("$.changes[0].srcFields.ciface.fields[15].operationType", "INSERT");
        changes.put("$.changes[0].srcFields.ciface.fields[15].values[0]", "method1");
        changes.put("$.changes[0].srcFields.ciface.fields[15].values[1]", "method2");

        changes.put("$.changes[0].srcFields.ciface.fields[16].field", "streams");
        changes.put("$.changes[0].srcFields.ciface.fields[16].operationType", "INSERT");
        changes.put("$.changes[0].srcFields.ciface.fields[16].values", "stream");

        changes.put("$.changes[0].srcFields.ciface.fields[17].field", "departments");
        changes.put("$.changes[0].srcFields.ciface.fields[17].operationType", "INSERT");
        changes.put("$.changes[0].srcFields.ciface.fields[17].values", "dept");

        changes.put("$.changes[0].srcFields.ciface.promotion[0].values[0].budget_fact", "140000");
        changes.put("$.changes[0].srcFields.ciface.promotion[0].values[0].budget_plan", "150000");
        changes.put("$.changes[0].srcFields.ciface.promotion[0].values[0].category", "Медийное размещение Главная");
        changes.put("$.changes[0].srcFields.ciface.promotion[0].values[0].catteam", "DiY");
        changes.put("$.changes[0].srcFields.ciface.promotion[0].values[0].channel",
                "Главная страница. Растяжка 500 тыс. показов");
        changes.put("$.changes[0].srcFields.ciface.promotion[0].values[0].comment", null);
        changes.put("$.changes[0].srcFields.ciface.promotion[0].values[0].count", "1");
        changes.put("$.changes[0].srcFields.ciface.promotion[0].values[0].count_unit", "нед");
        changes.put("$.changes[0].srcFields.ciface.promotion[0].values[0].id", "1");
        changes.put("$.changes[0].srcFields.ciface.promotion[0].values[0].is_custom_budget_plan", "false");

        ResultActions resultActions =
                mockMvc.perform(MockMvcRequestBuilders.get(URL).param("id", IntegrationPromoUtils.PROMO_ID))
                        .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes.length()").value(2))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.totalCount").value(2))

                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes[0].updatedAt").value("4070908800000"))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes[0].updatedBy").value("superuser"))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes[0].dbUser").value("postgres"))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes[0].requestId").value("requestId"))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes[0].source").value("CATEGORYIFACE"))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes[0].transactionId").value("123"))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes[0].fields").isEmpty())
                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes[0].srcFields.ciface.fields.length()")
                                .value(18))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes[0].srcFields.ciface.promotion.length()")
                                .value(1));

        for (Map.Entry<String, String> entry : changes.entrySet()) {
            resultActions.andExpect(MockMvcResultMatchers.jsonPath(entry.getKey()).value(entry.getValue()));
        }
    }

    @Test
    @DbUnitDataSet(before = "PromoHistoryIntegrationTest.getPromoHistory_constraintsInsert_success.csv")
    void getPromoHistory_constraintsInsert_success() throws Exception {
        Map<String, String> changes = new HashMap<>();

        changes.put("$.changes[0].constraints[0].name", "categories");
        changes.put("$.changes[0].constraints[0].operationType", "INSERT");
        changes.put("$.changes[0].constraints[0].values.length()", "2");
        changes.put("$.changes[0].constraints[0].values[0].category_id", "61");
        changes.put("$.changes[0].constraints[0].values[0].percent", "55");
        changes.put("$.changes[0].constraints[0].values[0].exclude", "false");
        changes.put("$.changes[0].constraints[0].values[1].category_id", "71");
        changes.put("$.changes[0].constraints[0].values[1].percent", "55");
        changes.put("$.changes[0].constraints[0].values[1].exclude", "false");

        changes.put("$.changes[0].constraints[1].name", "regions");
        changes.put("$.changes[0].constraints[1].operationType", "INSERT");
        changes.put("$.changes[0].constraints[1].values[0].region_id", "51");
        changes.put("$.changes[0].constraints[1].values[0].exclude", "false");

        changes.put("$.changes[0].constraints[2].name", "suppliers");
        changes.put("$.changes[0].constraints[2].operationType", "INSERT");
        changes.put("$.changes[0].constraints[2].values[0].supplier_id", "41");
        changes.put("$.changes[0].constraints[2].values[0].exclude", "false");

        changes.put("$.changes[0].constraints[3].name", "warehouses");
        changes.put("$.changes[0].constraints[3].operationType", "INSERT");
        changes.put("$.changes[0].constraints[3].values[0].warehouse_id", "81");
        changes.put("$.changes[0].constraints[3].values[0].exclude", "false");

        changes.put("$.changes[1].constraints[0].name", "vendors");
        changes.put("$.changes[1].constraints[0].operationType", "INSERT");
        changes.put("$.changes[1].constraints[0].values[0].vendor_id", "31");
        changes.put("$.changes[1].constraints[0].values[0].exclude", "false");

        changes.put("$.changes[2].constraints[0].name", "mskus");
        changes.put("$.changes[2].constraints[0].operationType", "INSERT");
        changes.put("$.changes[2].constraints[0].values[0].msku_id", "21");
        changes.put("$.changes[2].constraints[0].values[0].exclude", "false");


        ResultActions resultActions =
                mockMvc.perform(MockMvcRequestBuilders.get(URL).param("id", IntegrationPromoUtils.PROMO_ID))
                        .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes.length()").value(4))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.totalCount").value(4))

                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes[0].updatedAt").isNotEmpty())
                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes[0].updatedBy").isEmpty())
                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes[0].dbUser").value("postgres"))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes[0].requestId").isEmpty())
                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes[0].source").isEmpty())
                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes[0].transactionId").value("1236"))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes[0].constraints.length()").value("4"))

                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes[1].transactionId").value("1235"))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes[1].constraints.length()").value("1"))

                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes[2].transactionId").value("1234"))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes[2].constraints.length()").value("1"))

                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes[3].transactionId").isNotEmpty())
                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes[3].constraints.length()").isEmpty());

        for (Map.Entry<String, String> entry : changes.entrySet()) {
            resultActions.andExpect(MockMvcResultMatchers.jsonPath(entry.getKey()).value(entry.getValue()));
        }
    }

    @Test
    @DbUnitDataSet(before = "PromoHistoryIntegrationTest.getPromoHistory_promoUpdate_success.csv")
    void getPromoHistory_promoUpdate_success() throws Exception {
        Map<String, String> changes = new HashMap<>();

        changes.put("$.changes[0].fields[0].field", "status");
        changes.put("$.changes[0].fields[0].operationType", "UPDATE");
        changes.put("$.changes[0].fields[0].values", "READY");

        ResultActions resultActions =
                mockMvc.perform(MockMvcRequestBuilders.get(URL)
                                .param("id", IntegrationPromoUtils.PROMO_ID)
                                .param("pageSize", "1")
                                .param("pageNumber", "1")
                        )
                        .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes.length()").value(1))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.totalCount").value(2))

                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes[0].updatedAt").isNotEmpty())
                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes[0].updatedBy").value("superuser"))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes[0].dbUser").value("dbuser"))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes[0].requestId").value("requestId"))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes[0].source").value("CATEGORYIFACE"))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes[0].transactionId").value("123"))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes[0].fields.length()").value(1));

        for (Map.Entry<String, String> entry : changes.entrySet()) {
            resultActions.andExpect(MockMvcResultMatchers.jsonPath(entry.getKey()).value(entry.getValue()));
        }
    }

    @Test
    @DbUnitDataSet(before = "PromoHistoryIntegrationTest.getPromoHistory_promocodeUpdate_success.csv")
    void getPromoHistory_promocodeUpdate_success() throws Exception {
        Map<String, String> changes = new HashMap<>();

        changes.put("$.changes[0].fields[0].field", "promocodeAdditionalConditions");
        changes.put("$.changes[0].fields[0].operationType", "UPDATE");
        changes.put("$.changes[0].fields[0].values", "additional_conditions2");

        changes.put("$.changes[0].fields[1].field", "promocodeApplyMultipleTimes");
        changes.put("$.changes[0].fields[1].operationType", "UPDATE");
        changes.put("$.changes[0].fields[1].values", "false");

        changes.put("$.changes[0].fields[2].field", "promocodeValue");
        changes.put("$.changes[0].fields[2].operationType", "UPDATE");
        changes.put("$.changes[0].fields[2].values", "50");

        ResultActions resultActions =
                mockMvc.perform(MockMvcRequestBuilders.get(URL).param("id", IntegrationPromoUtils.PROMO_ID)
                                .param("pageSize", "1")
                                .param("pageNumber", "1"))
                        .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes.length()").value(1))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.totalCount").value(3))

                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes[0].updatedAt").isNotEmpty())
                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes[0].updatedBy").value("superuser"))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes[0].dbUser").value("postgres"))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes[0].requestId").value("requestId"))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes[0].source").value("CATEGORYIFACE"))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes[0].transactionId").value("124"))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes[0].fields.length()").value(3));

        for (Map.Entry<String, String> entry : changes.entrySet()) {
            resultActions.andExpect(MockMvcResultMatchers.jsonPath(entry.getKey()).value(entry.getValue()));
        }
    }

    @Test
    @DbUnitDataSet(before = "PromoHistoryIntegrationTest.getPromoHistory_cheapestAsGiftUpdate_success.csv")
    void getPromoHistory_cheapestAsGiftUpdate_success() throws Exception {
        Map<String, String> changes = new HashMap<>();

        changes.put("$.changes[0].fields[0].field", "cheapestAsGiftCount");
        changes.put("$.changes[0].fields[0].operationType", "UPDATE");
        changes.put("$.changes[0].fields[0].values", "12");

        ResultActions resultActions =
                mockMvc.perform(MockMvcRequestBuilders.get(URL).param("id", IntegrationPromoUtils.PROMO_ID)
                                .param("pageSize", "1")
                                .param("pageNumber", "1"))
                        .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes.length()").value(1))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.totalCount").value(3))

                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes[0].updatedAt").isNotEmpty())
                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes[0].updatedBy").value("superuser"))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes[0].dbUser").value("postgres"))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes[0].requestId").value("requestId"))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes[0].source").value("CATEGORYIFACE"))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes[0].transactionId").value("124"))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes[0].fields.length()").value(1));

        for (Map.Entry<String, String> entry : changes.entrySet()) {
            resultActions.andExpect(MockMvcResultMatchers.jsonPath(entry.getKey()).value(entry.getValue()));
        }
    }

    @Test
    @DbUnitDataSet(before = "PromoHistoryIntegrationTest.getPromoHistory_cifaceUpdate_success.csv")
    void getPromoHistory_cifaceUpdate_success() throws Exception {
        Map<String, String> changes = new HashMap<>();

        changes.put("$.changes[0].srcFields.ciface.fields[0].field", "purpose");
        changes.put("$.changes[0].srcFields.ciface.fields[0].operationType", "UPDATE");
        changes.put("$.changes[0].srcFields.ciface.fields[0].values", "promoPurpose2");

        changes.put("$.changes[0].srcFields.ciface.fields[1].field", "streams");
        changes.put("$.changes[0].srcFields.ciface.fields[1].operationType", "DELETE");
        changes.put("$.changes[0].srcFields.ciface.fields[1].values", "stream1");

        changes.put("$.changes[0].srcFields.ciface.fields[2].field", "streams");
        changes.put("$.changes[0].srcFields.ciface.fields[2].operationType", "INSERT");
        changes.put("$.changes[0].srcFields.ciface.fields[2].values", "stream2");

        changes.put("$.changes[0].srcFields.ciface.promotion[0].operationType", "DELETE");
        changes.put("$.changes[0].srcFields.ciface.promotion[0].values[0].budget_fact", "140000");
        changes.put("$.changes[0].srcFields.ciface.promotion[0].values[0].budget_plan", "150000");
        changes.put("$.changes[0].srcFields.ciface.promotion[0].values[0].category", "Медийное размещение Главная");
        changes.put("$.changes[0].srcFields.ciface.promotion[0].values[0].catteam", "DiY");
        changes.put("$.changes[0].srcFields.ciface.promotion[0].values[0].channel",
                "Главная страница. Растяжка 500 тыс. показов");
        changes.put("$.changes[0].srcFields.ciface.promotion[0].values[0].comment", null);
        changes.put("$.changes[0].srcFields.ciface.promotion[0].values[0].count", "1");
        changes.put("$.changes[0].srcFields.ciface.promotion[0].values[0].count_unit", "нед");
        changes.put("$.changes[0].srcFields.ciface.promotion[0].values[0].id", "1");
        changes.put("$.changes[0].srcFields.ciface.promotion[0].values[0].is_custom_budget_plan", "false");

        changes.put("$.changes[0].srcFields.ciface.promotion[1].operationType", "INSERT");
        changes.put("$.changes[0].srcFields.ciface.promotion[1].values[0].budget_fact", "140000");
        changes.put("$.changes[0].srcFields.ciface.promotion[1].values[0].budget_plan", "150000");
        changes.put("$.changes[0].srcFields.ciface.promotion[1].values[0].category", "Медийное размещение Главная");
        changes.put("$.changes[0].srcFields.ciface.promotion[1].values[0].catteam", "DiY");
        changes.put("$.changes[0].srcFields.ciface.promotion[1].values[0].channel",
                "Главная страница. Растяжка 500 тыс. показов");
        changes.put("$.changes[0].srcFields.ciface.promotion[1].values[0].comment", null);
        changes.put("$.changes[0].srcFields.ciface.promotion[1].values[0].count", "2");
        changes.put("$.changes[0].srcFields.ciface.promotion[1].values[0].count_unit", "нед");
        changes.put("$.changes[0].srcFields.ciface.promotion[1].values[0].id", "1");
        changes.put("$.changes[0].srcFields.ciface.promotion[1].values[0].is_custom_budget_plan", "false");

        ResultActions resultActions =
                mockMvc.perform(MockMvcRequestBuilders.get(URL).param("id", IntegrationPromoUtils.PROMO_ID)
                                .param("pageSize", "1")
                                .param("pageNumber", "1"))
                        .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes.length()").value(1))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.totalCount").value(3))

                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes[0].updatedAt").isNotEmpty())
                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes[0].updatedBy").value("superuser"))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes[0].dbUser").value("postgres"))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes[0].requestId").value("requestId"))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes[0].source").value("CATEGORYIFACE"))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes[0].transactionId").value("124"))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes[0].fields").isEmpty())
                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes[0].srcFields.ciface.fields.length()")
                                .value(3))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes[0].srcFields.ciface.promotion.length()")
                                .value(2));

        for (Map.Entry<String, String> entry : changes.entrySet()) {
            resultActions.andExpect(MockMvcResultMatchers.jsonPath(entry.getKey()).value(entry.getValue()));
        }
    }

    @Test
    @DbUnitDataSet(before = "PromoHistoryIntegrationTest.getPromoHistory_constraintsUpdate_success.csv")
    void getPromoHistory_constraintsUpdate_success() throws Exception {
        Map<String, String> changes = new HashMap<>();

        changes.put("$.changes[0].constraints[0].name", "categories");
        changes.put("$.changes[0].constraints[0].operationType", "DELETE");
        changes.put("$.changes[0].constraints[0].values.length()", "2");
        changes.put("$.changes[0].constraints[0].values[0].category_id", "61");
        changes.put("$.changes[0].constraints[0].values[0].percent", "55");
        changes.put("$.changes[0].constraints[0].values[0].exclude", "false");
        changes.put("$.changes[0].constraints[0].values[1].category_id", "71");
        changes.put("$.changes[0].constraints[0].values[1].percent", "55");
        changes.put("$.changes[0].constraints[0].values[1].exclude", "false");

        changes.put("$.changes[0].constraints[1].name", "regions");
        changes.put("$.changes[0].constraints[1].operationType", "DELETE");
        changes.put("$.changes[0].constraints[1].values[0].region_id", "51");
        changes.put("$.changes[0].constraints[1].values[0].exclude", "false");

        changes.put("$.changes[0].constraints[2].name", "regions");
        changes.put("$.changes[0].constraints[2].operationType", "INSERT");
        changes.put("$.changes[0].constraints[2].values.length()", "2");
        changes.put("$.changes[0].constraints[2].values[0].region_id", "52");
        changes.put("$.changes[0].constraints[2].values[0].exclude", "false");
        changes.put("$.changes[0].constraints[2].values[1].region_id", "53");
        changes.put("$.changes[0].constraints[2].values[1].exclude", "false");

        changes.put("$.changes[0].constraints[3].name", "mskus");
        changes.put("$.changes[0].constraints[3].operationType", "DELETE");
        changes.put("$.changes[0].constraints[3].values[0].msku_id", "21");
        changes.put("$.changes[0].constraints[3].values[0].exclude", "false");

        changes.put("$.changes[0].constraints[4].name", "suppliers");
        changes.put("$.changes[0].constraints[4].operationType", "DELETE");
        changes.put("$.changes[0].constraints[4].values[0].supplier_id", "41");
        changes.put("$.changes[0].constraints[4].values[0].exclude", "false");

        changes.put("$.changes[0].constraints[5].name", "suppliers");
        changes.put("$.changes[0].constraints[5].operationType", "INSERT");
        changes.put("$.changes[0].constraints[5].values[0].supplier_id", "41");
        changes.put("$.changes[0].constraints[5].values[0].exclude", "true");

        changes.put("$.changes[0].constraints[6].name", "vendors");
        changes.put("$.changes[0].constraints[6].operationType", "DELETE");
        changes.put("$.changes[0].constraints[6].values[0].vendor_id", "31");
        changes.put("$.changes[0].constraints[6].values[0].exclude", "false");

        changes.put("$.changes[0].constraints[7].name", "warehouses");
        changes.put("$.changes[0].constraints[7].operationType", "DELETE");
        changes.put("$.changes[0].constraints[7].values[0].warehouse_id", "81");
        changes.put("$.changes[0].constraints[7].values[0].exclude", "false");

        ResultActions resultActions =
                mockMvc.perform(MockMvcRequestBuilders.get(URL).param("id", IntegrationPromoUtils.PROMO_ID)
                                .param("pageSize", "1")
                                .param("pageNumber", "1"))
                        .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes.length()").value(1))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.totalCount").value(5))

                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes[0].updatedAt").isNotEmpty())
                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes[0].updatedBy").value("superuser"))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes[0].dbUser").value("postgres"))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes[0].requestId").value("requestId"))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes[0].source").value("CATEGORYIFACE"))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes[0].transactionId").value("1237"))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes[0].fields").isEmpty())
                        .andExpect(MockMvcResultMatchers.jsonPath("$.changes[0].constraints.length()").value(8));

        for (Map.Entry<String, String> entry : changes.entrySet()) {
            resultActions.andExpect(MockMvcResultMatchers.jsonPath(entry.getKey()).value(entry.getValue()));
        }
    }
}
