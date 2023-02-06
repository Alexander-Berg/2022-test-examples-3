package ru.yandex.market.stat.dicts.parsers;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.stat.dicts.config.ParsersDictsConfig;
import ru.yandex.market.stat.dicts.records.ShopRatingsDictionaryRecord;
import ru.yandex.market.stat.parsers.ParseException;

import java.sql.Date;
import java.time.LocalDate;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author Alexey Ostrikov <aostrikov@yandex-team.ru>
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = ParsersDictsConfig.class)
public class ShopRatingsDictionaryRecordParserTest {

    @Autowired
    @Qualifier("shopRatingsRecordParser")
    private GeneralDictionaryParser<ShopRatingsDictionaryRecord> parser;

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Test
    public void shouldParseSimpleLine() {
        ShopRatingsDictionaryRecord record = parser.parseRecord("277048\t4\t2016-05-30\t1\t0");

        assertThat(record.getShop_id(), is(277048L));
        assertThat(record.getRating(), is(4));
        assertThat(record.getRating_date(), is(Date.valueOf(LocalDate.of(2016, 5, 30))));
        assertThat(record.getIs_enabled(), is(true));
        assertThat(record.getRating_type(), is(0));
    }

    @Test
    public void shouldRejectIncorrectDate() {
        expectedEx.expect(ParseException.class);
        expectedEx.expectMessage("date: java.time.format.DateTimeParseException: " +
                "Text '2016-05-30 13:23:00' could not be parsed, unparsed text found at index 10");

        parser.parseRecord("277048\t4\t2016-05-30 13:23:00\t1\t0");
    }

    @Test
    public void shouldRejectIncorrectIsEnabledValue() {
        expectedEx.expect(ParseException.class);
        expectedEx.expectMessage("is_enabled: java.lang.IllegalArgumentException: Bad boolean format: 8");

        parser.parseRecord("277048\t4\t2016-05-30\t8\t0");
    }
}
