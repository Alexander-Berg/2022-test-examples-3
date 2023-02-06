package ru.yandex.market.http.test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.http.HttpStatus;

/**
 * @author dimkarp93
 */
public class HttpStatusTest {
    @Test
    public void is1XX() {
        Collection<HttpStatus> okStatuses = Arrays.asList(
                new HttpStatus(100),
                new HttpStatus(199),
                new HttpStatus(143)
        );

        Collection<HttpStatus> notOkStatuses = Arrays.asList(
                new HttpStatus(200),
                new HttpStatus(12)
        );

        doTest(
                HttpStatus.Type._1XX_INFORMATIONAL,
                okStatuses,
                notOkStatuses
        );

    }


    @Test
    public void is2XX() {
        Collection<HttpStatus> okStatuses = Arrays.asList(
                new HttpStatus(200),
                new HttpStatus(299),
                new HttpStatus(251)
        );

        Collection<HttpStatus> notOkStatuses = Arrays.asList(
                new HttpStatus(300),
                new HttpStatus(15)
        );

        doTest(
                HttpStatus.Type._2XX_SUCCESSFUL,
                okStatuses,
                notOkStatuses
        );

    }

    @Test
    public void is3XX() {
        Collection<HttpStatus> okStatuses = Arrays.asList(
                new HttpStatus(300),
                new HttpStatus(399),
                new HttpStatus(376)
        );

        Collection<HttpStatus> notOkStatuses = Arrays.asList(
                new HttpStatus(400),
                new HttpStatus(3)
        );

        doTest(
                HttpStatus.Type._3XX_REDIRECTION,
                okStatuses,
                notOkStatuses
        );
    }

    @Test
    public void is4XX() {
        Collection<HttpStatus> okStatuses = Arrays.asList(
                new HttpStatus(400),
                new HttpStatus(499),
                new HttpStatus(432)
        );

        Collection<HttpStatus> notOkStatuses = Arrays.asList(
                new HttpStatus(500),
                new HttpStatus(99)
        );

        doTest(
                HttpStatus.Type._4XX_CLIENT_ERROR,
                okStatuses,
                notOkStatuses
        );
    }

    @Test
    public void is5XX() {
        Collection<HttpStatus> okStatuses = Arrays.asList(
                new HttpStatus(500),
                new HttpStatus(599),
                new HttpStatus(507),
                new HttpStatus(601)
        );

        Collection<HttpStatus> notOkStatuses = Collections.singleton(
                new HttpStatus(78)
        );

        doTest(
                HttpStatus.Type._5XX_SERVER_ERROR,
                okStatuses,
                notOkStatuses
        );
    }

    private void doTest(HttpStatus.Type type,
                        Collection<HttpStatus> okStatuses,
                        Collection<HttpStatus> notOkStatuses) {
        for (HttpStatus status: okStatuses) {
            Assert.assertTrue(status.hasType(type));
        }

        for (HttpStatus status: notOkStatuses) {
            Assert.assertFalse(status.hasType(type));
        }
    }
}
