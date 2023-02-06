package ru.yandex.direct.core.entity.adgroup.service;

import java.util.List;
import java.util.function.Function;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.DynamicTextAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.StatusBLGenerated;
import ru.yandex.direct.core.entity.adgroup.model.StatusModerate;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.result.MassResult;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGroupsUpdateOperationOtherStatusesTest extends AdGroupsUpdateOperationTestBase {

    private static final String NEW_DOMAIN_URL = RandomStringUtils.randomAlphanumeric(5) + ".ru";

    @Test
    public void prepareAndApply_DomainUrlIsChanged_DomainUrlProcessingStatusIsProcessing() {
        AdGroupInfo adGroupInfo = adGroupSteps.createActiveDynamicTextAdGroup();

        adGroupRepository.updateAdGroups(
                adGroupInfo.getShard(),
                adGroupInfo.getClientId(),
                singleton(new ModelChanges<>(adGroupInfo.getAdGroupId(), DynamicTextAdGroup.class)
                        .process(StatusModerate.NO, AdGroup.STATUS_MODERATE)
                        .process(StatusBLGenerated.NO, DynamicTextAdGroup.STATUS_B_L_GENERATED)
                        .castModelUp(AdGroup.class)
                        .applyTo(adGroupInfo.getAdGroup())));

        List<AdGroup> adGroups = adGroupRepository.getAdGroups(
                adGroupInfo.getShard(), singleton(adGroupInfo.getAdGroupId())
        );
        checkState(adGroups.size() == 1);
        checkState(adGroups.get(0).getStatusModerate() == StatusModerate.NO);
        checkState(
                ((DynamicTextAdGroup) adGroups.get(0)).getStatusBLGenerated() == StatusBLGenerated.NO);

        prepareAndApplyAdGroupUpdateWithSuccess(adGroupInfo, modelChangesWithDomainUrl());

        List<AdGroup> updatedAdGroups = adGroupRepository.getAdGroups(
                adGroupInfo.getShard(), singleton(adGroupInfo.getAdGroupId())
        );

        assertThat(
                ((DynamicTextAdGroup) updatedAdGroups.get(0)).getStatusBLGenerated(),
                equalTo(StatusBLGenerated.PROCESSING));
    }

    private void prepareAndApplyAdGroupUpdateWithSuccess(
            AdGroupInfo adGroupInfo, Function<AdGroupInfo, ModelChanges<AdGroup>> modelChanges) {
        AdGroupsUpdateOperation updateOperation = createUpdateOperation(Applicability.PARTIAL,
                singletonList(modelChanges.apply(adGroupInfo)), adGroupInfo.getUid(),
                adGroupInfo.getClientId(), adGroupInfo.getShard());
        MassResult<Long> result = updateOperation.prepareAndApply();

        assumeThat("результат операции должен быть положительный",
                result.getValidationResult(), hasNoDefectsDefinitions());
    }

    private Function<AdGroupInfo, ModelChanges<AdGroup>> modelChangesWithDomainUrl() {
        return modelChangesWith(
                mc -> mc.process(NEW_DOMAIN_URL, DynamicTextAdGroup.DOMAIN_URL),
                DynamicTextAdGroup.class);
    }
}
