package ru.yandex.market.mbo.cms.core.utils;

import java.util.HashMap;
import java.util.Map;

import io.qameta.allure.Issue;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.mbo.cms.core.json.service.exceptions.JsonValidationException;
import ru.yandex.market.mbo.cms.core.models.Constants;

public class CmsTemplateUtilTest {

    @Test
    @Issue("MBO-14994")
    public void createValueTemplateDefaultVariablesTest() {
        Map<String, String> v1PhoneXml = CmsTemplateUtil.createValueTemplateDefaultVariables(
                "v1", Constants.Device.PHONE, Constants.Format.XML
        );
        Map<String, String> v2DesktopJson = CmsTemplateUtil.createValueTemplateDefaultVariables(
                "v2", Constants.Device.DESKTOP, Constants.Format.JSON
        );
        Map<String, String> v3DesktopJson = CmsTemplateUtil.createValueTemplateDefaultVariables(
                "v3", Constants.Device.TABLET, Constants.Format.JSON
        );

        Assert.assertEquals("v1", v1PhoneXml.get(CmsTemplateUtil.VALUE_TEMPLATE_PLACEHOLDER_VALUE));
        Assert.assertEquals("phone", v1PhoneXml.get(CmsTemplateUtil.VALUE_TEMPLATE_PLACEHOLDER_DEVICE));
        Assert.assertEquals("xml", v1PhoneXml.get(CmsTemplateUtil.VALUE_TEMPLATE_PLACEHOLDER_FORMAT));

        Assert.assertEquals("v2", v2DesktopJson.get(CmsTemplateUtil.VALUE_TEMPLATE_PLACEHOLDER_VALUE));
        Assert.assertEquals("desktop", v2DesktopJson.get(CmsTemplateUtil.VALUE_TEMPLATE_PLACEHOLDER_DEVICE));
        Assert.assertEquals("json", v2DesktopJson.get(CmsTemplateUtil.VALUE_TEMPLATE_PLACEHOLDER_FORMAT));

        Assert.assertEquals("v3", v3DesktopJson.get(CmsTemplateUtil.VALUE_TEMPLATE_PLACEHOLDER_VALUE));
        Assert.assertEquals("tablet", v3DesktopJson.get(CmsTemplateUtil.VALUE_TEMPLATE_PLACEHOLDER_DEVICE));
        Assert.assertEquals("json", v3DesktopJson.get(CmsTemplateUtil.VALUE_TEMPLATE_PLACEHOLDER_FORMAT));
    }

    @Test
    @Issue("MBO-14994")
    public void applySingleValueTemplateTest() {
        String template = "__p1__ __p2____param3__";
        Map<String, String> vars = new HashMap<>();

        Assert.assertEquals(
                "__p1__ __p2____param3__",
                CmsTemplateUtil.applySingleValueTemplate(template, vars)
        );
        vars.put("__p1__", "Hello");
        Assert.assertEquals(
                "Hello __p2____param3__",
                CmsTemplateUtil.applySingleValueTemplate(template, vars)
        );
        vars.put("__p2__", "world");
        Assert.assertEquals(
                "Hello world__param3__",
                CmsTemplateUtil.applySingleValueTemplate(template, vars)
        );
        vars.put("__param3__", "!!!");
        Assert.assertEquals(
                "Hello world!!!",
                CmsTemplateUtil.applySingleValueTemplate(template, vars)
        );
    }

    @Test(expected = JsonValidationException.class)
    public void sanitizeDoesNotHideException() throws Exception {
        CmsTemplateUtil.sanitizeJson("dummy", true);
    }

    @Test
    public void sanitizeNulls() throws Exception {
        Assert.assertEquals("{\"x\":[1,2]}",
                CmsTemplateUtil.sanitizeJson("{\"x\":[1,null,2], \"y\": null}", true));
    }
}
