package ru.yandex.market.mbi.api.controller.axapta;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.mbi.api.config.FunctionalTest;
import ru.yandex.market.mbi.util.MbiAsserts;

class AxaptaClientsControllerTest extends FunctionalTest {

    @Test
    @DbUnitDataSet(before = "AxaptaClientsControllerTest.before.csv")
    void testGetClients() {
        ResponseEntity<String> responseEntity = FunctionalTestHelper.get(
                "http://localhost:" + port + "/v1/axapta/revenue/clients",
                String.class
        );

        MbiAsserts.assertJsonEquals(
                "[\n" +
                        "  {\n" +
                        "    \"client_id\": 101,\n" +
                        "    \"persons\": [\n" +
                        "      {\n" +
                        "        \"person_id\": 1001,\n" +
                        "        \"inn\": \"12345\",\n" +
                        "        \"contracts\": [\n" +
                        "          {\n" +
                        "            \"contract_id\": 10002,\n" +
                        "            \"services\": [\n" +
                        "              701\n" +
                        "            ],\n" +
                        "          },\n" +
                        "          {\n" +
                        "            \"contract_id\": 10001,\n" +
                        "            \"services\": [\n" +
                        "              701,\n" +
                        "              702\n" +
                        "            ],\n" +
                        "          },\n" +
                        "          {\n" +
                        "            \"contract_id\": 10003,\n" +
                        "            \"services\": [\n" +
                        "              701\n" +
                        "            ],\n" +
                        "          }\n" +
                        "        ]\n" +
                        "      },\n" +
                        "      {\n" +
                        "        \"person_id\": 1003,\n" +
                        "        \"contracts\": []\n" +
                        "      },\n" +
                        "      {\n" +
                        "        \"person_id\": 1002,\n" +
                        "        \"inn\": \"12346\",\n" +
                        "        \"contracts\": [\n" +
                        "          {\n" +
                        "            \"contract_id\": 10004,\n" +
                        "            \"services\": [],\n" +
                        "          }\n" +
                        "        ]\n" +
                        "      }\n" +
                        "    ]\n" +
                        "  },\n" +
                        "  {\n" +
                        "    \"client_id\": 102,\n" +
                        "    \"persons\": [\n" +
                        "      {\n" +
                        "        \"person_id\": 1004,\n" +
                        "        \"inn\": \"12347\",\n" +
                        "        \"contracts\": []\n" +
                        "      }\n" +
                        "    ]\n" +
                        "  },\n" +
                        "  {\n" +
                        "    \"client_id\": 103,\n" +
                        "    \"persons\": []\n" +
                        "  }\n" +
                        "]",
                responseEntity.getBody()
        );
    }

    @Test
    void testEmptyResponse() {
        ResponseEntity<String> responseEntity = FunctionalTestHelper.get(
                "http://localhost:" + port + "/v1/axapta/revenue/clients",
                String.class
        );

        MbiAsserts.assertJsonEquals(
                "[]",
                responseEntity.getBody()
        );
    }
}
