package ru.yandex.market.olap2.dao;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mockito;
import org.springframework.dao.EmptyResultDataAccessException;

import ru.yandex.market.olap2.load.tasks.TaskPriority;
import ru.yandex.market.olap2.sla.ImportantCubesPaths;
import ru.yandex.market.olap2.sla.SlaCube;
import ru.yandex.market.olap2.sla.SlaCubesHolder;
import ru.yandex.market.olap2.step.model.StepEvent;
import ru.yandex.market.olap2.step.model.StepEventParams;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.junit.Assert.assertEquals;

@RunWith(value = Parameterized.class)
public class MetadataDaoTest {

    private final TaskPriority priority;
    private final StepEvent event;
    private final LoggingJdbcTemplate template = Mockito.mock(LoggingJdbcTemplate.class);
    private final SlaCubesHolder holder = new SlaCubesHolder(ImmutableMap.of("cube_fact_name",
            new SlaCube("cube_fact_name", 1, true, false)));
    private final ImportantCubesPaths slaPaths = new ImportantCubesPaths(holder);
    private final MetadataDao dao = new MetadataDao(template, slaPaths);

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

    public MetadataDaoTest(TaskPriority priority, StepEvent event) {
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

    @Test
    public void testDoSafeSelect() {
        AtomicInteger counter = new AtomicInteger(0);
        Optional<String> res = dao.doSafeSelect(
                () -> counter,
                AtomicInteger::incrementAndGet, // 1
                c -> String.valueOf(c.incrementAndGet()), // 2
                () -> {
                    throw new RuntimeException("Should not happen");
                }
        );
        assertSoftly(softly -> {
            softly.assertThat(res).isPresent();
            softly.assertThat(res.get()).isEqualTo("2");
            softly.assertThat(counter).hasValue(2);
        });
    }

    @Test
    public void testDoSafeSelectNoResult() {
        AtomicInteger counter = new AtomicInteger(0);
        Optional<String> res = dao.doSafeSelect(
                () -> {
                    if (true) {
                        throw new EmptyResultDataAccessException(1);
                    }
                    return counter; // чтобы он смог понять результирующий тип суплаера
                },
                c -> {
                    throw new RuntimeException("Should not happen");
                },
                c -> String.valueOf(c.incrementAndGet()),
                counter::incrementAndGet // 1
        );
        assertSoftly(softly -> {
            softly.assertThat(res).isEmpty();
            softly.assertThat(counter).hasValue(1);
        });
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
