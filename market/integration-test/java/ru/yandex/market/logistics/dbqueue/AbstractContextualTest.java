package ru.yandex.market.logistics.dbqueue;

import com.github.springtestdbunit.DbUnitTestExecutionListener;
import org.assertj.core.api.JUnitJupiterSoftAssertions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.logistics.dbqueue.configuration.SpringTestConfiguration;

@ExtendWith({
    SpringExtension.class,
})
@SpringBootTest(
    classes = SpringTestConfiguration.class,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
@ContextConfiguration(
    classes = {
        SpringTestConfiguration.class,
    })
@TestExecutionListeners(
    mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS,
    listeners = {
        DbUnitTestExecutionListener.class,
    }
)
@AutoConfigureMockMvc
public abstract class AbstractContextualTest {
    @RegisterExtension
    protected final JUnitJupiterSoftAssertions softly = new JUnitJupiterSoftAssertions();
}
