package onetime;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.markup2.AppContext;
import ru.yandex.market.markup2.loading.MarkupLoader;
import ru.yandex.market.markup2.tasks.fill_param_values.FillParamValuesDataItemPayload;
import ru.yandex.market.markup2.tasks.fill_param_values.FillParamValuesDataItemsProcessor;
import ru.yandex.market.markup2.tasks.fill_param_values.FillParamValuesIdentity;
import ru.yandex.market.markup2.tasks.fill_param_values.FillParamValuesResponse;
import ru.yandex.market.markup2.workflow.responseReceiver.ResponseReceiverContext;
import ru.yandex.market.markup2.workflow.taskType.processor.HitmanTaskTypeProcessor;

import javax.annotation.Resource;

/**
 * Should be used to emulate production server task processing, if it's required to debug
 * Markup Loader implementation loads only particular list of tasks.
 * Don't forget to set it in tool-common.xml
 *
 * @author anmalysh
 */
@SuppressWarnings("checkstyle:MagicNumber")
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:tool-stable.xml"})
public class WorkflowDebugTool extends ToolBase {

    private static final Logger log = LogManager.getLogger();

    @Resource
    private MarkupLoader markupLoader;

    @Resource
    private AppContext appContext;

    private HitmanTaskTypeProcessor<FillParamValuesIdentity, FillParamValuesDataItemPayload,
        FillParamValuesResponse> processor = new HitmanTaskTypeProcessor<>(new FillParamValuesDataItemsProcessor());

    @Test
    //@Ignore("Don't need to run receive responses debug tool with unit tests")
    public void receiveResponses() {
        ResponseReceiverContext context = new ResponseReceiverContext(appContext,
            appContext.getCache().getTask(491));

        processor.receiveResponses(context);
    }
}
