package ru.yandex.direct.grid.processing.service.showcondition.bids;

import java.util.Collections;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.bids.service.BidService;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.exception.GridValidationException;
import ru.yandex.direct.grid.processing.model.showcondition.mutation.GdSetAutoBids;
import ru.yandex.direct.grid.processing.model.showcondition.mutation.GdSetAutoBidsNetworkByCoverage;
import ru.yandex.direct.grid.processing.model.showcondition.mutation.GdSetAutoBidsSearchByTrafficVolume;
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;
import ru.yandex.direct.grid.processing.service.showcondition.validation.ShowConditionValidationService;
import ru.yandex.direct.grid.processing.service.validation.GridValidationService;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.direct.model.ModelProperty;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.test.utils.RandomNumberUtils;
import ru.yandex.direct.validation.defect.CommonDefects;
import ru.yandex.direct.validation.defect.NumberDefects;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.direct.core.entity.bids.validation.SetAutoBidValidator.MAX_CONTEXT_COVERAGE;
import static ru.yandex.direct.core.entity.bids.validation.SetAutoBidValidator.MAX_INCREASE_PERCENT;
import static ru.yandex.direct.core.entity.bids.validation.SetAutoBidValidator.MAX_TARGET_TRAFFIC_VOLUME;
import static ru.yandex.direct.core.entity.bids.validation.SetAutoBidValidator.MIN_CONTEXT_COVERAGE;
import static ru.yandex.direct.core.entity.bids.validation.SetAutoBidValidator.MIN_INCREASE_PERCENT;
import static ru.yandex.direct.core.entity.bids.validation.SetAutoBidValidator.MIN_TARGET_TRAFFIC_VOLUME;
import static ru.yandex.direct.grid.processing.service.validation.GridDefectDefinitions.mustContainNonNullProps;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationMatchers.gridDefect;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationMatchers.hasErrorsWith;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationMatchers.hasValidationResult;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
@ParametersAreNonnullByDefault
public class SetAutoBidsPreValidationTest {

    @Mock
    private GridContextProvider gridContextProvider;

    @Mock
    private BidService bidService;

    @Mock
    private GridValidationService gridValidationService;

    @Autowired
    private ShowConditionValidationService showConditionValidationService;

    private BidsDataService bidsDataService;

    private GdSetAutoBids input;

    @Before
    public void initTestData() {
        MockitoAnnotations.initMocks(this);

        bidsDataService = new BidsDataService(bidService, gridValidationService, null,
                showConditionValidationService, null, gridContextProvider);

        input = new GdSetAutoBids()
                .withKeywordIds(List.of(RandomNumberUtils.nextPositiveLong()))
                .withNetworkByCoverage(new GdSetAutoBidsNetworkByCoverage()
                        .withContextCoverage(MAX_CONTEXT_COVERAGE));
        input.setSearchByTrafficVolume(new GdSetAutoBidsSearchByTrafficVolume()
                .withTargetTrafficVolume(MAX_TARGET_TRAFFIC_VOLUME));

        GridGraphQLContext gridGraphQLContext = ContextHelper.buildDefaultContext();
        doReturn(gridGraphQLContext)
                .when(gridContextProvider).getGridContext();
        doReturn(MassResult.emptyMassAction())
                .when(bidService).setAutoBids(any(), anyLong(), any(), anyBoolean());
    }


    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void checkSetAutoBids_andExceptSuccessResult() {
        bidsDataService.setAutoBids(input);
    }

    @Test
    public void checkSetAutoBids_exceptInIntervalDefect_whenNetworkByCoverageIsToBig() {
        input.getNetworkByCoverage()
                .setContextCoverage(MAX_CONTEXT_COVERAGE + 1);

        checkGridValidationException(path(field(GdSetAutoBids.NETWORK_BY_COVERAGE),
                field(GdSetAutoBidsNetworkByCoverage.CONTEXT_COVERAGE)),
                NumberDefects.inInterval(MIN_CONTEXT_COVERAGE, MAX_CONTEXT_COVERAGE));
    }

    @Test
    public void checkSetAutoBids_exceptInIntervalDefect_whenSearchByTrafficVolumeIsToBig() {
        input.getSearchByTrafficVolume()
                .setTargetTrafficVolume(MAX_TARGET_TRAFFIC_VOLUME + 1);

        checkGridValidationException(path(field(GdSetAutoBids.SEARCH_BY_TRAFFIC_VOLUME),
                field(GdSetAutoBidsSearchByTrafficVolume.TARGET_TRAFFIC_VOLUME)),
                NumberDefects.inInterval(MIN_TARGET_TRAFFIC_VOLUME, MAX_TARGET_TRAFFIC_VOLUME));
    }

    @Test
    public void checkSetAutoBids_exceptInIntervalDefect_whenSearchIncreasePercentIsToBig() {
        input.getSearchByTrafficVolume()
                .setIncreasePercentage(MAX_INCREASE_PERCENT + 1);

        checkGridValidationException(path(field(GdSetAutoBids.SEARCH_BY_TRAFFIC_VOLUME),
                field(GdSetAutoBidsSearchByTrafficVolume.INCREASE_PERCENTAGE)),
                NumberDefects.inInterval(MIN_INCREASE_PERCENT, MAX_INCREASE_PERCENT));
    }

    @Test
    public void checkSetAutoBids_exceptInIntervalDefect_whenNetworkIncreasePercentIsToBig() {
        input.getNetworkByCoverage()
                .setIncreasePercent(MAX_INCREASE_PERCENT + 1);

        checkGridValidationException(path(field(GdSetAutoBids.NETWORK_BY_COVERAGE),
                field(GdSetAutoBidsNetworkByCoverage.INCREASE_PERCENT)),
                NumberDefects.inInterval(MIN_INCREASE_PERCENT, MAX_INCREASE_PERCENT));
    }

    @Test
    public void checkSetAutoBids_exceptNotNullDefect_whenKeywordIdsIsNull() {
        input.setKeywordIds(null);

        checkGridValidationException(GdSetAutoBids.KEYWORD_IDS, CommonDefects.notNull());
    }

    @Test
    public void checkSetAutoBids_exceptNotEmptyDefect_whenKeywordIdsIsEmpty() {
        input.setKeywordIds(Collections.emptyList());
        bidsDataService.setAutoBids(input);
    }

    @Test
    public void checkSetAutoBids_exceptNotNullDefect_whenNetworkByCoverageAndSearchByTrafficVolumeIsNull() {
        input.withNetworkByCoverage(null).setSearchByTrafficVolume(null);

        checkGridValidationException(mustContainNonNullProps());
    }

    @Test
    public void checkSetAutoBids_whenSearchByTrafficVolumeIsNull() {
        input.setSearchByTrafficVolume(null);
        bidsDataService.setAutoBids(input);
    }

    @Test
    public void checkSetAutoBids_whenNetworkByCoverageIsNull() {
        input.setNetworkByCoverage(null);
        bidsDataService.setAutoBids(input);
    }

    private void checkGridValidationException(Defect expectedDefectType) {
        checkGridValidationException(path(), expectedDefectType);
    }

    private void checkGridValidationException(ModelProperty modelProperty, Defect expectedDefectType) {
        checkGridValidationException(path(field(modelProperty)), expectedDefectType);
    }

    private void checkGridValidationException(Path path, Defect expectedDefectType) {
        thrown.expect(GridValidationException.class);
        thrown.expect(hasValidationResult(
                hasErrorsWith(gridDefect(path, expectedDefectType))));

        bidsDataService.setAutoBids(input);
    }

}
