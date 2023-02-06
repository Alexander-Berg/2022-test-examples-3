package ru.yandex.direct.core.entity.adgroup.service.complex.text.update;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.adgroup.container.ComplexTextAdGroup;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static com.google.common.base.Preconditions.checkState;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.adgroup.model.AdGroup.ID;
import static ru.yandex.direct.core.validation.defects.RightsDefects.noRights;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.objectNotFound;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class ComplexAdGroupUpdateOperationValidationRightsTest extends ComplexAdGroupUpdateOperationValidationTestBase {

    @Autowired
    private RbacService rbacService;

    @Test
    public void hasOnlyOneErrorWhenAdGroupHasIdOfAnotherClient() {
        AdGroupInfo otherClientAdGroupInfo = steps.adGroupSteps().createActiveTextAdGroup();
        ComplexTextAdGroup adGroup = fullAdGroup(otherClientAdGroupInfo.getAdGroupId());

        ValidationResult<?, Defect> vr = updateAndCheckResultIsFailed(adGroup);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(index(0), field(ID.name())), objectNotFound())));
        vr.flattenErrors().forEach(validationErrorLogger::info);
        assertThat("должна присутствовать всего одна ошибка", vr.flattenErrors(), hasSize(1));
    }

    @Test
    public void noChangesWhenAdGroupHasIdOfAnotherClient() {
        AdGroupInfo otherClientAdGroupInfo = steps.adGroupSteps().createActiveTextAdGroup();
        int otherClientShard = otherClientAdGroupInfo.getShard();
        ClientId otherClientId = otherClientAdGroupInfo.getClientId();

        ComplexTextAdGroup adGroup = fullAdGroup(otherClientAdGroupInfo.getAdGroupId());
        checkState(adGroup.getKeywords().size() > 0);

        ValidationResult<?, Defect> vr = updateAndCheckResultIsFailed(adGroup);
        assumeThat(vr, hasDefectDefinitionWith(validationError(path(index(0), field(ID.name())), objectNotFound())));
        vr.flattenErrors().forEach(validationErrorLogger::info);
        assumeThat("должна присутствовать всего одна ошибка", vr.flattenErrors(), hasSize(1));

        List<String> otherClientPhrases =
                testKeywordRepository.getClientPhrases(otherClientShard, otherClientId);
        assertThat(otherClientPhrases, emptyIterable());
    }

    @Test
    public void noChangesWhenOneAdGroupHasIdOfAnotherClientAndOneAdGroupIsValid() {
        AdGroupInfo otherClientAdGroupInfo = steps.adGroupSteps().createActiveTextAdGroup();
        int otherClientShard = otherClientAdGroupInfo.getShard();
        ClientId otherClientId = otherClientAdGroupInfo.getClientId();

        ComplexTextAdGroup thisClientAdGroup = fullAdGroup(adGroupId);
        ComplexTextAdGroup otherClientAdGroup = fullAdGroup(otherClientAdGroupInfo.getAdGroupId());
        checkState(thisClientAdGroup.getKeywords().size() > 0);

        ValidationResult<?, Defect> vr = updateAndCheckResultIsFailed(thisClientAdGroup, otherClientAdGroup);
        assumeThat(vr, hasDefectDefinitionWith(validationError(path(index(1), field(ID.name())), objectNotFound())));
        vr.flattenErrors().forEach(validationErrorLogger::info);
        assumeThat("должна присутствовать всего одна ошибка", vr.flattenErrors(), hasSize(1));

        List<String> thisClientPhrases =
                testKeywordRepository.getClientPhrases(campaignInfo.getShard(), campaignInfo.getClientId());
        assertThat(thisClientPhrases, emptyIterable());

        List<String> otherClientPhrases =
                testKeywordRepository.getClientPhrases(otherClientShard, otherClientId);
        assertThat(otherClientPhrases, emptyIterable());
    }

    @Test
    public void hasOnlyOneErrorWhenClientHasNoRightsToWrite() {
        operatorUid = steps.clientSteps().createDefaultClientWithRole(RbacRole.SUPERREADER).getUid();

        ComplexTextAdGroup adGroup = fullAdGroup(adGroupId);

        ValidationResult<?, Defect> vr = updateAndCheckResultIsFailed(adGroup);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(index(0)), noRights())));
        vr.flattenErrors().forEach(validationErrorLogger::info);
        assertThat("должна присутствовать всего одна ошибка", vr.flattenErrors(), hasSize(1));
    }
}
