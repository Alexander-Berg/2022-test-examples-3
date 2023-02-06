package onetime;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.ir.http.Markup;
import ru.yandex.market.markup2.tasks.fill_param_values.FillParamValuesDataItemPayload;
import ru.yandex.market.markup2.tasks.fill_param_values.FillParamValuesResponse;
import ru.yandex.market.markup2.tasks.fill_param_values.ParameterTemplate;
import ru.yandex.market.markup2.utils.model.ModelStorageService;
import ru.yandex.market.markup2.utils.param.ParamUtils;
import ru.yandex.market.markup2.workflow.general.TaskDataItem;
import ru.yandex.market.markup2.workflow.general.TaskDataItemState;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.ModelStorage;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author anmalysh
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:tool-stable.xml"})
public class ExportModelsTool extends ToolBase {

    @Resource(name = "modelStorageService")
    ModelStorageService modelStorageService;

    @Resource(name = "paramUtils")
    ParamUtils paramUtils;

    private static final List<Integer> CATEGORY_IDS = Arrays.asList(4547637);
    private static final int NOT_FILLED_MODELS_NEEDED = 100;
    private static final List<Pair<Integer, Integer>> INTERVALS = Arrays.asList(
        new Pair<>(0, 30),
        new Pair<>(30, 70),
        new Pair<>(70, 100)
    );

    @Test
    @Ignore("Don't need to run data export with unit tests")
    public void exportModelsByParamPercentage() {
        for (int categoryId : CATEGORY_IDS) {
            List<TaskDataItem<FillParamValuesDataItemPayload, FillParamValuesResponse>> items =
                getLastTaskDataItems(Markup.TaskType.FILL_PARAMETERS_VALUE, categoryId,
                TaskDataItemState.SUCCESSFULLY_PROCEEDED);

            Map<Long, Pair<Integer, Integer>> modelIdToFilledParams = new HashMap<>();

            for (TaskDataItem<FillParamValuesDataItemPayload, FillParamValuesResponse> item : items) {
                int paramsCount = item.getInputData().getAttributes().getTemplate().size();
                int filledParams = 0;
                Map<String, Object> agreedAnswers =
                    ParamUtils.buildAgreedAnswers(categoryId, item.getResponseInfo().getCharacteristics());

                if (!ParamUtils.hasCannotStatus(agreedAnswers)) {
                    for (ParameterTemplate template : item.getInputData().getAttributes().getTemplate()) {
                        Object answer = agreedAnswers.get(template.getXslName());
                        if (answer != null && !answer.equals(ParamUtils.NO_VALUE)) {
                            filledParams++;
                        }
                    }
                }

                Long modelId = item.getInputData().getModelId();
                Pair<Integer, Integer> currentFilledParams =
                    modelIdToFilledParams.computeIfAbsent(modelId, (k) -> new Pair<>(0, 0));
                modelIdToFilledParams.put(modelId, new Pair<>(currentFilledParams.getFirst() + paramsCount,
                    currentFilledParams.getSecond() + filledParams));
            }

            Map<Pair<Integer, Integer>, Set<Long>> percentageToModelId = new TreeMap<>(
                Comparator.comparing(Pair::getFirst));
            for (Map.Entry<Long, Pair<Integer, Integer>> modelParamsCount : modelIdToFilledParams.entrySet()) {
                int paramsCount = modelParamsCount.getValue().getFirst();
                int filledParams = modelParamsCount.getValue().getSecond();
                int percentage = filledParams * 100 / paramsCount;

                for (Pair<Integer, Integer> interval : INTERVALS) {
                    if (interval.getFirst() <= percentage && interval.getSecond() >= percentage) {
                        percentageToModelId.computeIfAbsent(interval, (k) -> new TreeSet<>())
                            .add(modelParamsCount.getKey());
                        break;
                    }
                }
            }

            for (Map.Entry<Pair<Integer, Integer>, Set<Long>> entry : percentageToModelId.entrySet()) {
                System.out.println("Models with percentage (" +
                    entry.getKey().getFirst() + " - " + entry.getKey().getSecond() +
                    ") for category " + categoryId + ":");
                entry.getValue().forEach(System.out::println);
                System.out.println();
            }
        }
    }

    @Test
    @Ignore("Don't need to run data export with unit tests")
    public void exportNotFilledModels() {
        AtomicInteger modelsFound = new AtomicInteger(0);
        for (int categoryId : CATEGORY_IDS) {
            Set<Long> paramsForFill = paramUtils.getParams(categoryId).stream()
                .map(MboParameters.Parameter::getId)
                .collect(Collectors.toSet());
            System.out.println("Not filled models for category " + categoryId + ":");
            modelStorageService.processModelsOfType(categoryId, ModelStorage.ModelType.GURU,
                model -> {
                    for (ModelStorage.ParameterValue value : model.getParameterValuesList()) {
                        if (paramsForFill.contains(value.getParamId())) {
                            return true;
                        }
                    }

                    System.out.println(model.getId());

                    if (modelsFound.incrementAndGet() >= NOT_FILLED_MODELS_NEEDED) {
                        return false;
                    }

                    return true;
                });
        }
    }
}
