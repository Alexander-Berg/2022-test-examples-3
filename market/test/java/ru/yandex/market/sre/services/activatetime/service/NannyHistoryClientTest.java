package ru.yandex.market.sre.services.activatetime.service;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import com.google.gson.Gson;
import org.junit.Test;

import ru.yandex.market.sre.services.tms.eventdetector.model.core.Period;
import ru.yandex.market.tsum.clients.nanny.task_group.children.history.NannyTaskHistory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class NannyHistoryClientTest {

    public NannyTaskHistory getHistory1() throws IOException {
        String history = "{\n" +
                "    \"instance_states\": [\n" +
                "        {\n" +
                "            \"timestamp\": 1598534346,\n" +
                "            \"success_count\": 0,\n" +
                "            \"error_count\": 0,\n" +
                "            \"processing_count\": 0\n" +
                "        },\n" +
                "        {\n" +
                "            \"timestamp\": 1598534356,\n" +
                "            \"success_count\": 0,\n" +
                "            \"error_count\": 0,\n" +
                "            \"processing_count\": 1\n" +
                "        },\n" +
                "        {\n" +
                "            \"timestamp\": 1598534366,\n" +
                "            \"success_count\": 0,\n" +
                "            \"error_count\": 0,\n" +
                "            \"processing_count\": 1\n" +
                "        },\n" +
                "        {\n" +
                "            \"timestamp\": 1598534376,\n" +
                "            \"success_count\": 1,\n" +
                "            \"error_count\": 0,\n" +
                "            \"processing_count\": 1\n" +
                "        },\n" +
                "        {\n" +
                "            \"timestamp\": 1598534386,\n" +
                "            \"success_count\": 1,\n" +
                "            \"error_count\": 0,\n" +
                "            \"processing_count\": 1\n" +
                "        },\n" +
                "        {\n" +
                "            \"timestamp\": 1598534396,\n" +
                "            \"success_count\": 1,\n" +
                "            \"error_count\": 0,\n" +
                "            \"processing_count\": 1\n" +
                "        },\n" +
                "        {\n" +
                "            \"timestamp\": 1598534398,\n" +
                "            \"success_count\": 2,\n" +
                "            \"error_count\": 0,\n" +
                "            \"processing_count\": 0\n" +
                "        }\n" +
                "    ],\n" +
                "    \"instance_count\": 2\n" +
                "}";
        return new Gson().getAdapter(NannyTaskHistory.class).fromJson(history);
    }
    public NannyTaskHistory getHistory2() throws IOException {
        String history = "{\n" +
                "    \"instance_states\": [\n" +
                "        {\n" +
                "            \"timestamp\": 1599229300,\n" +
                "            \"success_count\": 0,\n" +
                "            \"error_count\": 0,\n" +
                "            \"processing_count\": 0\n" +
                "        },\n" +
                "        {\n" +
                "            \"timestamp\": 1599229310,\n" +
                "            \"success_count\": 0,\n" +
                "            \"error_count\": 0,\n" +
                "            \"processing_count\": 2\n" +
                "        },\n" +
                "        {\n" +
                "            \"timestamp\": 1599229320,\n" +
                "            \"success_count\": 0,\n" +
                "            \"error_count\": 0,\n" +
                "            \"processing_count\": 2\n" +
                "        },\n" +
                "        {\n" +
                "            \"timestamp\": 1599229330,\n" +
                "            \"success_count\": 0,\n" +
                "            \"error_count\": 0,\n" +
                "            \"processing_count\": 2\n" +
                "        },\n" +
                "        {\n" +
                "            \"timestamp\": 1599229340,\n" +
                "            \"success_count\": 0,\n" +
                "            \"error_count\": 0,\n" +
                "            \"processing_count\": 2\n" +
                "        },\n" +
                "        {\n" +
                "            \"timestamp\": 1599229350,\n" +
                "            \"success_count\": 0,\n" +
                "            \"error_count\": 0,\n" +
                "            \"processing_count\": 2\n" +
                "        },\n" +
                "        {\n" +
                "            \"timestamp\": 1599229360,\n" +
                "            \"success_count\": 0,\n" +
                "            \"error_count\": 0,\n" +
                "            \"processing_count\": 2\n" +
                "        },\n" +
                "        {\n" +
                "            \"timestamp\": 1599229370,\n" +
                "            \"success_count\": 0,\n" +
                "            \"error_count\": 0,\n" +
                "            \"processing_count\": 2\n" +
                "        },\n" +
                "        {\n" +
                "            \"timestamp\": 1599229380,\n" +
                "            \"success_count\": 0,\n" +
                "            \"error_count\": 0,\n" +
                "            \"processing_count\": 2\n" +
                "        },\n" +
                "        {\n" +
                "            \"timestamp\": 1599229390,\n" +
                "            \"success_count\": 0,\n" +
                "            \"error_count\": 0,\n" +
                "            \"processing_count\": 2\n" +
                "        },\n" +
                "        {\n" +
                "            \"timestamp\": 1599229400,\n" +
                "            \"success_count\": 0,\n" +
                "            \"error_count\": 0,\n" +
                "            \"processing_count\": 2\n" +
                "        },\n" +
                "        {\n" +
                "            \"timestamp\": 1599229410,\n" +
                "            \"success_count\": 0,\n" +
                "            \"error_count\": 0,\n" +
                "            \"processing_count\": 2\n" +
                "        },\n" +
                "        {\n" +
                "            \"timestamp\": 1599229420,\n" +
                "            \"success_count\": 0,\n" +
                "            \"error_count\": 0,\n" +
                "            \"processing_count\": 2\n" +
                "        },\n" +
                "        {\n" +
                "            \"timestamp\": 1599229430,\n" +
                "            \"success_count\": 0,\n" +
                "            \"error_count\": 0,\n" +
                "            \"processing_count\": 2\n" +
                "        },\n" +
                "        {\n" +
                "            \"timestamp\": 1599229440,\n" +
                "            \"success_count\": 0,\n" +
                "            \"error_count\": 0,\n" +
                "            \"processing_count\": 2\n" +
                "        },\n" +
                "        {\n" +
                "            \"timestamp\": 1599229450,\n" +
                "            \"success_count\": 0,\n" +
                "            \"error_count\": 0,\n" +
                "            \"processing_count\": 2\n" +
                "        },\n" +
                "        {\n" +
                "            \"timestamp\": 1599229460,\n" +
                "            \"success_count\": 0,\n" +
                "            \"error_count\": 0,\n" +
                "            \"processing_count\": 2\n" +
                "        },\n" +
                "        {\n" +
                "            \"timestamp\": 1599229470,\n" +
                "            \"success_count\": 0,\n" +
                "            \"error_count\": 0,\n" +
                "            \"processing_count\": 2\n" +
                "        },\n" +
                "        {\n" +
                "            \"timestamp\": 1599229480,\n" +
                "            \"success_count\": 0,\n" +
                "            \"error_count\": 0,\n" +
                "            \"processing_count\": 2\n" +
                "        },\n" +
                "        {\n" +
                "            \"timestamp\": 1599229490,\n" +
                "            \"success_count\": 0,\n" +
                "            \"error_count\": 0,\n" +
                "            \"processing_count\": 2\n" +
                "        },\n" +
                "        {\n" +
                "            \"timestamp\": 1599229500,\n" +
                "            \"success_count\": 0,\n" +
                "            \"error_count\": 0,\n" +
                "            \"processing_count\": 2\n" +
                "        },\n" +
                "        {\n" +
                "            \"timestamp\": 1599229510,\n" +
                "            \"success_count\": 0,\n" +
                "            \"error_count\": 0,\n" +
                "            \"processing_count\": 2\n" +
                "        },\n" +
                "        {\n" +
                "            \"timestamp\": 1599229520,\n" +
                "            \"success_count\": 0,\n" +
                "            \"error_count\": 0,\n" +
                "            \"processing_count\": 2\n" +
                "        },\n" +
                "        {\n" +
                "            \"timestamp\": 1599229530,\n" +
                "            \"success_count\": 0,\n" +
                "            \"error_count\": 0,\n" +
                "            \"processing_count\": 2\n" +
                "        },\n" +
                "        {\n" +
                "            \"timestamp\": 1599229540,\n" +
                "            \"success_count\": 0,\n" +
                "            \"error_count\": 0,\n" +
                "            \"processing_count\": 2\n" +
                "        },\n" +
                "        {\n" +
                "            \"timestamp\": 1599229550,\n" +
                "            \"success_count\": 0,\n" +
                "            \"error_count\": 0,\n" +
                "            \"processing_count\": 2\n" +
                "        },\n" +
                "        {\n" +
                "            \"timestamp\": 1599229560,\n" +
                "            \"success_count\": 0,\n" +
                "            \"error_count\": 0,\n" +
                "            \"processing_count\": 2\n" +
                "        },\n" +
                "        {\n" +
                "            \"timestamp\": 1599229570,\n" +
                "            \"success_count\": 0,\n" +
                "            \"error_count\": 0,\n" +
                "            \"processing_count\": 2\n" +
                "        },\n" +
                "        {\n" +
                "            \"timestamp\": 1599229580,\n" +
                "            \"success_count\": 0,\n" +
                "            \"error_count\": 0,\n" +
                "            \"processing_count\": 2\n" +
                "        },\n" +
                "        {\n" +
                "            \"timestamp\": 1599229590,\n" +
                "            \"success_count\": 0,\n" +
                "            \"error_count\": 0,\n" +
                "            \"processing_count\": 2\n" +
                "        },\n" +
                "        {\n" +
                "            \"timestamp\": 1599229601,\n" +
                "            \"success_count\": 0,\n" +
                "            \"error_count\": 0,\n" +
                "            \"processing_count\": 2\n" +
                "        },\n" +
                "        {\n" +
                "            \"timestamp\": 1599229611,\n" +
                "            \"success_count\": 0,\n" +
                "            \"error_count\": 0,\n" +
                "            \"processing_count\": 2\n" +
                "        },\n" +
                "        {\n" +
                "            \"timestamp\": 1599229621,\n" +
                "            \"success_count\": 0,\n" +
                "            \"error_count\": 0,\n" +
                "            \"processing_count\": 2\n" +
                "        },\n" +
                "        {\n" +
                "            \"timestamp\": 1599229631,\n" +
                "            \"success_count\": 0,\n" +
                "            \"error_count\": 0,\n" +
                "            \"processing_count\": 2\n" +
                "        },\n" +
                "        {\n" +
                "            \"timestamp\": 1599229641,\n" +
                "            \"success_count\": 0,\n" +
                "            \"error_count\": 0,\n" +
                "            \"processing_count\": 2\n" +
                "        },\n" +
                "        {\n" +
                "            \"timestamp\": 1599229651,\n" +
                "            \"success_count\": 0,\n" +
                "            \"error_count\": 0,\n" +
                "            \"processing_count\": 2\n" +
                "        },\n" +
                "        {\n" +
                "            \"timestamp\": 1599229661,\n" +
                "            \"success_count\": 2,\n" +
                "            \"error_count\": 0,\n" +
                "            \"processing_count\": 2\n" +
                "        },\n" +
                "        {\n" +
                "            \"timestamp\": 1599229671,\n" +
                "            \"success_count\": 2,\n" +
                "            \"error_count\": 0,\n" +
                "            \"processing_count\": 2\n" +
                "        },\n" +
                "        {\n" +
                "            \"timestamp\": 1599229681,\n" +
                "            \"success_count\": 2,\n" +
                "            \"error_count\": 0,\n" +
                "            \"processing_count\": 2\n" +
                "        },\n" +
                "        {\n" +
                "            \"timestamp\": 1599229690,\n" +
                "            \"success_count\": 2,\n" +
                "            \"error_count\": 0,\n" +
                "            \"processing_count\": 2\n" +
                "        }\n" +
                "    ],\n" +
                "    \"instance_count\": 4\n" +
                "}";
        return new Gson().getAdapter(NannyTaskHistory.class).fromJson(history);
    }

    @Test
    public void test() throws IOException {

        List<Period> result = NannyHistoryClient.calcTimings(getHistory1(), 0.1);
        assertTrue(result.get(0).getFrom().equals(1598534346L));
        assertTrue(result.get(0).getTo().equals(1598534376L));
        assertTrue(result.get(1).getFrom().equals(1598534366L));
        assertTrue(result.get(1).getTo().equals(1598534398L));

        result.forEach(System.out::println);
    }

    @Test
    public void test2() throws IOException {
        //https://nanny.yandex-team.ru/ui/#/alemate/taskgroups/search-0126140653/tasks/job-0000000000/history/
        List<Period> result = NannyHistoryClient.calcTimings(getHistory2(), 0.5);
        assertTrue(result.size() == 2);
        result.forEach(System.out::println);
        System.out.println(Instant.ofEpochSecond(1599229300).toString());
        System.out.println(Instant.ofEpochSecond(1599229661).toString());
    }

    @Test
    public void getFirst() {
        String taskId = "search-0128714394";
        assertEquals(128714394, Integer.parseInt(taskId.substring(8)));
    }
}
