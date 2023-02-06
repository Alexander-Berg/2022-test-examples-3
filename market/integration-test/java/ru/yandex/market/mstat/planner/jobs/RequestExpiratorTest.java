package ru.yandex.market.mstat.planner.jobs;

import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mstat.planner.model.Request;
import ru.yandex.market.mstat.planner.service.RequestService;
import ru.yandex.market.mstat.planner.utils.AbstractDbIntegrationTest;
import ru.yandex.market.mstat.planner.task.cron.RequestExpirator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class RequestExpiratorTest extends AbstractDbIntegrationTest {

    @Autowired
    private RequestExpirator requestExpirator;

    @Autowired
    private RequestService requestService;

    @Test
    public void testRequestExpirator() {

        data.jdbcTemplate.exec("" +
                "INSERT INTO requests (request_project_id, request_created_at, request_deleted, request_type, request_status,\n"+
                "request_department_id_from, request_department_id_to, request_employee, request_date_start, request_date_end,\n"+
                "request_job_size_requested, request_job_size, request_job_load_requested, request_job_load, request_description,\n"+
                "request_status_changed_by, request_status_changed_at, request_date_start_requested, request_specialization_id,\n"+
                "request_cell, request_author) VALUES\n"+
                "(39401,'2021-03-31',false,'plan','pending',117,117,NULL,'2021-04-05','2021-07-04','1q','1q','1.0000','1.0000','Если будем ходить через Фронт то будет оценочно минимум 2Q, это второй','shpizel','2021-03-31','2021-04-05',22,NULL,'shpizel'),\n"+
                "(49062,'2021-03-30',false,'plan','pending',117,117,NULL,'2021-06-01','2021-07-12','6w','6w','1.0000','1.0000','Разработчик индексатораТикет https://st.yandex-team.ru/MARKETPROJECT-5242','v-shchukin','2021-03-30','2021-06-01',1,NULL,'v-shchukin'),\n"+
                "(54390,'2021-04-16',false,'plan','pending',97,97,NULL,'2021-07-01','2021-12-31','2q','2q','1.0000','1.0000','https://st.yandex-team.ru/MARKETFRONTECH-2088\nПеревод b2c репозитория на typescript','dgudenkov','2021-04-16','2021-07-01',NULL,NULL,'dgudenkov')"
        );
        List<Request> oldRequestList = requestService.getOldPendingRequests();
        assertEquals(3, oldRequestList.size());

        requestExpirator.execute();
        List<Request> emptyRequestList = requestService.getOldPendingRequests();
        assertTrue(emptyRequestList.isEmpty());

    }

}
