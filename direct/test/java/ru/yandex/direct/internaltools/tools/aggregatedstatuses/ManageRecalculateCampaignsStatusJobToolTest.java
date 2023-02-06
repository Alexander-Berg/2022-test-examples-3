package ru.yandex.direct.internaltools.tools.aggregatedstatuses;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.internaltools.configuration.InternalToolsTest;
import ru.yandex.direct.internaltools.tools.aggregatedstatuses.model.RecalculateCampaignsStatusJobParameters;
import ru.yandex.direct.validation.defect.ids.NumberDefectIds;

import static org.apache.commons.lang3.RandomUtils.nextInt;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.direct.internaltools.tools.aggregatedstatuses.model.RecalculateCampaignsStatusJobParameters.ACTION_LABEL;
import static ru.yandex.direct.internaltools.tools.aggregatedstatuses.model.RecalculateCampaignsStatusJobParameters.ALL_SHARDS_LABEL;
import static ru.yandex.direct.internaltools.tools.aggregatedstatuses.model.RecalculateCampaignsStatusJobParameters.RECALCULATION_DEPTH_LABEL;
import static ru.yandex.direct.internaltools.tools.aggregatedstatuses.model.RecalculateCampaignsStatusJobParameters.SHARD_LABEL;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.DefectIds.CANNOT_BE_NULL;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@InternalToolsTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ManageRecalculateCampaignsStatusJobToolTest {
    @Autowired
    private ManageRecalculateCampaignsStatusJobTool manageRecalculateCampaignsStatusJobTool;

    @Autowired
    private ShardHelper shardHelper;

    private int shardCount;

    @Before
    public void before() {
        shardCount = shardHelper.dbShards().size();
    }

    @Test
    public void validateNegativeWithoutAction() {
        var parameters = new RecalculateCampaignsStatusJobParameters()
                .withRecalculationDepth(RecalculateCampaignsStatusJobParameters.RecalculationDepth.ADGROUPS)
                .withAllShards(true);

        var validationResult = manageRecalculateCampaignsStatusJobTool.validate(parameters);
        assertThat("Пустая опция \"" + ACTION_LABEL + "\" бросает ошибку валидации", validationResult,
                hasDefectWithDefinition(validationError(path(field(ACTION_LABEL)), CANNOT_BE_NULL)));
    }

    @Test
    public void validateNegativeWithoutRecalculationDepth() {
        var parameters = new RecalculateCampaignsStatusJobParameters()
                .withAction(RecalculateCampaignsStatusJobParameters.Action.CONTINUE)
                .withAllShards(true);

        var validationResult = manageRecalculateCampaignsStatusJobTool.validate(parameters);
        assertThat("Пустая опция \"" + RECALCULATION_DEPTH_LABEL + "\" бросает ошибку валидации", validationResult,
                hasDefectWithDefinition(validationError(path(field(RECALCULATION_DEPTH_LABEL)), CANNOT_BE_NULL)));
    }

    @Test
    public void validatePositiveWithAllShardsWithoutChosenShard() {
        var parameters = new RecalculateCampaignsStatusJobParameters()
                .withAction(RecalculateCampaignsStatusJobParameters.Action.PAUSE)
                .withRecalculationDepth(RecalculateCampaignsStatusJobParameters.RecalculationDepth.ALL)
                .withAllShards(true);

        var validationResult = manageRecalculateCampaignsStatusJobTool.validate(parameters);
        assertThat("Включенная опция \"" + ALL_SHARDS_LABEL + "\" принимается без ошибок валидации",
                validationResult, hasNoDefectsDefinitions());
    }

    @Test
    public void validateNegativeWithoutAllShardsWithoutChosenShard() {
        var parameters = new RecalculateCampaignsStatusJobParameters()
                .withAction(RecalculateCampaignsStatusJobParameters.Action.NEW_START)
                .withRecalculationDepth(RecalculateCampaignsStatusJobParameters.RecalculationDepth.CAMPAIGNS)
                .withAllShards(false);

        var validationResult = manageRecalculateCampaignsStatusJobTool.validate(parameters);
        assertThat("Выключенная опция \"" + ALL_SHARDS_LABEL + "\" без заполненного поля \"" + SHARD_LABEL
                        + "\" бросает ошибку валидации", validationResult,
                hasDefectWithDefinition(validationError(path(field(SHARD_LABEL)), CANNOT_BE_NULL)));
    }

    @Test
    public void validatePositiveWithoutAllShardsWithCorrectChosenShard() {
        var parameters = new RecalculateCampaignsStatusJobParameters()
                .withAction(RecalculateCampaignsStatusJobParameters.Action.NEW_START)
                .withRecalculationDepth(RecalculateCampaignsStatusJobParameters.RecalculationDepth.ADGROUPSUBJECTS)
                .withAllShards(false)
                .withShard(nextInt(1, shardCount + 1));

        var validationResult = manageRecalculateCampaignsStatusJobTool.validate(parameters);
        assertThat("Выключенная опция \"" + ALL_SHARDS_LABEL + "\" вместе с существующим номером шарда в поле \""
                + SHARD_LABEL + "\" принимается без ошибок валидации", validationResult, hasNoDefectsDefinitions());
    }

    @Test
    public void validateNegativeWithoutAllShardsWithIncorrectChosenShard() {
        var parameters = new RecalculateCampaignsStatusJobParameters()
                .withAction(RecalculateCampaignsStatusJobParameters.Action.CONTINUE)
                .withRecalculationDepth(RecalculateCampaignsStatusJobParameters.RecalculationDepth.CAMPAIGNS)
                .withAllShards(false)
                .withShard(shardCount + 100);

        var validationResult = manageRecalculateCampaignsStatusJobTool.validate(parameters);
        assertThat("Выключенная опция \"" + ALL_SHARDS_LABEL + "\" вместе с несуществующим номером шарда в поле \""
                        + SHARD_LABEL + "\" бросает ошибку валидации", validationResult,
                hasDefectWithDefinition(validationError(path(field(SHARD_LABEL)),
                        NumberDefectIds.MUST_BE_IN_THE_INTERVAL_INCLUSIVE)));
    }
}
