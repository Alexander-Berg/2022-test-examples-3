package ru.yandex.market.abo.bpmn;

import org.camunda.bpm.engine.ProcessEngine;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.abo.bpmn.mbi.MbiModerationApiClient;
import ru.yandex.market.common.test.db.DbUnitTestExecutionListener;
import ru.yandex.market.javaframework.main.config.SpringApplicationConfig;

import static org.springframework.test.context.TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {
                SpringApplicationConfig.class
        }
)
@TestExecutionListeners(
        listeners = {
                DbUnitTestExecutionListener.class
        },
        mergeMode = MERGE_WITH_DEFAULTS
)
@Import(value = {
        TestBeansConfig.class
})
@ActiveProfiles({"functionalTest"})
@TestPropertySource({"classpath:functional-test.properties"})
public abstract class AbstractFunctionalTest {
    @Autowired
    protected ProcessEngine processEngine;

    @Autowired
    protected MbiModerationApiClient moderationApiClient;
}
