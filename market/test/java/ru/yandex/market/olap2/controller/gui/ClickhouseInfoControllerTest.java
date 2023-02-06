package ru.yandex.market.olap2.controller.gui;

import junit.framework.TestCase;
import lombok.SneakyThrows;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.olap2.dao.ClickhouseWebService;
import ru.yandex.market.olap2.model.EnvironmentDetector;


@RunWith(MockitoJUnitRunner.class)
public class ClickhouseInfoControllerTest extends TestCase {
    @Mock
    private ClickhouseWebService chService;
    @Mock
    private CloseableHttpClient httpClient;

    private ClickhouseInfoController controller;

    public void init(String environment) {
        EnvironmentDetector environmentDetector=new EnvironmentDetector(environment);
        controller = new ClickhouseInfoController(chService, httpClient, environmentDetector,
                "qwerty", "qwerty", "qwerty", null, null);
        Mockito.reset(chService, httpClient);
    }

    @Test(expected = UnsupportedOperationException.class)
    @SneakyThrows
    public void testCopyTableSchemaFromProdWithException() {
        // Метод для теста, на проде вызываться не должен никогда, должен падать
        init("production");
        controller.copyTableSchemaFromProd("cube_show_click_banner");
    }
}
