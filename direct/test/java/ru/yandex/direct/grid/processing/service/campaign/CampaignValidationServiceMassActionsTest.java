package ru.yandex.direct.grid.processing.service.campaign;

import java.math.BigDecimal;
import java.util.List;

import one.util.streamex.LongStreamEx;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.exception.GridValidationException;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdMeaningfulGoalRequest;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateMeaningfulGoals;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;

import static ru.yandex.direct.core.entity.client.Constants.DEFAULT_CAMPS_COUNT_LIMIT;
import static ru.yandex.direct.grid.processing.service.campaign.CampaignValidationService.MAX_CAMPAIGNS_COUNT_PER_UPDATE;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationMatchers.gridDefect;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationMatchers.hasErrorsWith;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationMatchers.hasValidationResult;
import static ru.yandex.direct.validation.defect.CollectionDefects.duplicatedElement;
import static ru.yandex.direct.validation.defect.CollectionDefects.maxCollectionSize;
import static ru.yandex.direct.validation.defect.CollectionDefects.notEmptyCollection;
import static ru.yandex.direct.validation.defect.CommonDefects.validId;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CampaignValidationServiceMassActionsTest {

    @Autowired
    public CampaignValidationService service;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testSuccess() {
        var request = new GdUpdateMeaningfulGoals()
                .withCampaignIds(List.of(1L))
                .withMeaningfulGoals(List.of(
                        new GdMeaningfulGoalRequest()
                                .withGoalId(1L)
                                .withConversionValue(BigDecimal.TEN)
                ));
        service.validateUpdateMeaningfulGoalsRequest(request);
    }

    @Test
    public void testNoCids() {
        var request = new GdUpdateMeaningfulGoals()
                .withCampaignIds(List.of())
                .withMeaningfulGoals(List.of(
                        new GdMeaningfulGoalRequest()
                                .withGoalId(1L)
                                .withConversionValue(BigDecimal.TEN)
                ));
        checkExceptional(request, path(field(GdUpdateMeaningfulGoals.CAMPAIGN_IDS)), notEmptyCollection());
    }

    @Test
    public void testTooLongCidsList() {
        var cids = LongStreamEx.range(1L, DEFAULT_CAMPS_COUNT_LIMIT + 2L).boxed().toList();
        var request = new GdUpdateMeaningfulGoals()
                .withCampaignIds(cids)
                .withMeaningfulGoals(List.of(
                        new GdMeaningfulGoalRequest()
                                .withGoalId(1L)
                                .withConversionValue(BigDecimal.TEN)
                ));
        checkExceptional(request,
                path(field(GdUpdateMeaningfulGoals.CAMPAIGN_IDS)),
                maxCollectionSize(MAX_CAMPAIGNS_COUNT_PER_UPDATE));
    }

    @Test
    public void testDuplicateCids() {
        var request = new GdUpdateMeaningfulGoals()
                .withCampaignIds(List.of(1L, 1L))
                .withMeaningfulGoals(List.of(
                        new GdMeaningfulGoalRequest()
                                .withGoalId(1L)
                                .withConversionValue(BigDecimal.TEN)
                ));
        checkExceptional(request, path(field(GdUpdateMeaningfulGoals.CAMPAIGN_IDS), index(0)), duplicatedElement());
    }

    @Test
    public void testInvalidCid() {
        var request = new GdUpdateMeaningfulGoals()
                .withCampaignIds(List.of(-1L))
                .withMeaningfulGoals(List.of(
                        new GdMeaningfulGoalRequest()
                                .withGoalId(1L)
                                .withConversionValue(BigDecimal.TEN)
                ));
        checkExceptional(request, path(field(GdUpdateMeaningfulGoals.CAMPAIGN_IDS), index(0)), validId());
    }

    @Test
    public void testInvalidGoalId() {
        var request = new GdUpdateMeaningfulGoals()
                .withCampaignIds(List.of(1L))
                .withMeaningfulGoals(List.of(
                        new GdMeaningfulGoalRequest()
                                .withGoalId(-1L)
                                .withConversionValue(BigDecimal.TEN)
                ));
        checkExceptional(request,
                path(field(GdUpdateMeaningfulGoals.MEANINGFUL_GOALS), index(0), field(GdMeaningfulGoalRequest.GOAL_ID)),
                validId());
    }

    private void checkExceptional(GdUpdateMeaningfulGoals request, Path path, Defect defect) {
        thrown.expect(GridValidationException.class);
        thrown.expect(hasValidationResult(hasErrorsWith(gridDefect(path, defect))));

        service.validateUpdateMeaningfulGoalsRequest(request);
    }
}
