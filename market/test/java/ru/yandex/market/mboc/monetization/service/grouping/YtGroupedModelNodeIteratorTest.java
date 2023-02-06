package ru.yandex.market.mboc.monetization.service.grouping;

import java.util.Collections;
import java.util.stream.IntStream;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.junit.Test;

import ru.yandex.market.mboc.common.db.jooq.generated.monetization.tables.pojos.ModelGroup;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.mboc.monetization.GroupingTestUtils.autoGroup;
import static ru.yandex.market.mboc.monetization.GroupingTestUtils.modelIteratorF;
import static ru.yandex.market.mboc.monetization.GroupingTestUtils.nextGroupedModel;

/**
 * @author danfertev
 * @since 29.01.2020
 */
public class YtGroupedModelNodeIteratorTest {
    private static final long SEED = "MBO-22407".hashCode();
    private static final int LIMIT = 1000;

    private static final EnhancedRandom RANDOM = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
        .seed(SEED)
        .build();

    @Test
    public void testEmptyIterator() {
        var iterator = new YtGroupedModelNodeIterator(Collections.emptyIterator());

        IntStream.range(0, LIMIT).forEach(i -> {
            var group = RANDOM.nextObject(ModelGroup.class);
            assertThat(iterator.hasNextForGroup(group)).isFalse();
        });
    }

    @Test
    public void testNextForGroup() {
        var group1 = autoGroup();
        var group2 = autoGroup();
        var model1 = nextGroupedModel(group1);
        var model2 = nextGroupedModel(group1);
        var iterator = new YtGroupedModelNodeIterator(modelIteratorF(group1, model1, model2));

        assertThat(iterator.hasNextForGroup(group2)).isFalse();
        assertThat(iterator.nextForGroup(group2)).isNull();

        assertThat(iterator.hasNextForGroup(group1)).isTrue();
        assertThat(iterator.nextForGroup(group1)).isNotNull();
        assertThat(iterator.nextForGroup(group1)).isNotNull();
        assertThat(iterator.nextForGroup(group1)).isNull();
    }
}
