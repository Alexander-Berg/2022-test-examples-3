package ru.yandex.market.mbo.db.modelstorage;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import ru.yandex.market.mbo.db.modelstorage.data.group.ModelSaveGroup;
import ru.yandex.market.mbo.db.modelstorage.validation.ModelValidationError;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;

import static ru.yandex.market.mbo.db.modelstorage.validation.ModelValidationError.ErrorType.MANDATORY_PARAM_EMPTY;
import static ru.yandex.market.mbo.db.modelstorage.validation.ModelValidationError.ErrorType.VALIDATION_ERROR_TYPE_UNDEFINED;

/**
 * @author apluhin
 * @created 3/2/21
 */
public class StorageStatisticServiceTest {

    private StorageStatisticService statisticService;
    private MeterRegistry meterRegistry = new SimpleMeterRegistry();

    @Before
    public void setUp() throws Exception {
        meterRegistry = Mockito.mock(MeterRegistry.class);
        statisticService = new StorageStatisticService(meterRegistry);
    }

    @Test
    public void testCorrectFillTagsForModelCounter() {
        CommonModel model = new CommonModel();
        model.setSource(CommonModel.Source.GURU);
        ModelSaveGroup modelSaveGroup = ModelSaveGroup.fromModels(Collections.singletonList(model));
        modelSaveGroup.addValidationErrors(model,
            Arrays.asList(
                new ModelValidationError(1L, VALIDATION_ERROR_TYPE_UNDEFINED),
                new ModelValidationError(2L, MANDATORY_PARAM_EMPTY)));

        ModelSaveContext modelSaveContext = new ModelSaveContext(0L);

        Counter counter = Mockito.mock(Counter.class);
        Mockito.when(meterRegistry.counter(Mockito.any(), Mockito.any(Iterable.class))).thenReturn(counter);

        statisticService.writeFailedSaveMetrics(modelSaveGroup, modelSaveContext);

        ArgumentCaptor<Iterable> captorByModel = ArgumentCaptor.forClass(Iterable.class);
        Mockito.verify(meterRegistry, Mockito.times(1)).counter(
            Mockito.eq("yt.modelstore.validation_failed_model_count_during_save.counter"),
            captorByModel.capture()
        );
        ArgumentCaptor<Iterable> captorByType = ArgumentCaptor.forClass(Iterable.class);
        Mockito.verify(meterRegistry, Mockito.times(2)).counter(
            Mockito.eq("yt.modelstore.validation_failed_type_count_during_save.counter"),
            captorByType.capture()
        );
        Set<String> tagsByModel = convertIterableTags((Iterable<Tag>) captorByModel.getValue());
        Set<String> tagsByFirstError = convertIterableTags((Iterable<Tag>) captorByType.getAllValues().get(0));
        Set<String> tagsBySecondError = convertIterableTags((Iterable<Tag>) captorByType.getAllValues().get(1));

        if (!tagsByFirstError.contains("error_type=MANDATORY_PARAM_EMPTY")) {
            tagsByFirstError = convertIterableTags((Iterable<Tag>) captorByType.getAllValues().get(1));
            tagsBySecondError = convertIterableTags((Iterable<Tag>) captorByType.getAllValues().get(0));
        }

        Assertions.assertThat(tagsByModel).containsExactlyInAnyOrder(
            "source_system=MBO",
            "source_type=GURU"
        );
        Assertions.assertThat(tagsByFirstError).containsExactlyInAnyOrder(
            "source_system=MBO",
            "error_type=MANDATORY_PARAM_EMPTY",
            "error_sub_type=",
            "source_type=GURU"
        );
        Assertions.assertThat(tagsBySecondError).containsExactlyInAnyOrder(
            "source_system=MBO",
            "error_type=VALIDATION_ERROR_TYPE_UNDEFINED",
            "error_sub_type=",
            "source_type=GURU"
        );
    }

    private Set<String> convertIterableTags(Iterable<Tag> tags) {
        return StreamSupport
            .stream(tags.spliterator(), false)
            .map(it -> String.format("%s=%s", it.getKey(), it.getValue()))
            .collect(Collectors.toSet());
    }
}
