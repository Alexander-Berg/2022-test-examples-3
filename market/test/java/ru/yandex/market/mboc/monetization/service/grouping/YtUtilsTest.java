package ru.yandex.market.mboc.monetization.service.grouping;

import java.util.stream.IntStream;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.junit.Test;

import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.market.mboc.common.repo.bindings.pojos.ModelGroupParameterValues;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author danfertev
 * @since 10.12.2019
 */
public class YtUtilsTest {
    private static final long SEED = "MBO-22405".hashCode();
    private static final int LIMIT = 100;

    private static final EnhancedRandom RANDOM = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
        .seed(SEED)
        .build();

    @Test
    public void testConvertModelGroupParameterValues() {
        IntStream.range(0, LIMIT).forEach(i -> {
            var values = RANDOM.nextObject(ModelGroupParameterValues.class);
            var node = YtUtils.convertModelGroupParamValues(values);
            assertThat(YtUtils.convertModelGroupParamValues(node)).isEqualTo(values);
        });
    }

    @Test
    public void testConvertModelGroupParameterValuesFailed() {
        var values = YtUtils.convertModelGroupParamValues(YTree.mapBuilder().buildMap());
        assertThat(values).isNull();
    }
}
