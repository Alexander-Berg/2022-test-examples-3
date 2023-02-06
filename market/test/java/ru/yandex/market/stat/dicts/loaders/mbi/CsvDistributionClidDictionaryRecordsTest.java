package ru.yandex.market.stat.dicts.loaders.mbi;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.stat.dicts.records.DistributionClidDictionaryRecord;

import java.io.IOException;
import java.io.StringReader;

import static org.junit.Assert.fail;

/**
 * @author zoom
 */
public class CsvDistributionClidDictionaryRecordsTest {
    
    @Test
    public void shouldParseCsvFieldsCorrectly() throws IOException {
        String line = "2222853,21,284560,146971,1361,26,0,37,\"ДС-0141-09/11, RS, 20.00 %\",ff.planbmedia.com,Ссылка на морду  &(clid 7)&(clid 11) - planbmedia.com - установк,ff.planbmedia.com,ff.planbmedia.com,planb-dl-partner,21";
        try (CSVParser records = CSVFormat.DEFAULT.parse(new StringReader(line))) {
            DistributionClidDictionaryRecord actualClid = DistributionClidDictionaryRecord.fromCsv(records.iterator().next());
            DistributionClidDictionaryRecord expectedClid =
                new DistributionClidDictionaryRecord(
                    2222853L,
                    21L,
                    284560L,
                    146971L,
                    1361L,
                    26L,
                    0L,
                    37L,
                    "ДС-0141-09/11, RS, 20.00 %",
                    "ff.planbmedia.com",
                    "Ссылка на морду  &(clid 7)&(clid 11) - planbmedia.com - установк",
                    "ff.planbmedia.com",
                    "ff.planbmedia.com",
                    "planb-dl-partner",
                     21L
                );
            Assert.assertEquals(expectedClid, actualClid);
        }
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowExceptionWhenCsvFieldCountIsLessThanExpected() throws IOException {
        String line = "21,284560,146971,1361,26,0,37,\"ДС-0141-09/11, RS, 20.00 %\",ff.planbmedia.com,Ссылка на морду  &(clid 7)&(clid 11) - planbmedia.com - установк,ff.planbmedia.com,ff.planbmedia.com,planb-dl-partner,21";
        try (CSVParser records = CSVFormat.DEFAULT.parse(new StringReader(line))) {
            DistributionClidDictionaryRecord.fromCsv(records.iterator().next());
            fail("Must throw exception");
        }
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowExceptionWhenCsvFieldCountIsMoreThanExpected() throws IOException {
        String line = "2222853,21,284560,146971,1361,26,0,37,\"ДС-0141-09/11, RS, 20.00 %\",ff.planbmedia.com,Ссылка на морду  &(clid 7)&(clid 11) - planbmedia.com - установк,ff.planbmedia.com,ff.planbmedia.com,planb-dl-partner,21,1111";
        try (CSVParser records = CSVFormat.DEFAULT.parse(new StringReader(line))) {
            DistributionClidDictionaryRecord.fromCsv(records.iterator().next());
            fail("Must throw exception");
        }
    }


    @Test(expected = RuntimeException.class)
    public void shouldThrowExceptionWhenIncorrectNumberFieldFormat() throws IOException {
        String line = "1a,21,284560,146971,1361,26,0,37,\"ДС-0141-09/11, RS, 20.00 %\",ff.planbmedia.com,Ссылка на морду  &(clid 7)&(clid 11) - planbmedia.com - установк,ff.planbmedia.com,ff.planbmedia.com,planb-dl-partner,21";
        try (CSVParser records = CSVFormat.DEFAULT.parse(new StringReader(line))) {
            DistributionClidDictionaryRecord.fromCsv(records.iterator().next());
            fail("Must throw exception");
        }
    }

    @Test
    public void shouldReturnRecordWithNullNumbersWhenEmptyString() throws IOException {
        String line = ",,,,,,,1111,\"ДС-0141-09/11, RS, 20.00 %\",ff.planbmedia.com,Ссылка на морду  &(clid 7)&(clid 11) - planbmedia.com - установк,ff.planbmedia.com,ff.planbmedia.com,planb-dl-partner,21";
        try (CSVParser records = CSVFormat.DEFAULT.parse(new StringReader(line))) {
            DistributionClidDictionaryRecord actualClid = DistributionClidDictionaryRecord.fromCsv(records.iterator().next());
            DistributionClidDictionaryRecord expectedClid =
                new DistributionClidDictionaryRecord(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    1111L,
                    "ДС-0141-09/11, RS, 20.00 %",
                    "ff.planbmedia.com",
                    "Ссылка на морду  &(clid 7)&(clid 11) - planbmedia.com - установк",
                    "ff.planbmedia.com",
                    "ff.planbmedia.com",
                    "planb-dl-partner",
                    21L
                );
            Assert.assertEquals(expectedClid, actualClid);
        }
    }
    
}

