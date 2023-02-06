package ru.yandex.market.mbo.db.transfer.step;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.mbo.gwt.models.transfer.ModelTransfer;
import ru.yandex.market.mbo.gwt.models.transfer.ModelTransferStep;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author danfertev
 * @since 02.10.2018
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {MboModelTransferStepConfig.class})
@SuppressWarnings("checkstyle:MagicNumber")
public class MboModelTransferStepConfigTest {
    private static final int NO_STEP_INDEX = -1;

    @Resource
    private Map<ModelTransfer.Type, List<ModelTransferStep.Type>> transferStepMap;

    @Resource
    private Map<ModelTransferStep.Type, List<ModelTransferStep.Type>> stepDependencies;

    @Test
    public void testNoForwardDependencies() {
        transferStepMap.forEach((transferType, steps) -> {
            Map<ModelTransferStep.Type, Integer> stepIndexes = IntStream.range(0, steps.size())
                .boxed()
                .collect(Collectors.toMap(steps::get, Function.identity()));
            steps.forEach(step -> {
                List<Integer> dependencyIndexes = stepDependencies.getOrDefault(step, Collections.emptyList()).stream()
                    .map(d -> stepIndexes.getOrDefault(d, NO_STEP_INDEX))
                    .collect(Collectors.toList());
                Integer currentStepIndex = stepIndexes.getOrDefault(step, NO_STEP_INDEX);
                Assertions.assertThat(dependencyIndexes)
                    .allMatch(i -> i < currentStepIndex,
                        "Dependency step index is less then step index " + currentStepIndex + " of step " + step);
            });
        });
    }

    @Test
    public void testNoDuplicateSteps() {
        transferStepMap.forEach((transferType, steps) -> Assertions.assertThat(steps).doesNotHaveDuplicates());
    }
}
