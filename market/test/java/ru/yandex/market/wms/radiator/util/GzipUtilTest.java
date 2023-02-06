package ru.yandex.market.wms.radiator.util;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

class GzipUtilTest {

    @Test
    void compress_decompress() {
        String original = "{\"test\":\"json\"}";
        var encoded = GzipUtil.compress(original);
        var decoded = GzipUtil.decompress(encoded);
        assertThat(decoded, is(equalTo(original)));
    }
}
