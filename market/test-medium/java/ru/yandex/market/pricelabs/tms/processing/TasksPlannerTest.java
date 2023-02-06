package ru.yandex.market.pricelabs.tms.processing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pricelabs.services.database.model.JobType;
import ru.yandex.market.pricelabs.services.database.model.TaskStatus;
import ru.yandex.market.pricelabs.tms.AbstractTmsSpringConfiguration;
import ru.yandex.market.pricelabs.tms.jobs.BusinessProcessingJob;
import ru.yandex.market.pricelabs.tms.services.database.TasksService;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TasksPlannerTest extends AbstractTmsSpringConfiguration {

    @Autowired
    private TasksPlanner tasksPlanner;

    @Autowired
    private BusinessProcessingJob businessProcessingJob;

    @Autowired
    private TasksService tasksService;

    @BeforeEach
    void init() {
        testControls.cleanupTasksService();
        executors.business().clearSourceTable();
    }

    @Test
    @Timeout(5)
    void runTasks() {
        var thread = new TasksPlannerThread(tasksPlanner, 1);
        thread.start();
        try {
            // Отправим простую задачу на исполнение и проверим, что она выполнилась
            testControls.checkNoScheduledTasks();
            testControls.executeJob(businessProcessingJob);

            var jobs = tasksService.getActiveJobs();
            assertEquals(1, jobs.size());

            var job = jobs.get(0);
            assertEquals(JobType.SYNC_BUSINESS_PRIORITY, job.getType());

            while (!Thread.interrupted()) {
                var tasks = tasksService.getAllTasks(job.getJob_id());
                assertEquals(1, tasks.size());

                var task = tasks.get(0);
                if (task.getStatus() == TaskStatus.SUCCESS) {
                    break;
                }
            }

        } finally {
            thread.stop();
        }
    }

}
