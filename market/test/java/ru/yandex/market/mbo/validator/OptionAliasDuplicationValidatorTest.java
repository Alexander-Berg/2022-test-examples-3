package ru.yandex.market.mbo.validator;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.mbo.common.model.Language;
import ru.yandex.market.mbo.gwt.exceptions.dto.OptionParametersDuplicationDto;
import ru.yandex.market.mbo.gwt.models.gurulight.ParameterValuesChanges;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.CategoryParamBuilder;
import ru.yandex.market.mbo.gwt.models.params.EnumAlias;
import ru.yandex.market.mbo.gwt.models.params.Option;
import ru.yandex.market.mbo.gwt.models.params.OptionBuilder;
import ru.yandex.market.mbo.gwt.models.params.Param;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.mbo.gwt.models.visual.Word.DEFAULT_LANG_ID;

/**
 * @author dmserebr
 * @date 22/02/2019
 */
@RunWith(MockitoJUnitRunner.class)
public class OptionAliasDuplicationValidatorTest extends OptionPropertyDuplicationValidatorTest {

    private OptionAliasDuplicationValidator validator;

    @Before
    public void before() {
        super.before();

        validator = Mockito.mock(OptionAliasDuplicationValidator.class, CALLS_REAL_METHODS);
        validator.setValueLinkService(valueLinkService);
        validator.setParameterLoaderService(parameterLoaderService);
    }

    @Test
    public void testOptionNotDuplicatedWhenAliasIsInDifferentCaseButNotChanged() {
        // Option which has lowercase duplicate in terms of alias (e.g. "OPTION_ALIAS" and "option_alias") is changed,
        // but the option name/aliases remains the same
        // In this case, we should skip option name validation

        CategoryParam param = CategoryParamBuilder.newBuilder(1, "param-1", Param.Type.ENUM)
            .addOption(OptionBuilder.newBuilder(PARAM_1_OPTION_1)
                .addName("param-1-first")
                .addAlias(new EnumAlias(0, Language.RUSSIAN.getId(), "param-1-first-alias")))
            .addOption(OptionBuilder.newBuilder(PARAM_1_OPTION_2)
                .addName("PARAM-1-FIRST")
                .addAlias(new EnumAlias(0, Language.RUSSIAN.getId(), "PARAM-1-FIRST-ALIAS")))
            .build();
        optionIdsByParamId.putAll(1L, Arrays.asList(PARAM_1_OPTION_1, PARAM_1_OPTION_2));

        ParameterValuesChanges changes = new ParameterValuesChanges();
        Option updatedOption = OptionBuilder.newBuilder(PARAM_1_OPTION_2)
            .addName("PARAM-1-FIRST")
            .addAlias(new EnumAlias(0, Language.RUSSIAN.getId(), "PARAM-1-FIRST-ALIAS"))
            .setPublished(true).build();
        changes.getUpdated().add(updatedOption);

        OptionParametersDuplicationDto duplications = validator.findDuplications(param, changes);

        assertThat(duplications.haveDuplications()).isFalse();
        verify(validator, Mockito.never()).getDuplications(anyCollection(), anyCollection(), anyLong());
    }

    @Test
    public void testOptionDuplicatedWhenAliasIsInDifferentCaseAndChanged() {
        // Option which has lowercase duplicate (e.g. "OPTION" and "option") is changed,
        // but the option name remains the same
        // In this case, we should skip option name validation
        // (for now, we allow option name duplicates, e.g. for vendors)

        CategoryParam param = CategoryParamBuilder.newBuilder(1, "param-1", Param.Type.ENUM)
            .addOption(OptionBuilder.newBuilder(PARAM_1_OPTION_1)
                .addName("param-1-first")
                .addAlias(new EnumAlias(0, Language.RUSSIAN.getId(), "param-1-first-alias")))
            .addOption(OptionBuilder.newBuilder(PARAM_1_OPTION_2)
                .addName("PARAM-1-FIRST")
                .addAlias(new EnumAlias(0, Language.RUSSIAN.getId(), "PARAM-1-FIRST-ALIAS-OLD")))
            .build();
        optionIdsByParamId.putAll(1L, Arrays.asList(PARAM_1_OPTION_1, PARAM_1_OPTION_2));

        ParameterValuesChanges changes = new ParameterValuesChanges();
        Option updatedOption = OptionBuilder.newBuilder(PARAM_1_OPTION_2)
            .addName("PARAM-1-FIRST")
            .addAlias(new EnumAlias(0, Language.RUSSIAN.getId(), "PARAM-1-FIRST-ALIAS"))
            .build();
        changes.getUpdated().add(updatedOption);

        OptionParametersDuplicationDto duplications = validator.findDuplications(param, changes);

        assertThat(duplications.haveDuplications()).isTrue();
        assertThat(duplications.getGroupedDuplications()).isEmpty();
        assertThat(duplications.getNonGroupedDuplications())
            .hasSize(2)
            .containsEntry("param-1-first", set(param.getOption(PARAM_1_OPTION_1), updatedOption))
            .containsEntry("param-1-first-alias", set(param.getOption(PARAM_1_OPTION_1), updatedOption));
        verify(validator, Mockito.times(1)).getDuplications(anyCollection(), anyCollection(), anyLong());
    }

    @Test
    public void testWithDontUseAsAliasesSet() {
        // The same as the previous case but 'dont use as aliases' flag is true

        CategoryParam param = CategoryParamBuilder.newBuilder(1, "param-1", Param.Type.ENUM)
            .addOption(OptionBuilder.newBuilder(PARAM_1_OPTION_1)
                .addName("param-1-first")
                .addAlias(new EnumAlias(0, Language.RUSSIAN.getId(), "param-1-first-alias"))
                .setDontUseAsAlias(true))
            .addOption(OptionBuilder.newBuilder(PARAM_1_OPTION_2)
                .addName("PARAM-1-FIRST")
                .addAlias(new EnumAlias(0, Language.RUSSIAN.getId(), "PARAM-1-FIRST-ALIAS-OLD"))
                .setDontUseAsAlias(true))
            .build();
        optionIdsByParamId.putAll(1L, Arrays.asList(PARAM_1_OPTION_1, PARAM_1_OPTION_2));

        ParameterValuesChanges changes = new ParameterValuesChanges();
        Option updatedOption = OptionBuilder.newBuilder(PARAM_1_OPTION_2)
            .addName("PARAM-1-FIRST")
            .addAlias(new EnumAlias(0, Language.RUSSIAN.getId(), "PARAM-1-FIRST-ALIAS"))
            .setDontUseAsAlias(true)
            .build();
        changes.getUpdated().add(updatedOption);

        OptionParametersDuplicationDto duplications = validator.findDuplications(param, changes);

        assertThat(duplications.haveDuplications()).isTrue();
        assertThat(duplications.getGroupedDuplications()).isEmpty();
        assertThat(duplications.getNonGroupedDuplications())
            .hasSize(1)
            .containsEntry("param-1-first-alias", set(param.getOption(PARAM_1_OPTION_1), updatedOption));
        verify(validator, Mockito.times(1)).getDuplications(anyCollection(), anyCollection(), anyLong());
    }

    @Test
    public void testOptionNotDuplicatedIfNamesAndAliasesDifferent() {
        // Good case - no duplications

        CategoryParam param = CategoryParamBuilder.newBuilder(1, "param-1", Param.Type.ENUM)
            .addOption(OptionBuilder.newBuilder(PARAM_1_OPTION_1)
                .addName("param-1-first")
                .addAlias(new EnumAlias(0, Language.RUSSIAN.getId(), "param-1-first-alias")))
            .build();

        ParameterValuesChanges changes = new ParameterValuesChanges();
        Option addOption = OptionBuilder.newBuilder()
            .addName("param-1-second")
            .addAlias(new EnumAlias(0, Language.RUSSIAN.getId(), "param-1-second-alias"))
            .build();
        changes.getAdded().add(addOption);

        OptionParametersDuplicationDto duplications = validator.findDuplications(param, changes);

        assertThat(duplications.haveDuplications()).isFalse();
        verify(validator, Mockito.times(1)).getDuplications(anyCollection(), anyCollection(), anyLong());
    }

    @Test
    public void testOptionDuplicatedSameAsAliasOfAnotherOption() {
        // Added option with name which is the same as alias of another option -> duplications

        CategoryParam param = CategoryParamBuilder.newBuilder(1, "param-1", Param.Type.ENUM)
            .addOption(OptionBuilder.newBuilder(PARAM_1_OPTION_1)
                .addName("param-1-first")
                .addAlias(new EnumAlias(0, Language.RUSSIAN.getId(), "param-1-first-alias")))
            .build();

        ParameterValuesChanges changes = new ParameterValuesChanges();
        Option addOption = OptionBuilder.newBuilder().addName("param-1-first-alias").build();
        changes.getAdded().add(addOption);

        OptionParametersDuplicationDto duplications = validator.findDuplications(param, changes);

        assertThat(duplications.haveDuplications()).isTrue();
        assertThat(duplications.getGroupedDuplications()).isEmpty();
        assertThat(duplications.getNonGroupedDuplications())
            .hasSize(1)
            .containsEntry("param-1-first-alias", set(param.getOption(PARAM_1_OPTION_1), addOption));
        verify(validator, Mockito.times(1)).getDuplications(anyCollection(), anyCollection(), anyLong());
    }

    @Test
    public void testOptionDuplicatedHasAliasSameAsAliasOfAnotherOption() {
        // Added options with names that have alias the same as alias of another option -> duplications

        CategoryParam param = CategoryParamBuilder.newBuilder(1, "param-1", Param.Type.ENUM)
            .addOption(OptionBuilder.newBuilder(PARAM_1_OPTION_1)
                .addName("param-1-first")
                .addAlias(new EnumAlias(0, Language.RUSSIAN.getId(), "param-1-first-alias")))
            .build();

        ParameterValuesChanges changes = new ParameterValuesChanges();
        Option addOption1 = OptionBuilder.newBuilder()
            .addName("param-1-second")
            .addAlias(new EnumAlias(0, Language.RUSSIAN.getId(), "param-1-first-alias"))
            .build();
        Option addOption2 = OptionBuilder.newBuilder()
            .addName("param-1-third")
            .addAlias(new EnumAlias(0, Language.RUSSIAN.getId(), "param-1-first-alias"))
            .build();
        changes.getAdded().add(addOption1);
        changes.getAdded().add(addOption2);

        OptionParametersDuplicationDto duplications = validator.findDuplications(param, changes);

        assertThat(duplications.haveDuplications()).isTrue();
        assertThat(duplications.getGroupedDuplications()).isEmpty();
        assertThat(duplications.getNonGroupedDuplications())
            .hasSize(1)
            .containsEntry("param-1-first-alias", set(param.getOption(PARAM_1_OPTION_1), addOption1, addOption2));
        verify(validator, Mockito.times(1)).getDuplications(anyCollection(), anyCollection(), anyLong());
    }

    @Test
    public void testOptionDuplicatedHasAliasSameAsNameOfAnotherOption() {
        // Added option with name which has alias the same as name of another option -> duplications

        CategoryParam param = CategoryParamBuilder.newBuilder(1, "param-1", Param.Type.ENUM)
            .addOption(OptionBuilder.newBuilder(PARAM_1_OPTION_1)
                .addName("param-1-first")
                .addAlias(new EnumAlias(0, Language.RUSSIAN.getId(), "param-1-first-alias")))
            .build();

        ParameterValuesChanges changes = new ParameterValuesChanges();
        Option addOption = OptionBuilder.newBuilder()
            .addName("param-1-second")
            .addAlias(new EnumAlias(0, Language.RUSSIAN.getId(), "param-1-first"))
            .build();
        changes.getAdded().add(addOption);

        OptionParametersDuplicationDto duplications = validator.findDuplications(param, changes);

        assertThat(duplications.haveDuplications()).isTrue();
        assertThat(duplications.getGroupedDuplications()).isEmpty();
        assertThat(duplications.getNonGroupedDuplications())
            .hasSize(1)
            .containsEntry("param-1-first", set(param.getOption(PARAM_1_OPTION_1), addOption));
        verify(validator, Mockito.times(1)).getDuplications(anyCollection(), anyCollection(), anyLong());
    }

    @Test
    public void testNoDuplicatesWhenOptionWithSameAliasAddedButCurrentDeleted() {
        // Option that is deleted should not be taken into account during alias uniqueness validation

        CategoryParam param = CategoryParamBuilder.newBuilder(1, "param-1", Param.Type.ENUM)
            .addOption(OptionBuilder.newBuilder(PARAM_1_OPTION_1)
                .addName("param-1-first")
                .addAlias(new EnumAlias(0, Language.RUSSIAN.getId(), "param-1-first-alias")))
            .addOption(OptionBuilder.newBuilder(PARAM_1_OPTION_2)
                .addName("param-1-second")
                .addAlias(new EnumAlias(0, Language.RUSSIAN.getId(), "param-1-second-alias")))
            .build();

        ParameterValuesChanges changes = new ParameterValuesChanges();
        Option addOption = OptionBuilder.newBuilder()
            .addName("param-1-third")
            .addAlias(new EnumAlias(0, Language.RUSSIAN.getId(), "param-1-second-alias"))
            .build();
        changes.getAdded().add(addOption);
        changes.getDeleted().add(param.getOption(PARAM_1_OPTION_2));

        OptionParametersDuplicationDto duplications = validator.findDuplications(param1, changes);

        assertThat(duplications.haveDuplications()).isFalse();
        verify(validator, Mockito.times(1)).getDuplications(anyCollection(), anyCollection(), anyLong());
    }

    @Test
    public void testDuplicateAliasesWithinSameAddedOptionFail() {
        ParameterValuesChanges changes = new ParameterValuesChanges();
        Option addedOption = OptionBuilder.newBuilder(PARAM_1_OPTION_1)
            .addAlias(new EnumAlias(PARAM_1_OPTION_1, DEFAULT_LANG_ID, "Супер производитель"))
            .addAlias(new EnumAlias(PARAM_1_OPTION_1, DEFAULT_LANG_ID, "Ну такой, нормальный производитель"))
            .addAlias(new EnumAlias(PARAM_1_OPTION_1, DEFAULT_LANG_ID, "Супер производитель"))
            .addName("param-1").build();
        changes.getAdded().add(addedOption);

        OptionParametersDuplicationDto duplications = validator.findDuplications(param1, changes);
        assertThat(duplications.haveDuplications()).isTrue();
        assertThat(duplications.getOptionsWithInternallyDuplicatedProperties()).containsExactlyInAnyOrder(addedOption);
    }
}
