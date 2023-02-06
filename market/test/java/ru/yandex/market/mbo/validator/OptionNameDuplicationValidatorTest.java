package ru.yandex.market.mbo.validator;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.mbo.gwt.exceptions.dto.OptionParametersDuplicationDto;
import ru.yandex.market.mbo.gwt.models.gurulight.ParameterValuesChanges;
import ru.yandex.market.mbo.gwt.models.linkedvalues.InitializedValueLink;
import ru.yandex.market.mbo.gwt.models.linkedvalues.LinkDirection;
import ru.yandex.market.mbo.gwt.models.linkedvalues.ValueLink;
import ru.yandex.market.mbo.gwt.models.linkedvalues.ValueLinkType;
import ru.yandex.market.mbo.gwt.models.params.CategoryParamBuilder;
import ru.yandex.market.mbo.gwt.models.params.Option;
import ru.yandex.market.mbo.gwt.models.params.OptionBuilder;
import ru.yandex.market.mbo.gwt.models.params.Param;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.verify;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 10.07.2018
 */
@RunWith(MockitoJUnitRunner.class)
public class OptionNameDuplicationValidatorTest extends OptionPropertyDuplicationValidatorTest {

    private OptionNameDuplicationValidator validator;

    @Before
    public void before() {
        super.before();

        validator = Mockito.mock(OptionNameDuplicationValidator.class, CALLS_REAL_METHODS);
        validator.setValueLinkService(valueLinkService);
        validator.setParameterLoaderService(parameterLoaderService);
    }

    @Test
    public void emptyDuplications() {
        ParameterValuesChanges changes = new ParameterValuesChanges();
        Option addOption = OptionBuilder.newBuilder().addName("param-1-third").build();
        changes.getAdded().add(addOption);

        OptionParametersDuplicationDto duplications = validator.findDuplications(param1, changes);

        assertThat(duplications.haveDuplications()).isFalse();
        assertThat(duplications.getGroupedDuplications()).isEmpty();
        assertThat(duplications.getNonGroupedDuplications()).isEmpty();
    }

    @Test
    public void findDuplicationsWithAddedOptions() {
        ParameterValuesChanges changes = new ParameterValuesChanges();
        Option addOption = OptionBuilder.newBuilder().addName("param-1-second").build();
        changes.getAdded().add(addOption);

        OptionParametersDuplicationDto duplications = validator.findDuplications(param1, changes);

        assertThat(duplications.haveDuplications()).isTrue();
        assertThat(duplications.getGroupedDuplications()).isEmpty();
        assertThat(duplications.getNonGroupedDuplications())
            .hasSize(1)
            .containsEntry("param-1-second", set(param1.getOption(PARAM_1_OPTION_2), addOption));
    }

    @Test
    public void dontFindAlreadyExistentConflict() {
        param1 = CategoryParamBuilder.newBuilder(1, "param-1", Param.Type.ENUM)
            .addOption(OptionBuilder.newBuilder(PARAM_1_OPTION_1).addName("param-1-first"))
            .addOption(OptionBuilder.newBuilder(PARAM_1_OPTION_2).addName("param-1-second"))
            .addOption(OptionBuilder.newBuilder(PARAM_1_OPTION_3).addName("param-1-first"))
            .build();

        ParameterValuesChanges changes = new ParameterValuesChanges();
        Option addOption = OptionBuilder.newBuilder().addName("param-1-second").build();
        changes.getAdded().add(addOption);

        OptionParametersDuplicationDto duplications = validator.findDuplications(param1, changes);

        parameterLoaderService.addAllCategoryParams(Collections.singletonList(param1));

        assertThat(duplications.haveDuplications()).isTrue();
        assertThat(duplications.getGroupedDuplications()).isEmpty();
        assertThat(duplications.getNonGroupedDuplications())
            .hasSize(1)
            .containsEntry("param-1-second", set(param1.getOption(PARAM_1_OPTION_2), addOption));
    }

    @Test
    public void testLinkedOptionNotDuplicated() {
        // param 1 is "vendor line", param 2 is "vendor" (link direction = DIRECT)

        optionIdsByParamId.putAll(1L, Arrays.asList(PARAM_1_OPTION_1, PARAM_1_OPTION_2));
        optionIdsByParamId.putAll(2L, Arrays.asList(PARAM_2_OPTION_1, PARAM_2_OPTION_2));

        valueLinkService.saveValueLink(
            new ValueLink(2L, PARAM_2_OPTION_1, 1L, PARAM_1_OPTION_1, LinkDirection.DIRECT));
        valueLinkService.saveValueLink(
            new ValueLink(2L, PARAM_2_OPTION_1, 1L, PARAM_1_OPTION_2, LinkDirection.DIRECT));

        ParameterValuesChanges changes = new ParameterValuesChanges();
        Option addOption = OptionBuilder.newBuilder().addName("param-1-third").build();
        changes.getAdded().add(addOption);
        changes.getAddedLinks().add(new InitializedValueLink(
            OptionBuilder.newBuilder(PARAM_2_OPTION_1).setParamId(2L).build(),
            OptionBuilder.newBuilder().setParamId(1L).build(),
            LinkDirection.DIRECT, ValueLinkType.GENERAL));

        OptionParametersDuplicationDto duplications = validator.findDuplications(param1, changes);

        assertThat(duplications.haveDuplications()).isFalse();
        assertThat(duplications.getGroupedDuplications()).isEmpty();
        assertThat(duplications.getNonGroupedDuplications()).isEmpty();
    }

    @Test
    public void testLinkedOptionDuplicatedInGroup() {
        // param 1 is "vendor line", param 2 is "vendor" (link direction = DIRECT)

        optionIdsByParamId.putAll(1L, Arrays.asList(PARAM_1_OPTION_1, PARAM_1_OPTION_2));
        optionIdsByParamId.putAll(2L, Arrays.asList(PARAM_2_OPTION_1, PARAM_2_OPTION_2));

        valueLinkService.saveValueLink(
            new ValueLink(2L, PARAM_2_OPTION_1, 1L, PARAM_1_OPTION_1, LinkDirection.DIRECT));
        valueLinkService.saveValueLink(
            new ValueLink(2L, PARAM_2_OPTION_1, 1L, PARAM_1_OPTION_2, LinkDirection.DIRECT));

        ParameterValuesChanges changes = new ParameterValuesChanges();
        Option addOption = OptionBuilder.newBuilder().addName("param-1-second").build();
        changes.getAdded().add(addOption);
        changes.getAddedLinks().add(new InitializedValueLink(
            OptionBuilder.newBuilder(PARAM_2_OPTION_1).setParamId(2L).addName("param-2-first").build(),
            OptionBuilder.newBuilder().addName("param-1-second").setParamId(1L).build(),
            LinkDirection.DIRECT, ValueLinkType.GENERAL));

        OptionParametersDuplicationDto duplications = validator.findDuplications(param1, changes);

        assertThat(duplications.haveDuplications()).isTrue();
        assertThat(duplications.getGroupedDuplications()).hasSize(1)
            .containsEntry(OptionBuilder.newBuilder(PARAM_2_OPTION_1).addName("param-2-first").build(),
                ImmutableMap.of("param-1-second",
                    set(OptionBuilder.newBuilder(PARAM_1_OPTION_2).addName("param-1-second").build(),
                        OptionBuilder.newBuilder().addName("param-1-second").build())));
        assertThat(duplications.getNonGroupedDuplications()).isEmpty();
    }

    @Test
    public void testLinkedOptionDuplicatedNotInGroup() {
        // param 1 is "vendor line", param 2 is "vendor" (link direction = DIRECT)
        // duplicated outside of group - no error (2 "param-1-second" options are allowed in different groups)

        optionIdsByParamId.putAll(1L, Arrays.asList(PARAM_1_OPTION_1, PARAM_1_OPTION_2));
        optionIdsByParamId.putAll(2L, Arrays.asList(PARAM_2_OPTION_1, PARAM_2_OPTION_2));

        valueLinkService.saveValueLink(
            new ValueLink(2L, PARAM_2_OPTION_1, 1L, PARAM_1_OPTION_1, LinkDirection.DIRECT));
        valueLinkService.saveValueLink(
            new ValueLink(2L, PARAM_2_OPTION_1, 1L, PARAM_1_OPTION_2, LinkDirection.DIRECT));

        ParameterValuesChanges changes = new ParameterValuesChanges();
        Option addOption = OptionBuilder.newBuilder().addName("param-1-second").build();
        changes.getAdded().add(addOption);
        changes.getAddedLinks().add(new InitializedValueLink(
            OptionBuilder.newBuilder(PARAM_2_OPTION_2).setParamId(2L).addName("param-2-second").build(),
            OptionBuilder.newBuilder().addName("param-1-second").setParamId(1L).build(),
            LinkDirection.DIRECT, ValueLinkType.GENERAL));

        OptionParametersDuplicationDto duplications = validator.findDuplications(param1, changes);

        assertThat(duplications.haveDuplications()).isFalse();
        assertThat(duplications.getGroupedDuplications()).isEmpty();
        assertThat(duplications.getNonGroupedDuplications()).isEmpty();
    }

    @Test
    public void testLinkedOptionHasBothGroupedAndNonGroupedDuplications() {
        // param 1 is "vendor line", param 2 is "vendor" (link direction = DIRECT)

        param1 = CategoryParamBuilder.newBuilder(1, "param-1", Param.Type.ENUM)
            .addOption(OptionBuilder.newBuilder(PARAM_1_OPTION_1).addName("param-1-first"))
            .addOption(OptionBuilder.newBuilder(PARAM_1_OPTION_2).addName("param-1-second"))
            .addOption(OptionBuilder.newBuilder(PARAM_1_OPTION_3).addName("param-1-third"))
            .build();

        optionIdsByParamId.putAll(1L, Arrays.asList(PARAM_1_OPTION_1, PARAM_1_OPTION_2, PARAM_1_OPTION_3));
        optionIdsByParamId.putAll(2L, Arrays.asList(PARAM_2_OPTION_1, PARAM_2_OPTION_2));

        valueLinkService.saveValueLink(
            new ValueLink(2L, PARAM_2_OPTION_1, 1L, PARAM_1_OPTION_1, LinkDirection.DIRECT));
        valueLinkService.saveValueLink(
            new ValueLink(2L, PARAM_2_OPTION_1, 1L, PARAM_1_OPTION_2, LinkDirection.DIRECT));

        ParameterValuesChanges changes = new ParameterValuesChanges();
        Option addOption = OptionBuilder.newBuilder().addName("param-1-second").build();
        Option addOption2 = OptionBuilder.newBuilder().addName("param-1-third").build();
        changes.getAdded().add(addOption);
        changes.getAdded().add(addOption2);

        changes.getAddedLinks().add(new InitializedValueLink(
            OptionBuilder.newBuilder(PARAM_2_OPTION_1).setParamId(2L).addName("param-2-first").build(),
            OptionBuilder.newBuilder().addName("param-1-second").setParamId(1L).build(),
            LinkDirection.DIRECT, ValueLinkType.GENERAL));

        OptionParametersDuplicationDto duplications = validator.findDuplications(param1, changes);

        assertThat(duplications.haveDuplications()).isTrue();
        assertThat(duplications.getGroupedDuplications()).hasSize(1)
            .containsEntry(OptionBuilder.newBuilder(PARAM_2_OPTION_1).addName("param-2-first").build(),
                ImmutableMap.of("param-1-second",
                    set(OptionBuilder.newBuilder(PARAM_1_OPTION_2).addName("param-1-second").build(),
                        OptionBuilder.newBuilder().addName("param-1-second").build())));
        assertThat(duplications.getNonGroupedDuplications()).hasSize(1)
            .containsEntry("param-1-third", set(param1.getOption(PARAM_1_OPTION_3), addOption2));
    }

    @Test
    public void testLinkedOptionDuplicatedWhenChangedInGroup() {
        // param 1 is "vendor line", param 2 is "vendor" (link direction = DIRECT)
        // Vendor lines #1 & #2 linked to vendor #1
        // We rename vendor line #2 as vendor line #1 - we get error, as both lines are in the same group

        optionIdsByParamId.putAll(1L, Arrays.asList(PARAM_1_OPTION_1, PARAM_1_OPTION_2));
        optionIdsByParamId.putAll(2L, Arrays.asList(PARAM_2_OPTION_1, PARAM_2_OPTION_2));

        valueLinkService.saveValueLink(
            new ValueLink(2L, PARAM_2_OPTION_1, 1L, PARAM_1_OPTION_1, LinkDirection.DIRECT));
        valueLinkService.saveValueLink(
            new ValueLink(2L, PARAM_2_OPTION_1, 1L, PARAM_1_OPTION_2, LinkDirection.DIRECT));

        ParameterValuesChanges changes = new ParameterValuesChanges();
        Option updatedOption = OptionBuilder.newBuilder(PARAM_1_OPTION_2).addName("param-1-first").build();
        changes.getUpdated().add(updatedOption);

        OptionParametersDuplicationDto duplications = validator.findDuplications(param1, changes);

        assertThat(duplications.haveDuplications()).isTrue();
        assertThat(duplications.getGroupedDuplications()).hasSize(1)
            .containsEntry(OptionBuilder.newBuilder(PARAM_2_OPTION_1).addName("param-2-first").build(),
                ImmutableMap.of("param-1-first",
                    set(OptionBuilder.newBuilder(PARAM_1_OPTION_1).addName("param-1-first").build(),
                        OptionBuilder.newBuilder(PARAM_1_OPTION_2).addName("param-1-first").build())));
        assertThat(duplications.getNonGroupedDuplications()).isEmpty();
    }

    @Test
    public void testLinkedOptionNotDuplicatedWhenChangedNotInGroup() {
        // param 1 is "vendor line", param 2 is "vendor" (link direction = DIRECT)
        // Vendor lines #1 & #2 linked to vendor #1. Option #3 is not linked to any vendor.
        // We rename vendor line #3 as vendor line #1 - no error, as duplication are checked only in group

        optionIdsByParamId.putAll(1L, Arrays.asList(PARAM_1_OPTION_1, PARAM_1_OPTION_2, PARAM_1_OPTION_3));
        optionIdsByParamId.putAll(2L, Arrays.asList(PARAM_2_OPTION_1, PARAM_2_OPTION_2));

        valueLinkService.saveValueLink(
            new ValueLink(2L, PARAM_2_OPTION_1, 1L, PARAM_1_OPTION_1, LinkDirection.DIRECT));
        valueLinkService.saveValueLink(
            new ValueLink(2L, PARAM_2_OPTION_1, 1L, PARAM_1_OPTION_2, LinkDirection.DIRECT));

        ParameterValuesChanges changes = new ParameterValuesChanges();
        Option updatedOption = OptionBuilder.newBuilder(PARAM_1_OPTION_3).addName("param-1-first").build();
        changes.getUpdated().add(updatedOption);

        OptionParametersDuplicationDto duplications = validator.findDuplications(param1, changes);

        assertThat(duplications.haveDuplications()).isFalse();
    }

    @Test
    public void testOptionNotDuplicatedWhenNameIsInDifferentCaseButNotChanged() {
        // Option which has lowercase duplicate (e.g. "OPTION" and "option") is changed,
        // but the option name remains the same
        // In this case, we should skip option name validation
        // (for now, we allow option name duplicates, e.g. for vendors)

        param1 = CategoryParamBuilder.newBuilder(1, "param-1", Param.Type.ENUM)
            .addOption(OptionBuilder.newBuilder(PARAM_1_OPTION_1).addName("param-1-first"))
            .addOption(OptionBuilder.newBuilder(PARAM_1_OPTION_2).addName("PARAM-1-FIRST"))
            .build();
        optionIdsByParamId.putAll(1L, Arrays.asList(PARAM_1_OPTION_1, PARAM_1_OPTION_2));

        ParameterValuesChanges changes = new ParameterValuesChanges();
        Option updatedOption = OptionBuilder.newBuilder(PARAM_1_OPTION_2).addName("PARAM-1-FIRST")
            .setPublished(true).build();
        changes.getUpdated().add(updatedOption);

        OptionParametersDuplicationDto duplications = validator.findDuplications(param1, changes);

        assertThat(duplications.haveDuplications()).isFalse();
        verify(validator, Mockito.never()).getDuplications(anyCollection(), anyCollection(), anyLong());
    }

    @Test
    public void testOptionDuplicatedWhenNameIsInDifferentCaseAndChanged() {
        // Option name validation should not be skipped if option name is changed

        param1 = CategoryParamBuilder.newBuilder(1, "param-1", Param.Type.ENUM)
            .addOption(OptionBuilder.newBuilder(PARAM_1_OPTION_1).addName("param-1-first"))
            .addOption(OptionBuilder.newBuilder(PARAM_1_OPTION_2).addName("param-1-second"))
            .build();
        optionIdsByParamId.putAll(1L, Arrays.asList(PARAM_1_OPTION_1, PARAM_1_OPTION_2));

        ParameterValuesChanges changes = new ParameterValuesChanges();
        Option updatedOption = OptionBuilder.newBuilder(PARAM_1_OPTION_2).addName("PARAM-1-FIRST")
            .setPublished(true).build();
        changes.getUpdated().add(updatedOption);

        OptionParametersDuplicationDto duplications = validator.findDuplications(param1, changes);

        assertThat(duplications.haveDuplications()).isTrue();
        assertThat(duplications.getGroupedDuplications()).isEmpty();
        assertThat(duplications.getNonGroupedDuplications())
            .hasSize(1)
            .containsEntry("param-1-first", set(param1.getOption(PARAM_1_OPTION_1), updatedOption));
        verify(validator, Mockito.times(1)).getDuplications(anyCollection(), anyCollection(), anyLong());
    }

    @Test
    public void testOptionDuplicatedWhenNameIsInDifferentCaseNotChangedButLinkIsAdded() {
        // If link is added, we should perform full duplication check even if option name is not changed

        param1 = CategoryParamBuilder.newBuilder(1, "param-1", Param.Type.ENUM)
            .addOption(OptionBuilder.newBuilder(PARAM_1_OPTION_1).addName("param-1-first"))
            .addOption(OptionBuilder.newBuilder(PARAM_1_OPTION_2).addName("param-1-second"))
            .build();
        optionIdsByParamId.putAll(1L, Arrays.asList(PARAM_1_OPTION_1, PARAM_1_OPTION_2));
        optionIdsByParamId.putAll(2L, Arrays.asList(PARAM_2_OPTION_1, PARAM_2_OPTION_2));

        ParameterValuesChanges changes = new ParameterValuesChanges();
        Option updatedOption = OptionBuilder.newBuilder(PARAM_1_OPTION_2).addName("PARAM-1-FIRST")
            .setPublished(true).build();
        changes.getUpdated().add(updatedOption);
        changes.getAddedLinks().add(new InitializedValueLink(
            OptionBuilder.newBuilder(PARAM_2_OPTION_1).setParamId(2L).addName("param-2-first").build(),
            OptionBuilder.newBuilder(PARAM_1_OPTION_1).addName("param-1-first").setParamId(1L).build(),
            LinkDirection.DIRECT, ValueLinkType.GENERAL));

        OptionParametersDuplicationDto duplications = validator.findDuplications(param1, changes);

        assertThat(duplications.haveDuplications()).isTrue();
        assertThat(duplications.getGroupedDuplications()).isEmpty();
        assertThat(duplications.getNonGroupedDuplications())
            .hasSize(1)
            .containsEntry("param-1-first", set(param1.getOption(PARAM_1_OPTION_1), updatedOption));
        verify(validator, Mockito.times(1)).getDuplications(anyCollection(), anyCollection(), anyLong());
    }

    @Test
    public void testOptionNotDuplicatedWhenSameAddedAndDeleted() {
        // Option that is deleted should not be taken into account during name uniqueness validation

        ParameterValuesChanges changes = new ParameterValuesChanges();
        Option addOption = OptionBuilder.newBuilder().addName("param-1-second").build();
        changes.getAdded().add(addOption);
        changes.getDeleted().add(param1.getOption(PARAM_1_OPTION_2));

        OptionParametersDuplicationDto duplications = validator.findDuplications(param1, changes);

        assertThat(duplications.haveDuplications()).isFalse();
        verify(validator, Mockito.times(1)).getDuplications(anyCollection(), anyCollection(), anyLong());
    }
}
