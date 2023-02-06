package ru.yandex.market.mbo.cms.core.utils.schema.changeset;

import java.util.Collections;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.mbo.cms.core.models.Constants;

public class TemplatesOperationsPlusTest {

    @Test
    public void testNullPlusNull() {
        Assert.assertNull(TemplateOperations.templatesPlus(null, null));
    }

    @Test
    public void testNullPlusTemplate() {
        Map<Constants.Device, Map<Constants.Format, String>> tem = TemplateOperations.templatesPlus(
                null,
                template(Constants.Device.DESKTOP, Constants.Format.JSON, "tem")
        );
        Assert.assertNotNull(tem);
        Assert.assertEquals(1, tem.get(Constants.Device.DESKTOP).size());
        Assert.assertEquals("tem", tem.get(Constants.Device.DESKTOP).get(Constants.Format.JSON));
    }

    @Test
    public void testTemplatePlusNull() {
        Map<Constants.Device, Map<Constants.Format, String>> tem = TemplateOperations.templatesPlus(
                template(Constants.Device.DESKTOP, Constants.Format.JSON, "tem"),
                null
        );
        Assert.assertNotNull(tem);
        Assert.assertEquals(1, tem.get(Constants.Device.DESKTOP).size());
        Assert.assertEquals("tem", tem.get(Constants.Device.DESKTOP).get(Constants.Format.JSON));
    }

    @Test
    public void testTemplatePlusTemplate() {
        Map<Constants.Device, Map<Constants.Format, String>> tem = TemplateOperations.templatesPlus(
                template(Constants.Device.DESKTOP, Constants.Format.JSON, "tem1"),
                template(Constants.Device.DESKTOP, Constants.Format.JSON, "tem2")
        );
        Assert.assertNotNull(tem);
        Assert.assertEquals(1, tem.get(Constants.Device.DESKTOP).size());
        Assert.assertEquals("tem2", tem.get(Constants.Device.DESKTOP).get(Constants.Format.JSON));

        tem = TemplateOperations.templatesPlus(
                template(Constants.Device.DESKTOP, Constants.Format.JSON, "temj"),
                template(Constants.Device.DESKTOP, Constants.Format.XML, "temx")
        );
        Assert.assertNotNull(tem);
        Assert.assertEquals(2, tem.get(Constants.Device.DESKTOP).size());
        Assert.assertEquals("temj", tem.get(Constants.Device.DESKTOP).get(Constants.Format.JSON));
        Assert.assertEquals("temx", tem.get(Constants.Device.DESKTOP).get(Constants.Format.XML));

        tem = TemplateOperations.templatesPlus(
                template(Constants.Device.DESKTOP, Constants.Format.JSON, "temd"),
                template(Constants.Device.PHONE, Constants.Format.JSON, "temp1")
        );
        tem = TemplateOperations.templatesPlus(
                tem,
                template(Constants.Device.PHONE, Constants.Format.JSON, "temp2")
        );
        tem = TemplateOperations.templatesPlus(
                tem,
                template(Constants.Device.PHONE, Constants.Format.XML, "tempx")
        );
        Assert.assertNotNull(tem);
        Assert.assertEquals(1, tem.get(Constants.Device.DESKTOP).size());
        Assert.assertEquals("temd", tem.get(Constants.Device.DESKTOP).get(Constants.Format.JSON));
        Assert.assertEquals(2, tem.get(Constants.Device.PHONE).size());
        Assert.assertEquals("temp2", tem.get(Constants.Device.PHONE).get(Constants.Format.JSON));
        Assert.assertEquals("tempx", tem.get(Constants.Device.PHONE).get(Constants.Format.XML));
    }

    private Map<Constants.Device, Map<Constants.Format, String>> template(
            Constants.Device device, Constants.Format format, String template
    ) {
        return Collections.singletonMap(device, Collections.singletonMap(format, template));
    }

}
