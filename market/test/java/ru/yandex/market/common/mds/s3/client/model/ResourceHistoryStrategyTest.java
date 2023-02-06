package ru.yandex.market.common.mds.s3.client.model;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import ru.yandex.market.common.mds.s3.client.exception.MdsS3Exception;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Unit-тесты для {@link ResourceHistoryStrategy}.
 *
 * @author Vladislav Bauer
 */
public class ResourceHistoryStrategyTest {

    @Test
    public void testCount() {
        // XXX(vbauer): При изменении enum'a необходимо проверить по всему коду библиотеки что все в порядке.
        assertThat(ResourceHistoryStrategy.values().length, equalTo(3));
    }

    @Test
    public void testValues() {
        // XXX(vbauer): Возможно значение параметра enum'a было изменено по ошибке.
        checkEnum(ResourceHistoryStrategy.HISTORY_ONLY, true, false);
        checkEnum(ResourceHistoryStrategy.LAST_ONLY, false, true);
        checkEnum(ResourceHistoryStrategy.HISTORY_WITH_LAST, true, true);
    }

    @Test
    public void testValuesCorrectness() {
        final List<ResourceHistoryStrategy> values = Arrays.asList(ResourceHistoryStrategy.values());
        for (final ResourceHistoryStrategy strategy : values) {
            assertThat(strategy.needLast() || strategy.needHistory(), is(true));
        }
        assertThat(
                values.stream().map(ResourceHistoryStrategy::getCode).collect(Collectors.toSet()).size(),
                is(values.size())
        );
    }

    @Test
    public void testByCode() {
        final ResourceHistoryStrategy[] values = ResourceHistoryStrategy.values();
        for (ResourceHistoryStrategy strategy : values) {
            assertThat(ResourceHistoryStrategy.byCode(strategy.getCode()), is(strategy));
        }
    }

    @Test(expected = MdsS3Exception.class)
    public void testByCodeNegative() {
        fail(String.valueOf(ResourceHistoryStrategy.byCode(-1)));
    }


    private void checkEnum(final ResourceHistoryStrategy value, final boolean needHistory, final boolean needLast) {
        assertThat(value.needHistory(), equalTo(needHistory));
        assertThat(value.needLast(), equalTo(needLast));
    }

}
