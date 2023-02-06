package ru.yandex.market.clickhouse.ddl;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 18/08/16
 */
public class TestData {
    public enum RequestType {
        IN, OUT, PROXY
    }

    public enum RequestTypeReordered {
        PROXY, IN, OUT
    }
}
