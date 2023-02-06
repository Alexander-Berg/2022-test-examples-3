package ru.yandex.market.common.mds.s3.client.model;

import java.time.temporal.ChronoUnit;

import org.junit.Test;

import ru.yandex.market.common.mds.s3.client.test.TestUtils;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Unit-тесты для {@link ResourceLifeTime}.
 *
 * @author Vladislav Bauer
 */
public class ResourceLifeTimeTest {

    @Test
    public void testBasicMethods() {
        TestUtils.checkEqualsAndHashCodeContract(ResourceLifeTime.class);
    }

    @Test
    public void testMajorConstantContract() {
        // XXX(vbauer): Нужно подумать дважды прежде чем менять эту константу.
        // Обычно в выгрузках достаточная единица измерения времени - дни.
        assertThat(ResourceLifeTime.DEFAULT_TIME_UNIT, equalTo(ChronoUnit.DAYS));
    }

    @Test
    public void testIsValidNegative() {
        assertThat(ResourceLifeTime.isValid(null, null), equalTo(false));
        assertThat(ResourceLifeTime.isValid(null, 1), equalTo(false));
        assertThat(ResourceLifeTime.isValid(ChronoUnit.DAYS, null), equalTo(false));
        assertThat(ResourceLifeTime.isValid(ChronoUnit.DAYS, 0), equalTo(false));
        assertThat(ResourceLifeTime.isValid(ChronoUnit.DAYS, 1), equalTo(true));
    }

    @Test
    public void testForever() {
        final ResourceLifeTime forever = ResourceLifeTime.forever();

        assertThat(forever.getTtlUnit(), equalTo(ChronoUnit.FOREVER));
        assertThat(forever.getTtl(), equalTo(1));
    }

    @Test
    public void testIsForever() {
        assertThat(ResourceLifeTime.isForever(ResourceLifeTime.forever()), equalTo(true));
        assertThat(ResourceLifeTime.isForever(ResourceLifeTime.create(1)), equalTo(false));
    }

}
