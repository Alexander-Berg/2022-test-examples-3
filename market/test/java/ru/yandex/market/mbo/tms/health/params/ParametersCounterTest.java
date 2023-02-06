package ru.yandex.market.mbo.tms.health.params;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.market.mbo.common.model.KnownIds;
import ru.yandex.market.mbo.common.model.Language;
import ru.yandex.market.mbo.db.CachedSizeMeasureService;
import ru.yandex.market.mbo.gwt.models.audit.AuditAction;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.CategoryParamBuilder;
import ru.yandex.market.mbo.gwt.models.params.EnumAlias;
import ru.yandex.market.mbo.gwt.models.params.InheritedParameter;
import ru.yandex.market.mbo.gwt.models.params.Option;
import ru.yandex.market.mbo.gwt.models.params.OptionBuilder;
import ru.yandex.market.mbo.gwt.models.visual.Word;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author V.Zaytsev (breezzo@yandex-team.ru)
 * @since 26.09.2017
 */
@SuppressWarnings({"checkstyle:MagicNumber"})
public class ParametersCounterTest {

    protected static final long PARAM_ID = 1L;
    private static final long OPTION_ID = 1L;
    private static final long CATEGORY_ID = 1L;
    protected static final long SIZE_MEASURE_PARAM_ID = PARAM_ID + 100;

    @Test
    public void countOnlyFiltered() {
        CachedSizeMeasureService cachedSizeMeasureService = mockSizeMeasureService(SIZE_MEASURE_PARAM_ID);
        ParameterStatsCounter counter = new ParameterStatsCounter(cachedSizeMeasureService);
        counter.addParameters(getParams());

        assertCounters(counter, 2, 0, 0);

        Mockito.verify(cachedSizeMeasureService, Mockito.atLeastOnce()).isSizeMeasureParameter(Mockito.anyLong());
    }

    @Test
    public void calcAliasesAudit() {
        //Учтется
        AuditAction paramAction = new AuditAction();
        paramAction.setParameterId(PARAM_ID + 2);
        paramAction.setActionType(AuditAction.ActionType.UPDATE);
        paramAction.setEntityType(AuditAction.EntityType.PARAMETER);
        paramAction.setOldValue("[1, 2]");
        paramAction.setNewValue("[1, 3]");

        //Отфильтруется и не учтется
        AuditAction skippedParamAction = new AuditAction();
        skippedParamAction.setParameterId(PARAM_ID);
        skippedParamAction.setActionType(AuditAction.ActionType.UPDATE);
        skippedParamAction.setEntityType(AuditAction.EntityType.PARAMETER);
        skippedParamAction.setOldValue("[1, 2]");
        skippedParamAction.setNewValue("[1, 3]");

        //Поменяли алиасы значений параметра
        AuditAction optionAction = new AuditAction();
        optionAction.setParameterId(OPTION_ID);
        optionAction.setActionType(AuditAction.ActionType.UPDATE);
        optionAction.setEntityType(AuditAction.EntityType.OPTION);
        optionAction.setParameterId(PARAM_ID + 2);
        optionAction.setOldValue("[4, 5, 6, 7]");
        optionAction.setNewValue("[9, 0]");

        //Создался булевский параметр, отфильтруется алиасы для его значений
        AuditAction boolAction = new AuditAction();
        boolAction.setParameterId(OPTION_ID);
        boolAction.setActionType(AuditAction.ActionType.UPDATE);
        boolAction.setEntityType(AuditAction.EntityType.OPTION);
        boolAction.setParameterId(PARAM_ID + 2);
        boolAction.setOldValue("[]");
        boolAction.setNewValue("[yes, да, есть, присутствует]");

        CachedSizeMeasureService cachedSizeMeasureService = mockSizeMeasureService(SIZE_MEASURE_PARAM_ID);
        ParameterStatsCounter counter = new ParameterStatsCounter(cachedSizeMeasureService);
        counter.addParameters(getParams());

        counter.calcAliasesAudit(Arrays.asList(paramAction, skippedParamAction),
                                 Arrays.asList(optionAction, boolAction));

        Assert.assertEquals(3, counter.getCreatedAliasesCount());
        Assert.assertEquals(5, counter.getDeletedAliasesCount());
    }

    @Test
    public void testNotListActionsWontUseInStatistics() {
        AuditAction optionAction = new AuditAction();
        optionAction.setParameterId(OPTION_ID);
        optionAction.setActionType(AuditAction.ActionType.UPDATE);
        optionAction.setEntityType(AuditAction.EntityType.OPTION);
        optionAction.setParameterId(PARAM_ID + 2);
        optionAction.setOldValue("");
        optionAction.setNewValue("aliase1");
        CachedSizeMeasureService cachedSizeMeasureService = mockSizeMeasureService(SIZE_MEASURE_PARAM_ID);
        ParameterStatsCounter counter = new ParameterStatsCounter(cachedSizeMeasureService);
        counter.addParameters(getParams());

        counter.calcAliasesAudit(Collections.emptyList(), Collections.singletonList(optionAction));

        Assert.assertEquals(0, counter.getCreatedAliasesCount());
        Assert.assertEquals(0, counter.getDeletedAliasesCount());
    }

    @Test
    public void checkParamAudit() {
        //Отфильтруется
        AuditAction action1 = new AuditAction();
        action1.setParameterId(PARAM_ID);
        action1.setActionType(AuditAction.ActionType.CREATE);
        action1.setEntityType(AuditAction.EntityType.PARAMETER);

        //Учтется
        AuditAction action2 = new AuditAction();
        action2.setParameterId(PARAM_ID + 2);
        action2.setActionType(AuditAction.ActionType.CREATE);
        action2.setEntityType(AuditAction.EntityType.PARAMETER);

        //Учтется
        AuditAction action3 = new AuditAction();
        action3.setParameterId(PARAM_ID + 2);
        action3.setCategoryId(CATEGORY_ID);
        action3.setActionType(AuditAction.ActionType.DELETE);
        action3.setEntityType(AuditAction.EntityType.PARAMETER);

        //Учитывается удаление только для уникального параметр айди
        AuditAction action4 = new AuditAction();
        action4.setParameterId(PARAM_ID + 2);
        action4.setCategoryId(CATEGORY_ID + 1);
        action4.setActionType(AuditAction.ActionType.DELETE);
        action4.setEntityType(AuditAction.EntityType.PARAMETER);


        CachedSizeMeasureService cachedSizeMeasureService = mockSizeMeasureService(SIZE_MEASURE_PARAM_ID);
        ParameterStatsCounter counter = new ParameterStatsCounter(cachedSizeMeasureService);
        counter.addParameters(getParams());

        counter.applyCreatedParamsAudit(Arrays.asList(action1, action2));
        counter.setDeletedParamsCount(Arrays.asList(action3, action4));

        Assert.assertEquals(1, counter.getCreatedParamsCount());
        Assert.assertEquals(1, counter.getDeletedParamsCount());
    }

    @Test
    public void countOnlyParameterLevelValuesAndAliases() {
        List<Option> parentOption = new ArrayList<>();
        parentOption.add(option(OPTION_ID, "parentOption"));
        CategoryParam parentParam = CategoryParamBuilder.newBuilder()
            .setId(PARAM_ID)
            .setUseForGuru(Boolean.TRUE)
            .setLocalizedAliases(Collections.singletonList(word("parentAlias")))
            .setOptions(parentOption)
            .build();

        InheritedParameter childParam = new InheritedParameter(parentParam);
        childParam.setLocalizedAliases(Arrays.asList(word("parentAlias"), word("childAlias1"), word("childAlias2")));
        childParam.setOptions(
            Arrays.asList(
                option(OPTION_ID + 1, "childOption1"), option(OPTION_ID + 2, "childOption2")));

        InheritedParameter emptyChildParam = new InheritedParameter(parentParam);

        ParameterStatsCounter counter = new ParameterStatsCounter(mockSizeMeasureService());
        counter.addParameters(Arrays.asList(parentParam, childParam, emptyChildParam));

        assertCounters(counter, 1, 3, 6);
    }

    private List<CategoryParam> getParams() {
        CategoryParamBuilder paramBuilder = CategoryParamBuilder.newBuilder();
        return Arrays.asList(
                paramBuilder
                        .setId(PARAM_ID)
                        .setService(Boolean.TRUE)
                        .build(),
                paramBuilder
                        .setId(PARAM_ID + 1)
                        .setService(Boolean.FALSE)
                        .build(),
                paramBuilder
                        .setId(PARAM_ID + 2)
                        .setUseForGuru(Boolean.TRUE)
                        .build(),
                paramBuilder
                        .setId(PARAM_ID + 3)
                        .setUseForGuru(Boolean.FALSE)
                        .setUseForGurulight(Boolean.TRUE)
                        .build(),
                paramBuilder
                        .setId(KnownIds.VENDOR_PARAM_ID)
                        .build(),
                paramBuilder
                        .setId(SIZE_MEASURE_PARAM_ID)
                        .build()
        );
    }

    protected static CachedSizeMeasureService mockSizeMeasureService(Long... sizeMeasureParamIds) {
        Set<Long> ids = new HashSet<>(Arrays.asList(sizeMeasureParamIds));

        CachedSizeMeasureService mock = Mockito.mock(CachedSizeMeasureService.class);
        Mockito.when(mock.isSizeMeasureParameter(Mockito.anyLong()))
            .thenAnswer(i -> ids.contains(i.getArgument(0)));

        return mock;
    }

    private static Word word(String word) {
        return new Word(Language.RUSSIAN.getId(), word);
    }

    private static Option option(long id, String name) {
        return OptionBuilder.newBuilder(id)
            .addName(word(name))
            .addAlias(new EnumAlias(id, Language.RUSSIAN.getId(), name))
            .build();
    }

    private static void assertCounters(
        ParameterStatsCounter counter, int paramsCount, int valuesCount, int aliasesCount) {
        Assert.assertEquals(paramsCount, counter.getParametersCount());
        Assert.assertEquals(valuesCount, counter.getValuesCount());
        Assert.assertEquals(aliasesCount, counter.getAliasesCount());
    }
}
