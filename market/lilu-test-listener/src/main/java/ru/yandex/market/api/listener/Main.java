package ru.yandex.market.api.listener;

import com.google.common.base.Charsets;
import ru.yandex.market.api.listener.expectations.HttpExpectations;
import ru.yandex.market.api.listener.expectations.HttpRequestExpectationBuilder;

/**
 * Just for self-testing
 *
 * @author dimkarp93
 */
public class Main {
    private static final int PORT = 13666;
    private static final int TIMES_INF = 1_000_000_000;

    private static final String JSON = "[\n" +
        "  {\n" +
        "    \"contactPhone\": \"79261234567\",\n" +
        "    \"createdAt\": \"2018-02-12 00:00:00.0\",\n" +
        "    \"factAddress\": \"factAddr 1\",\n" +
        "    \"juridicalAddress\": \"jurAddrr 1\",\n" +
        "    \"name\": \"orgName 1\",\n" +
        "    \"ogrn\": \"12345\",\n" +
        "    \"prepayRequestId\": \"1\",\n" +
        "    \"regnumName\": \"ОГРН\",\n" +
        "    \"shopPhoneNumber\": \"84950950505\",\n" +
        "    \"supplierDomain\": \"my.shop.ru\",\n" +
        "    \"supplierId\": \"10263774\",\n" +
        "    \"supplierName\": \"my shop 1\",\n" +
        "    \"type\": \"1\",\n" +
        "    \"inn\": \"772734154100\"\n" +
        "  }\n" +
        "]";

    public static void main(String[] args) {
        HttpExpectations httpExpectations = config();
        Listener.listener(httpExpectations, PORT).start();
    }

    private static HttpExpectations config() {
        HttpExpectations httpExpectations = new HttpExpectations();

        httpExpectations
            .configure(new HttpRequestExpectationBuilder())
            .ok()
            .body(out(JSON))
            .times(TIMES_INF);

        return httpExpectations;
    }

    private static byte[] out(String text) {
        return text.getBytes(Charsets.UTF_8);
    }
}
