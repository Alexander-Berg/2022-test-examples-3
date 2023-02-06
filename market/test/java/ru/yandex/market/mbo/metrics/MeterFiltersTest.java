package ru.yandex.market.mbo.metrics;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import org.assertj.core.api.Assertions;
import org.junit.Test;

/**
 * @author danfertev
 * @since 05.07.2020
 */
public class MeterFiltersTest {
    @Test
    public void removeQueryParamsNoUri() {
        Meter.Id id = new Meter.Id("name", Tags.of("k1", "v1"), null, null, Meter.Type.COUNTER);
        Meter.Id result = MeterFilters.removeQueryParamsFromUrl().map(id);
        Assertions.assertThat(result).isEqualTo(id);
    }

    @Test
    public void removeQueryParamsUriNoQueryParams() {
        Meter.Id id = new Meter.Id("name", Tags.of("uri", "/test"), null, null, Meter.Type.COUNTER);
        Meter.Id result = MeterFilters.removeQueryParamsFromUrl().map(id);
        Assertions.assertThat(result).isEqualTo(id);
    }

    @Test
    public void removeQueryParams() {
        Meter.Id id = new Meter.Id("name", Tags.of("uri", "/test?param=value"), null, null, Meter.Type.COUNTER);
        Meter.Id result = MeterFilters.removeQueryParamsFromUrl().map(id);
        Meter.Id expected = id.withTag(Tag.of("uri", "/test"));
        Assertions.assertThat(result).isEqualTo(expected);
    }

    @Test
    public void replaceIdsFromUrlNoUri() {
        Meter.Id id = new Meter.Id("name", Tags.of("k1", "v1"), null, null, Meter.Type.COUNTER);
        Meter.Id result = MeterFilters.replaceIdsFromUrl().map(id);
        Assertions.assertThat(result).isEqualTo(id);
    }

    @Test
    public void replaceIdsFromUrlNoIds() {
        Meter.Id id = new Meter.Id("name", Tags.of("uri", "/test"), null, null, Meter.Type.COUNTER);
        Meter.Id result = MeterFilters.replaceIdsFromUrl().map(id);
        Assertions.assertThat(result).isEqualTo(id);
    }

    @Test
    public void replaceIdsFromUrlSingleDigit() {
        Meter.Id id = new Meter.Id("name", Tags.of("uri", "/test/1/"), null, null, Meter.Type.COUNTER);
        Meter.Id result = MeterFilters.replaceIdsFromUrl().map(id);
        Assertions.assertThat(result).isEqualTo(id);
    }

    @Test
    public void replaceIdsFromUrlDefault() {
        Meter.Id id = new Meter.Id("name", Tags.of("uri", "/test/10/"), null, null, Meter.Type.COUNTER);
        Meter.Id result = MeterFilters.replaceIdsFromUrl().map(id);
        Meter.Id expected = id.withTag(Tag.of("uri", "/test/0000/"));
        Assertions.assertThat(result).isEqualTo(expected);
    }

    @Test
    public void replaceIdsFromUrlNoPrecedingSlash() {
        Meter.Id id = new Meter.Id("name", Tags.of("uri", "/test10/"), null, null, Meter.Type.COUNTER);
        Meter.Id result = MeterFilters.replaceIdsFromUrl().map(id);
        Assertions.assertThat(result).isEqualTo(id);
    }

    @Test
    public void replaceIdsFromUrlNoFollowingSlash() {
        Meter.Id id = new Meter.Id("name", Tags.of("uri", "/test/10"), null, null, Meter.Type.COUNTER);
        Meter.Id result = MeterFilters.replaceIdsFromUrl().map(id);
        Meter.Id expected = id.withTag(Tag.of("uri", "/test/0000"));
        Assertions.assertThat(result).isEqualTo(expected);
    }

    @Test
    public void replaceIdsFromUrlMultipleIds() {
        Meter.Id id = new Meter.Id("name", Tags.of("uri", "/test/10/11/12"), null, null, Meter.Type.COUNTER);
        Meter.Id result = MeterFilters.replaceIdsFromUrl().map(id);
        Meter.Id expected = id.withTag(Tag.of("uri", "/test/0000/0000/0000"));
        Assertions.assertThat(result).isEqualTo(expected);
    }

    @Test
    public void replaceIdsFromUrlWithCustomReplacement() {
        Meter.Id id = new Meter.Id("name", Tags.of("uri", "/test/10/11/12"), null, null, Meter.Type.COUNTER);
        Meter.Id result = MeterFilters.replaceIdsFromUrl("**").map(id);
        Meter.Id expected = id.withTag(Tag.of("uri", "/test/**/**/**"));
        Assertions.assertThat(result).isEqualTo(expected);
    }
}
