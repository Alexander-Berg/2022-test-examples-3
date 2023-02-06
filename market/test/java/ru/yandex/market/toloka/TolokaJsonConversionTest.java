package ru.yandex.market.toloka;

import org.junit.Test;
import ru.yandex.market.toloka.model.Filter;
import ru.yandex.market.toloka.model.Pool;
import ru.yandex.market.toloka.model.Result;
import ru.yandex.market.toloka.model.Skill;
import ru.yandex.market.toloka.model.Task;
import ru.yandex.market.toloka.model.TaskSuite;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author galaev
 * @since 2019-05-16
 */
public class TolokaJsonConversionTest {

    private TolokaTestData testData = new TolokaTestData();

    @Test
    public void testTaskConversion() throws IOException {
        Task task = testData.sampleTask();
        byte[] bytes = TolokaJsonUtils.serializeObject(task);
        Task taskCopy = TolokaJsonUtils.parseJson(bytes, Task.class);
        assertThat(task).isEqualTo(taskCopy);
    }

    @Test
    public void testTaskSuiteConversion() throws IOException {
        TaskSuite taskSuite = testData.sampleTaskSuite();
        byte[] bytes = TolokaJsonUtils.serializeObject(taskSuite);
        TaskSuite taskSuiteCopy = TolokaJsonUtils.parseJson(bytes, TaskSuite.class);
        assertThat(taskSuite).isEqualTo(taskSuiteCopy);
    }

    @Test
    public void testPoolConversion() throws IOException {
        Pool pool = testData.samplePool();
        byte[] bytes = TolokaJsonUtils.serializeObject(pool);
        Pool poolCopy = TolokaJsonUtils.parseJson(bytes, Pool.class);
        assertThat(pool).isEqualTo(poolCopy);
    }

    @Test
    public void testFilterConversion() throws IOException {
        Filter filter = testData.sampleFilter();
        byte[] bytes = TolokaJsonUtils.serializeObject(filter);
        Filter filterCopy = TolokaJsonUtils.parseJson(bytes, Filter.class);
        assertThat(filter).isEqualTo(filterCopy);
    }

    @Test
    public void testSkillConversion() throws IOException {
        Skill skill = testData.sampleSkill();
        byte[] bytes = TolokaJsonUtils.serializeObject(skill);
        Skill skillCopy = TolokaJsonUtils.parseJson(bytes, Skill.class);
        assertThat(skill).isEqualTo(skillCopy);
    }

    @Test
    public void testResultConversion() throws IOException {
        Result result = testData.sampleResult();
        byte[] bytes = TolokaJsonUtils.serializeObject(result);
        Result resultCopy = TolokaJsonUtils.parseJson(bytes, Result.class);
        assertThat(result).isEqualTo(resultCopy);
    }
}
