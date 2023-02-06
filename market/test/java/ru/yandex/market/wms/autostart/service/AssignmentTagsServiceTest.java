package ru.yandex.market.wms.autostart.service;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.dao.entity.TaskDetail;
import ru.yandex.market.wms.common.spring.service.AssignmentTagsService;

public class AssignmentTagsServiceTest extends IntegrationTest {

    @Autowired
    AssignmentTagsService assignmentTagsService;

    @Test
    @DatabaseSetup("/service/assignment-tags/before-1.xml")
    @ExpectedDatabase(
            value = "/service/assignment-tags/after-1.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void happyPath() {
        assignmentTagsService.createTagsForTaskDetails(List.of(
                TaskDetail.builder().assignmentNumber("00001").putawayzone("MEZ1").build(),
                TaskDetail.builder().assignmentNumber("00001").putawayzone("MEZ2").build(),
                TaskDetail.builder().assignmentNumber("00001").putawayzone("MEZ9").build(),
                TaskDetail.builder().assignmentNumber("00002").putawayzone("MEZ3").build(),
                TaskDetail.builder().assignmentNumber("00002").putawayzone("MEZ3").build(),
                TaskDetail.builder().assignmentNumber("00002").putawayzone("MEZ3").build(),
                TaskDetail.builder().assignmentNumber("00003").putawayzone("MEZ1").build(),
                TaskDetail.builder().assignmentNumber("00004").putawayzone("MEZ2").build(),
                TaskDetail.builder().assignmentNumber("00004").putawayzone("MEZ9").build(),
                TaskDetail.builder().putawayzone("MEZ2").build(),
                TaskDetail.builder().assignmentNumber("10001").build(),
                TaskDetail.builder().build()
        ));
    }

    @Test
    @DatabaseSetup("/service/assignment-tags/before-2.xml")
    @ExpectedDatabase(
            value = "/service/assignment-tags/after-2.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void withExistingTags() {
        assignmentTagsService.createTagsForTaskDetails(List.of(
                TaskDetail.builder().assignmentNumber("00001").putawayzone("MEZ3").build(),
                TaskDetail.builder().assignmentNumber("00003").putawayzone("MEZ2").build(),
                TaskDetail.builder().assignmentNumber("00003").putawayzone("MEZ4").build(),
                TaskDetail.builder().assignmentNumber("00005").putawayzone("MEZ1").build(),
                TaskDetail.builder().assignmentNumber("00005").putawayzone("MEZ3").build(),
                TaskDetail.builder().assignmentNumber("00005").putawayzone("MEZ9").build(),
                TaskDetail.builder().build()
        ));
    }


}
