package ru.yandex.direct.grid.processing.service.campaign;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.campaign.service.CampMetrikaCountersService;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.grid.model.campaign.GdTextCampaign;
import ru.yandex.direct.grid.model.campaign.timetarget.GdHolidaysSettings;
import ru.yandex.direct.grid.model.campaign.timetarget.GdTimeTarget;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.exception.GridValidationException;
import ru.yandex.direct.grid.processing.model.api.GdValidationResult;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignUnion;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaigns;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateTextCampaign;
import ru.yandex.direct.grid.processing.service.validation.GridDefectIds;
import ru.yandex.direct.grid.processing.service.validation.GridValidationService;
import ru.yandex.direct.libs.timetarget.TimeTarget;
import ru.yandex.direct.validation.defect.CommonDefects;
import ru.yandex.direct.validation.defect.NumberDefects;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.PathNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.joda.time.DateTimeConstants.DAYS_PER_WEEK;
import static org.joda.time.DateTimeConstants.HOURS_PER_DAY;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.MAX_RATE_CORRECTION;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.MIN_RATE_CORRECTION;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationMatchers.gridDefect;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationMatchers.hasErrorsWith;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationMatchers.hasValidationResult;
import static ru.yandex.direct.result.MassResult.emptyMassAction;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CampaignValidationServiceTimeTargetUpdateTest {
    @Autowired
    private GridValidationService gridValidationService;

    @Autowired
    private CampMetrikaCountersService campMetrikaCountersService;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private FeatureService mockedFeatureService;
    private CampaignValidationService campaignValidationService;
    private GdTimeTarget inputTimeTarget;
    private GdHolidaysSettings inputHolidaysSettings;

    @Before
    public void setUp() {
        mockedFeatureService = mock(FeatureService.class);
        CampaignRepository campaignRepository = mock(CampaignRepository.class);
        ShardHelper shardHelper = mock(ShardHelper.class);
        campaignValidationService = new CampaignValidationService(gridValidationService, campMetrikaCountersService,
                mockedFeatureService, campaignRepository, shardHelper);
    }

    @Test
    public void testHappyCase() {
        final var clientId = ClientId.fromLong(1L);
        GdTimeTarget timeTarget = new GdTimeTarget()
                .withTimeBoard(getCorrectTimeBoard())
                .withUseWorkingWeekends(false)
                .withEnabledHolidaysMode(false)
                .withIdTimeZone(1L);

        campaignValidationService.validateUpdateCampaigns(clientId, pack(timeTarget));
        GdValidationResult result = campaignValidationService.getValidationResult(emptyMassAction(),
                path(field(GdUpdateCampaigns.CAMPAIGN_UPDATE_ITEMS)));

        assertThat(result).isNull();
    }

    @Test
    public void testEmptyTimeBoard() {
        final var clientId = ClientId.fromLong(1L);
        GdTimeTarget timeTarget = new GdTimeTarget()
                .withTimeBoard(null)
                .withUseWorkingWeekends(false)
                .withEnabledHolidaysMode(false);

        campaignValidationService.validateUpdateCampaigns(clientId, pack(timeTarget));
        GdValidationResult result = campaignValidationService.getValidationResult(emptyMassAction(),
                path(field(GdUpdateCampaigns.CAMPAIGN_UPDATE_ITEMS)));

        assertThat(result).isNull();
    }

    @Test
    public void testNoTimeTarget() {
        final var clientId = ClientId.fromLong(1L);
        GdTimeTarget timeTarget = null;
        campaignValidationService.validateUpdateCampaigns(clientId, pack(timeTarget));
        GdValidationResult result = campaignValidationService.getValidationResult(emptyMassAction(),
                path(field(GdUpdateCampaigns.CAMPAIGN_UPDATE_ITEMS)));

        assertThat(result).isNull();
    }

    @Test
    public void testTimeTarget_exact40WorkingHoursTimeBoard() {
        final var clientId = ClientId.fromLong(1L);
        GdTimeTarget timeTarget = new GdTimeTarget()
                .withTimeBoard(getExactly40WorkingHoursTimeBoard())
                .withUseWorkingWeekends(false)
                .withEnabledHolidaysMode(false)
                .withIdTimeZone(1L);

        campaignValidationService.validateUpdateCampaigns(clientId, pack(timeTarget));
        GdValidationResult result = campaignValidationService.getValidationResult(emptyMassAction(),
                path(field(GdUpdateCampaigns.CAMPAIGN_UPDATE_ITEMS)));

        assertThat(result).isNull();
    }

    @Test
    public void testNotEnoughDaysInTimeBoard() {
        inputTimeTarget = new GdTimeTarget()
                .withTimeBoard(getNotEnoughDaysTimeBoard())
                .withUseWorkingWeekends(false)
                .withEnabledHolidaysMode(false);

        checkTimeboardValidationException();
    }

    @Test
    public void testTooManyDaysInTimeBoard() {
        inputTimeTarget = new GdTimeTarget()
                .withTimeBoard(getTooManyDaysTimeBoard())
                .withUseWorkingWeekends(false)
                .withEnabledHolidaysMode(false);

        checkTimeboardValidationException();
    }

    @Test
    public void testNotEnoughHoursInTimeBoard() {
        inputTimeTarget = new GdTimeTarget()
                .withTimeBoard(getNotEnoughHoursTimeBoard())
                .withUseWorkingWeekends(false)
                .withEnabledHolidaysMode(false);

        checkTimeboardValidationException();
    }

    @Test
    public void testTooManyHoursInTimeBoard() {
        inputTimeTarget = new GdTimeTarget()
                .withTimeBoard(getTooManyHoursTimeBoard())
                .withUseWorkingWeekends(false)
                .withEnabledHolidaysMode(false);

        checkTimeboardValidationException();
    }

    @Test
    public void testTooLowCoefInTimeBoard() {
        inputTimeTarget = new GdTimeTarget()
                .withTimeBoard(getTooLowCoefTimeBoard())
                .withUseWorkingWeekends(false)
                .withEnabledHolidaysMode(false);

        checkTimeboardValidationException();
    }

    @Test
    public void testTooHighCoefInTimeBoard() {
        inputTimeTarget = new GdTimeTarget()
                .withTimeBoard(getTooHighCoefTimeBoard())
                .withUseWorkingWeekends(false)
                .withEnabledHolidaysMode(false);

        checkTimeboardValidationException();
    }

    @Test
    public void testTimeTarget_lessThan40WorkingHoursSelected() {
        inputTimeTarget = new GdTimeTarget()
                .withTimeBoard(getEmptyTimeBoard())
                .withEnabledHolidaysMode(false)
                .withUseWorkingWeekends(false);

        checkTimeboardValidationException();
    }

    @Test
    public void testCorrectHolidaysSettingsShowEnabled() {
        final var clientId = ClientId.fromLong(1L);
        GdHolidaysSettings settings = new GdHolidaysSettings()
                .withIsShow(true)
                .withStartHour(0)
                .withEndHour(24)
                .withRateCorrections(MAX_RATE_CORRECTION);

        campaignValidationService.validateUpdateCampaigns(clientId, pack(settings));
        GdValidationResult result = campaignValidationService.getValidationResult(emptyMassAction(),
                path(field(GdUpdateCampaigns.CAMPAIGN_UPDATE_ITEMS)));

        assertThat(result).isNull();
    }

    @Test
    public void testCorrectHolidaysSettingsShowDisabled() {
        final var clientId = ClientId.fromLong(1L);
        GdHolidaysSettings settings = new GdHolidaysSettings().withIsShow(false);

        campaignValidationService.validateUpdateCampaigns(clientId, pack(settings));
        GdValidationResult result = campaignValidationService.getValidationResult(emptyMassAction(),
                path(field(GdUpdateCampaigns.CAMPAIGN_UPDATE_ITEMS)));

        assertThat(result).isNull();
    }

    @Test
    public void testNullStartHourAtHolidays() {
        inputHolidaysSettings = new GdHolidaysSettings().withIsShow(true)
                .withStartHour(null)
                .withEndHour(24)
                .withRateCorrections(MIN_RATE_CORRECTION);

        checkHolidaysSettingsValidationException(
                timeTargetPath(field(GdTimeTarget.HOLIDAYS_SETTINGS), field(GdHolidaysSettings.START_HOUR)),
                CommonDefects.notNull()
        );
    }

    @Test
    public void testTooLowStartHourAtHolidays() {
        inputHolidaysSettings = new GdHolidaysSettings().withIsShow(true)
                .withStartHour(-1)
                .withEndHour(24)
                .withRateCorrections(MIN_RATE_CORRECTION);

        checkHolidaysSettingsValidationException(
                timeTargetPath(field(GdTimeTarget.HOLIDAYS_SETTINGS), field(GdHolidaysSettings.START_HOUR)),
                NumberDefects.inInterval(0, 23)
        );
    }

    @Test
    public void testTooHighStartHourAtHolidays() {
        inputHolidaysSettings = new GdHolidaysSettings().withIsShow(true)
                .withStartHour(25)
                .withEndHour(24)
                .withRateCorrections(MIN_RATE_CORRECTION);

        checkHolidaysSettingsValidationException(
                timeTargetPath(field(GdTimeTarget.HOLIDAYS_SETTINGS), field(GdHolidaysSettings.START_HOUR)),
                NumberDefects.inInterval(0, 23)
        );
    }

    @Test
    public void testNullEndHourAtHolidays() {
        inputHolidaysSettings = new GdHolidaysSettings().withIsShow(true)
                .withStartHour(0)
                .withEndHour(null)
                .withRateCorrections(MIN_RATE_CORRECTION);

        checkHolidaysSettingsValidationException(
                timeTargetPath(field(GdTimeTarget.HOLIDAYS_SETTINGS), field(GdHolidaysSettings.END_HOUR)),
                CommonDefects.notNull()
        );
    }

    @Test
    public void testTooHighEndHourAtHolidays() {
        inputHolidaysSettings = new GdHolidaysSettings().withIsShow(true)
                .withStartHour(0)
                .withEndHour(25)
                .withRateCorrections(MIN_RATE_CORRECTION);

        checkHolidaysSettingsValidationException(
                timeTargetPath(field(GdTimeTarget.HOLIDAYS_SETTINGS), field(GdHolidaysSettings.END_HOUR)),
                NumberDefects.inInterval(1, 24)
        );
    }

    @Test
    public void testStartHourGreaterThanEndHourAtHolidays() {
        inputHolidaysSettings = new GdHolidaysSettings().withIsShow(true)
                .withStartHour(10)
                .withEndHour(9)
                .withRateCorrections(MIN_RATE_CORRECTION);

        checkHolidaysSettingsValidationException(
                timeTargetPath(field(GdTimeTarget.HOLIDAYS_SETTINGS), field(GdHolidaysSettings.START_HOUR)),
                NumberDefects.lessThan(9)
        );
    }

    @Test
    public void testStartHourEqualToEndHourHourAtHolidays() {
        inputHolidaysSettings = new GdHolidaysSettings().withIsShow(true)
                .withStartHour(0)
                .withEndHour(0)
                .withRateCorrections(MIN_RATE_CORRECTION);

        checkHolidaysSettingsValidationException(
                timeTargetPath(field(GdTimeTarget.HOLIDAYS_SETTINGS), field(GdHolidaysSettings.START_HOUR)),
                NumberDefects.lessThan(0)
        );
    }

    @Test
    public void testCoefTooLowAtHolidays() {
        inputHolidaysSettings = new GdHolidaysSettings().withIsShow(true)
                .withStartHour(0)
                .withEndHour(24)
                .withRateCorrections(MIN_RATE_CORRECTION - 1);

        checkHolidaysSettingsValidationException(
                timeTargetPath(field(GdTimeTarget.HOLIDAYS_SETTINGS), field(GdHolidaysSettings.RATE_CORRECTIONS)),
                NumberDefects.inInterval(MIN_RATE_CORRECTION, MAX_RATE_CORRECTION)
        );
    }

    @Test
    public void testCoefTooHighAtHolidays() {
        inputHolidaysSettings = new GdHolidaysSettings().withIsShow(true)
                .withStartHour(0)
                .withEndHour(24)
                .withRateCorrections(MAX_RATE_CORRECTION + 1);

        checkHolidaysSettingsValidationException(
                timeTargetPath(field(GdTimeTarget.HOLIDAYS_SETTINGS), field(GdHolidaysSettings.RATE_CORRECTIONS)),
                NumberDefects.inInterval(MIN_RATE_CORRECTION, MAX_RATE_CORRECTION)
        );
    }

    @Test
    public void testInvalidTimezoneId() {
        inputTimeTarget = new GdTimeTarget()
                .withTimeBoard(getCorrectTimeBoard())
                .withEnabledHolidaysMode(false)
                .withUseWorkingWeekends(false)
                .withIdTimeZone(-1L);

        checkValidationException(timeTargetPath(field(GdTimeTarget.ID_TIME_ZONE)), CommonDefects.validId());
    }

    @Test
    public void testTimeTarget_exact8HoursTimeBoard() {
        setCampaignNewMinDaysLimit(true);

        final var clientId = ClientId.fromLong(1L);
        GdTimeTarget timeTarget = new GdTimeTarget()
                .withTimeBoard(getExactly8WorkingHoursTimeBoard())
                .withUseWorkingWeekends(false)
                .withEnabledHolidaysMode(false)
                .withIdTimeZone(1L);

        campaignValidationService.validateUpdateCampaigns(clientId, pack(timeTarget));
        GdValidationResult result = campaignValidationService.getValidationResult(emptyMassAction(),
                path(field(GdUpdateCampaigns.CAMPAIGN_UPDATE_ITEMS)));

        assertThat(result).isNull();
    }

    @Test
    public void testTimeTarget_less8HoursTimeBoard() {
        setCampaignNewMinDaysLimit(true);

        final var clientId = ClientId.fromLong(1L);
        GdTimeTarget timeTarget = new GdTimeTarget()
                .withTimeBoard(getLessThen8HoursTimeBoard())
                .withUseWorkingWeekends(false)
                .withEnabledHolidaysMode(false)
                .withIdTimeZone(1L);

        try {
            campaignValidationService.validateUpdateCampaigns(clientId, pack(timeTarget));
            campaignValidationService.getValidationResult(emptyMassAction(),
                    path(field(GdUpdateCampaigns.CAMPAIGN_UPDATE_ITEMS)));

            Assert.fail();
        } catch (GridValidationException e) {

        }
    }

    private void setCampaignNewMinDaysLimit(Boolean value) {
        when(mockedFeatureService.isEnabledForClientId(
                ClientId.fromLong(1L), FeatureName.CAMPAIGN_NEW_MIN_DAYS_LIMIT)
        ).thenReturn(value);
    }

    private static List<List<Integer>> getCorrectTimeBoard() {
        return Collections.nCopies(DAYS_PER_WEEK,
                Collections.nCopies(HOURS_PER_DAY, TimeTarget.PredefinedCoefs.USUAL.getValue()));
    }

    private static List<List<Integer>> getEmptyTimeBoard() {
        return Collections.nCopies(DAYS_PER_WEEK, Collections.nCopies(HOURS_PER_DAY, 0));
    }

    private static List<List<Integer>> getExactly40WorkingHoursTimeBoard() {
        List<Integer> workingDay = new ArrayList<>(24);
        workingDay.addAll(Collections.nCopies(8, TimeTarget.PredefinedCoefs.USUAL.getValue()));
        workingDay.addAll(Collections.nCopies(16, 0));
        List<List<Integer>> result = new ArrayList<>();
        result.addAll(Collections.nCopies(DAYS_PER_WEEK - 2, workingDay));
        result.addAll(Collections.nCopies(2, Collections.nCopies(HOURS_PER_DAY, 0)));
        return result;
    }

    private static List<List<Integer>> getExactly8WorkingHoursTimeBoard() {
        List<Integer> workingDay = new ArrayList<>(24);
        workingDay.addAll(Collections.nCopies(2, TimeTarget.PredefinedCoefs.USUAL.getValue()));
        workingDay.addAll(Collections.nCopies(22, 0));
        List<List<Integer>> result = new ArrayList<>();
        result.addAll(Collections.nCopies(4, workingDay));
        result.addAll(Collections.nCopies(3, Collections.nCopies(HOURS_PER_DAY, 0)));
        return result;
    }

    private static List<List<Integer>> getLessThen8HoursTimeBoard() {
        List<Integer> workingDay = new ArrayList<>(24);
        workingDay.addAll(Collections.nCopies(1, TimeTarget.PredefinedCoefs.USUAL.getValue()));
        workingDay.addAll(Collections.nCopies(23, 0));
        List<List<Integer>> result = new ArrayList<>();
        result.addAll(Collections.nCopies(4, workingDay));
        result.addAll(Collections.nCopies(3, Collections.nCopies(HOURS_PER_DAY, 0)));
        return result;
    }

    private static List<List<Integer>> getNotEnoughDaysTimeBoard() {
        return Collections.nCopies(DAYS_PER_WEEK - 1,
                Collections.nCopies(HOURS_PER_DAY, TimeTarget.PredefinedCoefs.USUAL.getValue()));
    }

    private static List<List<Integer>> getTooManyDaysTimeBoard() {
        return Collections.nCopies(DAYS_PER_WEEK + 1,
                Collections.nCopies(HOURS_PER_DAY, TimeTarget.PredefinedCoefs.USUAL.getValue()));
    }

    private static List<List<Integer>> getNotEnoughHoursTimeBoard() {
        return Collections.nCopies(DAYS_PER_WEEK,
                Collections.nCopies(HOURS_PER_DAY - 1, TimeTarget.PredefinedCoefs.USUAL.getValue()));
    }

    private static List<List<Integer>> getTooManyHoursTimeBoard() {
        return Collections.nCopies(DAYS_PER_WEEK,
                Collections.nCopies(HOURS_PER_DAY + 1, TimeTarget.PredefinedCoefs.USUAL.getValue()));
    }

    private static List<List<Integer>> getTooLowCoefTimeBoard() {
        return Collections.nCopies(DAYS_PER_WEEK, Collections.nCopies(HOURS_PER_DAY, MIN_RATE_CORRECTION - 1));
    }

    private static List<List<Integer>> getTooHighCoefTimeBoard() {
        return Collections.nCopies(DAYS_PER_WEEK, Collections.nCopies(HOURS_PER_DAY, MAX_RATE_CORRECTION + 1));
    }

    private static GdUpdateCampaigns pack(@Nullable GdTimeTarget timeTarget) {
        GdUpdateTextCampaign campaignUpdate = new GdUpdateTextCampaign().withTimeTarget(timeTarget);
        GdUpdateCampaignUnion union = new GdUpdateCampaignUnion().withTextCampaign(campaignUpdate);
        return new GdUpdateCampaigns().withCampaignUpdateItems(List.of(union));
    }

    private static GdUpdateCampaigns pack(GdHolidaysSettings settings) {
        GdTimeTarget timeTarget = new GdTimeTarget()
                .withTimeBoard(getCorrectTimeBoard())
                .withUseWorkingWeekends(false)
                .withEnabledHolidaysMode(true)
                .withHolidaysSettings(settings)
                .withIdTimeZone(1L);
        return pack(timeTarget);
    }

    private void checkTimeboardValidationException() {
        final var clientId = ClientId.fromLong(1L);
        Path path = timeTargetPath(field(GdTimeTarget.TIME_BOARD));
        Defect defect = new Defect<>(GridDefectIds.TimeTarget.INVALID_TIME_BOARD_FORMAT);
        thrown.expect(GridValidationException.class);
        thrown.expect(hasValidationResult(hasErrorsWith(gridDefect(path, defect))));

        campaignValidationService.validateUpdateCampaigns(clientId, pack(inputTimeTarget));
    }

    private void checkHolidaysSettingsValidationException(Path path, Defect expectedDefectType) {
        final var clientId = ClientId.fromLong(1L);
        thrown.expect(GridValidationException.class);
        thrown.expect(hasValidationResult(hasErrorsWith(gridDefect(path, expectedDefectType))));

        campaignValidationService.validateUpdateCampaigns(clientId, pack(inputHolidaysSettings));
    }

    private void checkValidationException(Path path, Defect expectedDefectType) {
        final var clientId = ClientId.fromLong(1L);
        thrown.expect(GridValidationException.class);
        thrown.expect(hasValidationResult(hasErrorsWith(gridDefect(path, expectedDefectType))));

        campaignValidationService.validateUpdateCampaigns(clientId, pack(inputTimeTarget));
    }

    private static Path timeTargetPath(PathNode.Field... nodes) {
        List<PathNode> resultNodes = new ArrayList<>();
        resultNodes.add(field("campaignUpdateItems"));
        resultNodes.add(index(0));
        resultNodes.add(field(GdUpdateCampaignUnion.TEXT_CAMPAIGN));
        resultNodes.add(field(GdTextCampaign.TIME_TARGET));
        resultNodes.addAll(Arrays.asList(nodes));
        return new Path(resultNodes);
    }
}

