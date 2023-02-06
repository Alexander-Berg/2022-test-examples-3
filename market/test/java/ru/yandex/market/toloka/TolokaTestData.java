package ru.yandex.market.toloka;

import org.apache.commons.io.IOUtils;
import ru.yandex.market.toloka.model.Filter;
import ru.yandex.market.toloka.model.Pool;
import ru.yandex.market.toloka.model.Result;
import ru.yandex.market.toloka.model.Skill;
import ru.yandex.market.toloka.model.Task;
import ru.yandex.market.toloka.model.TaskSuite;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * @author galaev
 * @since 2019-06-10
 */
public class TolokaTestData {

    public Task sampleTask() throws IOException {
        return TolokaJsonUtils.parseJson(getResource("toloka/sample_task.json"), Task.class);
    }

    public TaskSuite sampleTaskSuite() throws IOException {
        return TolokaJsonUtils.parseJson(getResource("toloka/sample_task_suite.json"), TaskSuite.class);
    }

    public Pool samplePool() throws IOException {
        return TolokaJsonUtils.parseJson(getResource("toloka/sample_pool.json"), Pool.class);
    }

    public Result sampleResult() throws IOException {
        return TolokaJsonUtils.parseJson(getResource("toloka/sample_result.json"), Result.class);
    }

    public Skill sampleSkill() throws IOException {
        return TolokaJsonUtils.parseJson(getResource("toloka/sample_skill.json"), Skill.class);
    }

    public Filter sampleFilter() throws IOException {
        return TolokaJsonUtils.parseJson(getResource("toloka/sample_filter.json"), Filter.class);
    }

    private byte[] getResource(String filePath) throws IOException {
        InputStream resourceStream = Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(filePath));
        return IOUtils.toByteArray(resourceStream);
    }
}
