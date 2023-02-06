package ru.yandex.market.logistics.dbqueue;

import com.github.springtestdbunit.DbUnitTestExecutionListener;
import org.assertj.core.api.JUnitJupiterSoftAssertions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
    SpringTestConfiguration.class,
})
@TestExecutionListeners(
    mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS,
    listeners = {
        DbUnitTestExecutionListener.class,
    }
)
public abstract class AbstractContextualTest {
    @RegisterExtension
    protected final JUnitJupiterSoftAssertions softly = new JUnitJupiterSoftAssertions();
}
