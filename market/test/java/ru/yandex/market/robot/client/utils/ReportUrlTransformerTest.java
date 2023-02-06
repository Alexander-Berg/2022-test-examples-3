package ru.yandex.market.robot.client.utils;

import org.junit.Assert;
import org.junit.Test;

public class ReportUrlTransformerTest {
    @Test
    public void urlWithoutParamsIsRetainedAsIs() {
        String transformedHash = ReportUrlTransformer.transform(new String[]{"reports", "gcSingleTicket"});
        Assert.assertEquals("reports/gcSingleTicket", transformedHash);
    }

    @Test
    public void validParamsAreTransformedToCamelCase() {
        String transformedHash = ReportUrlTransformer.transform(
            new String[]{"reports", "gcSingleTicket", "request_id=34&process_id=12&status=CANCELED"}
        );
        Assert.assertEquals(
            "reports/gcSingleTicket?requestId=34&processId=12&status=CANCELED",
            transformedHash);
    }

    @Test
    public void multiWordParamNamesAreTransformedToCamelCase() {
        String transformedHash = ReportUrlTransformer.transform(
            new String[]{"reports", "gcSingleTicket", "some_param_with_long_name=34"}
        );
        Assert.assertEquals(
            "reports/gcSingleTicket?someParamWithLongName=34",
            transformedHash);
    }

    @Test
    public void urlWithUnexpectedPrefixIsReplacedWithReportsRoot() {
        String transformedHash = ReportUrlTransformer.transform(
            new String[]{"something", "gcSingleTicket", "request_id=123"}
        );
        Assert.assertEquals("reports", transformedHash);
    }

    @Test
    public void urlOfUnexpectedFormIsTrimmedToReport() {
        String transformedHash = ReportUrlTransformer.transform(
            new String[]{"reports", "gcSingleTicket", "request_id=123", "and", "more"}
        );
        Assert.assertEquals("reports/gcSingleTicket", transformedHash);
    }

    @Test
    public void paramWithoutValueIsDropped() {
        String transformedHash = ReportUrlTransformer.transform(
            new String[]{"reports", "gcSingleTicket", "request_id=34&param&process_id=12&status=CANCELED"}
        );
        Assert.assertEquals(
            "reports/gcSingleTicket?requestId=34&processId=12&status=CANCELED",
            transformedHash);
    }

    @Test
    public void paramWithEmptyValueIsDropped() {
        String transformedHash = ReportUrlTransformer.transform(
            new String[]{"reports", "gcSingleTicket", "request_id=34&param_=&process_id=12&status=CANCELED"}
        );
        Assert.assertEquals(
            "reports/gcSingleTicket?requestId=34&processId=12&status=CANCELED",
            transformedHash);
    }

    @Test
    public void trailingAmpersandIsDropped() {
        String transformedHash = ReportUrlTransformer.transform(
            new String[]{"reports", "gcSingleTicket", "request_id=34&process_id=12&"}
        );
        Assert.assertEquals(
            "reports/gcSingleTicket?requestId=34&processId=12",
            transformedHash);
    }

    @Test
    public void prefixAmpersandIsDropped() {
        String transformedHash = ReportUrlTransformer.transform(
            new String[]{"reports", "gcSingleTicket", "&request_id=34&process_id=12&"}
        );
        Assert.assertEquals(
            "reports/gcSingleTicket?requestId=34&processId=12",
            transformedHash);
    }

    @Test
    public void paramWithInvalidNameIsDropped() {
        String transformedHash = ReportUrlTransformer.transform(
            new String[]{"reports", "gcSingleTicket", "&__=34&process_id=12&"}
        );
        Assert.assertEquals(
            "reports/gcSingleTicket?processId=12",
            transformedHash);
    }


    @Test
    public void paramWithInvalidValueIsDropped() {
        String transformedHash = ReportUrlTransformer.transform(
            new String[]{"reports", "gcSingleTicket", "request_id=12=34&process_id=12"}
        );
        Assert.assertEquals(
            "reports/gcSingleTicket?processId=12",
            transformedHash);
    }
}
