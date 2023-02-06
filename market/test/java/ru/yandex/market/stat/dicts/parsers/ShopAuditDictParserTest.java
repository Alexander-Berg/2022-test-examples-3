package ru.yandex.market.stat.dicts.parsers;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.stat.dicts.config.ParsersDictsConfig;
import ru.yandex.market.stat.dicts.records.ShopAuditDictionaryRecord;

import static java.util.Arrays.asList;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author aostrikov
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = ParsersDictsConfig.class)
public class ShopAuditDictParserTest {

    @Autowired
    @Qualifier("shopAuditRecordParser")
    private GeneralDictionaryParser<ShopAuditDictionaryRecord> parser;

    @Test
    public void shouldParseRecordWithNullMetaInfo() {
        ShopAuditDictionaryRecord record = parser.parseRecord("\"1300\",\"gardenhouse.ru\",\"345\",\"70\",\"0\",\"null\",\"12\"");

        assertThat(record.getShop_id(), is(1300L));
        assertThat(record.getUrl(), is("gardenhouse.ru"));
        assertThat(record.getOthers(), is(asList("345", "70", "0", "null", "12")));
    }
}
