package ru.yandex.chemodan.uploader.log;

import org.junit.Test;

import ru.yandex.misc.test.Assert;


/**
 * @author akirakozov
 */
public class EventsTest {

    @Test
    public void decorateCause() {
        String result = Events.formatCauseForLogging("pish-pish\nbatman\nHelloWorld!\ttabs\talso");
        Assert.equals("\"pish-pish batman HelloWorld! tabs also\"", result);
    }

}
