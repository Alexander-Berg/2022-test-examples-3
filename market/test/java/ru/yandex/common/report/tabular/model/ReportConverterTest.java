package ru.yandex.common.report.tabular.model;

import org.jdom2.Element;
import org.jdom2.output.XMLOutputter;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author imelnikov
 */
public class ReportConverterTest {

    @Test
    public void testVaribles() {
        ReportConverter c = new ReportConverter();
        List<Cell> cells = Arrays.asList(
                new StringCell("a"),
                new BigDecimalCell(BigDecimal.valueOf(10.3)),
                new StringCell("text"));
        ReportQueryInfo queryInfo = new ReportQueryInfo();
        QueryResultInfo col1 = new QueryResultInfo("");
        col1.setVariable("a");
        QueryResultInfo col2 = new QueryResultInfo("");
        col2.setVariable("b");
        QueryResultInfo col3 = new QueryResultInfo("title3");
        col3.setLinkFormat("var1 ${a} var2 ${b} col3 %s");
        queryInfo.setResultInfoList(Arrays.asList(col1, col2, col3));

        Element xml = new Element("queryInfo");
        c.addRow(queryInfo, xml, "row", new Row(cells));

        XMLOutputter xmlOutputter = new XMLOutputter();

        String report = "<queryInfo><row>" +
                "<cell>a</cell><cell>10.3</cell><cell link=\"var1 a var2 10.3 col3 text\">text</cell>" +
                "</row></queryInfo>";
        assertEquals(report, xmlOutputter.outputString(xml));
    }

    @Test
    public void withoutVariables() {
        ReportConverter report = new ReportConverter();

        List<Cell> cells = Arrays.asList(
                new StringCell("a"));

        ReportQueryInfo queryInfo = new ReportQueryInfo();
        QueryResultInfo col = new QueryResultInfo("");
        col.setLinkFormat("a = %s");
        queryInfo.setResultInfoList(Arrays.asList(col));

        Element xml = new Element("queryInfo");
        report.addRow(queryInfo, xml, "row", new Row(cells));

        XMLOutputter xmlOutputter = new XMLOutputter();
        assertEquals("<queryInfo><row><cell link=\"a = a\">a</cell></row></queryInfo>", xmlOutputter.outputString(xml));
    }
}
