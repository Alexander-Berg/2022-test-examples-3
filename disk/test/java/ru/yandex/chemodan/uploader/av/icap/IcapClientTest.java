package ru.yandex.chemodan.uploader.av.icap;

import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.IteratorF;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.chemodan.uploader.av.AntivirusResult;
import ru.yandex.chemodan.uploader.av.AntivirusTestUtils;
import ru.yandex.misc.io.InputStreamSource;
import ru.yandex.misc.io.cl.ClassLoaderUtils;
import ru.yandex.misc.test.Assert;

/**
 * @author akirakozov
 */
public class IcapClientTest {

    @Test
    @Ignore
    public void scanInfectedFile() throws Exception {
        try (IcapClient client = createIcapClient()) {
            Assert.equals(AntivirusResult.INFECTED, client.scanFile(AntivirusTestUtils.infectedInputStreamSource()));
        }
    }

    @Test
    @Ignore
    public void scanHealthyFile() throws Exception {
        try (IcapClient client = createIcapClient()) {
            Assert.equals(AntivirusResult.HEALTHY, client.scanFile(AntivirusTestUtils.healthyInputStreamSource()));
        }
    }

    @Test
    @Ignore
    public void scan11TimesZippedFile() throws Exception {
        try (IcapClient client = createIcapClient()) {
            InputStreamSource source = ClassLoaderUtils.streamSourceForResource(this.getClass(), "a11.zip");
            Assert.equals(AntivirusResult.UNKNOWN, client.scanFile(source));
        }
    }

    @Test
    public void testInfectedResponseParsing() {
        Assert.equals(AntivirusResult.INFECTED, new IcapClientStub(Cf.list(IcapResponseParserTest.YAVS_ANTIVIRUS_INFECTED_RESPONSE)).testResponseParsing());
        Assert.equals(AntivirusResult.INFECTED, new IcapClientStub(Cf.list(IcapResponseParserTest.INFECTED_RESPONSE)).testResponseParsing());
    }

    @Test
    public void testHealthyResponseParsing() {
        Assert.equals(AntivirusResult.HEALTHY, new IcapClientStub(Cf.list(IcapResponseParserTest.YAVS_ANTIVIRUS_HEALTHY_RESPONSE)).testResponseParsing());
        Assert.equals(AntivirusResult.HEALTHY, new IcapClientStub(Cf.list(IcapResponseParserTest.NOT_MODIFIED_SUCCESS_RESPONSE)).testResponseParsing());
    }

    @Test
    public void testUnknownResponseParsing() {
        Assert.equals(AntivirusResult.UNKNOWN, new IcapClientStub(Cf.list(IcapResponseParserTest.NOTSCANNED_RESPONSE)).testResponseParsing());
    }

    @Test
    public void testInfectedResponseParsingByParts() {
        Assert.equals(AntivirusResult.INFECTED, new IcapClientStub(Cf.list(
                "ICAP/1.0 200 OK\r\n" +
                        "Server: C-ICAP/1.0\r\n" +
                        "Connection: keep-alive\r\n" +
                        "ISTag: CI0001YAVS\r\n" +
                        "Encapsulated: res-hdr=0, res-body=383\r\n\r\n" +
                        "HTTP/1.0 403 Forbidden\r\n" +
                        "X-Infection-Found: Type=0; Resolution=0; Threat=EICAR_test_file\r\n" +
                        "X-Virus-ID: EICAR_test_file\r\n" +
                        "Date: Tue Mar 03 2020 16:08:30 GMT\r\n" +
                        "Last-Modified: Tue Mar 03 2020 16:08:30 GMT\r\n" +
                        "Content-Length: 8\r\n" +
                        "Via: ICAP/1.0 (C-ICAP/1.0 Yandex AV Service)\r\n" +
                        "Content-Type: text/html\r\n" +
                        "Connection: close\r\n\r\n",
                "a\r\n" +
                        "infected\r\n" +
                        "Via: ICAP/1.0 uploader2h.dst.yandex.net (C-ICAP/1.0 Yandex AV Service )\r\n" +
                        "\r\n\r\n"
        )).testResponseParsing());
    }

    private IcapClient createIcapClient() {
        // for local testing use uploader.dst.yandex.net
        return new IcapClient("127.0.0.1", 1344, "esets_icap", new IcapClientConfiguration(5000, 5000, 5000, 65536));
    }

    private static class IcapClientStub extends IcapClient {

        private final IcapResponseProvider responseProvider;

        public IcapClientStub(ListF<String> responseParts) {
            super();
            this.responseProvider = new StubIcapResponseProvider(responseParts);
        }

        public AntivirusResult testResponseParsing() {
            return getAntivirusResult();
        }

        @Override
        protected IcapResponseProvider getIcapResponseProvider() {
            return this.responseProvider;
        }
    }

    private static class StubIcapResponseProvider implements IcapResponseProvider {

        private final IteratorF<String> responseParts;

        public StubIcapResponseProvider(ListF<String> responseParts) {
            this.responseParts = responseParts.iterator();
        }

        @Override
        public String nextResponsePart() {
            return responseParts.next();
        }

        @Override
        public boolean endHasBeenReached() {
            return !responseParts.hasNext();
        }
    }

}
