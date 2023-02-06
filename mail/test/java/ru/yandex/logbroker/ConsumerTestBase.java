package ru.yandex.logbroker;

import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import ru.yandex.http.test.StaticHttpResource;
import ru.yandex.http.test.StaticServer;
import ru.yandex.test.util.TestBase;

public class ConsumerTestBase extends TestBase {
    protected void waitForRequest(
        final StaticServer server,
        final String uri,
        final int timeout)
        throws Exception
    {
        waitForRequests((r) -> server.accessCount(uri) == r, 1, timeout);
    }

    protected void waitForRequests(
        final StaticHttpResource res,
        final int reqs,
        final int timeout)
        throws Exception
    {
        waitForRequests((r) -> res.accessCount() == r, reqs, timeout);
    }

    //CSOFF: MagicNumber
    protected void waitForRequests(
        final Function<Integer, Boolean> func,
        final int reqs,
        final int timeout)
        throws Exception
    {
        int sleepTime = 100;
        int waiting = 0;
        while (!func.apply(reqs)) {
            Thread.sleep(sleepTime);
            waiting += sleepTime;
            if (waiting > timeout) {
                throw new TimeoutException("Timeout waiting requests");
            }
        }
    }
    //CSON: MagicNumber
}
