package ru.yandex.market.vendor;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.mbi.web.solomon.pull.SolomonUtils;
import ru.yandex.market.vendor.util.FunctionalTestHelper;
import ru.yandex.monlib.metrics.MetricConsumer;
import ru.yandex.monlib.metrics.encode.MetricDecoder;
import ru.yandex.monlib.metrics.encode.MetricFormat;
import ru.yandex.monlib.metrics.encode.json.MetricJsonDecoder;
import ru.yandex.monlib.metrics.encode.json.MetricJsonEncoder;
import ru.yandex.monlib.metrics.encode.spack.MetricSpackDecoder;
import ru.yandex.monlib.metrics.encode.spack.MetricSpackEncoder;
import ru.yandex.monlib.metrics.encode.spack.format.CompressionAlg;
import ru.yandex.monlib.metrics.encode.spack.format.TimePrecision;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class SolomonFunctionalTest extends AbstractVendorPartnerFunctionalTest {
    /**
     * Проверка, что /solomon отвечает корректно
     */
    @Test
    void solomonTestOk() {
        ResponseEntity<String> responseEntity = FunctionalTestHelper.getAsEntity(
                baseUrl + "/solomon",
                String.class
        );
        assertSame(responseEntity.getStatusCode(), HttpStatus.OK);
    }

    @Test
    void solomonJvmTestOk() {
        ResponseEntity<String> responseEntity = FunctionalTestHelper.getAsEntity(
                baseUrl + "/solomon-jvm",
                String.class
        );
        assertSame(responseEntity.getStatusCode(), HttpStatus.OK);
    }

    @Test
    void solomonSpackTrueRequestTest() {
        SolomonUtils.getMetricRegistry().lazyGaugeInt64("test_sensor", () -> 1L);
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT, MetricFormat.SPACK.contentType());
        headers.addAll(
                HttpHeaders.ACCEPT_ENCODING,
                Arrays.asList(
                        CompressionAlg.ZSTD.encoding(),
                        CompressionAlg.LZ4.encoding(),
                        CompressionAlg.ZLIB.encoding(),
                        "deflate"
                )
        );

        ResponseEntity<byte[]> result = solomonRequest(headers, "/solomon");
        checkResponse(
                new MetricSpackDecoder(),
                new MetricSpackEncoder(TimePrecision.SECONDS, CompressionAlg.LZ4, new ByteArrayOutputStream()),
                result
        );

        result = solomonRequest(headers, "/solomon-jvm");
        checkResponse(
                new MetricSpackDecoder(),
                new MetricSpackEncoder(TimePrecision.SECONDS, CompressionAlg.LZ4, new ByteArrayOutputStream()),
                result
        );
    }

    @Test
    void solomonJsonTrueRequestTest() {
        SolomonUtils.getMetricRegistry().lazyGaugeInt64("test_sensor", () -> 1L);
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT, MetricFormat.JSON.contentType());

        ResponseEntity<byte[]> result = solomonRequest(headers, "/solomon");
        checkResponse(
                new MetricJsonDecoder(),
                new MetricJsonEncoder(new ByteArrayOutputStream()),
                result
        );

        result = solomonRequest(headers, "/solomon-jvm");
        checkResponse(
                new MetricJsonDecoder(),
                new MetricJsonEncoder(new ByteArrayOutputStream()),
                result
        );

        //language=json
        String expected = "" +
                "{\n" +
                "  \"sensors\": [\n" +
                "    {\n" +
                "      \"kind\": \"IGAUGE\",\n" +
                "      \"labels\": {\n" +
                "        \"sensor\": \"test_sensor\"\n" +
                "      },\n" +
                "      \"ts\": 1005,\n" + //1005, а не 1005000, т.к. Соломон пишет время в секундах
                "      \"value\": 123\n" +
                "    }\n" +
                "  ]\n" +
                "}";
        JsonTestUtil.assertEquals(expected, new String(result.getBody()));
    }

    private ResponseEntity<byte[]> solomonRequest(HttpHeaders headers, String solomonUrl) {
        return FunctionalTestHelper.getAsEntityWithHeaders(
                baseUrl + solomonUrl,
                byte[].class,
                headers
        );
    }

    private void checkResponse(MetricDecoder decoder, MetricConsumer consumer, ResponseEntity<byte[]> result) {
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertDoesNotThrow(() -> decoder.decode(result.getBody(), consumer));
    }
}
