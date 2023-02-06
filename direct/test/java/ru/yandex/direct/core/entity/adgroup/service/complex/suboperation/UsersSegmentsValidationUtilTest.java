package ru.yandex.direct.core.entity.adgroup.service.complex.suboperation;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.adgroup.container.ComplexAdGroup;
import ru.yandex.direct.core.entity.adgroup.container.ComplexCpmAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.model.AdShowType;
import ru.yandex.direct.core.entity.adgroup.model.TextAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.UsersSegment;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;
import ru.yandex.direct.validation.wrapper.ModelItemValidationBuilder;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.userssegments.service.validation.UsersSegmentDefects.adGroupTypeNotSupported;
import static ru.yandex.direct.core.entity.userssegments.service.validation.UsersSegmentDefects.goalTypeNotSupportedInAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeTextAdGroup;
import static ru.yandex.direct.core.testing.data.TestUserSegments.segmentsFromTypes;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@RunWith(SpringRunner.class)
@CoreTest
public class UsersSegmentsValidationUtilTest {

    @Autowired
    private Steps steps;

    private AdGroup cpmVideoAdGroup;
    private AdGroup outdoorAdGroup;
    private AdGroup indoorAdGroup;

    private ClientInfo clientInfo;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        cpmVideoAdGroup = steps.adGroupSteps().createActiveCpmVideoAdGroup(clientInfo).getAdGroup();
        outdoorAdGroup = steps.adGroupSteps().createActiveCpmOutdoorAdGroup(clientInfo).getAdGroup();
        indoorAdGroup = steps.adGroupSteps().createActiveCpmIndoorAdGroup(clientInfo).getAdGroup();
    }

    @Test
    public void validateVideoGoalsOnInsert_ValidGoalsInCpmVideoAdGroup() {
        ComplexCpmAdGroup complexCpmAdGroup = new ComplexCpmAdGroup()
                .withAdGroup(cpmVideoAdGroup)
                .withUsersSegments(segmentsFromTypes(AdShowType.FIRST_QUARTILE, AdShowType.THIRD_QUARTILE));
        ValidationResult<List<AdGroup>, Defect> vr = validateOnInsert(complexCpmAdGroup);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateVideoGoalsOnInsert_ValidGoalsInCpmOutdoorAdGroup() {
        ComplexCpmAdGroup complexCpmAdGroup = new ComplexCpmAdGroup()
                .withAdGroup(outdoorAdGroup)
                .withUsersSegments(segmentsFromTypes(AdShowType.START));
        ValidationResult<List<AdGroup>, Defect> vr = validateOnInsert(complexCpmAdGroup);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateVideoGoalsOnInsert_ValidInCpmVideoAndOutdoor() {
        ComplexCpmAdGroup complexCpmVideoAdGroup = new ComplexCpmAdGroup()
                .withAdGroup(cpmVideoAdGroup)
                .withUsersSegments(segmentsFromTypes(AdShowType.FIRST_QUARTILE, AdShowType.THIRD_QUARTILE));
        ComplexCpmAdGroup complexCpmOutdoorAdGroup = new ComplexCpmAdGroup()
                .withAdGroup(outdoorAdGroup)
                .withUsersSegments(singletonList(new UsersSegment().withType(AdShowType.START)));
        ValidationResult<List<AdGroup>, Defect> vr = validateOnInsert(complexCpmVideoAdGroup, complexCpmOutdoorAdGroup);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateVideoGoalsOnInsert_GoalIsNull() {
        ComplexCpmAdGroup complexCpmAdGroup = new ComplexCpmAdGroup()
                .withAdGroup(cpmVideoAdGroup)
                .withUsersSegments(singletonList(null));
        ValidationResult<List<AdGroup>, Defect> vr = validateOnInsert(complexCpmAdGroup);
        assertThat(vr, hasDefectDefinitionWith(
                validationError(path(index(0), field("usersSegments"), index(0)), notNull())));
    }

    @Test
    public void validateVideoGoalsOnInsert_GoalTypeIsNull() {
        ComplexCpmAdGroup complexCpmAdGroup = new ComplexCpmAdGroup()
                .withAdGroup(cpmVideoAdGroup)
                .withUsersSegments(segmentsFromTypes((AdShowType) null));
        ValidationResult<List<AdGroup>, Defect> vr = validateOnInsert(complexCpmAdGroup);
        assertThat(vr, hasDefectDefinitionWith(
                validationError(path(index(0), field("usersSegments"), index(0), field(UsersSegment.TYPE)), notNull())));
    }

    @Test
    public void validateVideoGoalsOnInsert_GoalTypeIsNotSupportedForOutdoorAdGroup() {
        ComplexCpmAdGroup complexCpmAdGroup = new ComplexCpmAdGroup()
                .withAdGroup(outdoorAdGroup)
                .withUsersSegments(segmentsFromTypes(AdShowType.COMPLETE));
        ValidationResult<List<AdGroup>, Defect> vr = validateOnInsert(complexCpmAdGroup);
        assertThat(vr, hasDefectDefinitionWith(
                validationError(path(index(0), field("usersSegments"), index(0), field(UsersSegment.TYPE)),
                        goalTypeNotSupportedInAdGroup())));
    }

    @Test
    public void validateVideoGoalsOnInsert_GoalTypeIsNotSupportedForIndoorAdGroup() {
        ComplexCpmAdGroup complexCpmAdGroup = new ComplexCpmAdGroup()
                .withAdGroup(indoorAdGroup)
                .withUsersSegments(segmentsFromTypes(AdShowType.COMPLETE));
        ValidationResult<List<AdGroup>, Defect> vr = validateOnInsert(complexCpmAdGroup);
        assertThat(vr, hasDefectDefinitionWith(
                validationError(path(index(0), field("usersSegments"), index(0), field(UsersSegment.TYPE)),
                        goalTypeNotSupportedInAdGroup())));
    }

    @Test
    public void validateVideoGoalsOnInsert_ValidGoalsInCpmBannerGroup() {
        ComplexCpmAdGroup complexCpmAdGroup = new ComplexCpmAdGroup()
                .withAdGroup(steps.adGroupSteps().createActiveCpmBannerAdGroup().getAdGroup())
                .withUsersSegments(segmentsFromTypes(AdShowType.MIDPOINT));
        ValidationResult<List<AdGroup>, Defect> vr = validateOnInsert(complexCpmAdGroup);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateVideoGoalsOnInsert_AdGroupTypeIsNotSupported() {
        ComplexCpmAdGroup complexCpmAdGroup = new ComplexCpmAdGroup()
                .withAdGroup(steps.adGroupSteps().createActiveMobileContentAdGroup().getAdGroup())
                .withUsersSegments(segmentsFromTypes(AdShowType.MIDPOINT));
        ValidationResult<List<AdGroup>, Defect> vr = validateOnInsert(complexCpmAdGroup);
        assertThat(vr, hasDefectDefinitionWith(
                validationError(path(index(0), field("usersSegments"), index(0), field(UsersSegment.AD_GROUP_ID)),
                        adGroupTypeNotSupported())));
    }

    @Test
    public void validateVideoGoalsOnInsert_OneValidAndOneInvalid() {
        ComplexCpmAdGroup validAdGroup = new ComplexCpmAdGroup()
                .withAdGroup(cpmVideoAdGroup)
                .withUsersSegments(segmentsFromTypes(AdShowType.FIRST_QUARTILE, AdShowType.THIRD_QUARTILE));
        ComplexCpmAdGroup invalidAdGroup = new ComplexCpmAdGroup()
                .withAdGroup(outdoorAdGroup)
                .withUsersSegments(singletonList(new UsersSegment().withType(AdShowType.COMPLETE)));
        ValidationResult<List<AdGroup>, Defect> vr = validateOnInsert(validAdGroup, invalidAdGroup);
        assertThat(vr, hasDefectDefinitionWith(
                validationError(path(index(1), field("usersSegments"), index(0), field(UsersSegment.TYPE)),
                        goalTypeNotSupportedInAdGroup())));
    }

    @Test
    public void validateVideoGoalsOnInsert_SeveralVideoGoalsWithSecondInvalid() {
        ComplexCpmAdGroup complexCpmAdGroup = new ComplexCpmAdGroup()
                .withAdGroup(cpmVideoAdGroup)
                .withUsersSegments(
                        segmentsFromTypes(AdShowType.FIRST_QUARTILE, null, AdShowType.THIRD_QUARTILE));
        ValidationResult<List<AdGroup>, Defect> vr = validateOnInsert(complexCpmAdGroup);
        assertThat(vr, hasDefectDefinitionWith(
                validationError(path(index(0), field("usersSegments"), index(1), field(UsersSegment.TYPE)),
                        notNull())));
    }

    @Test
    public void validateVideoGoalsOnInsert_VideoGoalsListIsNull_NoErrors() {
        ComplexCpmAdGroup complexCpmAdGroup = new ComplexCpmAdGroup()
                .withAdGroup(cpmVideoAdGroup)
                .withUsersSegments(null);
        ValidationResult<List<AdGroup>, Defect> vr = validateOnInsert(complexCpmAdGroup);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateVideoGoalsOnUpdate_ValidGoalsInCpmVideoAdGroup() {
        ComplexCpmAdGroup complexCpmAdGroup = new ComplexCpmAdGroup()
                .withAdGroup(cpmVideoAdGroup)
                .withUsersSegments(
                        mapList(segmentsFromTypes(AdShowType.FIRST_QUARTILE, AdShowType.THIRD_QUARTILE),
                                videoGoal -> videoGoal.withAdGroupId(cpmVideoAdGroup.getId())));
        ValidationResult<List<AdGroup>, Defect> vr = validateOnInsert(complexCpmAdGroup);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateVideoGoalsOnUpdate_ValidGoalsInCpmOutdoorAdGroup() {
        ComplexCpmAdGroup complexCpmAdGroup = new ComplexCpmAdGroup()
                .withAdGroup(outdoorAdGroup)
                .withUsersSegments(singletonList(new UsersSegment()
                        .withAdGroupId(outdoorAdGroup.getId())
                        .withType(AdShowType.START)));
        ValidationResult<List<AdGroup>, Defect> vr = validateOnInsert(complexCpmAdGroup);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateVideoGoalsOnUpdate_AdGroupIdIsNull() {
        ComplexCpmAdGroup complexCpmAdGroup = new ComplexCpmAdGroup()
                .withAdGroup(cpmVideoAdGroup)
                .withUsersSegments(singletonList(new UsersSegment().withAdGroupId(null)));
        ValidationResult<List<AdGroup>, Defect> vr = validateOnUpdate(complexCpmAdGroup);
        assertThat(vr, hasDefectDefinitionWith(
                validationError(path(index(0), field("usersSegments"), index(0), field(UsersSegment.AD_GROUP_ID)),
                        notNull())));
    }

    private ValidationResult<List<AdGroup>, Defect> validateOnInsert(ComplexCpmAdGroup... adGroups) {
        List<ComplexCpmAdGroup> complexAdGroupsList = asList(adGroups);
        ValidationResult<List<AdGroup>, Defect> adGroupsResult =
                new ValidationResult<>(mapList(complexAdGroupsList, ComplexAdGroup::getAdGroup));
        UsersSegmentsValidationUtil.validateCpmUserSegmentsOnInsert(adGroupsResult, complexAdGroupsList);
        return adGroupsResult;
    }

    private ValidationResult<List<AdGroup>, Defect> validateOnUpdate(ComplexCpmAdGroup... adGroups) {
        List<ComplexCpmAdGroup> complexAdGroupsList = asList(adGroups);
        ValidationResult<List<AdGroup>, Defect> adGroupsResult =
                new ValidationResult<>(mapList(complexAdGroupsList, ComplexAdGroup::getAdGroup));
        UsersSegmentsValidationUtil.validateCpmUsersSegmentsOnUpdate(adGroupsResult, complexAdGroupsList);
        return adGroupsResult;
    }

    @Test
    public void validateSegmentsOnInsert_ValidAdShowTypeForTextAdGroup() {
        TextAdGroup textAdGroup = activeTextAdGroup().withUsersSegments(
                List.of(new UsersSegment().withType(AdShowType.START)));
        ModelItemValidationBuilder<TextAdGroup> validationBuilder = ModelItemValidationBuilder.of(textAdGroup);
        UsersSegmentsValidationUtil.validateAdGroupWithUsersSegments(validationBuilder, AdGroupType.BASE, false);
        ValidationResult<TextAdGroup, Defect> vr = validationBuilder.getResult();
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateSegmentsOnInsert_AdShowTypeIsNotSupportedForTextAdGroup() {
        TextAdGroup textAdGroup = activeTextAdGroup().withUsersSegments(
                List.of(new UsersSegment().withType(AdShowType.FIRST_QUARTILE)));
        ModelItemValidationBuilder<TextAdGroup> validationBuilder = ModelItemValidationBuilder.of(textAdGroup);
        UsersSegmentsValidationUtil.validateAdGroupWithUsersSegments(validationBuilder, AdGroupType.BASE, false);
        ValidationResult<TextAdGroup, Defect> vr = validationBuilder.getResult();
        assertThat(vr, hasDefectDefinitionWith(
                validationError(path(field("usersSegments"), index(0), field(UsersSegment.TYPE)),
                        goalTypeNotSupportedInAdGroup())));
    }
}
