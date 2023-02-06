package ru.yandex.market.delivery.mdbapp.configuration;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mockito;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import steps.outletSteps.OutletSteps;
import steps.shopSteps.ShopSteps;

import ru.yandex.market.mbi.api.client.entity.outlets.Outlet;
import ru.yandex.market.mbi.api.client.entity.shops.Shop;

@RunWith(value = Parameterized.class)
public class XmlConverterTest {
    private final HttpMessageConverter converter = new Jaxb2RootElementHttpMessageConverter();

    @Parameter
    public Class<?> clazz;

    @Parameter(1)
    public String simpleClassName;

    @Parameter(2)
    public Object result;

    @Parameters(name = "{1}")
    public static Collection<Object[]> getParameters() {
        return Arrays.asList(new Object[][]{
            {
                Outlet.class,
                Outlet.class.getSimpleName(),
                OutletSteps.getDefaultOutlet()
            },
            {Shop.class, Shop.class.getSimpleName(), ShopSteps.getDefaultShop()}
        });
    }

    @Test
    public void canReadXmlConvertTest() {
        String message = "Can't read from xml class " + simpleClassName;

        Assert.assertTrue(
            message,
            converter.canRead(clazz, MediaType.APPLICATION_XML)
        );
    }

    @Test
    public void convertXmlTest() throws IOException {
        String path = "/" + simpleClassName.toLowerCase() + ".xml";

        HttpInputMessage inputMessage = Mockito.mock(HttpInputMessage.class);
        Mockito.when(inputMessage.getBody()).thenReturn(XmlConverterTest.class.getResourceAsStream(path));

        Object defaultOutletObject = converter.read(clazz, inputMessage);

        Assert.assertEquals(result.toString(), defaultOutletObject.toString());
    }
}
