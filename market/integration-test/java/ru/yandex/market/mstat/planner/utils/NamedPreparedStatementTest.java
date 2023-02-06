package ru.yandex.market.mstat.planner.utils;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.mstat.planner.model.Request;
import ru.yandex.market.mstat.planner.service.RequestService;
import ru.yandex.market.mstat.planner.util.NamedPreparedStatement;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class NamedPreparedStatementTest extends AbstractDbIntegrationTest {

    @Autowired
    private RequestService requestService;

    @Test
    public void testInsert() throws SQLException {
        final String sql = "insert into requests (\n" +
                "    request_project_id,\n" +
                "    request_department_id_from,\n" +
                "    request_department_id_to,\n" +
                "    request_employee,\n" +
                "    request_date_start_requested,\n" +
                "    request_date_start,\n" +
                "    request_date_end,\n" +
                "    request_job_size_requested,\n" +
                "    request_job_size,\n" +
                "    request_job_load_requested,\n" +
                "    request_job_load,\n" +
                "    request_description,\n" +
                "    request_type,\n" +
                "    request_specialization_id,\n" +
                "    request_status_changed_by,\n" +
                "    request_status_changed_at,\n" +
                "    request_status,\n" +
                "    request_author\n" +
                ") values (\n" +
                "    :request_project_id,\n" +
                "    :request_department_id_from,\n" +
                "    :request_department_id_to,\n" +
                "    :request_employee,\n" +
                "    :request_date_start_requested,\n" +
                "    :request_date_start,\n" +
                "    :request_date_end,\n" +
                "    :request_job_size_requested,\n" +
                "    :request_job_size,\n" +
                "    :request_job_load_requested,\n" +
                "    :request_job_load,\n" +
                "    :request_description,\n" +
                "    :request_type,\n" +
                "    :request_specialization_id,\n" +
                "    :request_status_changed_by,\n" +
                "    now()," +
                "    :request_status,\n" +
                "    :request_author\n" +
                ") \n" +
                "returning request_id";

        final NamedPreparedStatement namedPreparedStatement = new NamedPreparedStatement(sql, data.getDatasource().getConnection());

        final Request request = data.createPlanTemplate(12.0, data.departmentId, data.login, "100d", Date.valueOf("2021-05-10"), Date.valueOf("2021-05-20"), null, null);
        request.setDepartment_id_from(data.parentDepartmentId);
        request.setDepartment_id_to(data.parentDepartmentId);

        final Map<String, Object> params = new HashMap<>();
        params.put("request_project_id", 34);
        params.put("request_department_id_from", request.getDepartment_id_from());
        params.put("request_department_id_to", request.getDepartment_id_to());
        params.put("request_employee", request.getEmployee());
        params.put("request_date_start_requested", request.getDate_start_requested());
        params.put("request_date_start", request.getDate_start());
        params.put("request_date_end", request.getDate_end());
        params.put("request_job_size_requested", request.getJob_size_requested());
        params.put("request_job_size", request.getJob_size());
        params.put("request_job_load_requested", request.getJob_load_requested());
        params.put("request_job_load", request.getJob_load());
        params.put("request_description", request.getDescription());
        params.put("request_type", request.getType());
        params.put("request_specialization_id", null);
        params.put("request_status", request.getStatus());
        params.put("request_status_changed_by", request.getStatus_changed_by());
        params.put("request_author", request.getRequest_author());

        namedPreparedStatement.updateParams(params);

        final ResultSet resultSet = namedPreparedStatement.getPreparedStatement().executeQuery();

        final long id;
        if (resultSet.next()) {
            id = resultSet.getLong(1);
        } else {
            Assert.fail("Id not found");
            return;
        }

        final Request reqFromDb = requestService.getRequestById(id);

        Assert.assertEquals(request.getDepartment_id_from(), reqFromDb.getDepartment_id_from());
        Assert.assertEquals(request.getDepartment_id_to(), reqFromDb.getDepartment_id_to());
        Assert.assertEquals(request.getEmployee(), reqFromDb.getEmployee());
        Assert.assertEquals(request.getDate_start_requested(), reqFromDb.getDate_start_requested());
        Assert.assertEquals(request.getDate_start(), reqFromDb.getDate_start());
        Assert.assertEquals(request.getDate_end(), reqFromDb.getDate_end());
        Assert.assertEquals(request.getJob_size_requested(), reqFromDb.getJob_size_requested());
        Assert.assertEquals(request.getJob_size(), reqFromDb.getJob_size());
        Assert.assertEquals(request.getJob_load_requested().doubleValue(), reqFromDb.getJob_load_requested().doubleValue(), 1e-3);
        Assert.assertEquals(request.getJob_load().doubleValue(), reqFromDb.getJob_load().doubleValue(), 1e-3);
        Assert.assertEquals(request.getDescription(), reqFromDb.getDescription());
        Assert.assertEquals(request.getType(), reqFromDb.getType());
        Assert.assertEquals(/*request.getSpecialization_id()*/Long.valueOf(0), reqFromDb.getSpecialization_id());
        Assert.assertEquals(request.getStatus(), reqFromDb.getStatus());
        Assert.assertEquals(request.getStatus_changed_by(), reqFromDb.getStatus_changed_by());
        Assert.assertEquals(request.getRequest_author(), reqFromDb.getRequest_author());
    }
}
