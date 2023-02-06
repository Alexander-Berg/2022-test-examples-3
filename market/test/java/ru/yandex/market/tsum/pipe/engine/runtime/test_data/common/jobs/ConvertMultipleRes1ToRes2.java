package ru.yandex.market.tsum.pipe.engine.runtime.test_data.common.jobs;

import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipe.engine.definition.job.JobExecutor;
import ru.yandex.market.tsum.pipe.engine.definition.resources.Produces;
import ru.yandex.market.tsum.pipe.engine.definition.resources.WiredResourceList;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.common.resources.Res1;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.common.resources.Res2;

import java.util.List;
import java.util.UUID;

/**
 * @author Nikolay Firov
 * @date 15.12.2017
 */
@Produces(multiple = Res2.class)
public class ConvertMultipleRes1ToRes2 implements JobExecutor {
    @WiredResourceList(Res1.class)
    private List<Res1> resources;

    @Override
    public UUID getSourceCodeId() {
        return UUID.fromString("b298395c-9630-4b17-ac9b-fbf682fadff5");
    }

    @Override
    public void execute(JobContext context) throws Exception {
        for (int i = 0; i < resources.size(); ++i) {
            context.resources().produce(new Res2(resources.get(i).getS() + i));
        }
    }
}