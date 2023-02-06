package ru.yandex.common.report.graph;

import java.util.Arrays;
import java.util.List;

import junit.framework.Assert;
import junit.framework.TestCase;

import ru.yandex.common.report.graph.model.GraphReportAdapter;
import ru.yandex.common.util.StringUtils;

/**
 * @author Sergey Simonchik ssimonchik@yandex-team.ru
 */
public class ColumnIdentifyingTest extends TestCase {

    public void test1() {
        List<String> list = Arrays.asList("A", "X", "Y1", "Y2", "5", "y3");
        int xpos = GraphReportAdapter.findXColumnPosition(list);
        int count = StringUtils.countMatches(list, GraphReportAdapter.GRAPH_Y_COLUMN_REGEX);
        Assert.assertEquals(1, xpos);
        Assert.assertEquals(3, count);
    }

    public void test2() {
        List<String> list = Arrays.asList("x", "y1123", "Y");
        int xpos = GraphReportAdapter.findXColumnPosition(list);
        int count = StringUtils.countMatches(list, GraphReportAdapter.GRAPH_Y_COLUMN_REGEX);
        Assert.assertEquals(0, xpos);
        Assert.assertEquals(2, count);
    }

    public void test3() {
        List<String> list = Arrays.asList("y1123", "Y0", "X");
        int xpos = GraphReportAdapter.findXColumnPosition(list);
        int count = StringUtils.countMatches(list, GraphReportAdapter.GRAPH_Y_COLUMN_REGEX);
        Assert.assertEquals(2, xpos);
        Assert.assertEquals(2, count);
    }

    public void test4() {
        List<String> list = Arrays.asList("ya", "Y0egl", "Xeigue");
        int xpos = GraphReportAdapter.findXColumnPosition(list);
        int count = StringUtils.countMatches(list, GraphReportAdapter.GRAPH_Y_COLUMN_REGEX);
        Assert.assertEquals(2, xpos);
        Assert.assertEquals(2, count);
    }

    public void test5() {
        List<String> list = Arrays.asList("yandex", "Y#@!", "X_!eigue");
        int xpos = GraphReportAdapter.findXColumnPosition(list);
        int count = StringUtils.countMatches(list, GraphReportAdapter.GRAPH_Y_COLUMN_REGEX);
        Assert.assertEquals(2, xpos);
        Assert.assertEquals(2, count);
    }

    public void test6() {
        List<String> list = Arrays.asList("yandex", "Y#@!", "_!eigue");
        try {
            int xpos = GraphReportAdapter.findXColumnPosition(list);
        } catch (Exception e) {
        }
        int count = StringUtils.countMatches(list, GraphReportAdapter.GRAPH_Y_COLUMN_REGEX);
        Assert.assertEquals(2, count);
    }
}
