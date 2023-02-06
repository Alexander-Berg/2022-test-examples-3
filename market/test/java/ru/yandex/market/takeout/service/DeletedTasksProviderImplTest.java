package ru.yandex.market.takeout.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.takeout.config.DaoTestConfiguration;
import ru.yandex.market.takeout.config.Delete;
import ru.yandex.market.takeout.config.ServiceDescription;
import ru.yandex.market.takeout.config.Takeout20Configuration;
import ru.yandex.market.takeout.config.TakeoutDescription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {DaoTestConfiguration.class})
@Sql(executionPhase = BEFORE_TEST_METHOD, scripts = "classpath:sql/delete_tasks.sql")
public class DeletedTasksProviderImplTest {

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Test
    public void getTasksForHardDelete() {
        DeletedTasksProvider provider = new DeletedTasksProviderImpl(jdbcTemplate, buildConfiguration(), 1000);
        List<DeleteTask> tasksForHardDelete = provider.getTasksForDeleteHard();
        Set<Long> uids = tasksForHardDelete.stream().map(DeleteTask::getUid).collect(Collectors.toSet());
        assertEquals(2, tasksForHardDelete.size());
        assertThat(uids).containsExactlyInAnyOrder(1L, 4L);
    }

    @Test
    public void shouldReturnNoTasksForHardDelete() {
        DeletedTasksProvider provider =
                new DeletedTasksProviderImpl(jdbcTemplate, buildConfigurationWithoutDeleteHard(), 1000);
        List<DeleteTask> tasksForHardDelete = provider.getTasksForDeleteHard();
        assertEquals(0, tasksForHardDelete.size());
    }

    private Takeout20Configuration buildConfiguration() {
        ImmutableMap<String, ServiceDescription> serviceDescriptions = ImmutableMap.of(
                "checkouter",
                ServiceDescription.builder().deleteHardAfterYears(3).deleteHard(Delete.builder().build()).build());
        ImmutableMap<String, TakeoutDescription> takeoutDescriptions = ImmutableMap.of(
                "pers-qa",
                TakeoutDescription.builder().deleteHardAfterYears(1).build());
        return Takeout20Configuration.builder()
                .services(serviceDescriptions)
                .takeouts(takeoutDescriptions)
                .build();
    }

    private Takeout20Configuration buildConfigurationWithoutDeleteHard() {
        ImmutableMap<String, ServiceDescription> serviceDescriptions = ImmutableMap.of(
                "carter-blue", ServiceDescription.builder().delete(Delete.builder().build()).build());
        ImmutableMap<String, TakeoutDescription> takeoutDescriptions = ImmutableMap.of(
                "pers-basket", TakeoutDescription.builder().build());
        return Takeout20Configuration.builder()
                .services(serviceDescriptions)
                .takeouts(takeoutDescriptions)
                .build();
    }
}
