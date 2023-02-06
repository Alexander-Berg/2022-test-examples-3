package ru.yandex.market.ir.nirvana.find_msku_info.report_parser;

import org.junit.Ignore;
import ru.yandex.market.ir.nirvana.find_msku_info.Main;

import java.io.IOException;

/**
 * @author inenakhov
 */
@Ignore
public class IntegrationalTest {
    private IntegrationalTest() {
    }

    public static void main(String[] args) throws IOException {
        System.setProperty("inputPath", "src/test/resources/ids.tsv");
        System.setProperty("outputPath", "src/test/resources/output.json");
        System.setProperty("report.search.url", "http://localhost:9999/yandsearch");
        System.setProperty("report.client.max-connection-per-route", "2");
        System.setProperty("report.client.max-connection-total", "2");
        System.setProperty("report.client.batch-size", "25");
        System.setProperty("report.client.max-retry-count", "10");

        Main.main(new String[]{});
    }
}
