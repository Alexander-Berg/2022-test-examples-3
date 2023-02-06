package ru.yandex.market.logshatter.parser.delivery;

import java.util.Date;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;

/**
 * @author Aleksandr Kondrashin aezhko@yandex-team.ru
 * 03.03.16.
 */
public class DeliveryApplicationLogParserTest {
    static LogParserChecker checker = new LogParserChecker(new DeliveryApplicationLogParser());

    @Test
    @SuppressWarnings("MethodLength")
    public void testParser() throws Exception {
        checker.check(
            "[2016-02-25 00:00:08] DS.Command:INFO: Обновление статусов для СД «DPD» запущено. " +
                "{\"deliveryUniqueName\":\"DPD\"} {\"executionTime\":\"00:00:00\",\"resourceId\":\"0\"," +
                "\"userId\":\"0\",\"hostname\":\"deliback01h\"," +
                "\"applicationId\":\"548000138abaf54ccd8e6fdeb42a722e1a97b05a07e027912e0107e67a7a3d0d\"}",
            new Date(1456347608000L),
            new String[]{"DS", "Command"},
            "INFO",
            "Обновление статусов для СД «DPD» запущено.",
            //Context
            "", //getAttributes(),
            "", //getDeliveryId(),
            "DPD", //getDeliveryUniqueName(),
            "", //getEmail(),
            "", //getException(),
            "", //getGroup(),
            "", //getLogin(),
            "", //getMergeVars(),
            "", //getOptions(),
            "", //getOrderId(),
            "", //getPickupPointCode(),
            "{\"deliveryUniqueName\":\"DPD\"}", //getContext(),
            //External data
            "548000138abaf54ccd8e6fdeb42a722e1a97b05a07e027912e0107e67a7a3d0d", //getApplicationId()
            "00:00:00", //getExecutionTime()
            "deliback01h", //getHostname()
            "", //getRequest()

            "",  // getMethodName(),
            "",  // getHash(),
            "",  // getToken(),
            new String[]{},  // getSenderIdYandexArray(),
            new String[]{},  // getWarehouseIdYandexArray(),
            new String[]{},  // getWarehouseIdDeliveryArray(),
            new String[]{},  // getShipmentIdYandexArray(),
            new String[]{},  // getShipmentIdDeliveryArray(),
            new String[]{},  // getOrderIdCleanArray(),
            new String[]{},  // getOrderIdYandexArray(),
            new String[]{},  // getOrderIdDeliveryArray(),
            new String[]{},  // getOrderIdExternalArray(),
            new String[]{},  // getIntakeIdYandexArray(),
            new String[]{},  // getRegisterIdYandexArray(),
            new String[]{},  // getSelfExportIdYandexArray(),
            new String[]{},  // getSelfExportIdDeliveryArray(),

            "", //getResponse()

            "",  // getMethodName(),
            "",  // getHash(),
            0,  // getIsError(),
            new String[]{},  // getIntakeIdYandexArray(),
            new String[]{},  // getOrderIdCleanArray(),
            new String[]{},  // getOrderIdYandexArray(),
            new String[]{},  // getOrderIdDeliveryArray(),
            new String[]{},  // getOrderIdExternalArray(),
            new String[]{},  // getRegisterIdYandexArray(),
            new String[]{},  // getRegisterIdDeliveryArray(),
            new String[]{},  // getSelfExportIdYandexArray(),
            new String[]{},  // getSelfExportIdDeliveryArray(),
            new String[]{},  // getDeliveryNumArray(),

            "0", //getResourceId()
            "", //getUrl
            "0", //getUserId
            "{\"executionTime\":\"00:00:00\",\"resourceId\":\"0\",\"userId\":\"0\",\"hostname\":\"deliback01h\"," +
                "\"applicationId\":\"548000138abaf54ccd8e6fdeb42a722e1a97b05a07e027912e0107e67a7a3d0d\"}"
            //externalDataFull
        );

        checker.check(
            "[2016-02-25 00:00:11] Zendesk:INFO: deliback01h Guzzle/5.3.0 curl/7.35.0 PHP/5.6.13-1+yandex1 - " +
                "[2016-02-24T21:00:11+00:00] \"GET /api/v2/search.json?query=type%3Aticket%20external_id%3A54 /\" 200" +
                " 76 [] {\"executionTime\":\"00:00:03\",\"request\":\"[object] (GuzzleHttp\\\\Message\\\\Request: GET" +
                " /api/v2/search.json?query=type%3Aticket%20external_id%3A54 HTTP/1.1\\r\\nHost: yadostavka.zendesk" +
                ".com\\r\\nUser-Agent: Guzzle/5.3.0 curl/7.35.0 PHP/5.6.13-1+yandex1\\r\\nAuthorization: Basic " +
                "bWRtc2hpcEB5YW5kZXgtdGVhbS5ydS90b2tlbjpWSFJTZDhQU0dkeXRmWDgwRjROMjVobFplM2VPZkJuWnR4SGlDNDZD\\r\\n" +
                "\\r\\n)\",\"response\":\"[object] (GuzzleHttp\\\\Message\\\\Response: HTTP/1.1 200 OK\\r\\nServer: " +
                "nginx\\r\\nDate: Wed, 24 Feb 2016 21:00:11 GMT\\r\\nContent-Type: application/json; " +
                "charset=UTF-8\\r\\nContent-Length: 76\\r\\nConnection: keep-alive\\r\\nX-Zendesk-API-Version: " +
                "v2\\r\\nX-Zendesk-Application-Version: v3.68.8\\r\\nX-Frame-Options: " +
                "SAMEORIGIN\\r\\nStrict-Transport-Security: max-age=0;\\r\\nX-UA-Compatible: IE=Edge," +
                "chrome=1\\r\\nETag: W/\\\"788813e9d5f7a30ad995b89094cc9745\\\"\\r\\nCache-Control: must-revalidate, " +
                "private, max-age=0\\r\\nX-Zendesk-Origin-Server: app18.pod5.iad1.zdsys.com\\r\\nX-Request-Id: " +
                "6ace3d5b-8ce7-4d6c-cd46-b8ca3a6bdb18\\r\\nX-Runtime: 0.172787\\r\\nX-Rack-Cache: " +
                "miss\\r\\nX-Zendesk-Request-Id: 98c17384c65da67d0f81\\r\\nX-Content-Type-Options: " +
                "nosniff\\r\\n\\r\\n{\\\"results\\\":[],\\\"facets\\\":null,\\\"next_page\\\":null," +
                "\\\"previous_page\\\":null,\\\"count\\\":0})\",\"url\":\"https://yadostavka.zendesk" +
                ".com/api/v2/search.json?query=type%3Aticket%20external_id%3A54\",\"resourceId\":\"0\"," +
                "\"userId\":\"0\",\"hostname\":\"deliback01h\"," +
                "\"applicationId\":\"2634ede374d609cdfa15b9907ac818ce5e0fe6270042aa08e47416998b0a5fb9\"}",
            new Date(1456347611000L),
            new String[]{"Zendesk"},
            "INFO",
            "deliback01h Guzzle/5.3.0 curl/7.35.0 PHP/5.6.13-1+yandex1 - [2016-02-24T21:00:11+00:00] \"GET " +
                "/api/v2/search.json?query=type%3Aticket%20external_id%3A54 /\" 200 76",
            //Context
            "", //getAttributes(),
            "", //getDeliveryId(),
            "", //getDeliveryUniqueName(),
            "", //getEmail(),
            "", //getException(),
            "", //getGroup(),
            "", //getLogin(),
            "", //getMergeVars(),
            "", //getOptions(),
            "", //getOrderId(),
            "", //getPickupPointCode(),
            "{}", //getContext(),
            //External data
            "2634ede374d609cdfa15b9907ac818ce5e0fe6270042aa08e47416998b0a5fb9", //getApplicationId()
            "00:00:03", //getExecutionTime()
            "deliback01h", //getHostname()
            "[object] (GuzzleHttp\\Message\\Request: GET /api/v2/search.json?query=type%3Aticket%20external_id%3A54 " +
                "HTTP/1.1\r\n" +
                "Host: yadostavka.zendesk.com\r\n" +
                "User-Agent: Guzzle/5.3.0 curl/7.35.0 PHP/5.6.13-1+yandex1\r\n" +
                "Authorization: Basic " +
                "bWRtc2hpcEB5YW5kZXgtdGVhbS5ydS90b2tlbjpWSFJTZDhQU0dkeXRmWDgwRjROMjVobFplM2VPZkJuWnR4SGlDNDZD\r\n" +
                "\r\n" +
                ")", //getRequest()

            "",  // getMethodName(),
            "",  // getHash(),
            "",  // getToken(),
            new String[]{},  // getSenderIdYandexArray(),
            new String[]{},  // getWarehouseIdYandexArray(),
            new String[]{},  // getWarehouseIdDeliveryArray(),
            new String[]{},  // getShipmentIdYandexArray(),
            new String[]{},  // getShipmentIdDeliveryArray(),
            new String[]{},  // getOrderIdCleanArray(),
            new String[]{},  // getOrderIdYandexArray(),
            new String[]{},  // getOrderIdDeliveryArray(),
            new String[]{},  // getOrderIdExternalArray(),
            new String[]{},  // getIntakeIdYandexArray(),
            new String[]{},  // getRegisterIdYandexArray(),
            new String[]{},  // getSelfExportIdYandexArray(),
            new String[]{},  // getSelfExportIdDeliveryArray(),

            "[object] (GuzzleHttp\\Message\\Response: HTTP/1.1 200 OK\r\n" +
                "Server: nginx\r\n" +
                "Date: Wed, 24 Feb 2016 21:00:11 GMT\r\n" +
                "Content-Type: application/json; charset=UTF-8\r\n" +
                "Content-Length: 76\r\n" +
                "Connection: keep-alive\r\n" +
                "X-Zendesk-API-Version: v2\r\n" +
                "X-Zendesk-Application-Version: v3.68.8\r\n" +
                "X-Frame-Options: SAMEORIGIN\r\n" +
                "Strict-Transport-Security: max-age=0;\r\n" +
                "X-UA-Compatible: IE=Edge,chrome=1\r\n" +
                "ETag: W/\"788813e9d5f7a30ad995b89094cc9745\"\r\n" +
                "Cache-Control: must-revalidate, private, max-age=0\r\n" +
                "X-Zendesk-Origin-Server: app18.pod5.iad1.zdsys.com\r\n" +
                "X-Request-Id: 6ace3d5b-8ce7-4d6c-cd46-b8ca3a6bdb18\r\n" +
                "X-Runtime: 0.172787\r\n" +
                "X-Rack-Cache: miss\r\n" +
                "X-Zendesk-Request-Id: 98c17384c65da67d0f81\r\n" +
                "X-Content-Type-Options: nosniff\r\n" +
                "\r\n" +
                "{\"results\":[],\"facets\":null,\"next_page\":null,\"previous_page\":null,\"count\":0})", //getResponse

            "",  // getMethodName(),
            "",  // getHash(),
            0,  // getIsError(),
            new String[]{},  // getIntakeIdYandexArray(),
            new String[]{},  // getOrderIdCleanArray(),
            new String[]{},  // getOrderIdYandexArray(),
            new String[]{},  // getOrderIdDeliveryArray(),
            new String[]{},  // getOrderIdExternalArray(),
            new String[]{},  // getRegisterIdYandexArray(),
            new String[]{},  // getRegisterIdDeliveryArray(),
            new String[]{},  // getSelfExportIdYandexArray(),
            new String[]{},  // getSelfExportIdDeliveryArray(),
            new String[]{},  // getDeliveryNumArray(),

            "0", //getResourceId()
            "https://yadostavka.zendesk.com/api/v2/search.json?query=type%3Aticket%20external_id%3A54", //getUrl
            "0", //getUserId
            "{\"executionTime\":\"00:00:03\",\"request\":\"[object] (GuzzleHttp\\\\Message\\\\Request: GET " +
                "/api/v2/search.json?query=type%3Aticket%20external_id%3A54 HTTP/1.1\\r\\nHost: yadostavka.zendesk" +
                ".com\\r\\nUser-Agent: Guzzle/5.3.0 curl/7.35.0 PHP/5.6.13-1+yandex1\\r\\nAuthorization: Basic " +
                "bWRtc2hpcEB5YW5kZXgtdGVhbS5ydS90b2tlbjpWSFJTZDhQU0dkeXRmWDgwRjROMjVobFplM2VPZkJuWnR4SGlDNDZD\\r\\n" +
                "\\r\\n)\",\"response\":\"[object] (GuzzleHttp\\\\Message\\\\Response: HTTP/1.1 200 OK\\r\\nServer: " +
                "nginx\\r\\nDate: Wed, 24 Feb 2016 21:00:11 GMT\\r\\nContent-Type: application/json; " +
                "charset=UTF-8\\r\\nContent-Length: 76\\r\\nConnection: keep-alive\\r\\nX-Zendesk-API-Version: " +
                "v2\\r\\nX-Zendesk-Application-Version: v3.68.8\\r\\nX-Frame-Options: " +
                "SAMEORIGIN\\r\\nStrict-Transport-Security: max-age=0;\\r\\nX-UA-Compatible: IE=Edge," +
                "chrome=1\\r\\nETag: W/\\\"788813e9d5f7a30ad995b89094cc9745\\\"\\r\\nCache-Control: must-revalidate, " +
                "private, max-age=0\\r\\nX-Zendesk-Origin-Server: app18.pod5.iad1.zdsys.com\\r\\nX-Request-Id: " +
                "6ace3d5b-8ce7-4d6c-cd46-b8ca3a6bdb18\\r\\nX-Runtime: 0.172787\\r\\nX-Rack-Cache: " +
                "miss\\r\\nX-Zendesk-Request-Id: 98c17384c65da67d0f81\\r\\nX-Content-Type-Options: " +
                "nosniff\\r\\n\\r\\n{\\\"results\\\":[],\\\"facets\\\":null,\\\"next_page\\\":null," +
                "\\\"previous_page\\\":null,\\\"count\\\":0})\",\"url\":\"https://yadostavka.zendesk" +
                ".com/api/v2/search.json?query=type%3Aticket%20external_id%3A54\",\"resourceId\":\"0\"," +
                "\"userId\":\"0\",\"hostname\":\"deliback01h\"," +
                "\"applicationId\":\"2634ede374d609cdfa15b9907ac818ce5e0fe6270042aa08e47416998b0a5fb9\"}"
            //ExternalData full
        );

        checker.check(
            "[2016-03-09 01:30:23] DS.Api:INFO: deliback01h Guzzle/5.3.0 curl/7.35.0 PHP/5.6.13-1+yandex1 - " +
                "[2016-03-08T22:30:23+00:00] \"POST / /\" 200 309 [] {\"executionTime\":\"00:00:15\"," +
                "\"request\":\"[object] (GuzzleHttp\\\\Message\\\\Request: POST / HTTP/1.1\\r\\nHost: yandex.inpost" +
                ".ru\\r\\nUser-Agent: Guzzle/5.3.0 curl/7.35.0 PHP/5.6.13-1+yandex1\\r\\nContent-Type: text/xml; " +
                "utf-8\\r\\nContent-Length: 2339\\r\\n\\r\\n<?xml version=\\\"1.0\\\" " +
                "encoding=\\\"UTF-8\\\"?>\\n<root>\\n  " +
                "<token>e5b46ffacb8d27059768b3647492a0283dd45c63276fe00914a2c35458bd6e9f</token>\\n  " +
                "<hash>5f84e97cda3c6481af91a113bd729162</hash>\\n  <request type=\\\"getOrdersStatus\\\">\\n    " +
                "<ordersId>\\n      <orderId>\\n        <yandexId>48-YD106723</yandexId>\\n        " +
                "<deliveryId>863624996YXDLVR</deliveryId>\\n      </orderId>\\n      <orderId>\\n        " +
                "<yandexId>51-YD106729</yandexId>\\n        <deliveryId>727300552YXDLVR</deliveryId>\\n      " +
                "</orderId>\\n      <orderId>\\n        <yandexId>54-YD106738</yandexId>\\n        " +
                "<deliveryId>751595318YXDLVR</deliveryId>\\n      </orderId>\\n      </ordersId>\\n  " +
                "</request>\\n</root>\\n)\",\"response\":\"[object] (GuzzleHttp\\\\Message\\\\Response: HTTP/1.1 200 " +
                "OK\\r\\nDate: Tue, 08 Mar 2016 22:33:51 GMT\\r\\nServer: Apache/2.2.15 (CentOS)\\r\\nX-Powered-By: " +
                "PHP/5.3.3\\r\\nContent-Length: 309\\r\\nConnection: close\\r\\nContent-Type: text/xml; " +
                "charset=utf-8\\r\\n\\r\\n<?xml version=\\\"1.0\\\" " +
                "encoding=\\\"UTF-8\\\"?>\\n<root>\\n<hash>5f84e97cda3c6481af91a113bd729162</hash>\\n<requestState" +
                ">\\n<isError>true</isError><errors>Заказа с номером yandexId=55-YD106777, deliveryId= не существует." +
                " </errors>\\n</requestState>\\n<response type=\\\"getOrdersStatus\\\">\\n</response>\\n</root>)\"," +
                "\"url\":\"https://yandex.inpost.ru\",\"resourceId\":\"0\",\"userId\":\"0\"," +
                "\"hostname\":\"deliback01h\"," +
                "\"applicationId\":\"547d3100680c36ce5d5b968e358e218705198315d8c5eb5468f225a3722f52ba\"}\n",
            new Date(1457476223000L),
            new String[]{"DS", "Api"},
            "INFO",
            "deliback01h Guzzle/5.3.0 curl/7.35.0 PHP/5.6.13-1+yandex1 - [2016-03-08T22:30:23+00:00] \"POST / /\" 200" +
                " 309",
            //Context
            "", //getAttributes(),
            "", //getDeliveryId(),
            "", //getDeliveryUniqueName(),
            "", //getEmail(),
            "", //getException(),
            "", //getGroup(),
            "", //getLogin(),
            "", //getMergeVars(),
            "", //getOptions(),
            "", //getOrderId(),
            "", //getPickupPointCode(),
            "{}", //getContext(),
            //External data
            "547d3100680c36ce5d5b968e358e218705198315d8c5eb5468f225a3722f52ba", //getApplicationId()
            "00:00:15", //getExecutionTime()
            "deliback01h", //getHostname()
            "[object] (GuzzleHttp\\Message\\Request: POST / HTTP/1.1\r\n" +
                "Host: yandex.inpost.ru\r\n" +
                "User-Agent: Guzzle/5.3.0 curl/7.35.0 PHP/5.6.13-1+yandex1\r\n" +
                "Content-Type: text/xml; utf-8\r\n" +
                "Content-Length: 2339\r\n" +
                "\r\n" +
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<root>\n" +
                "  <token>e5b46ffacb8d27059768b3647492a0283dd45c63276fe00914a2c35458bd6e9f</token>\n" +
                "  <hash>5f84e97cda3c6481af91a113bd729162</hash>\n" +
                "  <request type=\"getOrdersStatus\">\n" +
                "    <ordersId>\n" +
                "      <orderId>\n" +
                "        <yandexId>48-YD106723</yandexId>\n" +
                "        <deliveryId>863624996YXDLVR</deliveryId>\n" +
                "      </orderId>\n" +
                "      <orderId>\n" +
                "        <yandexId>51-YD106729</yandexId>\n" +
                "        <deliveryId>727300552YXDLVR</deliveryId>\n" +
                "      </orderId>\n" +
                "      <orderId>\n" +
                "        <yandexId>54-YD106738</yandexId>\n" +
                "        <deliveryId>751595318YXDLVR</deliveryId>\n" +
                "      </orderId>\n" +
                "      </ordersId>\n" +
                "  </request>\n" +
                "</root>\n" +
                ")", //getRequest()

            "getOrdersStatus",  // getMethodName(),
            "5f84e97cda3c6481af91a113bd729162",  // getHash(),
            "e5b46ffacb8d27059768b3647492a0283dd45c63276fe00914a2c35458bd6e9f",  // getToken(),
            new String[]{},  // getSenderIdYandexArray(),
            new String[]{},  // getWarehouseIdYandexArray(),
            new String[]{},  // getWarehouseIdDeliveryArray(),
            new String[]{},  // getShipmentIdYandexArray(),
            new String[]{},  // getShipmentIdDeliveryArray(),
            new String[]{"106723", "106729", "106738"},  // getOrderIdCleanArray(),
            new String[]{"48-YD106723", "51-YD106729", "54-YD106738"},  // getOrderIdYandexArray(),
            new String[]{"863624996YXDLVR", "727300552YXDLVR", "751595318YXDLVR"},  // getOrderIdDeliveryArray(),
            new String[]{},  // getOrderIdExternalArray(),
            new String[]{},  // getIntakeIdYandexArray(),
            new String[]{},  // getRegisterIdYandexArray(),
            new String[]{},  // getSelfExportIdYandexArray(),
            new String[]{},  // getSelfExportIdDeliveryArray(),

            "[object] (GuzzleHttp\\Message\\Response: HTTP/1.1 200 OK\r\n" +
                "Date: Tue, 08 Mar 2016 22:33:51 GMT\r\n" +
                "Server: Apache/2.2.15 (CentOS)\r\n" +
                "X-Powered-By: PHP/5.3.3\r\n" +
                "Content-Length: 309\r\n" +
                "Connection: close\r\n" +
                "Content-Type: text/xml; charset=utf-8\r\n" +
                "\r\n" +
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<root>\n" +
                "<hash>5f84e97cda3c6481af91a113bd729162</hash>\n" +
                "<requestState>\n" +
                "<isError>true</isError><errors>Заказа с номером yandexId=55-YD106777, deliveryId= не существует. " +
                "</errors>\n" +
                "</requestState>\n" +
                "<response type=\"getOrdersStatus\">\n" +
                "</response>\n" +
                "</root>)", //getResponse

            "getOrdersStatus",  // getMethodName(),
            "5f84e97cda3c6481af91a113bd729162",  // getHash(),
            1,  // getIsError(),
            new String[]{},  // getIntakeIdYandexArray(),
            new String[]{},  // getOrderIdCleanArray(),
            new String[]{},  // getOrderIdYandexArray(),
            new String[]{},  // getOrderIdDeliveryArray(),
            new String[]{},  // getOrderIdExternalArray(),
            new String[]{},  // getRegisterIdYandexArray(),
            new String[]{},  // getRegisterIdDeliveryArray(),
            new String[]{},  // getSelfExportIdYandexArray(),
            new String[]{},  // getSelfExportIdDeliveryArray(),
            new String[]{},  // getDeliveryNumArray(),

            "0", //getResourceId()
            "https://yandex.inpost.ru", //getUrl
            "0", //getUserId
            "{\"executionTime\":\"00:00:15\",\"request\":\"[object] (GuzzleHttp\\\\Message\\\\Request: POST / HTTP/1" +
                ".1\\r\\nHost: yandex.inpost.ru\\r\\nUser-Agent: Guzzle/5.3.0 curl/7.35.0 PHP/5.6" +
                ".13-1+yandex1\\r\\nContent-Type: text/xml; utf-8\\r\\nContent-Length: 2339\\r\\n\\r\\n<?xml " +
                "version=\\\"1.0\\\" encoding=\\\"UTF-8\\\"?>\\n<root>\\n  " +
                "<token>e5b46ffacb8d27059768b3647492a0283dd45c63276fe00914a2c35458bd6e9f</token>\\n  " +
                "<hash>5f84e97cda3c6481af91a113bd729162</hash>\\n  <request type=\\\"getOrdersStatus\\\">\\n    " +
                "<ordersId>\\n      <orderId>\\n        <yandexId>48-YD106723</yandexId>\\n        " +
                "<deliveryId>863624996YXDLVR</deliveryId>\\n      </orderId>\\n      <orderId>\\n        " +
                "<yandexId>51-YD106729</yandexId>\\n        <deliveryId>727300552YXDLVR</deliveryId>\\n      " +
                "</orderId>\\n      <orderId>\\n        <yandexId>54-YD106738</yandexId>\\n        " +
                "<deliveryId>751595318YXDLVR</deliveryId>\\n      </orderId>\\n      </ordersId>\\n  " +
                "</request>\\n</root>\\n)\",\"response\":\"[object] (GuzzleHttp\\\\Message\\\\Response: HTTP/1.1 200 " +
                "OK\\r\\nDate: Tue, 08 Mar 2016 22:33:51 GMT\\r\\nServer: Apache/2.2.15 (CentOS)\\r\\nX-Powered-By: " +
                "PHP/5.3.3\\r\\nContent-Length: 309\\r\\nConnection: close\\r\\nContent-Type: text/xml; " +
                "charset=utf-8\\r\\n\\r\\n<?xml version=\\\"1.0\\\" " +
                "encoding=\\\"UTF-8\\\"?>\\n<root>\\n<hash>5f84e97cda3c6481af91a113bd729162</hash>\\n<requestState" +
                ">\\n<isError>true</isError><errors>Заказа с номером yandexId=55-YD106777, deliveryId= не существует." +
                " </errors>\\n</requestState>\\n<response type=\\\"getOrdersStatus\\\">\\n</response>\\n</root>)\"," +
                "\"url\":\"https://yandex.inpost.ru\",\"resourceId\":\"0\",\"userId\":\"0\"," +
                "\"hostname\":\"deliback01h\"," +
                "\"applicationId\":\"547d3100680c36ce5d5b968e358e218705198315d8c5eb5468f225a3722f52ba\"}"
            //externalDataFull
        );

        checker.check(
            "[2016-03-09 01:15:49] FF.Module:INFO: deliback01h Guzzle/5.3.0 curl/7.35.0 PHP/5.6.13-1+yandex1 - " +
                "[2016-03-08T22:15:49+00:00] \"POST /Yandex/hs/ya /\" 200 200 [] {\"executionTime\":\"00:09:34\"," +
                "\"request\":\"[object] (GuzzleHttp\\\\Message\\\\Request: POST /Yandex/hs/ya HTTP/1.1\\r\\nHost: " +
                "yandex.strizh-logistic.ru\\r\\nUser-Agent: Guzzle/5.3.0 curl/7.35.0 PHP/5.6" +
                ".13-1+yandex1\\r\\nContent-Type: text/xml; utf-8\\r\\nContent-Length: 831\\r\\n\\r\\n<?xml " +
                "version=\\\"1.0\\\" encoding=\\\"UTF-8\\\"?>\\n<root>\\n  " +
                "<token>QZbRLzZbNogdzhuk2yogNgSGMKqpMnXKubyXK9ZJPHhRQFtUPg3hriZKr6zXUBQK</token>\\n  " +
                "<hash>cc09eab0c75094355a8104def4be7d42</hash>\\n  <request type=\\\"createWHReturnRegister\\\">\\n  " +
                "  <ordersId>\\n      <orderId>\\n        <yandexId>209738-YD96778</yandexId>\\n        " +
                "<deliveryId>00000022941</deliveryId>\\n        <externalId>YAP1008414</externalId>\\n      " +
                "</orderId>\\n    </ordersId>\\n    <sender>\\n      <id>\\n        <yandexId>1121</yandexId>\\n     " +
                " </id>\\n      <incorporation>ООО «Пум-Пу.Ру»</incorporation>\\n      <phones>\\n        <phone>\\n " +
                "         <phone>79688260012</phone>\\n        </phone>\\n      </phones>\\n      <contact>\\n       " +
                " <name>Елена</name>\\n        <surname>Моторная</surname>\\n      </contact>\\n      <name>Пум-Пу" +
                ".Ру</name>\\n    </sender>\\n  </request>\\n</root>\\n)\",\"response\":\"[object] " +
                "(GuzzleHttp\\\\Message\\\\Response: HTTP/1.1 200 OK\\r\\nContent-Length: 200\\r\\nServer: " +
                "Microsoft-IIS/8.5\\r\\nX-Powered-By: ASP.NET\\r\\nDate: Tue, 08 Mar 2016 16:15:38 " +
                "GMT\\r\\n\\r\\n<?xml version=\\\"1.0\\\" " +
                "encoding=\\\"UTF-8\\\"?><root><hash>cc09eab0c75094355a8104def4be7d42</hash><requestState><isError" +
                ">false</isError></requestState><response type=\\\"createWHReturnRegister\\\"></response></root>)\"," +
                "\"url\":\"https://yandex.strizh-logistic.ru/Yandex/hs/ya\",\"resourceId\":\"0\",\"userId\":\"0\"," +
                "\"hostname\":\"deliback01h\"," +
                "\"applicationId\":\"eeb122070198a11e11bdf19317a882bde10e2d6c372fc3fe44c61a0eca3a0b87\"}",
            new Date(1457475349000L),
            new String[]{"FF", "Module"},
            "INFO",
            "deliback01h Guzzle/5.3.0 curl/7.35.0 PHP/5.6.13-1+yandex1 - [2016-03-08T22:15:49+00:00] \"POST " +
                "/Yandex/hs/ya /\" 200 200",
            //Context
            "", //getAttributes(),
            "", //getDeliveryId(),
            "", //getDeliveryUniqueName(),
            "", //getEmail(),
            "", //getException(),
            "", //getGroup(),
            "", //getLogin(),
            "", //getMergeVars(),
            "", //getOptions(),
            "", //getOrderId(),
            "", //getPickupPointCode(),
            "{}", //getContext(),
            //External data

            "eeb122070198a11e11bdf19317a882bde10e2d6c372fc3fe44c61a0eca3a0b87", //getApplicationId()
            "00:09:34", //getExecutionTime()
            "deliback01h", //getHostname()

            "[object] (GuzzleHttp\\Message\\Request: POST /Yandex/hs/ya HTTP/1.1\r\n" +
                "Host: yandex.strizh-logistic.ru\r\n" +
                "User-Agent: Guzzle/5.3.0 curl/7.35.0 PHP/5.6.13-1+yandex1\r\n" +
                "Content-Type: text/xml; utf-8\r\n" +
                "Content-Length: 831\r\n" +
                "\r\n" +
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<root>\n" +
                "  <token>QZbRLzZbNogdzhuk2yogNgSGMKqpMnXKubyXK9ZJPHhRQFtUPg3hriZKr6zXUBQK</token>\n" +
                "  <hash>cc09eab0c75094355a8104def4be7d42</hash>\n" +
                "  <request type=\"createWHReturnRegister\">\n" +
                "    <ordersId>\n" +
                "      <orderId>\n" +
                "        <yandexId>209738-YD96778</yandexId>\n" +
                "        <deliveryId>00000022941</deliveryId>\n" +
                "        <externalId>YAP1008414</externalId>\n" +
                "      </orderId>\n" +
                "    </ordersId>\n" +
                "    <sender>\n" +
                "      <id>\n" +
                "        <yandexId>1121</yandexId>\n" +
                "      </id>\n" +
                "      <incorporation>ООО «Пум-Пу.Ру»</incorporation>\n" +
                "      <phones>\n" +
                "        <phone>\n" +
                "          <phone>79688260012</phone>\n" +
                "        </phone>\n" +
                "      </phones>\n" +
                "      <contact>\n" +
                "        <name>Елена</name>\n" +
                "        <surname>Моторная</surname>\n" +
                "      </contact>\n" +
                "      <name>Пум-Пу.Ру</name>\n" +
                "    </sender>\n" +
                "  </request>\n" +
                "</root>\n" +
                ")", //getRequest()

            "createWHReturnRegister",  // getMethodName(),
            "cc09eab0c75094355a8104def4be7d42",  // getHash(),
            "QZbRLzZbNogdzhuk2yogNgSGMKqpMnXKubyXK9ZJPHhRQFtUPg3hriZKr6zXUBQK",  // getToken(),
            new String[]{"1121"},  // getSenderIdYandexArray(),
            new String[]{},  // getWarehouseIdYandexArray(),
            new String[]{},  // getWarehouseIdDeliveryArray(),
            new String[]{},  // getShipmentIdYandexArray(),
            new String[]{},  // getShipmentIdDeliveryArray(),
            new String[]{"96778"},  // getOrderIdCleanArray(),
            new String[]{"209738-YD96778"},  // getOrderIdYandexArray(),
            new String[]{"00000022941"},  // getOrderIdDeliveryArray(),
            new String[]{"YAP1008414"},  // getOrderIdExternalArray(),
            new String[]{},  // getIntakeIdYandexArray(),
            new String[]{},  // getRegisterIdYandexArray(),
            new String[]{},  // getSelfExportIdYandexArray(),
            new String[]{},  // getSelfExportIdDeliveryArray(),

            "[object] (GuzzleHttp\\Message\\Response: HTTP/1.1 200 OK\r\n" +
                "Content-Length: 200\r\n" +
                "Server: Microsoft-IIS/8.5\r\n" +
                "X-Powered-By: ASP.NET\r\n" +
                "Date: Tue, 08 Mar 2016 16:15:38 GMT\r\n" +
                "\r\n" +
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root><hash>cc09eab0c75094355a8104def4be7d42</hash" +
                "><requestState><isError>false</isError></requestState><response " +
                "type=\"createWHReturnRegister\"></response></root>)", //getResponse

            "createWHReturnRegister",  // getMethodName(),
            "cc09eab0c75094355a8104def4be7d42",  // getHash(),
            0,  // getIsError(),
            new String[]{},  // getIntakeIdYandexArray(),
            new String[]{},  // getOrderIdCleanArray(),
            new String[]{},  // getOrderIdYandexArray(),
            new String[]{},  // getOrderIdDeliveryArray(),
            new String[]{},  // getOrderIdExternalArray(),
            new String[]{},  // getRegisterIdYandexArray(),
            new String[]{},  // getRegisterIdDeliveryArray(),
            new String[]{},  // getSelfExportIdYandexArray(),
            new String[]{},  // getSelfExportIdDeliveryArray(),
            new String[]{},  // getDeliveryNumArray(),

            "0", //getResourceId()
            "https://yandex.strizh-logistic.ru/Yandex/hs/ya", //getUrl
            "0", //getUserId
            "{\"executionTime\":\"00:09:34\",\"request\":\"[object] (GuzzleHttp\\\\Message\\\\Request: POST " +
                "/Yandex/hs/ya HTTP/1.1\\r\\nHost: yandex.strizh-logistic.ru\\r\\nUser-Agent: Guzzle/5.3.0 curl/7.35" +
                ".0 PHP/5.6.13-1+yandex1\\r\\nContent-Type: text/xml; utf-8\\r\\nContent-Length: 831\\r\\n\\r\\n<?xml" +
                " version=\\\"1.0\\\" encoding=\\\"UTF-8\\\"?>\\n<root>\\n  " +
                "<token>QZbRLzZbNogdzhuk2yogNgSGMKqpMnXKubyXK9ZJPHhRQFtUPg3hriZKr6zXUBQK</token>\\n  " +
                "<hash>cc09eab0c75094355a8104def4be7d42</hash>\\n  <request type=\\\"createWHReturnRegister\\\">\\n  " +
                "  <ordersId>\\n      <orderId>\\n        <yandexId>209738-YD96778</yandexId>\\n        " +
                "<deliveryId>00000022941</deliveryId>\\n        <externalId>YAP1008414</externalId>\\n      " +
                "</orderId>\\n    </ordersId>\\n    <sender>\\n      <id>\\n        <yandexId>1121</yandexId>\\n     " +
                " </id>\\n      <incorporation>ООО «Пум-Пу.Ру»</incorporation>\\n      <phones>\\n        <phone>\\n " +
                "         <phone>79688260012</phone>\\n        </phone>\\n      </phones>\\n      <contact>\\n       " +
                " <name>Елена</name>\\n        <surname>Моторная</surname>\\n      </contact>\\n      <name>Пум-Пу" +
                ".Ру</name>\\n    </sender>\\n  </request>\\n</root>\\n)\",\"response\":\"[object] " +
                "(GuzzleHttp\\\\Message\\\\Response: HTTP/1.1 200 OK\\r\\nContent-Length: 200\\r\\nServer: " +
                "Microsoft-IIS/8.5\\r\\nX-Powered-By: ASP.NET\\r\\nDate: Tue, 08 Mar 2016 16:15:38 " +
                "GMT\\r\\n\\r\\n<?xml version=\\\"1.0\\\" " +
                "encoding=\\\"UTF-8\\\"?><root><hash>cc09eab0c75094355a8104def4be7d42</hash><requestState><isError" +
                ">false</isError></requestState><response type=\\\"createWHReturnRegister\\\"></response></root>)\"," +
                "\"url\":\"https://yandex.strizh-logistic.ru/Yandex/hs/ya\",\"resourceId\":\"0\",\"userId\":\"0\"," +
                "\"hostname\":\"deliback01h\"," +
                "\"applicationId\":\"eeb122070198a11e11bdf19317a882bde10e2d6c372fc3fe44c61a0eca3a0b87\"}"
            //externalDataFull
        );
    }
}
