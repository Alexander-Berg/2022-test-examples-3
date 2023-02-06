package ru.yandex.market.stat.dicts.parsers;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.stat.dicts.config.ParsersDictsConfig;
import ru.yandex.market.stat.dicts.records.SupplierDictionaryRecord;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.stat.dicts.utils.ParserTestUtil.loadRecords;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = ParsersDictsConfig.class)
public class SupplierParserTest {

    @Autowired
    @Qualifier("suppliersParser")
    private DictionaryParser<SupplierDictionaryRecord> suppliersParser;

    @Test
    public void test() throws Exception{
        List<SupplierDictionaryRecord> records = loadRecords(suppliersParser, "/parsers/suppliers.dat");
        assertThat(records.size(), is(17));
    }
}
