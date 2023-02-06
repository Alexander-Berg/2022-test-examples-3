package ru.yandex.market.stat.dicts.parsers;

import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.yandex.market.stat.dicts.records.VendorsDictionaryRecord;

import java.io.IOException;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.market.stat.dicts.utils.ParserTestUtil.loadRecords;

/**
 * @author Denis Khurtin <dkhurtin@yandex-team.ru>
 */
@RunWith(DataProviderRunner.class)
public class VendorsParserTest {

    @Test
    public void testWithSandboxData() throws IOException {
        // Given
        DictionaryParser<VendorsDictionaryRecord> vendorsParser = new VendorsDictionaryRecord.VendorsParser();

        // When
        List<VendorsDictionaryRecord> records = loadRecords(vendorsParser, "sandbox:global.vendors.xml.gz");

        // Then
        assertThat(records.size(), equalTo(80620));
        assertThat(records.get(0), equalTo(new VendorsDictionaryRecord(152808L, "Bowers & Wilkins", false)));
        assertThat(records.get(1), equalTo(new VendorsDictionaryRecord(1006806L, "B.O.N.E.", true)));
        assertThat(records.get(2), equalTo(new VendorsDictionaryRecord(969570L, "Baby Expert", false)));

    }
}
