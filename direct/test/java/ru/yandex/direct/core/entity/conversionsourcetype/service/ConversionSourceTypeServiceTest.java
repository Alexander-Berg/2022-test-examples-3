package ru.yandex.direct.core.entity.conversionsourcetype.service;

import java.util.List;

import one.util.streamex.StreamEx;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.conversionsourcetype.model.ConversionSourceType;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.result.Result;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ConversionSourceTypeServiceTest {
    @Autowired
    private Steps steps;

    @Autowired
    private ConversionSourceTypeService service;

    @Test
    public void addConversionSourceTypeSuccess() {
        var type1 = steps.conversionSourceTypeSteps().getDefaultConversionSourceType();

        MassResult<Long> result = service.add(singletonList(type1));

        List<Long> ids = StreamEx.of(result.getResult()).map(Result::getResult).nonNull().toList();
        List<ConversionSourceType> actualConversionSourceTypes =
                steps.conversionSourceTypeSteps().getConversionSourceTypeByIds(ids);
        assertSoftly(softly -> {
            softly.assertThat(result.getValidationResult().flattenErrors()).isEmpty();
            softly.assertThat(ids).hasSize(1);
            softly.assertThat(actualConversionSourceTypes).isEqualTo(singletonList(type1));
        });
    }

    @Test
    public void addConversionSourceTypeSuccess2() {
        var type1 = steps.conversionSourceTypeSteps().getDefaultConversionSourceType().withName("name1");
        var type2 = steps.conversionSourceTypeSteps().getDefaultConversionSourceType().withName("name2");

        MassResult<Long> result = service.add(List.of(type1, type2));

        List<Long> ids = StreamEx.of(result.getResult()).map(Result::getResult).nonNull().toList();
        List<ConversionSourceType> actualConversionSourceTypes =
                steps.conversionSourceTypeSteps().getConversionSourceTypeByIds(ids);
        assertSoftly(softly -> {
            softly.assertThat(result.getValidationResult().flattenErrors()).isEmpty();
            softly.assertThat(ids).hasSize(2).doesNotHaveDuplicates();
            softly.assertThat(actualConversionSourceTypes).containsExactlyInAnyOrder(type1, type2);
        });
    }

    @Test
    public void addConversionSourceTypeWithValidationError() {
        ConversionSourceType conversionSourceTypeWithInvalidName =
                steps.conversionSourceTypeSteps().getConversionSourceTypeWithInvalidName();
        MassResult<Long> result = service.add(singletonList(conversionSourceTypeWithInvalidName));

        assertSoftly(softly -> {
            softly.assertThat(result.isSuccessful()).isTrue();
            softly.assertThat(result.getValidationResult().flattenErrors()).isNotEmpty();
            softly.assertThat(result.getResult().size()).isEqualTo(0);
        });
    }

    @Test
    public void updateConversionSourceTypeSuccess() {
        ConversionSourceType expectedConversionSourceType =
                steps.conversionSourceTypeSteps().addDraftConversionSourceType().withIsEditable(false);

        var modelChanges =
                steps.conversionSourceTypeSteps().getModelChanges(singletonList(expectedConversionSourceType));

        MassResult<Long> result = service.update(modelChanges);

        assertSoftly(softly -> {
            softly.assertThat(result.isSuccessful()).isTrue();
            softly.assertThat(result.getValidationResult().flattenErrors()).isEmpty();
        });

        ConversionSourceType actualConversionSourceType =
                steps.conversionSourceTypeSteps().getConversionSourceTypeByIds(
                        singletonList(result.getResult().get(0).getResult())
                ).get(0);

        assertThat(actualConversionSourceType).isEqualTo(expectedConversionSourceType);
    }

    @Test
    public void updateConversionSourceTypeWithValidationError() {
        ConversionSourceType expectedConversionSourceType =
                steps.conversionSourceTypeSteps().addNotEditableConversionSourceType().withIsEditable(true);

        var modelChanges =
                steps.conversionSourceTypeSteps().getModelChanges(singletonList(expectedConversionSourceType));

        MassResult<Long> result = service.update(modelChanges);

        assertSoftly(softly -> {
            softly.assertThat(result.isSuccessful()).isTrue();
            softly.assertThat(result.getValidationResult().flattenErrors()).isNotEmpty();
        });

        ConversionSourceType actualConversionSourceType =
                steps.conversionSourceTypeSteps().getConversionSourceTypeByIds(
                        singletonList(expectedConversionSourceType.getId())
                ).get(0);

        assertThat(actualConversionSourceType).isEqualTo(expectedConversionSourceType.withIsEditable(false));
    }

    @Test
    public void removeConversionSourceTypeSuccess() {
        ConversionSourceType removedConversionSourceType =
                steps.conversionSourceTypeSteps().addDraftConversionSourceType();

        MassResult<Long> result = service.remove(singletonList(removedConversionSourceType.getId()));

        assertSoftly(softly -> {
            softly.assertThat(result.isSuccessful()).isTrue();
            softly.assertThat(result.getValidationResult().flattenErrors()).isEmpty();
        });

        List<ConversionSourceType> allConversionSourceTypes = service.getAll();

        assertThat(removedConversionSourceType).isNotIn(allConversionSourceTypes);
    }

    @Test
    public void removeConversionSourceTypeWithValidationError() {
        ConversionSourceType notEditableConversionSourceType =
                steps.conversionSourceTypeSteps().addNotEditableConversionSourceType();

        MassResult<Long> result = service.remove(singletonList(notEditableConversionSourceType.getId()));

        assertSoftly(softly -> {
            softly.assertThat(result.isSuccessful()).isTrue();
            softly.assertThat(result.getValidationResult().flattenErrors()).isNotEmpty();
        });

        List<ConversionSourceType> allConversionSourceTypes = service.getAll();

        assertThat(notEditableConversionSourceType).isIn(allConversionSourceTypes);
    }

    @Test
    public void removeConversionSourceTypeWhenNotExist() {
        ConversionSourceType notEditableConversionSourceType =
                steps.conversionSourceTypeSteps().getNotEditableConversionSourceType();

        MassResult<Long> result = service.remove(singletonList(notEditableConversionSourceType.getId()));

        assertThat(result.isSuccessful()).isFalse();
    }
}
