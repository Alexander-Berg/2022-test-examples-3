package ru.yandex.market.sre.services.activatetime;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.sre.services.activatetime.dao.AppParameterRepository;
import ru.yandex.market.sre.services.tms.MarketSreTms;
import ru.yandex.market.sre.services.tms.config.ApplicationConfig;
import ru.yandex.market.sre.services.tms.eventdetector.model.core.Period;
import ru.yandex.market.tsum.clients.nanny.NannyClient;
import ru.yandex.market.tsum.clients.nanny.task_group.children.NannyTask;
import ru.yandex.market.tsum.clients.nanny.task_group.children.history.NannyTaskHistory;

@Ignore
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {MarketSreTms.class, ApplicationConfig.class})
public class ActivateTimeHistoryTest {

    @Autowired
    AppParameterRepository repository;

    @Autowired
    ActivateTimeHistory executor;

    @Autowired
    NannyClient client;

    private List<Period> parseTimings(NannyTaskHistory history) {
        List <Period> processed = new ArrayList<>(history.getInstanceCount());
        Queue <Long> timings = new LinkedList<>();
        timings.add(history.getInstanceStates().get(0).getTimestamp());
        history.getInstanceStates().forEach(state -> {
            int diff = processed.size() - (state.getSuccessCount() + state.getErrorCount());
            if (diff < 0) {
                for (; diff <0; diff++) {
                    Period period = new Period (timings.poll(), state.getTimestamp());
                    processed.add(period);
                }
            }
            diff = timings.size() - state.getProcessingCount();
            if (diff < 0) {
                for (; diff <0; diff++)
                    timings.offer(state.getTimestamp());
            }
        });
        return processed;
    }

    @Ignore
    @Test
    public void serializeTest() throws IOException {
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
        NannyTaskHistory h = new Gson().getAdapter(NannyTaskHistory.class).fromJson(history);
        parseTimings(h).forEach(System.out::println);
    }

    @Ignore
    @Test
    public void serializeNannyTaskTest() {
        String text = "[{\"task_data\": {\"status\": {\"blocked\": {\"status\": \"Ready\", \"reason\": \"\", " +
                "\"message\": \"\", \"last_transition_time\": 1598544222}}}, \"taskGroupRevisionId\": null, " +
                "\"runtimeOptions\": {\"hasConfirm\": true, \"maxFailsCount\": 0, \"worker\": {\"state\": null, " +
                "\"hostname\": \"production-yp-alemate-worker-40.vla.yp-c.yandex.net\", \"pid\": 405842, \"port\": " +
                "81}, \"taskProgress\": {\"progress\": 100.0, \"additionalCounters\": {\"errors\": \"0\", " +
                "\"successful_hosts\": \"1\", \"successes\": \"1\", \"processing\": \"0\", \"finish_time\": \"0\", " +
                "\"degrade_level\": \"0.0\", \"phase\": \"Processing instances\"}}, \"failCount\": 0, " +
                "\"blockReason\": \"Not implemented\", \"maxFailsStatus\": null, \"blockedTill\": 0}, " +
                "\"dispatcherTaskInfo\": {\"dispatcherType\": \"WORKER_TASK_DISPATCHER\", \"metaTask\": null}, " +
                "\"processorOptions\": {\"task_type\": null, \"type\": \"NewConfigurationActivateTask\", \"options\":" +
                " {\"description\": \"Activate prestable_market_nesu_iva-1598544147942\"}}, \"id\": " +
                "\"search-0124877257/job-0000000000\", \"schedulerOptions\": {\"status\": \"DONE\", \"confirmed\": " +
                "null, \"dependencies\": [], \"state\": null, \"needConfirm\": true}}, {\"task_data\": {\"status\": " +
                "{\"blocked\": {\"status\": \"Ready\", \"reason\": \"\", \"message\": \"\", \"last_transition_time\":" +
                " 1598544248}}}, \"taskGroupRevisionId\": null, \"runtimeOptions\": {\"hasConfirm\": true, " +
                "\"maxFailsCount\": 0, \"worker\": {\"state\": null, \"hostname\": \"production-yp-alemate-worker-21" +
                ".sas.yp-c.yandex.net\", \"pid\": 235164, \"port\": 81}, \"taskProgress\": {\"progress\": 100.0, " +
                "\"additionalCounters\": {}}, \"failCount\": 0, \"blockReason\": \"Not implemented\", " +
                "\"maxFailsStatus\": null, \"blockedTill\": 0}, \"dispatcherTaskInfo\": {\"dispatcherType\": " +
                "\"WORKER_TASK_DISPATCHER\", \"metaTask\": null}, \"processorOptions\": {\"task_type\": null, " +
                "\"type\": \"GlobalConfigurationActivateTask\", \"options\": {\"description\": \"Global activate " +
                "prestable_market_nesu_iva-1598544147942\"}}, \"id\": \"search-0124877257/job-0000000001\", " +
                "\"schedulerOptions\": {\"status\": \"DONE\", \"confirmed\": null, \"dependencies\": " +
                "[\"search-0124877257/job-0000000000\"], \"state\": null, \"needConfirm\": true}}]";

        List<NannyTask> result = new Gson().fromJson(text, new TypeToken<List<NannyTask>>() {
        }.getType());
        System.out.println(result);
    }

    @Ignore
    @Test
    public void execute() {
        executor.execute();
    }
}
