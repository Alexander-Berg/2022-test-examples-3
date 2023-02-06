package ru.yandex.market.mstat.planner.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Ignore;
import org.junit.Test;
import ru.yandex.market.mstat.planner.util.RestUtil;

import java.io.IOException;
import java.text.SimpleDateFormat;

import static org.junit.Assert.assertEquals;

public class RequestTest {

    @Test
    @Ignore
    public void t() throws IOException {
        ObjectMapper m = new ObjectMapper();
        Request p = m.readValue("{" +
                "\"request_id\":2," +
                "\"project_id\":1," +
                "\"created_at\":\"2019-01-24T19:33:21.772+0000\"," +
                "\"type\":\"plan\"," +
                "\"status\":\"new\"," +
                "\"department_id_from\":11," +
                "\"department_id_to\":1," +
                "\"employee\":\"oroboros\"," +
                "\"date_start\":\"04.01.2019\"," +
                "\"job_size\":\"2w\"," +
                "\"job_load_perc\":1," +
                "\"description\":\"fwsrewerg 4\"}",
            Request.class);
        System.out.println(p.getJob_load());
        System.out.println(p.getDate_end());
        System.out.println(p.getDescription());
//        System.out.println(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(
//            p.getCreated_at()));
//
//        System.out.println(m.writeValueAsString(p));
    }

    @Test
    public void testCalcDays() {
        Request r = new Request();
        r.setDate_start(new java.sql.Date(RestUtil.parseDate("01.01.2019").getTime()));
        r.setDate_end(new java.sql.Date(RestUtil.parseDate("10.01.2019").getTime()));
        assertEquals(9, r.calculateDaysCount());
        assertEquals(3, r.calculateDaysCount("03.01.2019", "06.01.2019"));
        assertEquals(4, r.calculateDaysCount("20.12.2018", "05.01.2019"));
        assertEquals(4, r.calculateDaysCount("06.01.2019", "28.05.2019"));
    }



}
