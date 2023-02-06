package ru.yandex.chemodan.app.docviewer.crypt;

import org.joda.time.Duration;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.misc.test.Assert;

/**
 * @author ssytnik
 */
public class CryptUtilsTest {

    private static final String SECRET = "h74Kb3F9kP1MsQzGe56K8e";

    @Test
    public void validateToken() {
        ListF<Object> params = Cf.<Object>list("p1", 2, 3L);
        CryptUtils.validateToken(CryptUtils.calcToken(params, SECRET), params, SECRET);
    }

    @Test
    public void tokenWithHexTs() {
        ListF<Object> params = Cf.<Object>list("aa", "bb", "cc");

        long startMillis = System.currentTimeMillis();
        Tuple2<String, String> tokenAndTs = CryptUtils.calcTokenWithHexTs(params, SECRET);
        long endMillis = System.currentTimeMillis();

        String token = tokenAndTs.get1();
        String hexTs = tokenAndTs.get2();
        long ts = CryptUtils.getMillis(tokenAndTs.get2());

        Assert.le(startMillis, ts);
        Assert.le(ts, endMillis);

        CryptUtils.validateTokenWithHexTs(token, hexTs, Duration.standardMinutes(1), params, SECRET);
    }

}
