package ru.yandex.market.partner.mvc.controller.supplier.contract;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

/**
 * Тесты для {@link SupplierContractController}
 */
@DbUnitDataSet(before = "SupplierContractControllerFunctionalTest.before.csv")
class SupplierContractControllerFunctionalTest extends FunctionalTest {

    @Test
    void testClosed() {
        ResponseEntity<String> response = getContractsInfo(10);
        String expected = //language=json
                "{\n" +
                        "  \"email\": \"sup1@ya.ru\",\n" +
                        "  \"income\": {\n" +
                        "    \"contractId\": \"incomeContract1\",\n" +
                        "    \"award\": {\n" +
                        "      \"accountId\": \"account100\",\n" +
                        "      \"status\": \"CLOSED\",\n" +
                        "      \"actDate\": \"2019-12-31\",\n" +
                        "      \"amount\": 0\n" +
                        "    },\n" +
                        "    \"fulfillment\": {\n" +
                        "      \"accountId\": \"account101\",\n" +
                        "      \"status\": \"CLOSED\",\n" +
                        "      \"actDate\": \"2019-12-31\",\n" +
                        "      \"amount\": 0\n" +
                        "    },\n" +
                        "    \"remainToHeldAmount\": 0,\n" +
                        "    \"heldPaymentsAmount\": 0\n" +
                        "  },\n" +
                        "  \"outcome\": {\n" +
                        "    \"contractId\": \"outcomeContract1\",\n" +
                        "    \"status\": \"CLOSED\",\n" +
                        "    \"deadline\": \"2020-02-15\"\n" +
                        "  }\n" +
                        "}";
        JsonTestUtil.assertEquals(response, expected);
    }

    @Test
    void testClosedAfterObsolete() {
        ResponseEntity<String> response = getContractsInfo(100);
        String expected = //language=json
                "{\n" +
                        "  \"email\": \"sup10@ya.ru\",\n" +
                        "  \"income\": {\n" +
                        "    \"remainToHeldAmount\": 0,\n" +
                        "    \"heldPaymentsAmount\": 0\n" +
                        "  },\n" +
                        "  \"outcome\": {\n" +
                        "    \"contractId\": \"outcomeContract10\",\n" +
                        "    \"status\": \"CLOSED\",\n" +
                        "    \"deadline\": \"2017-03-15\"\n" +
                        "  }\n" +
                        "}";
        JsonTestUtil.assertEquals(response, expected);
    }

    @Test
    void testPending() {
        ResponseEntity<String> response = getContractsInfo(20);
        String expected = //language=json
                "{\n" +
                        "  \"email\": \"sup2@ya.ru\",\n" +
                        "  \"income\": {\n" +
                        "    \"contractId\": \"incomeContract2\",\n" +
                        "    \"award\": {\n" +
                        "      \"accountId\": \"account200\",\n" +
                        "      \"status\": \"PENDING\",\n" +
                        "      \"actDate\": \"2020-01-31\",\n" +
                        "      \"amount\": 100.00\n" +
                        "    },\n" +
                        "    \"fulfillment\": {\n" +
                        "      \"accountId\": \"account201\",\n" +
                        "      \"status\": \"PENDING\",\n" +
                        "      \"actDate\": \"2020-01-31\",\n" +
                        "      \"amount\": 200.00\n" +
                        "    },\n" +
                        "    \"remainToHeldAmount\": 0,\n" +
                        "    \"heldPaymentsAmount\": 0\n" +
                        "  },\n" +
                        "  \"outcome\": {\n" +
                        "    \"contractId\": \"outcomeContract2\",\n" +
                        "    \"status\": \"PENDING\",\n" +
                        "    \"deadline\": \"2020-02-15\"\n" +
                        "  }\n" +
                        "}";
        JsonTestUtil.assertEquals(response, expected);
    }

    @Test
    void testOverpaid() {
        ResponseEntity<String> response = getContractsInfo(30);
        String expected = //language=json
                "{\n" +
                        "  \"email\": \"sup3@ya.ru\",\n" +
                        "  \"income\": {\n" +
                        "    \"contractId\": \"incomeContract3\",\n" +
                        "    \"award\": {\n" +
                        "      \"accountId\": \"account300\",\n" +
                        "      \"status\": \"OVERPAID\",\n" +
                        "      \"actDate\": \"2020-01-31\",\n" +
                        "      \"amount\": 200.00\n" +
                        "    },\n" +
                        "    \"fulfillment\": {\n" +
                        "      \"accountId\": \"account301\",\n" +
                        "      \"status\": \"OVERPAID\",\n" +
                        "      \"actDate\": \"2020-01-31\",\n" +
                        "      \"amount\": 100.00\n" +
                        "    },\n" +
                        "    \"remainToHeldAmount\": 0,\n" +
                        "    \"heldPaymentsAmount\": 0\n" +
                        "  },\n" +
                        "  \"outcome\": {\n" +
                        "    \"contractId\": \"outcomeContract3\",\n" +
                        "    \"status\": \"CLOSED\",\n" +
                        "    \"deadline\": \"2020-02-15\"\n" +
                        "  }\n" +
                        "}";
        JsonTestUtil.assertEquals(response, expected);
    }

    @Test
    @DbUnitDataSet(before = "SupplierContractControllerFunctionalTestEnableFlag.before.csv")
    void testDebtBySumWithFlag() {
        testDebtBySum();
    }

    @Test
    void testDebtBySumWithoutFlag() {
        testDebtBySum();
    }
    void testDebtBySum() {
        ResponseEntity<String> response = getContractsInfo(40);
        String expected = //language=json
                "{\n" +
                        "  \"email\": \"sup4@ya.ru\",\n" +
                        "  \"income\": {\n" +
                        "    \"contractId\": \"incomeContract4\",\n" +
                        "    \"award\": {\n" +
                        "      \"accountId\": \"account400\",\n" +
                        "      \"status\": \"DEBT\",\n" +
                        "      \"actDate\": \"2020-01-31\",\n" +
                        "      \"amount\": 300.00\n" +
                        "    },\n" +
                        "    \"fulfillment\": {\n" +
                        "      \"accountId\": \"account401\",\n" +
                        "      \"status\": \"DEBT\",\n" +
                        "      \"actDate\": \"2020-01-31\",\n" +
                        "      \"amount\": 450.00\n" +
                        "    },\n" +
                        "    \"remainToHeldAmount\": 250.00,\n" +
                        "    \"heldPaymentsAmount\": 200,\n" +
                        "    \"heldStartingDate\": \"2022-04-20\"\n" +
                        "  },\n" +
                        "  \"outcome\": {\n" +
                        "    \"contractId\": \"outcomeContract4\",\n" +
                        "    \"status\": \"OBSOLETE\",\n" +
                        "    \"deadline\": \"2020-01-15\"\n" +
                        "  }\n" +
                        "}";
        JsonTestUtil.assertEquals(response, expected);
    }

    @Test
    void testDebtByDateWithoutFlag() {
        testDebtByDate();
    }

    @Test
    @DbUnitDataSet(before = "SupplierContractControllerFunctionalTestEnableFlag.before.csv")
    void testDebtByDateWithFlag() {
        testDebtByDate();
    }

    void testDebtByDate() {
        ResponseEntity<String> response = getContractsInfo(50);
        String expected = //language=json
                "{\n" +
                        "  \"email\": \"sup5@ya.ru\",\n" +
                        "  \"income\": {\n" +
                        "    \"contractId\": \"incomeContract5\",\n" +
                        "    \"award\": {\n" +
                        "      \"accountId\": \"account500\",\n" +
                        "      \"status\": \"DEBT\",\n" +
                        "      \"actDate\": \"2019-01-31\",\n" +
                        "      \"amount\": 100.00\n" +
                        "    },\n" +
                        "    \"fulfillment\": {\n" +
                        "      \"accountId\": \"account501\",\n" +
                        "      \"status\": \"DEBT\",\n" +
                        "      \"actDate\": \"2019-01-31\",\n" +
                        "      \"amount\": 200.00\n" +
                        "    },\n" +
                        "    \"remainToHeldAmount\": 200.00,\n" +
                        "    \"heldPaymentsAmount\": 0\n" +
                        "  },\n" +
                        "  \"outcome\": {\n" +
                        "    \"contractId\": \"outcomeContract5\",\n" +
                        "    \"status\": \"OBSOLETE\",\n" +
                        "    \"deadline\": \"2019-02-15\"\n" +
                        "  }\n" +
                        "}";
        JsonTestUtil.assertEquals(response, expected);
    }

    @Test
    void testNoActsAtAll() {
        ResponseEntity<String> response = getContractsInfo(60);
        String expected = //language=json
                "{\n" +
                        "  \"email\": \"sup6@ya.ru\",\n" +
                        "  \"income\": {\n" +
                        "    \"contractId\": \"incomeContract6\",\n" +
                        "    \"remainToHeldAmount\": 0,\n" +
                        "    \"heldPaymentsAmount\": 0\n" +
                        "  },\n" +
                        "  \"outcome\": {\n" +
                        "    \"contractId\": \"outcomeContract6\"\n" +
                        "  }\n" +
                        "}";
        JsonTestUtil.assertEquals(response, expected);
    }

    @Test
    void testNoOutcomeAct() {
        ResponseEntity<String> response = getContractsInfo(70);
        String expected = //language=json
                "{\n" +
                        "  \"email\": \"sup7@ya.ru\",\n" +
                        "  \"income\": {\n" +
                        "    \"contractId\": \"incomeContract7\",\n" +
                        "    \"fulfillment\": {\n" +
                        "      \"accountId\": \"account701\",\n" +
                        "      \"status\": \"PENDING\",\n" +
                        "      \"actDate\": \"2020-01-31\",\n" +
                        "      \"amount\": 100.00\n" +
                        "    },\n" +
                        "    \"remainToHeldAmount\": 100.00,\n" +
                        "    \"heldPaymentsAmount\": 0\n" +
                        "  },\n" +
                        "  \"outcome\": {\n" +
                        "    \"contractId\": \"outcomeContract7\"\n" +
                        "  }\n" +
                        "}";
        JsonTestUtil.assertEquals(response, expected);
    }

    @Test
    void testOldObsoleteAct() {
        ResponseEntity<String> response = getContractsInfo(80);
        String expected = //language=json
                "{\n" +
                        "  \"email\": \"sup8@ya.ru\",\n" +
                        "  \"income\": {\n" +
                        "    \"contractId\": \"incomeContract8\",\n" +
                        "    \"fulfillment\": {\n" +
                        "      \"accountId\": \"account801\",\n" +
                        "      \"status\": \"PENDING\",\n" +
                        "      \"actDate\": \"2020-01-31\",\n" +
                        "      \"amount\": 100.00\n" +
                        "    },\n" +
                        "    \"remainToHeldAmount\": 0,\n" +
                        "    \"heldPaymentsAmount\": 0\n" +
                        "  },\n" +
                        "  \"outcome\": {\n" +
                        "    \"contractId\": \"outcomeContract8\",\n" +
                        "    \"status\": \"OBSOLETE\",\n" +
                        "    \"deadline\": \"2019-02-15\"\n" +
                        "  }\n" +
                        "}";
        JsonTestUtil.assertEquals(response, expected);
    }

    @Test
    void testOldPendingAct() {
        ResponseEntity<String> response = getContractsInfo(90);
        String expected = //language=json
                "{\n" +
                        "  \"email\": \"sup9@ya.ru\",\n" +
                        "  \"income\": {\n" +
                        "    \"contractId\": \"incomeContract9\",\n" +
                        "    \"fulfillment\": {\n" +
                        "      \"accountId\": \"account901\",\n" +
                        "      \"status\": \"PENDING\",\n" +
                        "      \"actDate\": \"2020-01-31\",\n" +
                        "      \"amount\": 100.00\n" +
                        "    },\n" +
                        "    \"remainToHeldAmount\": 0,\n" +
                        "    \"heldPaymentsAmount\": 0\n" +
                        "  },\n" +
                        "  \"outcome\": {\n" +
                        "    \"contractId\": \"outcomeContract9\",\n" +
                        "    \"status\": \"PENDING\",\n" +
                        "    \"deadline\": \"2020-02-15\"\n" +
                        "  }\n" +
                        "}";
        JsonTestUtil.assertEquals(response, expected);
    }

    private ResponseEntity<String> getContractsInfo(long campaignId) {
        return FunctionalTestHelper.get(baseUrl + "/suppliers/" + campaignId + "/contracts-info");
    }
}
