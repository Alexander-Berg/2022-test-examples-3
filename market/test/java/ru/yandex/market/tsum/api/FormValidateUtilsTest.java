package ru.yandex.market.tsum.api;

import org.junit.Test;
import ru.yandex.market.tsum.balancer.utils.FormValidateUtils;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;


public class FormValidateUtilsTest {

    @Test
    public void checkPortsTest() {
        String httpPort = "80";
        String httpsPort = "443";

        List<String> errors = FormValidateUtils.checkPorts(httpPort, httpsPort);
        assertEquals(errors, new ArrayList<>());

        httpPort = "80";
        httpsPort = "80";

        errors = FormValidateUtils.checkPorts(httpPort, httpsPort);
        List<String> expectErrors = new ArrayList<>();
        expectErrors.add("Http port and https port both have the same value, but have to is different.");
        assertEquals(errors, expectErrors);
    }

}