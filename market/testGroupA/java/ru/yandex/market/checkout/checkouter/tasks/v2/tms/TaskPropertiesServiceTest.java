package ru.yandex.market.checkout.checkouter.tasks.v2.tms;

import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractServicesTestBase;

public class TaskPropertiesServiceTest extends AbstractServicesTestBase {

    @Autowired
    TaskPropertiesService taskPropertiesService;
    @Autowired
    TaskPropertiesDao taskPropertiesDao;

    @Test
    public void setAndCheckEnabled() {
        taskPropertiesService.setEnabled("taskName1", true);
        Assertions.assertTrue(taskPropertiesService.getEnabled("taskName1"));
    }

    @Test
    public void getEnabledInsertIfTaskAbsent() {
        var enabled = taskPropertiesService.getEnabled("taskName2");
        Assertions.assertFalse(enabled);
    }

    @Test
    public void setPayload() {
        taskPropertiesDao.save("taskName3");
        var list = List.of(1, 2, 3);
        taskPropertiesService.setPayload("taskName3", list);

        Assertions.assertEquals(list, taskPropertiesService.getPayload("taskName3", new TypeReference<>() {
        }, List.of(10, 20, 30)));
    }
}
