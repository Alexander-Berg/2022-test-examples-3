package ru.yandex.direct.core.entity.banner.type.moderation.add;

import java.util.Set;

import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.banner.model.BannerWithAdGroupId;
import ru.yandex.direct.core.entity.banner.service.BannersAddOperationFactory;
import ru.yandex.direct.core.entity.banner.type.moderation.ModerationTestBase;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.operation.Operation;

import static java.util.Collections.singletonList;

public abstract class AddModerationTestBase extends ModerationTestBase {

    protected static final boolean SAVE_DRAFT_YES = true;
    protected static final boolean SAVE_DRAFT_NO = false;

    @Parameterized.Parameter(4)
    public boolean saveDraft;

    @Autowired
    public BannersAddOperationFactory operationFactory;

    public BannerWithAdGroupId banner;

    protected abstract BannerWithAdGroupId getBannerForAddition();

    protected Operation<Long> createOperation(AdGroupInfo adGroupInfo) {
        banner = getBannerForAddition()
                .withAdGroupId(adGroupInfo.getAdGroupId());
        Set<String> clientEnabledFeatures = featureService.getEnabledForClientId(adGroupInfo.getClientId());

        return operationFactory.createAddOperation(Applicability.FULL, false,
                singletonList(banner), adGroupInfo.getShard(), adGroupInfo.getClientId(),
                adGroupInfo.getUid(), saveDraft, false, false, false, clientEnabledFeatures);
    }
}
