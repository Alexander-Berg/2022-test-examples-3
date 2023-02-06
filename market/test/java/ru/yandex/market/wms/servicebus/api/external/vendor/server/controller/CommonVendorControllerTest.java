package ru.yandex.market.wms.servicebus.api.external.vendor.server.controller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;
import org.mockito.invocation.Invocation;

import ru.yandex.market.wms.servicebus.IntegrationTest;

public class CommonVendorControllerTest extends IntegrationTest {

    protected static final String TRANSPORT_UNIT_TRACKING_QUEUE = "{mq}_{wrh}_transport-unit-tracking";
    protected static final String TRANSPORT_UNIT_TRACKING_LOG_QUEUE = "{mq}_{wrh}_transport-unit-tracking-log";
    protected static final String TRANSPORTER_PUSH_DIMENSION = "{mq}_{wrh}_transporter-push-dimension";

    protected <T> void assertionActualInvocation(Object mockService,
                                                 String method,
                                                 int indexOfParameter,
                                                 T expectedDTO) {
        List<T> trackingDTOs = new ArrayList<>();
        Collection<Invocation> invocations = Mockito.mockingDetails(mockService).getInvocations();
        invocations.stream()
                .filter(inv -> inv.getMethod().getName().equals(method))
                .forEach(inv -> {
                    trackingDTOs.add(inv.getArgument(indexOfParameter));
                });
        Assertions.assertEquals(1, trackingDTOs.stream().filter(f -> f.equals(expectedDTO)).count());
    }
}
