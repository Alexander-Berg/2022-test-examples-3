package ru.yandex.direct.internaltools.tools.conversionsourcetype.tool;

import java.util.Objects;
import java.util.stream.Collectors;

import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.conversionsourcetype.model.ConversionSourceType;
import ru.yandex.direct.core.entity.conversionsourcetype.model.ConversionSourceTypeCode;
import ru.yandex.direct.core.entity.conversionsourcetype.service.ConversionSourceTypeService;
import ru.yandex.direct.core.testing.steps.ConversionSourceTypeSteps;
import ru.yandex.direct.internaltools.configuration.InternalToolsTest;
import ru.yandex.direct.internaltools.tools.conversionsourcetype.model.ConversionSourceTypeAction;
import ru.yandex.direct.internaltools.tools.conversionsourcetype.model.ConversionSourceTypeInput;

import static java.util.Collections.singletonList;

@InternalToolsTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ConversionSourceTypeManagingToolTest {
    @Autowired
    private ConversionSourceTypeManagingTool conversionSourceTypeManagingTool;

    @Autowired
    private ConversionSourceTypeService conversionSourceTypeService;

    @Autowired
    private ConversionSourceTypeSteps conversionSourceTypeSteps;

    @Test
    public void processCreate_success() {
        var input = new ConversionSourceTypeInput()
                .withAction(ConversionSourceTypeAction.CREATE)
                .withName("test processCreate_success")
                .withDescription(ConversionSourceTypeSteps.DESCRIPTION)
                .withIsDraft(true)
                .withPosition(10L)
                .withCode(ConversionSourceTypeCode.OTHER)
                .withIsEditable(true);

        var result = conversionSourceTypeManagingTool.process(input);

        var addedConversionSourceTypeId = conversionSourceTypeService.getAll().stream()
                .filter(conversionSourceType -> Objects.equals(conversionSourceType.getName(), input.getName()))
                .map(ConversionSourceType::getId)
                .collect(Collectors.toList())
                .get(0);

        var expectedConversionSourceType = ConversionSourceTypeConverter.toConversionSourceType(input.withId(addedConversionSourceTypeId));
        var actualConversionSourceType = conversionSourceTypeSteps.getConversionSourceTypeByIds(singletonList(addedConversionSourceTypeId)).get(0);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result.getMessage()).isEqualTo(String.format(
                    ConversionSourceTypeManagingTool.OPERATION_SUCCESSFUL_STRING,
                    0,
                    ConversionSourceTypeManagingTool.CREATING_OPERATION_STRING
            ));
            softly.assertThat(actualConversionSourceType).isEqualTo(expectedConversionSourceType);
        });
    }

    @Test
    public void processCreate_withValidationError() {
        var input = new ConversionSourceTypeInput()
                .withAction(ConversionSourceTypeAction.CREATE)
                .withName("test processCreate_withValidationError")
                .withDescription(ConversionSourceTypeSteps.INVALID_DESCRIPTION)
                .withIsDraft(true)
                .withPosition(10L)
                .withCode(ConversionSourceTypeCode.OTHER)
                .withIsEditable(true);

        var result = conversionSourceTypeManagingTool.process(input);

        var addedConversionSourceTypeIds = conversionSourceTypeService.getAll().stream()
                .filter(conversionSourceType -> Objects.equals(conversionSourceType.getName(), input.getName()))
                .map(ConversionSourceType::getId)
                .collect(Collectors.toList());

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result.getMessage()).containsSubsequence(String.format(
                    ConversionSourceTypeManagingTool.OPERATION_WITH_VALIDATION_ERRORS_STRING,
                    ConversionSourceTypeManagingTool.CREATING_OPERATION_STRING,
                    0,
                    ""
            ));
            softly.assertThat(addedConversionSourceTypeIds).isEmpty();
        });
    }

    @Test
    public void processUpdate_success() {
        var conversionSourceType = conversionSourceTypeSteps.addDefaultConversionSourceType();
        var id = conversionSourceType.getId();

        var input = new ConversionSourceTypeInput()
                .withAction(ConversionSourceTypeAction.UPDATE)
                .withId(id)
                .withName("test processUpdate_withValidationError")
                .withUnsetIconUrl(false)
                .withUnsetActivationUrl(false)
                .withIsDraft(true)
                .withIsEditable(true);

        var result = conversionSourceTypeManagingTool.process(input);

        var expectedConversionSourceType = conversionSourceType
                .withName(input.getName())
                .withIsDraft(input.getIsDraft())
                .withIsEditable(input.getIsEditable());
        var actualConversionSourceType =
                conversionSourceTypeSteps.getConversionSourceTypeByIds(singletonList(id)).get(0);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result.getMessage()).isEqualTo(String.format(
                    ConversionSourceTypeManagingTool.OPERATION_SUCCESSFUL_STRING,
                    id,
                    ConversionSourceTypeManagingTool.UPDATING_OPERATION_STRING
            ));
            softly.assertThat(actualConversionSourceType).isEqualTo(expectedConversionSourceType);
        });
    }

    @Test
    public void processUpdate_withValidationError() {
        var expectedConversionSourceType = conversionSourceTypeSteps.addNotEditableConversionSourceType();
        var id = expectedConversionSourceType.getId();

        var input = new ConversionSourceTypeInput()
                .withAction(ConversionSourceTypeAction.UPDATE)
                .withId(id)
                .withName("test processUpdate_withValidationError")
                .withUnsetIconUrl(false)
                .withUnsetActivationUrl(false)
                .withIsDraft(false)
                .withIsEditable(true);

        var result = conversionSourceTypeManagingTool.process(input);

        var actualConversionSourceType =
                conversionSourceTypeSteps.getConversionSourceTypeByIds(singletonList(id)).get(0);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result.getMessage()).containsSubsequence(String.format(
                    ConversionSourceTypeManagingTool.OPERATION_WITH_VALIDATION_ERRORS_STRING,
                    ConversionSourceTypeManagingTool.UPDATING_OPERATION_STRING,
                    id,
                    ""
            ));
            softly.assertThat(actualConversionSourceType).isEqualTo(expectedConversionSourceType);
        });
    }

    @Test
    public void processRemove_success() {
        var conversionSourceType = conversionSourceTypeSteps.addDefaultConversionSourceType();
        var id = conversionSourceType.getId();
        var input = new ConversionSourceTypeInput()
                .withAction(ConversionSourceTypeAction.REMOVE)
                .withId(id);

        var result = conversionSourceTypeManagingTool.process(input);

        var allConversionSourceTypes = conversionSourceTypeService.getAll();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result.getMessage()).isEqualTo(String.format(
                    ConversionSourceTypeManagingTool.OPERATION_SUCCESSFUL_STRING,
                    id,
                    ConversionSourceTypeManagingTool.REMOVING_OPERATION_STRING
            ));
            softly.assertThat(allConversionSourceTypes).doesNotContain(conversionSourceType);
        });
    }

    @Test
    public void processRemove_withError() {
        var id = -1L;

        var input = new ConversionSourceTypeInput()
                .withAction(ConversionSourceTypeAction.REMOVE)
                .withId(id);

        var result = conversionSourceTypeManagingTool.process(input);

        var foundedConversionSourceType =
                conversionSourceTypeSteps.getConversionSourceTypeByIds(singletonList(id));

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result.getMessage()).containsSubsequence(String.format(
                    ConversionSourceTypeManagingTool.OPERATION_WITH_ERRORS_STRING,
                    ConversionSourceTypeManagingTool.REMOVING_OPERATION_STRING,
                    id,
                    ""
            ));
            softly.assertThat(foundedConversionSourceType).isEmpty();
        });
    }
}
