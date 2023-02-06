package ru.yandex.market.mstat.planner.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class ProjectTest {

    @Test
    @Ignore
    public void t() throws IOException {
        ObjectMapper m = new ObjectMapper();
        Project p = m.readValue("{" +
                "\"project_id\":\"\"," +
                "\"priority\":0.0000," +
                "\"created_by\":\"polosatik\"," +
                "\"created_at\":\"2019-01-24T13:30:48.849+0000\"," +
                "\"load_r\":0.0000,\"load_w\":1.0000,\"load_b\":0.0000,\"load_l\":0.0000," +
                "\"project_desc\":\"описание\"}",
            Project.class);
        System.out.println(p);

        System.out.println(m.writeValueAsString(p));
    }



}
