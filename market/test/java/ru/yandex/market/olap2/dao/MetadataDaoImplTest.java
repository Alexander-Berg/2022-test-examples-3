package ru.yandex.market.olap2.dao;

import java.util.Arrays;
import java.util.Collection;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mockito;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.olap2.load.tasks.TaskPriority;
import ru.yandex.market.olap2.model.SlaCube;
import ru.yandex.market.olap2.model.SlaCubesHolder;
import ru.yandex.market.olap2.step.model.StepEvent;
import ru.yandex.market.olap2.step.model.StepEventParams;
import ru.yandex.market.olap2.util.ImportantCubesPaths;

import static org.junit.Assert.assertEquals;

@RunWith(value = Parameterized.class)
public class MetadataDaoImplTest {

    private final TaskPriority priority;
    private final StepEvent event;
    private final NamedParameterJdbcTemplate template = Mockito.mock(NamedParameterJdbcTemplate.class);
    private final SlaCubesHolder holder = new SlaCubesHolder(ImmutableMap.of("cube_fact_name",
            new SlaCube("cube_fact_name", 1, 1, false)));
    private final ImportantCubesPaths slaPaths = new ImportantCubesPaths(holder);
    private final MetadataDao dao = new MetadataDaoImpl(template, slaPaths);

    @Parameterized.Parameters(name = "{index}: testGetTaskPriority({1}) = {0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {TaskPriority.LOW, createEvent("marketstat_olap2_rebuild","//any")},
                {TaskPriority.HIGH, createEvent("marketstat_olap2_publish",ImportantCubesPaths.CUBES_YT_DIR_PREFIX + "cube_fact_name")},
                {TaskPriority.HIGH, createEvent("marketstat_olap2_publish",ImportantCubesPaths.CUBES_YT_DIR_PREFIX + "cube_fact_name/2020-01-02")},
                {TaskPriority.DEFAULT, createEvent("marketstat_olap2_publish",ImportantCubesPaths.CUBES_YT_DIR_PREFIX + "not_important_fact")},
                {TaskPriority.DEFAULT, createEvent("marketstat_olap2_publish",ImportantCubesPaths.CUBES_YT_DIR_PREFIX + "not_important_fact/2020-01-01")}
        });
    }

    public MetadataDaoImplTest(TaskPriority priority, StepEvent event) {
        this.priority = priority;
        this.event = event;
    }

    @Test
    public void testGetTaskPriority() {
        assertEquals(priority, dao.getTaskPriority(event));
    }

    @Test
    public void testUpdateSuccess() {
        dao.updateEventLoadedSuccessfully("//path/202006", 202006, "event_id_1", "clickhouse");
    }

    @Test
    public void testUpdateSuccessNotPartitioned() {
        dao.updateEventLoadedSuccessfully("//path", null, "event_id_1", "clickhouse");
    }

    private static StepEvent createEvent(String name, String path) {
        StepEventParams params = new StepEventParams();
        params.setPath(path);
        StepEvent e = new StepEvent();
        e.setName(name);
        e.setStepEventParams(params);
        return e;
    }

}
