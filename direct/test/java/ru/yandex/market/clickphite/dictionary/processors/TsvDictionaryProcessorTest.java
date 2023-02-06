package ru.yandex.market.clickphite.dictionary.processors;

import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.market.clickhouse.ClickhouseTemplate;
import ru.yandex.market.clickphite.dictionary.ClickhouseService;
import ru.yandex.market.clickphite.dictionary.dicts.HostToDcDictionary;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

/**
 * @author Denis Khurtin <dkhurtin@yandex-team.ru>
 */
public class TsvDictionaryProcessorTest {

    @Test
    public void test() throws IOException {
        // Given
        String line = "browser-mac07.techadmin.yandex.net\tiva\tiva1";

        // When
        HostToDcDictionary dictionary = new HostToDcDictionary();
        ClickhouseTemplate clickhouseTemplate = Mockito.mock(ClickhouseTemplate.class);
        ClickhouseService clickhouseService = new ClickhouseService();
        clickhouseService.setClickhouseTemplate(clickhouseTemplate);

        ClickhouseService.BulkUpdater bulkUpdater =
            clickhouseService.createBulkUpdater(dictionary, "tmp_tbl", 1, "test-host");

        new TsvDictionaryProcessor().insertData(dictionary, new BufferedReader(new StringReader(line)),
            bulkUpdater::submit);
        bulkUpdater.done();

        // Then
        Mockito.verify(clickhouseTemplate).update("INSERT INTO tmp_tbl (host, dc, stage) VALUES\n" +
            "('browser-mac07.techadmin.yandex.net','iva','iva1')\n", "test-host");
    }
}
