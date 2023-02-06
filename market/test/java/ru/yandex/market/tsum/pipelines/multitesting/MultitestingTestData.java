package ru.yandex.market.tsum.pipelines.multitesting;

import java.util.Collections;

import ru.yandex.market.tsum.multitesting.CreateEnvironmentRequest;
import ru.yandex.market.tsum.multitesting.model.MultitestingEnvironment;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 27.12.2017
 */
public class MultitestingTestData {
    private MultitestingTestData() {
    }

    public static CreateEnvironmentRequest.Builder defaultEnvironment() {
        return defaultEnvironment("test", "name1");
    }

    public static CreateEnvironmentRequest.Builder defaultEnvironment(String project, String name) {
        return new CreateEnvironmentRequest.Builder()
            .withProject(project)
            .withName(name)
            .withIsStatic(true)
            .withType(MultitestingEnvironment.Type.USE_EXISTING_PIPELINE)
            .withAuthor("some user")
            .withPipelineId(MultitestingTestConfig.PIPE_WITH_CUSTOM_CLEANUP_ID)
            .withDefaultPipelineResources(Collections.emptyList());
    }
}
