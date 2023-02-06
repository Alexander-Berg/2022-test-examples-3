package ru.yandex.chemodan.app.docviewer.copy;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.bolts.collection.Option;
import ru.yandex.misc.io.http.Timeout;
import ru.yandex.misc.io.http.apache.v4.ApacheHttpClientUtils;

public class BlockedUrlCheckerTest {

    @Test
    @Ignore
    public void testGet() {

        BlockedUrlChecker urlChecker = new BlockedUrlChecker("https://sba.yandex.net/safety", "saved-copy",
                ApacheHttpClientUtils.singleConnectionClient(Option.of(Timeout.seconds(5)), Option.empty(), true));
        boolean blocked = urlChecker
                .isBlocked("http://rusinst.ru/docs/books/O.A.Platonov-Sionskie_protokoly_v_mirovoi_politike.pdf");
        Assert.assertTrue(blocked);

        boolean notBlocked = urlChecker
                .isBlocked("https://yandex.ru");
        Assert.assertFalse(notBlocked);

    }
}
