package ru.yandex.market.logistics.logging.rest.transactional;

import org.assertj.core.api.JUnitJupiterSoftAssertions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
    TestConfiguration.class,
})
@TestExecutionListeners(
    mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS,
    listeners = {
        TransactionalTestExecutionListener.class,
    }
)
@TestPropertySource(locations = "classpath:application.properties")
public abstract class AbstractContextualTest {
    @RegisterExtension
    protected final JUnitJupiterSoftAssertions softly = new JUnitJupiterSoftAssertions();
}
