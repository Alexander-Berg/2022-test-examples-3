package ru.yandex.hadoop.woodsman.loaders.logbroker.parser;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.CharSource;
import com.google.common.io.CharStreams;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.BeanFactory;
import ru.yandex.hadoop.woodsman.loaders.logbroker.parser.antifraud.FraudFilter;
import ru.yandex.hadoop.woodsman.loaders.logbroker.parser.entities.LogRecord;
import ru.yandex.hadoop.woodsman.loaders.logbroker.parser.entities.annotations.FraudFilters;
import ru.yandex.hadoop.woodsman.loaders.logbroker.parser.entities.annotations.Rowid;
import ru.yandex.hadoop.woodsman.loaders.logbroker.parser.entities.fieldmarkers.HasStateAndFilter;
import ru.yandex.hadoop.woodsman.loaders.logbroker.parser.service.MetricsService;
import ru.yandex.market.stat.parsers.ParseException;
import ru.yandex.market.stat.parsers.ParserUtil;
import ru.yandex.market.stat.parsers.annotations.Copy;
import ru.yandex.market.stat.parsers.annotations.DateTimeField;
import ru.yandex.market.stat.parsers.annotations.Parser;
import ru.yandex.market.stat.parsers.annotations.Table;
import ru.yandex.market.stat.parsers.fields.FieldsParser;
import ru.yandex.market.stat.parsers.services.FieldParserHelper;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.Date;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Aleksandr Kormushin <kormushin@yandex-team.ru>
 */
@RunWith(DataProviderRunner.class)
public class ParserHelperTest {

    private BeanFactory beanFactory;

    private FieldsParser fieldsParser;

    private FraudFilter fraudFilter;

    private ParserHelper parserHelper;

    @DataProvider
    public static Object[][] parseDefaultDataProvider() {
        return new Object[][]{
                new Object[]{ImmutableMap.of(
                        "time", "1431505963",
                        "intField", "123",
                        "longField", "123123123123123",
                        "decimalField", "10.28",
                        "stringField", "some string"
                )},
                new Object[]{ImmutableMap.of(
                        "time", "1431505963",
                        "intField", "123",
                        "decimalField", "10",
                        "stringField", "some string"
                )},
        };
    }

    @Before
    public void init() {
        fieldsParser = mock(FieldsParser.class);
        beanFactory = mock(BeanFactory.class);
        fraudFilter = mock(FraudFilter.class);
        when(beanFactory.getBean(eq("MyParser"))).thenReturn(fieldsParser);
        when(beanFactory.getBean(eq("MyFraudFilter"))).thenReturn(fraudFilter);
        when(fraudFilter.tryMarkAsFraud(any())).thenAnswer(invocationOnMock -> {
            MyLogRecord logRecord = (MyLogRecord) invocationOnMock.getArguments()[0];
            if(logRecord.rowid.equals("must_be_filtered")) {
                logRecord.filter = 1003;
                return true;
            }
            else return false;
        });

        MetricsService metricsService = new MetricsService(new MetricRegistry());
        FieldParserHelper fieldsParser = new FieldParserHelper(metricsService, beanFactory);
        parserHelper = new ParserHelper(fieldsParser, metricsService, beanFactory);
    }

    @Test
    @UseDataProvider("parseDefaultDataProvider")
    public void testParseDefault(ImmutableMap<String, String> values) {
        MyLogRecord logRecord = parserHelper.parseDefault("must_be_filtered", new MyLogRecord(), values);

        assertThat(logRecord.rowid, is("must_be_filtered"));
        assertThat(logRecord.eventtime.getTime(), is(Long.parseLong(values.get("time")) * 1000L));
        assertThat(logRecord.intField, is(Integer.parseInt(values.get("intField"))));
        assertThat(logRecord.longField, is(values.get("longField") == null ? null : Long.parseLong(values.get("longField"))));
        assertThat(logRecord.decimalField.toString(), is(values.get("decimalField")));
        assertThat(logRecord.stringField, is(ParserUtil.truncateTo(values.get("stringField"), 3)));
        assertThat(logRecord.filter, is(1003));

        //FIXME wtf?
        //verify(fieldsParser).parse(anyMap(), anyObject());
    }

    @DataProvider
    public static Object[][] parseDefaultDataProvider_on_error() {
        return new Object[][]{
                new Object[]{ImmutableMap.of(
                        "intField", "123",
                        "longField", "123123123123123",
                        "decimalField", "10.28",
                        "stringField", "some string"
                )},
                new Object[]{ImmutableMap.of(
                        "time", "1431505963",
                        "intField", "123",
                        "decimalField", "invalid decimal",
                        "stringField", "some string"
                )},
                new Object[]{ImmutableMap.of(
                        "time", "1431505963",
                        "intField", "invalid int",
                        "decimalField", "10.28",
                        "stringField", "some string"
                )},
        };
    }

    @Test(expected = ParseException.class)
    @UseDataProvider("parseDefaultDataProvider_on_error")
    public void testParseDefault_on_error(ImmutableMap<String, String> values) {
        parserHelper.parseDefault("rowid1", new MyLogRecord(), values);
    }

    @Test
    public void testNullValue() throws ParseException {
        @Table(name = "my_log")
        class MyLogRecord implements LogRecord {
            @Rowid
            String rowid;
            @Copy(optional = true, nullValue = "-1")
            Integer intField;
            @Copy(optional = true, nullValue = "0")
            Long longField;
            @Copy(optional = true, nullValue = "0.0")
            BigDecimal decimalField;
            @Copy(optional = true, nullValue = "undefined")
            String stringField;
            @Copy(optional = true)
            String stringFieldNullable;
            @Copy(optional = true)
            String stringFieldNullableFromEmpty;

            @Override
            public Date getEventtime() {
                return null;
            }
        }

        MyLogRecord logRecord = parserHelper.parseDefault("rowid1", new MyLogRecord(), Collections.emptyMap());

        assertThat(logRecord.rowid, is("rowid1"));
        assertThat(logRecord.intField, is(-1));
        assertThat(logRecord.longField, is(0L));
        assertThat(logRecord.decimalField.toString(), is("0.0"));
        assertThat(logRecord.stringField, is("undefined"));
        assertThat(logRecord.stringFieldNullable, is(""));
    }

    @DataProvider
    public static Object[][] parseDefaultDataProvider_maxLength() {
        return new Object[][]{
                new Object[]{ImmutableMap.of(
                        "intField", "123",
                        "longField", "1234",
                        "decimalField", "12345.67",
                        "stringField", "123456"
                ), true},
                new Object[]{ImmutableMap.of(
                        "intField", "1234"
                ), false},
                new Object[]{ImmutableMap.of(
                        "longField", "12345"
                ), false},
                new Object[]{ImmutableMap.of(
                        "decimalField", "123456.67"
                ), false},
                new Object[]{ImmutableMap.of(
                        "decimalField", "1.234567"
                ), true},
                new Object[]{ImmutableMap.of(
                        "stringField", "1234567"
                ), false},
        };
    }

    @Test
    @UseDataProvider("parseDefaultDataProvider_maxLength")
    public void testMaxLength(ImmutableMap<String, String> values, boolean result) throws ParseException {
        @Table(name = "my_log")
        class MyLogRecord implements LogRecord {
            @Rowid
            String rowid;
            @Copy(maxLength = 3, optional = true)
            Integer intField;
            @Copy(maxLength = 4, optional = true)
            Long longField;
            @Copy(maxLength = 5, optional = true)
            BigDecimal decimalField;
            @Copy(maxLength = 6, optional = true)
            String stringField;

            @Override
            public Date getEventtime() {
                return null;
            }
        }

        try {
            parserHelper.parseDefault("rowid1", new MyLogRecord(), values);

            if (!result) {
                fail();
            }
        } catch (ParseException e) {
            if (result) {
                fail();
            }
        }

    }

    @DataProvider
    public static Object[][] parseDefaultDataProvider_skipLeadingZeroes() {
        return new Object[][]{
                new Object[]{"123", "123"},
                new Object[]{"0123", "123"},
                new Object[]{"000000000000", "0"},
                new Object[]{"", ""},
                new Object[]{null, ""},
        };
    }

    @Test
    @UseDataProvider("parseDefaultDataProvider_skipLeadingZeroes")
    public void testSkipLeadingZeroes(String value, String result) throws ParseException {
        @Table(name = "my_log")
        class MyLogRecord implements LogRecord {
            @Rowid
            String rowid;
            @Copy(optional = true, skipLeadingZeroes = true)
            String stringField;

            @Override
            public Date getEventtime() {
                return null;
            }
        }

        MyLogRecord logRecord = new MyLogRecord();
        parserHelper.parseDefault("rowid1", logRecord, value == null ? Collections.emptyMap() : ImmutableMap.of("stringField", value));

        assertThat(logRecord.stringField, is(result));
    }

    @Table(name = "my_log")
    @Parser("MyParser")
    @FraudFilters("MyFraudFilter")
    static class MyLogRecord implements HasStateAndFilter, LogRecord {
        @Rowid
        public String rowid;
        @Copy(column = "time")
        @DateTimeField(isTimestamp = true)
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

    public static String readLine(String resource) throws IOException {
        try (InputStream is = ParserHelperTest.class.getResourceAsStream(resource)) {
            return CharSource.wrap(CharStreams.toString(new InputStreamReader(is, "UTF-8"))).readFirstLine();
        }
    }
}
