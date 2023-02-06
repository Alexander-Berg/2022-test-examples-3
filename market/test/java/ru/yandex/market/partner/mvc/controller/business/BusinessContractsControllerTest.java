package ru.yandex.market.partner.mvc.controller.business;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import com.google.gson.JsonElement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.mbi.api.billing.client.MbiBillingClient;
import ru.yandex.market.mbi.api.billing.client.model.CurrentAndNextMonthPayoutFrequencyDTO;
import ru.yandex.market.mbi.api.billing.client.model.PayoutFrequencyDTO;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@DbUnitDataSet(before = "BusinessContractsControllerTest.before.csv")
class BusinessContractsControllerTest extends FunctionalTest {

    @Autowired
    private MbiBillingClient mbiBillingClient;

    @Autowired
    private Clock clock;

    @BeforeEach
    void init() {
        when(clock.instant()).thenReturn(
                Clock.fixed(Instant.parse("2022-03-25T00:00:00Z"), ZoneId.of("UTC")).instant()
        );
        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));
    }

    @Test
    void getContractForManyPartners() {
        when(mbiBillingClient.getCurrentAndNextMonthPayoutFrequencies(any()))
                .thenReturn(List.of(
                        createFrequencyDTO(500L, PayoutFrequencyDTO.WEEKLY, PayoutFrequencyDTO.DAILY, false)
                ));

        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl + "businesses/100/contracts-info");

        String expected = "{\n" +
                "  \"contractsInfo\": [\n" +
                "    {\n" +
                "      \"income\": {\n" +
                "        \"contractId\": 500,\n" +
                "        \"contractExternalId\": \"incomeContract5\",\n" +
                "        \"award\": {\n" +
                "          \"accountId\": \"account500\",\n" +
                "          \"status\": \"DEBT\",\n" +
                "          \"actDate\": \"2019-01-31\",\n" +
                "          \"amount\": 1900\n" +
                "        },\n" +
                "        \"fulfillment\": {\n" +
                "          \"accountId\": \"account501\",\n" +
                "          \"status\": \"DEBT\",\n" +
                "          \"actDate\": \"2019-01-31\",\n" +
                "          \"amount\": 3800\n" +
                "        },\n" +
                "        \"willPayAmount\": 500.00,\n" +
                "        \"remainToHoldAmount\": 3745.00,\n" +
                "        \"paidThisMonth\": 200" +
                "      },\n" +
                "      \"outcome\": {\n" +
                "        \"contractId\": 555,\n" +
                "        \"contractExternalId\": \"outcomeContract5\",\n" +
                "        \"status\": \"OBSOLETE\",\n" +
                "        \"deadline\": \"2019-02-15\",\n" +
                "        \"willPayAmount\": 0,\n" +
                "        \"paidThisMonth\": 0" +
                "      },\n" +
                "      \"contractPartners\": [\n" +
                "        {\n" +
                "          \"partnerId\": 1,\n" +
                "          \"name\": \"supplier1\",\n" +
                "          \"campaignId\": 10,\n" +
                "          \"isSupplier\": true\n" +
                "        },\n" +
                "        {\n" +
                "          \"partnerId\": 2,\n" +
                "          \"name\": \"supplier2\",\n" +
                "          \"campaignId\": 20,\n" +
                "          \"isSupplier\": true\n" +
                "        },\n" +
                "        {\n" +
                "          \"partnerId\": 3,\n" +
                "          \"name\": \"supplier3\",\n" +
                "          \"campaignId\": 30,\n" +
                "          \"isSupplier\": true\n" +
                "        },\n" +
                "        {\n" +
                "          \"partnerId\": 4,\n" +
                "          \"name\": \"supplier4\",\n" +
                "          \"campaignId\": 40,\n" +
                "          \"isSupplier\": true\n" +
                "        },\n" +
                "        {\n" +
                "          \"partnerId\": 5,\n" +
                "          \"name\": \"supplier5\",\n" +
                "          \"campaignId\": 50,\n" +
                "          \"isSupplier\": true\n" +
                "        }\n" +
                "      ],\n" +
                "      \"dateOfNextPayout\": \"2022-03-28T00:00:00Z\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";
        JsonTestUtil.assertEquals(response, expected);
    }

    @Test
    void getContractForPartnersInOneContractByIncomeId() {
        when(clock.instant()).thenReturn(
                Clock.fixed(Instant.parse("2022-03-16T00:00:00Z"), ZoneId.of("UTC")).instant()
        );

        when(mbiBillingClient.getCurrentAndNextMonthPayoutFrequencies(any()))
                .thenReturn(List.of(
                        createFrequencyDTO(600L, PayoutFrequencyDTO.DAILY, PayoutFrequencyDTO.DAILY, false),
                        createFrequencyDTO(700L, PayoutFrequencyDTO.WEEKLY, PayoutFrequencyDTO.WEEKLY, false),
                        createFrequencyDTO(800L, PayoutFrequencyDTO.WEEKLY, PayoutFrequencyDTO.BIWEEKLY, false),
                        createFrequencyDTO(900L, PayoutFrequencyDTO.BIWEEKLY, PayoutFrequencyDTO.WEEKLY, false),
                        createFrequencyDTO(1000L, PayoutFrequencyDTO.BIWEEKLY, PayoutFrequencyDTO.DAILY, false),
                        createFrequencyDTO(1100L, PayoutFrequencyDTO.DAILY, PayoutFrequencyDTO.DAILY, false)
                ));

        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl + "businesses/101/contracts-info?contract_id=600");
        JsonElement expected = JsonTestUtil.parseJson(this.getClass(), "contract/getContractForPartnersInOneContractByIncomeId.json");

        JsonTestUtil.assertEquals(response, expected);
    }

    @Test
    void getContractForPartnersInOneContractByOutcomeId() {
        when(clock.instant()).thenReturn(
                Clock.fixed(Instant.parse("2022-03-18T00:00:00Z"), ZoneId.of("UTC")).instant()
        );

        when(mbiBillingClient.getCurrentAndNextMonthPayoutFrequencies(any()))
                .thenReturn(List.of(
                        createFrequencyDTO(600L, PayoutFrequencyDTO.DAILY, PayoutFrequencyDTO.DAILY, false),
                        createFrequencyDTO(700L, PayoutFrequencyDTO.WEEKLY, PayoutFrequencyDTO.WEEKLY, false),
                        createFrequencyDTO(800L, PayoutFrequencyDTO.WEEKLY, PayoutFrequencyDTO.BIWEEKLY, false),
                        createFrequencyDTO(900L, PayoutFrequencyDTO.BIWEEKLY, PayoutFrequencyDTO.WEEKLY, false),
                        createFrequencyDTO(1000L, PayoutFrequencyDTO.BIWEEKLY, PayoutFrequencyDTO.DAILY, false),
                        createFrequencyDTO(1100L, PayoutFrequencyDTO.DAILY, PayoutFrequencyDTO.DAILY, false)
                ));

        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl + "businesses/101/contracts-info?contract_id=666");
        JsonElement expected = JsonTestUtil.parseJson(this.getClass(), "contract/getContractForPartnersInOneContractByOutcomeId.json");

        JsonTestUtil.assertEquals(response, expected);
    }

    @Test
    void testBillingIsDead() {
        when(mbiBillingClient.getCurrentAndNextMonthPayoutFrequencies(any()))
                .thenReturn(null);

        ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl + "businesses/101/contracts-info?contract_id=666");
    }

        @Test
    void getContractsDistinctForEachPartner() {
        when(mbiBillingClient.getCurrentAndNextMonthPayoutFrequencies(any()))
                .thenReturn(List.of(
                        createFrequencyDTO(600L, PayoutFrequencyDTO.DAILY, PayoutFrequencyDTO.DAILY, false),
                        createFrequencyDTO(700L, PayoutFrequencyDTO.WEEKLY, PayoutFrequencyDTO.WEEKLY, null),
                        createFrequencyDTO(null, PayoutFrequencyDTO.BIWEEKLY, PayoutFrequencyDTO.WEEKLY, true),
                        createFrequencyDTO(1000L, null, null, false),
                        createFrequencyDTO(1100L, PayoutFrequencyDTO.MONTHLY, null, false)
                ));

        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl + "businesses/101/contracts-info");
        JsonElement expected = JsonTestUtil.parseJson(this.getClass(), "contract/getContractsDistinctForEachPartner.json");

        JsonTestUtil.assertEquals(response, expected);
    }

    @Test
    @DisplayName("Получение списка партнеров на балансовых договорах")
    void getPartnerRelatedContracts() {
        String expected = "[\n" +
                "    {\n" +
                "        \"externalId\": \"156890/21\",\n" +
                "        \"orgName\": \"ООО biz3\",\n" +
                "        \"inn\": \"987567123\",\n" +
                "        \"contractPartnersInfo\": [\n" +
                "            {\n" +
                "                \"partnerId\": 14,\n" +
                "                \"name\": \"supplier_114\",\n" +
                "                \"campaignId\": 114,\n" +
                "                \"isSupplier\": true\n" +
                "            },\n" +
                "            {\n" +
                "                \"partnerId\": 15,\n" +
                "                \"name\": \"supplier_115\",\n" +
                "                \"campaignId\": 115,\n" +
                "                \"isSupplier\": true\n" +
                "            },\n" +
                "            {\n" +
                "                \"partnerId\": 16,\n" +
                "                \"name\": \"shopTestDSBS\",\n" +
                "                \"campaignId\": 116,\n" +
                "                \"isSupplier\": false\n" +
                "            }\n" +
                "        ]\n" +
                "    },\n" +
                "    {\n" +
                "        \"externalId\": \"ОФ-896784\",\n" +
                "        \"orgName\": \"ООО biz3\",\n" +
                "        \"inn\": \"987567123\",\n" +
                "        \"contractPartnersInfo\": [\n" +
                "            {\n" +
                "                \"partnerId\": 14,\n" +
                "                \"name\": \"supplier_114\",\n" +
                "                \"campaignId\": 114,\n" +
                "                \"isSupplier\": true\n" +
                "            },\n" +
                "            {\n" +
                "                \"partnerId\": 15,\n" +
                "                \"name\": \"supplier_115\",\n" +
                "                \"campaignId\": 115,\n" +
                "                \"isSupplier\": true\n" +
                "            },\n" +
                "            {\n" +
                "                \"partnerId\": 16,\n" +
                "                \"name\": \"shopTestDSBS\",\n" +
                "                \"campaignId\": 116,\n" +
                "                \"isSupplier\": false\n" +
                "            }\n" +
                "        ]\n" +
                "    }\n" +
                "]";
        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl +
                "/businesses/12445/contracts-related-partners?contract=156890/21,ОФ-896784");
        JsonTestUtil.assertEquals(response, expected);
    }

    @Test
    @DisplayName("Получение списка партнеров на балансовых договорах - некорректные номера договоров")
    void getPartnerRelatedContractsIncorrectContracts() {
        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl + "/businesses/12445/contracts-related" +
                "-partners?contract=156891/20,ОФ-11111111");
        JsonTestUtil.assertEquals(response, "[]");
    }

    private CurrentAndNextMonthPayoutFrequencyDTO createFrequencyDTO(
            Long contractId,
            PayoutFrequencyDTO currentFrequency,
            PayoutFrequencyDTO nextFrequency,
            Boolean isDefaultCurrentFrequency
    ) {
        CurrentAndNextMonthPayoutFrequencyDTO dto = new CurrentAndNextMonthPayoutFrequencyDTO();
        dto.setContractId(contractId);
        dto.setCurrentMonthFrequency(currentFrequency);
        dto.setNextMonthFrequency(nextFrequency);
        dto.isDefaultCurrentMonthFrequency(isDefaultCurrentFrequency);

        return dto;
    }
}
