package ru.yandex.market.tsum.pipelines.front.jobs.logs;

import java.util.ArrayList;
import java.util.Date;

import com.google.common.collect.LinkedListMultimap;
import org.junit.Test;

import ru.yandex.misc.test.Assert;

public class ApplicationLogsReportBuilderTest {

    @Test
    public void groupByCodeEmpty() throws Exception {
        ArrayList<ApplicationLogsRecord> applicationLogsRecords = new ArrayList<>();

        LinkedListMultimap<String, Integer> expected = LinkedListMultimap.create();

        Assert.isTrue(ApplicationLogsReportBuilder.groupByCodes(applicationLogsRecords).equals(expected));
    }

    @Test
    public void groupByCodeTwoDimensions() throws Exception {
        ArrayList<ApplicationError> applicationErrors1 = new ArrayList<>();
        applicationErrors1.add(new ApplicationError("D", 8));
        applicationErrors1.add(new ApplicationError("A", 10));
        applicationErrors1.add(new ApplicationError("B", 9));
        applicationErrors1.add(new ApplicationError("Z", 88));

        ArrayList<ApplicationError> applicationErrors2 = new ArrayList<>();
        applicationErrors2.add(new ApplicationError("B", 8));
        applicationErrors2.add(new ApplicationError("C", 27));
        applicationErrors2.add(new ApplicationError("A", 11));

        ArrayList<ApplicationLogsRecord> applicationLogsRecords = new ArrayList<>();
        applicationLogsRecords.add(buildApplicationLogsRecord(applicationErrors1));
        applicationLogsRecords.add(buildApplicationLogsRecord(applicationErrors2));

        LinkedListMultimap<String, Integer> expected = LinkedListMultimap.create();
        expected.put("Z", 88);
        expected.put("Z", 0);
        expected.put("C", 0);
        expected.put("C", 27);
        expected.put("A", 10);
        expected.put("A", 11);
        expected.put("B", 9);
        expected.put("B", 8);
        expected.put("D", 8);
        expected.put("D", 0);

        Assert.isTrue(ApplicationLogsReportBuilder.groupByCodes(applicationLogsRecords).equals(expected));
    }

    @Test
    public void groupByCodeFirstResultEmpty() throws Exception {
        ArrayList<ApplicationError> applicationErrors1 = new ArrayList<>();

        ArrayList<ApplicationError> applicationErrors2 = new ArrayList<>();
        applicationErrors2.add(new ApplicationError("C", 1));
        applicationErrors2.add(new ApplicationError("B", 2));
        applicationErrors2.add(new ApplicationError("A", 3));

        ArrayList<ApplicationLogsRecord> applicationLogsRecords = new ArrayList<>();
        applicationLogsRecords.add(buildApplicationLogsRecord(applicationErrors1));
        applicationLogsRecords.add(buildApplicationLogsRecord(applicationErrors2));

        LinkedListMultimap<String, Integer> expected = LinkedListMultimap.create();
        expected.put("A", 0);
        expected.put("A", 3);
        expected.put("B", 0);
        expected.put("B", 2);
        expected.put("C", 0);
        expected.put("C", 1);

        Assert.isTrue(ApplicationLogsReportBuilder.groupByCodes(applicationLogsRecords).equals(expected));
    }

    private ApplicationLogsRecord buildApplicationLogsRecord(ArrayList<ApplicationError> applicationErrors) {
        return new ApplicationLogsRecord(
            "tableName", new Date(), "PRESTABLE", "regression", applicationErrors
        );
    }

}
