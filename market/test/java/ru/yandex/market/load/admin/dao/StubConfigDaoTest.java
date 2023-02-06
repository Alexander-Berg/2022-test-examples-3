package ru.yandex.market.load.admin.dao;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.load.admin.AbstractFunctionalTest;
import ru.yandex.market.load.admin.entity.StubConfig;
import ru.yandex.market.load.admin.entity.StubConfigType;
import ru.yandex.market.load.admin.entity.TaskType;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by aproskriakov on 6/23/22
 */
public class StubConfigDaoTest extends AbstractFunctionalTest {

    @Autowired
    StubConfigDao stubConfigDao;

    @Test
    void testGetStubConfig() {
        StubConfig config = StubConfig.builder()
                .projectId(12)
                .task(TaskType.GENERATE)
                .type(StubConfigType.PLUGIN)
                .customParams("{}")
                .build();
        stubConfigDao.saveOrUpdate(config);
        config = stubConfigDao.getStubConfig(12, TaskType.GENERATE).get();
        assertEquals(config.getProjectId(), 12);
        assertEquals(config.getType(), StubConfigType.PLUGIN);
        assertEquals(config.getTask(), TaskType.GENERATE);
    }
}
