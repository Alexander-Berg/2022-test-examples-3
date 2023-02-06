package ru.yandex.direct.oneshot.oneshots.segmentlogdate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import one.util.streamex.StreamEx;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.model.AdShowType;
import ru.yandex.direct.core.entity.adgroup.model.UsersSegment;
import ru.yandex.direct.core.entity.userssegments.repository.UsersSegmentRepository;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.oneshot.configuration.OneshotTest;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestUserSegments.readyForCreateSegment;
import static ru.yandex.direct.core.testing.data.TestUserSegments.readyForUpdateSegment;

@OneshotTest
@RunWith(SpringRunner.class)
public class SegmentLogReadDateOneshotTest {

    private static final LocalDateTime DATE_TIME_OLD =
            LocalDateTime.of(LocalDate.now(), LocalTime.parse("00:00")).withNano(0);

    private static final LocalDate DATE_NEW = LocalDate.now().minusDays(10);
    private static final LocalDateTime DATE_TIME_NEW =
            LocalDateTime.of(DATE_NEW, LocalTime.parse("00:00")).withNano(0);

    @Autowired
    private Steps steps;

    @Autowired
    private UsersSegmentRepository usersSegmentRepository;

    @Autowired
    private SegmentLogReadDateOneshot oneshot;

    @Test
    public void validationSuccessOnValidInputData() {
        SegmentLogReadDateOneshotData inputData = new SegmentLogReadDateOneshotData();
        inputData.setAdGroupType("CPM_VIDEO");
        inputData.setNewLastReadLogDate("2020-04-12");

        ValidationResult<SegmentLogReadDateOneshotData, Defect> vr = oneshot.validate(inputData);
        assertThat(vr.hasAnyErrors())
                .describedAs("наличие ошибок в результате валидации")
                .isFalse();
    }

    @Test
    public void validationFailOnInvalidDate() {
        SegmentLogReadDateOneshotData inputData = new SegmentLogReadDateOneshotData();
        inputData.setAdGroupType("CPM_VIDEO");
        inputData.setNewLastReadLogDate("2020.04.12");

        ValidationResult<SegmentLogReadDateOneshotData, Defect> vr = oneshot.validate(inputData);
        assertThat(vr.hasAnyErrors())
                .describedAs("наличие ошибок в результате валидации")
                .isTrue();
    }

    @Test
    public void validationFailOnInvalidAdGroupType() {
        SegmentLogReadDateOneshotData inputData = new SegmentLogReadDateOneshotData();
        inputData.setAdGroupType("CPM_VIDEO_AD_GROUP");
        inputData.setNewLastReadLogDate("2020-04-12");

        ValidationResult<SegmentLogReadDateOneshotData, Defect> vr = oneshot.validate(inputData);
        assertThat(vr.hasAnyErrors())
                .describedAs("наличие ошибок в результате валидации")
                .isTrue();
    }

    @Test
    public void updateDateOnlyForSpecifiedAdGroupType() {
        AdGroupInfo videoAdGroupInfo1 = steps.adGroupSteps().createActiveCpmVideoAdGroup();
        addReadyForCreateSegmentVideoGoal(videoAdGroupInfo1);

        AdGroupInfo videoAdGroupInfo2 = steps.adGroupSteps().createActiveCpmVideoAdGroup();
        addReadyForUpdateSegmentVideoGoal(videoAdGroupInfo2, AdShowType.COMPLETE);

        AdGroupInfo audioAdGroupInfo = steps.adGroupSteps().createActiveCpmAudioAdGroup();
        addReadyForCreateSegmentVideoGoal(audioAdGroupInfo);

        checkState(videoAdGroupInfo1.getShard().equals(audioAdGroupInfo.getShard()));

        oneshot.execute(inputData(AdGroupType.CPM_VIDEO.name(), DATE_NEW), null, audioAdGroupInfo.getShard());

        UsersSegment videoAdGroupGoal1 = getGoal(videoAdGroupInfo1);
        UsersSegment videoAdGroupGoal2 = getGoal(videoAdGroupInfo2, AdShowType.COMPLETE);
        UsersSegment audioAdGroupGoal = getGoal(audioAdGroupInfo);

        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(videoAdGroupGoal1.getLastSuccessUpdateTime())
                    .describedAs("дата логов первого сегмента, которая должна была обновиться")
                    .isEqualTo(DATE_TIME_NEW);
            assertions.assertThat(videoAdGroupGoal2.getLastSuccessUpdateTime())
                    .describedAs("дата логов второго сегмента, которая должна была обновиться")
                    .isEqualTo(DATE_TIME_NEW);
            assertions.assertThat(audioAdGroupGoal.getLastSuccessUpdateTime())
                    .describedAs("дата логов сегмента, которая НЕ должна была обновиться")
                    .isEqualTo(DATE_TIME_OLD);
        });
    }

    private void addReadyForCreateSegmentVideoGoal(AdGroupInfo adGroupInfo) {
        addReadyForCreateSegmentVideoGoal(adGroupInfo, AdShowType.START);
    }

    private void addReadyForCreateSegmentVideoGoal(AdGroupInfo adGroupInfo, AdShowType type) {
        UsersSegment videoGoal = readyForCreateSegment(adGroupInfo.getAdGroupId())
                .withType(type)
                .withLastSuccessUpdateTime(DATE_TIME_OLD);
        addVideoGoal(adGroupInfo, videoGoal);
    }

    private void addReadyForUpdateSegmentVideoGoal(AdGroupInfo adGroupInfo) {
        addReadyForUpdateSegmentVideoGoal(adGroupInfo, AdShowType.START);
    }

    private void addReadyForUpdateSegmentVideoGoal(AdGroupInfo adGroupInfo, AdShowType type) {
        UsersSegment videoGoal = readyForUpdateSegment(adGroupInfo.getAdGroupId(), adGroupInfo.getUid())
                .withType(type)
                .withLastSuccessUpdateTime(DATE_TIME_OLD);
        addVideoGoal(adGroupInfo, videoGoal);
    }

    private void addVideoGoal(AdGroupInfo adGroupInfo, UsersSegment videoGoal) {
        usersSegmentRepository.addSegments(adGroupInfo.getShard(), singletonList(videoGoal));
    }

    private SegmentLogReadDateOneshotData inputData(String adGroupType, LocalDate localDate) {
        SegmentLogReadDateOneshotData inputData = new SegmentLogReadDateOneshotData();
        inputData.setAdGroupType(adGroupType);
        inputData.setNewLastReadLogDate(localDate.format(DateTimeFormatter.ISO_DATE));
        return inputData;
    }

    private UsersSegment getGoal(AdGroupInfo adGroupInfo) {
        return getGoal(adGroupInfo, AdShowType.START);
    }

    private UsersSegment getGoal(AdGroupInfo adGroupInfo, AdShowType type) {
        List<UsersSegment> videoGoals = usersSegmentRepository.getSegments(adGroupInfo.getShard(),
                singletonList(adGroupInfo.getAdGroupId()));
        return StreamEx.of(videoGoals)
                .findFirst(goal -> goal.getType() == type)
                .orElse(null);
    }
}
