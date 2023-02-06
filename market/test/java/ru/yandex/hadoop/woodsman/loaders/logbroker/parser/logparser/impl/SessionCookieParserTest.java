package ru.yandex.hadoop.woodsman.loaders.logbroker.parser.logparser.impl;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.yandex.hadoop.woodsman.loaders.logbroker.parser.logparser.impl.SessionCookieParser.SessionFields;
import ru.yandex.market.stats.test.data.TestDataResolver;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Aleksandr Kormushin <kormushin@yandex-team.ru>
 */
@RunWith(DataProviderRunner.class)
public class SessionCookieParserTest {

    private final SessionCookieParser sessionCookieParser = new SessionCookieParser();

    @DataProvider
    public static Object[][] sessions() {
        return new Object[][]{
                new Object[]{"1456767704.0.1.113000812.0.108108.95399.6ca1a535b32ade0137b9dcac7ea62f90", "113000812"},
                new Object[]{"3:1461934954.5.0.1461934954507:VlSwq:dd.0|999678.0.689|146488.982760.gPOiCoPifqbbXEO", "999678"},
                new Object[]{"1456767704.0.1.113000812.1:23.3:23.108108.95399.6ca1a535b32ade0137b9dcac7ea62f90", "113000812"},
                new Object[]{"1456767704.0.1.113000812.1:23.3:23.1:2:108108.95399.6ca1a535b32ade0137b9dcac7ea62f90", "113000812"},
                new Object[]{"2:1385132628.-5812.5.57649554.8:1382619054075:3564239411:20.0.1.1.0.101934.9571.e5f0a25984ee7cfd9c93ec5ef71c9ffb", "57649554"},
                new Object[]{"3:1461558183.5.0.1456323179713:X_ykTw:4d.0|113000812.0.2|144733.399959.WJeusaOX610zSNRqPCUZl5szJSY", "113000812"},
                new Object[]{"3:1461274601.5.0.1413230809000:GKqLTw:4d.0|103065285.0.2|298452313.7341194.2|144575.15101.LGYTuX1XGc8Ql1VtCbPRGfc1-Zo;", "103065285"},
                new Object[]{"3:1461327967.5.0.1445495086584:Mr3FWw:56.0|1130000019398589.0.2|1130000013947786.182101.2.2:182101|363901968.10111949.2.2:10111949|144605.179288.wGzEbwbfdb44Hlui27IOknqsnn0", "1130000019398589"},
                new Object[]{"3:1461436040.5.0.1461118860000:zKVruQ:86.1|379007299.-1.0.1:69006569.2:13|144665.192373.SgURVNZrR2Pno_b3a9zZZNdybJ8", "379007299"},
                new Object[]{"3:1461506053.5.0.1460980253000:SH112Q:6.0|13167825.0.2|144703.719387.cFDvd_d8X9LjBN8I6SDkLB5za4I", "13167825"},
                new Object[]{"3:1461580699.5.0.1414079182000:0KtKgA:20.1|116316542.0.402|144745.288572.CL2cXyrubloLSFbSMWafdnP7goQ", "116316542"},
                new Object[]{"3%3A1461382895.5.0.1419250274000%3AcSwnTQ%3A4b.0%7C34629879.0.2%7C144635.314144.COSQPPHmTnzrYqOWsNqYqDMqE0Y", "34629879"},
                new Object[]{"3:1461575384.5.b.1443087573804:phtDwg:1e.0|248912365.46.2.2:46|1130000003815262.69.2.2:69|1130000013749316.90.2.2:90|1130000001688889.108.2.2:108|107435787.879281.2.2:879281|1130000018678994.952130.2.2:952130|1130000018372021.965965.2.2:965965|1130000001417652.1136939.2.2:1136939|1130000019307884.1292809.2.2:1292809|335917577.2164959.2.2:2164959|1130000001417551.2769805.2.2:2769805|1130000001799694.18177844.2.2:18177844|144743.804620.8kqLGJep59v1BroiKFd_fquL-_g", "1130000001799694"},
                new Object[]{"noauth:1450239757;", null},
        };
    }

    @Test
    @UseDataProvider("sessions")
    public void parseSession(String sessionCookie, String uid) {
        SessionFields sessionFields = sessionCookieParser.parseSession(sessionCookie);

        assertThat(sessionFields.getUid(), is(uid));
    }

    @Test
    public void parseSession_from_prod() throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new GZIPInputStream(TestDataResolver.getResource("sandbox:sessions.tsv.gz"))))) {
            reader.lines().forEach(sessionCookieParser::parseSession);
        }
    }
}
