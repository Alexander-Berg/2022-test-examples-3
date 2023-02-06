package ru.yandex.travel.hotels.searcher.partners;

import org.junit.Test;

import ru.yandex.travel.hotels.proto.EPartnerId;
import ru.yandex.travel.hotels.proto.ERequestClass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class AbstractPartnerTaskHandlerTests {

    @Test
    public void checkQueueConsumerForEachRequestClass() {
        var maps = AbstractPartnerTaskHandler.queueConsumersForRequestClasses(
                new AbstractPartnerTaskHandlerProperties() {
                },
                EPartnerId.PI_BNOVO,
                t -> { // dummy rate limit action
                },
                t -> { // dummy concurrency limit action
                },
                (a, b, c) -> { // dummy startNow method
                }
        );
        assertEquals("Not enough or too many queue executors, check that all ERequestClass-es are mapped",
                ERequestClass.values().length - 1, maps.size());
        assertFalse("No need to map UNRECOGNIZED ERequestClass", maps.containsKey(ERequestClass.UNRECOGNIZED));
    }
}
