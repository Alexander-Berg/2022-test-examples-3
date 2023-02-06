package ru.yandex.market.mbo.cms.core.utils.schema.changeset;

import java.util.Collections;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.mbo.cms.core.models.Constants;

public class TemplatesOperationsMinusTest {

    @Test
    public void testNullMinusNull() {
        Assert.assertNull(TemplateOperations.templatesMinus(null, null));
    }

    @Test
    public void testNullMinusTemplate() {
        Map<Constants.Device, Map<Constants.Format, String>> tem = TemplateOperations.templatesMinus(
                null,
                template(Constants.Device.DESKTOP, Constants.Format.JSON, "tem")
        );
        Assert.assertNull(tem);
    }

    @Test
    public void testTemplateMinusNull() {
        Map<Constants.Device, Map<Constants.Format, String>> tem = TemplateOperations.templatesMinus(
                template(Constants.Device.DESKTOP, Constants.Format.JSON, "tem"),
                null
        );
        Assert.assertNotNull(tem);
        Assert.assertEquals(1, tem.get(Constants.Device.DESKTOP).size());
        Assert.assertEquals("tem", tem.get(Constants.Device.DESKTOP).get(Constants.Format.JSON));
    }

    @Test
    public void testTemplateMinusTemplate() {
        Map<Constants.Device, Map<Constants.Format, String>> tem = TemplateOperations.templatesPlus(
                template(Constants.Device.DESKTOP, Constants.Format.JSON, "temdj"),
                template(Constants.Device.PHONE, Constants.Format.JSON, "tempj")
        );
        tem = TemplateOperations.templatesPlus(
                tem,
                template(Constants.Device.DESKTOP, Constants.Format.XML, "temdx")
        );
        tem = TemplateOperations.templatesPlus(
                tem,
                template(Constants.Device.PHONE, Constants.Format.XML, "tempx")
        );
        tem = TemplateOperations.templatesMinus(
                tem,
                template(Constants.Device.PHONE, Constants.Format.XML, "tempx")
        );

        Assert.assertNotNull(tem);
        Assert.assertEquals(2, tem.get(Constants.Device.DESKTOP).size());
        Assert.assertEquals("temdj", tem.get(Constants.Device.DESKTOP).get(Constants.Format.JSON));
        Assert.assertEquals("temdx", tem.get(Constants.Device.DESKTOP).get(Constants.Format.XML));
        Assert.assertEquals(1, tem.get(Constants.Device.PHONE).size());
        Assert.assertEquals("tempj", tem.get(Constants.Device.PHONE).get(Constants.Format.JSON));

        tem = TemplateOperations.templatesMinus(
                tem,
                template(Constants.Device.PHONE, Constants.Format.JSON, "tt")
        );
        Assert.assertNotNull(tem);
        Assert.assertEquals(1, tem.size());
        Assert.assertEquals(2, tem.get(Constants.Device.DESKTOP).size());
        Assert.assertEquals("temdj", tem.get(Constants.Device.DESKTOP).get(Constants.Format.JSON));
        Assert.assertEquals("temdx", tem.get(Constants.Device.DESKTOP).get(Constants.Format.XML));

        tem = TemplateOperations.templatesMinus(
                tem,
                template(Constants.Device.DESKTOP, Constants.Format.JSON, "tt")
        );
        Assert.assertNotNull(tem);
        Assert.assertEquals(1, tem.size());
        Assert.assertEquals(1, tem.get(Constants.Device.DESKTOP).size());
        Assert.assertEquals("temdx", tem.get(Constants.Device.DESKTOP).get(Constants.Format.XML));

        tem = TemplateOperations.templatesMinus(
                tem,
                template(Constants.Device.DESKTOP, Constants.Format.XML, "tt")
        );
        Assert.assertNotNull(tem);
        Assert.assertEquals(0, tem.size());
    }

    private Map<Constants.Device, Map<Constants.Format, String>> template(
            Constants.Device device, Constants.Format format, String template
    ) {
        return Collections.singletonMap(device, Collections.singletonMap(format, template));
    }

}
