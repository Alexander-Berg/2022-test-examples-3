package ru.yandex.hadoop.woodsman.loaders.logbroker.parser;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.BeanFactory;

import ru.yandex.hadoop.woodsman.loaders.logbroker.parser.antifraud.FraudFilter;
import ru.yandex.hadoop.woodsman.loaders.logbroker.parser.entities.LogRecord;
import ru.yandex.hadoop.woodsman.loaders.logbroker.parser.entities.annotations.FraudFilters;
import ru.yandex.hadoop.woodsman.loaders.logbroker.parser.entities.annotations.Rowid;
import ru.yandex.hadoop.woodsman.loaders.logbroker.parser.entities.fieldmarkers.HasStateAndFilter;
import ru.yandex.market.stat.parsers.ParseException;
import ru.yandex.market.stat.parsers.ParserUtil;
import ru.yandex.market.stat.parsers.annotations.Copy;
import ru.yandex.market.stat.parsers.annotations.Parser;
import ru.yandex.market.stat.parsers.annotations.Table;
import ru.yandex.market.stat.parsers.fields.FieldsParser;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.stat.parsers.ParserAnnotationUtils.getColumnName;
import static ru.yandex.market.stat.parsers.ParserAnnotationUtils.getTableName;

/**
 * @author Aleksandr Kormushin <kormushin@yandex-team.ru>
 */
@RunWith(DataProviderRunner.class)
public class AnnotationUtilsTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @DataProvider
    public static Object[][] truncateToDataProvider() {
        return new Object[][]{
                new Object[]{"abcdefg", 3, "abc"},
                new Object[]{"abcdefg", 0, "abcdefg"},
                new Object[]{"abcdefg", 7, "abcdefg"},
                new Object[]{"abcdefg", 10, "abcdefg"},
                new Object[]{"", 10, ""},
                new Object[]{null, 10, null},
        };
    }

    @Test
    @UseDataProvider("truncateToDataProvider")
    public void testTruncateTo(String string, int truncateTo, String expected) throws Exception {
        assertThat(ParserUtil.truncateTo(string, truncateTo), is(expected));
    }

    @Test
    public void testGetTable() throws Exception {
        assertThat(getTableName(MyLogRecord.class), is("my_log"));
    }

    @Test
    public void testGetColumnName() throws Exception {
        Field eventtime = MyLogRecord.class.getField("eventtime");
        assertThat(getColumnName(eventtime, eventtime.getAnnotation(Copy.class)), is("time"));

        Field intField = MyLogRecord.class.getField("intField");
        assertThat(getColumnName(intField, intField.getAnnotation(Copy.class)), is("intField"));
    }

    @Test
    public void testGetParsers() throws Exception {
        BeanFactory beanFactory = mock(BeanFactory.class);
        FieldsParser fieldsParser = mock(FieldsParser.class);
        when(beanFactory.getBean(eq("FieldsParserStub"))).thenReturn(fieldsParser);

        assertThat(AnnotationUtils.getParsers(MyLogRecord.class, beanFactory), contains(fieldsParser));
    }

    @Test
    public void testGetFilters() throws Exception {
        BeanFactory beanFactory = mock(BeanFactory.class);
        FraudFilter fraudFilter1 = mock(FraudFilter.class);
        FraudFilter fraudFilter2 = mock(FraudFilter.class);
        when(beanFactory.getBean(eq("FilterStub1"))).thenReturn(fraudFilter1);
        when(beanFactory.getBean(eq("FilterStub2"))).thenReturn(fraudFilter2);

        List<FraudFilter> fraudFilterList = new ArrayList<>();
        fraudFilterList.add(fraudFilter1);
        fraudFilterList.add(fraudFilter2);

        assertThat(AnnotationUtils.getFilters(MyLogRecord.class, beanFactory), equalTo(fraudFilterList));
    }

    @Test
    public void testSetRowId() throws Exception {
        MyLogRecord record = new MyLogRecord();
        AnnotationUtils.setRowid("rowid1", record);

        assertThat(record.rowid, is("rowid1"));
    }

    @Test
    public void shouldFailIfNoRowidFieldPresent() {
        expectedException.expect(ParseException.class);
        expectedException.expectMessage("rowid: Field with @Rowid is not defined in class MyLogRecord");

        @Table(name = "new_log")
        class MyLogRecord implements LogRecord {
            @Copy
            private String stringField;

            @Override
            public Date getEventtime() {
                return null;
            }
        }

        AnnotationUtils.setRowid("iddqd", new MyLogRecord());
    }

    @Table(name = "my_log")
    @Parser("FieldsParserStub")
    @FraudFilters({"FilterStub1", "FilterStub2"})
    static class MyLogRecord implements HasStateAndFilter, LogRecord {
        @Rowid
        public String rowid;
        @Copy(column = "time")
        public Timestamp eventtime;
        @Copy
        public Integer intField;
        @Copy(optional = true)
        public Long longField;
        @Copy
        public BigDecimal decimalField;
        @Copy(truncateTo = 3)
        public String stringField;
        private Integer filter = 0;
        private Integer state;

        public int parsableField = 0;

        @Override
        public Date getEventtime() {
            return eventtime;
        }

        @Override
        public Integer getFilter() {
            return filter;
        }

        @Override
        public void setFilter(Integer filter) {
            this.filter = filter;
        }

        @Override
        public Integer getState() {
            return state;
        }

        @Override
        public void setState(Integer state) {
            this.state = state;
        }
    }

    static class FieldsParserStub implements FieldsParser<MyLogRecord> {

        @Override
        public void parse(Map<String, String> rawValues, MyLogRecord parsedRecord) throws ParseException {
            parsedRecord.parsableField++;
        }
    }
}
