package ru.yandex.market.core.testing;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Random;
import java.util.UUID;

import org.apache.commons.lang3.SerializationUtils;
import org.junit.Test;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Unit-тесты для {@link FullTestingState}.
 *
 * @author Vladislav Bauer
 */
public class FullTestingStateTest {

    @Test
    public void testSerialization() {
        final FullTestingState state = createFullTestingState();
        final FullTestingState clone = SerializationUtils.clone(state);

        assertThat(clone, notNullValue());
    }


    private FullTestingState createFullTestingState() {
        final Date now = new Date();
        final Random rnd = new Random();

        final Collection<TestingState> testingStates = Collections.singleton(
                new TestingState(
                        rnd.nextInt(), rnd.nextInt(), rnd.nextBoolean(), rnd.nextBoolean(), rnd.nextBoolean(),
                        rnd.nextBoolean(), rnd.nextInt(), rnd.nextBoolean(), UUID.randomUUID().toString(), now, now,
                        TestingType.CPA_CHECK, rnd.nextInt(), rnd.nextBoolean(), rnd.nextBoolean(),
                        TestingStatus.FAILED, rnd.nextInt(), rnd.nextInt()
                )
        );

        return new FullTestingState(testingStates);
    }

}
