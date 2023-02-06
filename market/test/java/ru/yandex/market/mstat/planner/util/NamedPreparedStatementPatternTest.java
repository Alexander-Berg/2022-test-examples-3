package ru.yandex.market.mstat.planner.util;

import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ru.yandex.market.mstat.planner.util.NamedPreparedStatement.PARAMS_PATTERN;

public class NamedPreparedStatementPatternTest {

    @NotNull
    private List<String> parseParams(String sql) {
        Pattern findParametersPattern = Pattern.compile(PARAMS_PATTERN);

        Matcher matcher = findParametersPattern.matcher(sql);

        final List<String> namedParams = new ArrayList<>();
        while (matcher.find()) {
            namedParams.add(matcher.group().substring(1));
        }
        return namedParams;
    }

    @Test
    public void testSqlInsert() {
        String sql = "with insert_projects as (\n" +
                "    insert into projects (project_created_by,\n" +
                "                          project_colors,\n" +
                "                          project_desc,\n" +
                "                          project_contours)\n" +
                "        values (:project_created_by,\n" +
                "                :project_colors::jsonb,\n" +
                "                :project_desc,\n" +
                "                :project_contours::jsonb)\n" +
                "        returning project_id\n" +
                ")\n" +
                "insert into requests (\n" +
                "                            request_project_id,\n" +
                "                            request_department_id_from,\n" +
                "                            request_department_id_to,\n" +
                "                            request_employee,\n" +
                "                            request_date_start_requested,\n" +
                "                            request_date_start,\n" +
                "                            request_date_end,\n" +
                "                            request_job_size_requested,\n" +
                "                            request_job_size,\n" +
                "                            request_job_load_requested,\n" +
                "                            request_job_load,\n" +
                "                            request_description,\n" +
                "                            request_type,\n" +
                "                            request_specialization_id,\n" +
                "                            request_status,\n" +
                "                            request_status_changed_by,\n" +
                "                            request_status_changed_at,\n" +
                "                            request_author\n" +
                "                        ) values (\n" +
                "                            (select project_id from insert_projects),\n" +
                "                            :request_department_id_from,\n" +
                "                            :request_department_id_to,\n" +
                "                            :request_employee,\n" +
                "                            :request_date_start_requested,\n" +
                "                            :request_date_start,\n" +
                "                            :request_date_end,\n" +
                "                            :request_job_size_requested,\n" +
                "                            :request_job_size,\n" +
                "                            :request_job_load_requested,\n" +
                "                            :request_job_load,\n" +
                "                            :request_description,\n" +
                "                            :request_type,\n" +
                "                            :request_specialization_id,\n" +
                "                            :request_status,\n" +
                "                            :request_status_changed_by,\n" +
                "                            now(),\n" +
                "                            :request_author\n" +
                "                        )\n" +
                "                        returning request_id, request_project_id";

        final List<String> namedParams = parseParams(sql);

        final String[] expectedArr = {
                "project_created_by",
                "project_colors",
                "project_desc",
                "project_contours",
                "request_department_id_from",
                "request_department_id_to",
                "request_employee",
                "request_date_start_requested",
                "request_date_start",
                "request_date_end",
                "request_job_size_requested",
                "request_job_size",
                "request_job_load_requested",
                "request_job_load",
                "request_description",
                "request_type",
                "request_specialization_id",
                "request_status",
                "request_status_changed_by",
                "request_author"
        };

        Assert.assertEquals(Arrays.asList(expectedArr), namedParams);
    }

    @Test
    public void testSqlSelect1() {
        final String sql = "with recursive\n" +
                "    days as (\n" +
                "        select generate_series(lower(daterange(:from::date, :to::date)), upper(daterange(:from::date, :to::date)), '1 day'::interval)::date as day\n" +
                "    ),\n" +
                "    type_requests as (\n" +
                "        select *\n" +
                "        from requests r\n" +
                "        where (daterange(request_date_start, request_date_end, '[]') && daterange(:from::date, :to::date, '[]')\n" +
                "            and request_deleted = false\n" +
                "            and request_status = 'accepted'\n" +
                "            and request_type = :type\n" +
                "            and request_employee = :employee\n" +
                "            ) and request_id != :id" +
                "    ),\n" +
                "    all_requests_by_days as (\n" +
                "        select *\n" +
                "        from days\n" +
                "            left join type_requests on (days.day <@ daterange(request_date_start, request_date_end, '[]') * daterange(:from::date, :to::date, '[]'))\n" +
                "    ),\n" +
                "    types as (\n" +
                "        select day, (sum(request_job_load) + :job_load) as sum_job_load, array_agg(request_id) as requests, true as loaded\n" +
                "        from all_requests_by_days\n" +
                "        group by day\n" +
                "        having (sum(request_job_load) + :job_load) > 1\n" +
                "        order by day\n" +
                "    )\n" +
                "select distinct unnest(types.requests) as request\n" +
                "from types";

        final List<String> namedParams = parseParams(sql);

        final String[] expectedArr = {
                "from",
                "to",
                "from",
                "to",
                "from",
                "to",
                "type",
                "employee",
                "id",
                "from",
                "to",
                "job_load",
                "job_load"
        };

        Assert.assertEquals(Arrays.asList(expectedArr), namedParams);
    }

    @Test
    public void testSqlSelect2() {
        final String sql = "with projects_contours as (\n" +
                "    select project_id, \n" +
                "           jsonb_object_keys(project_contours)::bigint as contour_id\n" +
                "    from projects\n" +
                "),\n" +
                "     requests_sizes as (\n" +
                "         select contour_id,\n" +
                "                request_status,\n" +
                "                (least(request_date_end, :to::date) - greatest(request_date_start, :from::date)) *\n" +
                "                request_job_load as request_days_load\n" +
                "         from requests r\n" +
                "                  left join projects_contours p on r.request_project_id = p.project_id\n" +
                "         where request_type = 'plan'\n" +
                "           and request_deleted = false\n" +
                "           and (daterange(request_date_start, request_date_end, '[]') && daterange(:from::date, :to::date, '[]'))\n" +
                "           and request_department_id_from = ANY (ARRAY [ :deps ])\n" +
                "     ),\n" +
                "     grouped_requests as (\n" +
                "         select contour_id,\n" +
                "                request_status,\n" +
                "                grouping(request_status),\n" +
                "                sum(request_days_load),\n" +
                "                count(*),\n" +
                "                abs(sum(request_days_load) / (:to::date - :from::date)::numeric) as fte\n" +
                "         from requests_sizes\n" +
                "         group by grouping sets ((contour_id, request_status), (contour_id))\n" +
                "         order by contour_id, request_status\n" +
                "     )\n" +
                "select gr.*, c.name as contour_name\n" +
                "from grouped_requests gr\n" +
                "         left join contours c on gr.contour_id = c.id";

        final List<String> namedParams = parseParams(sql);

        final String[] expectedArr = {
                "to",
                "from",
                "from",
                "to",
                "deps",
                "to",
                "from",
        };

        Assert.assertEquals(Arrays.asList(expectedArr), namedParams);
    }
}
