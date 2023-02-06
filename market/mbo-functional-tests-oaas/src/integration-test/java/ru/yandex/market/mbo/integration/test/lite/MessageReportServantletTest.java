package ru.yandex.market.mbo.integration.test.lite;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;

import ru.yandex.common.framework.filter.SimpleQueryFilter;
import ru.yandex.common.framework.pager.Pager;
import ru.yandex.common.util.xml.XmlConvertable;
import ru.yandex.market.mbo.core.report.MessageReportServantlet;
import ru.yandex.market.mbo.core.report.ReportRowCallbackHandler;
import ru.yandex.market.mbo.core.report.filter.AbstractFilterDescriptor;
import ru.yandex.market.mbo.core.report.filter.EnumSqlFilterDescriptor;
import ru.yandex.market.mbo.core.report.filter.SelectorOperator;
import ru.yandex.market.mbo.core.report.summary.SummaryElement;
import ru.yandex.market.mbo.core.report.summary.SummaryFunction;
import ru.yandex.market.mbo.integration.test.BasicOracleTest;

@SuppressWarnings("checkstyle:MagicNumber")
public class MessageReportServantletTest extends BasicOracleTest {
    private MessageReportServantlet servantlet = new MessageReportServantlet();

    @Before
    public void setUp() {
        List<AbstractFilterDescriptor> filters = new ArrayList<>();
        filters.add(new EnumSqlFilterDescriptor("type", "descr", SelectorOperator.EQUALS));
        servantlet.setTable("ng_sys_message");
        servantlet.setJdbcTemplate(siteCatalogJdbcTemplate);
        servantlet.setFilters(filters);

        Calendar time = Calendar.getInstance();
        time.setTimeInMillis(0);
        String[] messages = new String[] {"test_message1", "test_message2", "test_message3"};
        int[] types = new int[] {0, 1, 2};
        siteCatalogJdbcTemplate.batchUpdate(
            "INSERT INTO NG_SYS_MESSAGE (ID, TYPE, TIME, MESSAGE) VALUES (?, ?, ?, ?)",
            new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    ps.setInt(1, i);
                    ps.setInt(2, types[i]);
                    ps.setTime(3, new java.sql.Time(time.getTime().getTime()));
                    ps.setString(4, messages[i]);
                }

                @Override
                public int getBatchSize() {
                    return messages.length;
                }
            }
        );
    }

    @Test
    public void testQueryForItems() {
        List<String> expected = new ArrayList<>();
        Calendar time = Calendar.getInstance();
        time.setTimeInMillis(0);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
        String date = dateFormat.format(time.getTime());
        expected.add("<report-item><id>0</id><type>0</type><time>" + date + "</time>" +
            "<message>test_message1</message><descr>System message</descr></report-item>");
        expected.add("<report-item><id>1</id><type>1</type><time>" + date + "</time>" +
            "<message>test_message2</message><descr>Action tracker info message</descr></report-item>");
        expected.add("<report-item><id>2</id><type>2</type><time>" + date + "</time>" +
            "<message>test_message3</message><descr>Action tracker warn message</descr></report-item>");

        SimpleQueryFilter queryFilter = new SimpleQueryFilter();
        Pager pager = new Pager(0, 10, true);
        String conditions = servantlet.getConditions(queryFilter, null, Collections.emptyMap());
        String orderSuf = "";
        String query = servantlet.getQuery(null, conditions, pager, orderSuf);
        ReportRowCallbackHandler<? extends XmlConvertable> rowCallbackHandler = servantlet.getReportRowHandler(pager);

        List<? extends XmlConvertable> res = servantlet.queryForItems(siteCatalogJdbcTemplate, null,
            Collections.emptyMap(), queryFilter, query, rowCallbackHandler);

        for (int i = 0; i < expected.size(); i++) {
            XmlConvertable item = res.get(i);
            StringBuilder builder = new StringBuilder();
            item.toXml(builder);
            Assert.assertEquals(expected.get(i), builder.toString());
        }
    }

    @Test
    public void testFetchMetaData() {
        Map<String, String> expected = ImmutableMap.<String, String>builder()
            .put("id", "id")
            .put("type", "type")
            .put("descr", "descr")
            .put("time", "time")
            .put("message", "message")
            .build();

        Map<String, String> actual = servantlet.fetchMetaData(null);

        for (String key : expected.keySet()) {
            Assert.assertEquals(expected.get(key), actual.getOrDefault(key, ""));
        }
        Set<String> extraKeys = actual.keySet();
        extraKeys.removeAll(expected.keySet());
        if (extraKeys.size() > 0) {
            Assert.fail("Actual map contains extra keys");
        }
    }

    @Test
    public void testGetSessionSummary() {
        List<SummaryElement> summary = new ArrayList<>();
        summary.add(new SummaryElement("ID", SummaryFunction.SUM));
        summary.add(new SummaryElement("DESCR", SummaryFunction.COUNT));
        servantlet.setSummary(summary);
        List<String> expected = new ArrayList<>();
        expected.add("<column><name>id</name>\n<value>3</value>\n</column>");
        expected.add("<column><name>descr</name>\n<value>3</value>\n</column>");

        SimpleQueryFilter queryFilter = new SimpleQueryFilter();
        String conditions = servantlet.getConditions(queryFilter, null, Collections.emptyMap());
        String orderSuf = "";
        Pager pager = new Pager(0, 10, true);

        List<SummaryElement> actual = servantlet.getSessionSummary(null, conditions, pager, orderSuf);

        Assert.assertEquals("Expected size is " + expected.size() + ". Actual size is " + actual.size(),
            expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++) {
            SummaryElement element = actual.get(i);
            StringBuilder builder = new StringBuilder();
            element.toXml(builder);
            Assert.assertEquals(expected.get(i), builder.toString());
        }
    }

    @Test
    public void testFillOptions() {
        String expected = "<filter><name>descr</name>\n" +
            "<type>ENUM</type>\n" +
            "<options><option><value>0</value>\n" +
            "<text>System message</text>\n" +
            "</option><option><value>1</value>\n" +
            "<text>Action tracker info message</text>\n" +
            "</option><option><value>2</value>\n" +
            "<text>Action tracker warn message</text>\n" +
            "</option></options></filter>";
        EnumSqlFilterDescriptor descriptor = new EnumSqlFilterDescriptor(
            "type", "descr", SelectorOperator.EQUALS);

        servantlet.fillOptions(descriptor, null, Collections.emptyMap(), "", new Object[0]);

        StringBuilder builder = new StringBuilder();
        descriptor.toXml(builder);
        Assert.assertEquals(expected, builder.toString());
    }
}
