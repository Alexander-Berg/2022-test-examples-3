package ru.yandex.market.graphite;

import junit.framework.Assert;
import org.junit.Test;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"></a>
 * @date 03/06/15
 */
public class MetricValidatorTest {


    @Test
    public void testValidate() throws Exception {
        invalid("-嘊-嘊嘍-嘍-aaa_.._tt_personal-billing-report_xml.0_995");
        invalid("market.mbo-front-iva.timings-dynamic.3febfdd52d4fea02xxx'x22<>_.._tt_personal-billing-report_xml_" +
            ".0_995");
        invalid("market.mbo-front-iva.timings-dynamic.tt_personal-billing-report_xml_3febfdd52d4fea02xxx'x22<>_.." +
            ".0_99");

        invalid("gdsgsgs");
        invalid("one_min.fdsfdsfs..fdsfsfsd");
        invalid("one_min.fdsfdsfs.fdsfsfsd.");
        invalid(".one_min.fdsfdsfs.fdsfsfsd");
        invalid("one_min..x");
        invalid("one_min.x.x.d.d.d.d.d.d.x.x.x.x.d.x.d.d");
        invalid("ten_min.fdsfdsfs.fdsfsfsd");
        valid("one_min.fdsfdsfs.fdsfsfsd");
    }

    private void valid(String metric) {
        Assert.assertTrue(MetricValidator.DEFAULT.validate(metric));
    }

    private void invalid(String metric) {
        Assert.assertFalse(MetricValidator.DEFAULT.validate(metric));
    }
}
