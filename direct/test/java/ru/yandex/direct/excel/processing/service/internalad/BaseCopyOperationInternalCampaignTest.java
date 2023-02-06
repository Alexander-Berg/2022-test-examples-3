package ru.yandex.direct.excel.processing.service.internalad;

import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.copyentity.CopyOperation;
import ru.yandex.direct.core.copyentity.CopyOperationAssert;
import ru.yandex.direct.core.copyentity.CopyOperationFactory;
import ru.yandex.direct.core.entity.campaign.model.CommonCampaign;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;

@ParametersAreNonnullByDefault
public class BaseCopyOperationInternalCampaignTest {

    @Autowired
    protected Steps steps;

    @Autowired
    protected CopyOperationFactory factory;

    @Autowired
    protected CopyOperationAssert asserts;

    @Autowired
    protected AddOrUpdateInternalAdGroupsService addOrUpdateInternalAdGroupsService;

    protected Long uid;
    protected ClientInfo clientInfo;
    protected CopyOperation xerox;

    protected void assertCampaignIsCopied(Set<Long> copiedCampaignIds, Long autobudgetCampaignId) {
        asserts.assertCampaignIsCopied(copiedCampaignIds, autobudgetCampaignId,
                campaign -> ((CommonCampaign) campaign).setStatusShow(false));
    }

}
