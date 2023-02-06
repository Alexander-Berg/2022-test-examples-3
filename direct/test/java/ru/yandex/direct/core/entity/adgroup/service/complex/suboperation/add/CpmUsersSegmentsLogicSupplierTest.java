package ru.yandex.direct.core.entity.adgroup.service.complex.suboperation.add;

import java.util.List;

import org.junit.Test;

import ru.yandex.direct.core.entity.adgroup.container.ComplexAdGroup;
import ru.yandex.direct.core.entity.adgroup.container.ComplexCpmAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.model.AdShowType;
import ru.yandex.direct.core.entity.adgroup.model.CpmVideoAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.UsersSegment;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

public class CpmUsersSegmentsLogicSupplierTest {

    public static final long OPERATOR_UID = 1000;
    public static final int SHARD = 1;
    public static final long AD_GROUP_ID = 123;

    @Test
    public void prepare_BadVideoGoal_HasErrors() {
        UsersSegment badVideoGoal = new UsersSegment().withAdGroupId(AD_GROUP_ID); // no type
        ValidationResult<List<AdGroup>, Defect> adGroupsResult = getListDefectValidationResult(
                singletonList(badVideoGoal));
        assertTrue(adGroupsResult.hasAnyErrors());
    }

    @Test
    public void prepare_TwoBadVideoGoal_HasErrors() {
        UsersSegment badVideoGoal = new UsersSegment().withAdGroupId(AD_GROUP_ID); // no type
        UsersSegment badVideoGoalTwo = new UsersSegment().withAdGroupId(AD_GROUP_ID); // no type
        ValidationResult<List<AdGroup>, Defect> adGroupsResult =
                getListDefectValidationResult(asList(badVideoGoal, badVideoGoalTwo));
        assertTrue(adGroupsResult.hasAnyErrors());
    }


    @Test
    public void prepare_GoodVideoGoal_NoErrors() {
        UsersSegment usersSegment = new UsersSegment().withAdGroupId(AD_GROUP_ID).withType(AdShowType.COMPLETE);
        ValidationResult<List<AdGroup>, Defect> adGroupsResult =
                getListDefectValidationResult(singletonList(usersSegment));
        assertFalse(adGroupsResult.hasAnyErrors());
    }

    @Test
    public void prepare_NullVideoGoal_NoErrors() {
        ValidationResult<List<AdGroup>, Defect> adGroupsResult = getListDefectValidationResult(null);
        assertFalse(adGroupsResult.hasAnyErrors());
    }

    private static ValidationResult<List<AdGroup>, Defect> getListDefectValidationResult(List<UsersSegment> usersSegments) {
        ComplexCpmAdGroup complexCpmAdGroup = new ComplexCpmAdGroup();

        complexCpmAdGroup
                .withAdGroup(new CpmVideoAdGroup()
                        .withType(AdGroupType.CPM_VIDEO))
                .withUsersSegments(usersSegments);
        ClientId clientId = ClientId.fromLong(OPERATOR_UID);
        List<ComplexCpmAdGroup> complexAdGroups = singletonList(complexCpmAdGroup);
        CpmUsersSegmentsLogicSupplier cpmUsersSegmentsLogicSupplier =
                new CpmUsersSegmentsLogicSupplier(complexAdGroups,
                        null, SHARD);
        ValidationResult<List<AdGroup>, Defect> adGroupsResult =
                new ValidationResult<>(mapList(complexAdGroups, ComplexAdGroup::getAdGroup));
        cpmUsersSegmentsLogicSupplier.prepare(adGroupsResult);
        return adGroupsResult;
    }
}
