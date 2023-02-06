package ru.yandex.market.markup2.tasks.fill_param_values_metric.metric;

import org.mockito.Mock;
import ru.yandex.market.markup2.entries.task.TaskInfo;
import ru.yandex.market.markup2.processors.task.TaskProgress;
import ru.yandex.market.markup2.tasks.fill_param_values.FillParamValuesResponse;
import ru.yandex.market.markup2.tasks.fill_param_values_metric.etalon.EtalonParamValuesDataItemPayload;
import ru.yandex.market.markup2.tasks.fill_param_values_metric.etalon.EtalonParamValuesTestBase;
import ru.yandex.market.markup2.workflow.general.TaskDataItem;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author anmalysh
 */
public class FillParamValuesMetricTestBase extends EtalonParamValuesTestBase {

    protected FillParamValuesMetricRequestGenerator metricGenerator;

    @Mock
    protected TaskInfo headTaskInfo;

    @Mock
    protected TaskProgress headTaskProgress;

    protected List<TaskDataItem<EtalonParamValuesDataItemPayload, FillParamValuesResponse>> headItems;

    public void setup() {
        super.setup(true);

        metricGenerator = new FillParamValuesMetricRequestGenerator();
    }

    protected void setResponses(Collection<FillParamValuesResponse> responses,
                                Collection<? extends TaskDataItem<?, FillParamValuesResponse>> taskDataItems) {
        Iterator<FillParamValuesResponse> respIter = responses.iterator();
        for (TaskDataItem<?, FillParamValuesResponse> item : taskDataItems) {
            item.setResponseInfo(respIter.next());
        }
    }

    protected FillParamValuesResponse createResponse(Map<String, Object> characteristics) {
        return new FillParamValuesResponse(1, Arrays.asList(characteristics), Collections.emptyList());
    }
}
