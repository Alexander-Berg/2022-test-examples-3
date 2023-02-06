package ru.yandex.direct.core.entity.campaign.repository;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.campaign.model.CampaignForAccessCheckApiImpl;
import ru.yandex.direct.core.entity.campaign.model.CampaignSource;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.RequestSource;
import ru.yandex.direct.core.entity.campaign.service.accesschecker.CampaignForAccessCheckRepositoryAdapter;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.repository.TestCampaignRepository;
import ru.yandex.direct.dbutil.model.ClientId;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.campaign.service.accesschecker.api5.Api5CampaignAccessibilityChecker.getApi5AccessibilityChecker;

@CoreTest
@RunWith(Parameterized.class)
public class CampaignForAccessCheckApiRepositoryTest extends
        CampaignForAccessCheckRepositoryTestBase<CampaignForAccessCheckApiImpl> {

    @Autowired
    private TestCampaignRepository testCampaignRepository;

    @Override
    protected CampaignForAccessCheckRepositoryAdapter<CampaignForAccessCheckApiImpl>
    getAllClientCampaignsForAccessCheckRepositoryAdapter(ClientId clientId) {
        return getApi5AccessibilityChecker().toAllCampaignsRepositoryAdapter(clientId);
    }

    @Override
    protected CampaignForAccessCheckRepositoryAdapter<CampaignForAccessCheckApiImpl>
    getAllowableCampaignsForAccessCheckRepositoryAdapter(ClientId clientId, Set<CampaignType> campaignTypes) {
        return getApi5AccessibilityChecker().toAllowableCampaignsRepositoryAdapter(clientId);
    }

    @Test
    public void invalidCampaignType_CampaignNotReturned() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveCpmYndxFrontpageCampaign();
        Long entityId = entitySupplier.apply(steps, campaignInfo);

        Map<Long, CampaignForAccessCheckApiImpl> campaignsByIdMap = subObjectRetrieverSupplier
                .apply(campaignAccessCheckRepository).get(
                        campaignInfo.getShard(),
                        getApi5AccessibilityChecker().toAllowableCampaignsRepositoryAdapter(
                                campaignInfo.getClientId()),
                        List.of(entityId));

        assertThat(campaignsByIdMap.keySet(), empty());
    }

    @Test
    public void universalCampaign_AllowableCampaignsNotReturned() {
        CampaignInfo campaignInfo = steps.campaignSteps().createDefaultCampaign();
        testCampaignRepository.setOptsToUniversal(campaignInfo.getShard(), campaignInfo.getCampaignId());
        Long entityId = entitySupplier.apply(steps, campaignInfo);

        Map<Long, CampaignForAccessCheckApiImpl> campaignsByIdMap = subObjectRetrieverSupplier
                .apply(campaignAccessCheckRepository).get(
                        campaignInfo.getShard(),
                        getApi5AccessibilityChecker().toAllowableCampaignsRepositoryAdapter(
                                campaignInfo.getClientId()),
                        List.of(entityId));

        assertThat(campaignsByIdMap.keySet(), empty());
    }

    @Test
    public void uacSourceCampaign_AllowableCampaignsNotReturned() {
        var campaignInfo = steps.campaignSteps().createDefaultCampaign();
        testCampaignRepository.setSource(campaignInfo.getShard(), campaignInfo.getCampaignId(), CampaignSource.UAC);
        var entityId = entitySupplier.apply(steps, campaignInfo);

        var campaignsByIdMap = subObjectRetrieverSupplier
                .apply(campaignAccessCheckRepository).get(
                        campaignInfo.getShard(),
                        getApi5AccessibilityChecker()
                                .toAllowableCampaignsRepositoryAdapter(campaignInfo.getClientId()),
                        List.of(entityId));

        assertThat(campaignsByIdMap.keySet(), empty());
    }

    @Test
    public void universalCampaign_AllCampaignsReturned() {
        CampaignInfo campaignInfo = steps.campaignSteps().createDefaultCampaign();
        testCampaignRepository.setOptsToUniversal(campaignInfo.getShard(), campaignInfo.getCampaignId());
        Long entityId = entitySupplier.apply(steps, campaignInfo);

        Map<Long, CampaignForAccessCheckApiImpl> campaignsByIdMap = subObjectRetrieverSupplier
                .apply(campaignAccessCheckRepository).get(
                        campaignInfo.getShard(),
                        getApi5AccessibilityChecker().toAllCampaignsRepositoryAdapter(campaignInfo.getClientId()),
                        List.of(entityId));

        assertThat(campaignsByIdMap.keySet(), contains(entityId));
    }

    @Test
    public void uacSourceCampaign_AllCampaignsReturned() {
        var campaignInfo = steps.campaignSteps().createDefaultCampaign();
        testCampaignRepository.setSource(campaignInfo.getShard(), campaignInfo.getCampaignId(), CampaignSource.UAC);
        var entityId = entitySupplier.apply(steps, campaignInfo);

        var campaignsByIdMap = subObjectRetrieverSupplier
                .apply(campaignAccessCheckRepository).get(
                        campaignInfo.getShard(),
                        getApi5AccessibilityChecker().toAllCampaignsRepositoryAdapter(campaignInfo.getClientId()),
                        List.of(entityId));

        assertThat(campaignsByIdMap.keySet(), contains(entityId));
    }

    @Test
    public void uslugiSourceCampaign_AllowableCampaignsReturned() {
        var campaignInfo = steps.campaignSteps().createDefaultCampaign();
        testCampaignRepository.setSource(campaignInfo.getShard(), campaignInfo.getCampaignId(), CampaignSource.USLUGI);
        var entityId = entitySupplier.apply(steps, campaignInfo);

        var campaignsByIdMap = subObjectRetrieverSupplier
                .apply(campaignAccessCheckRepository).get(
                        campaignInfo.getShard(),
                        getApi5AccessibilityChecker()
                                .toAllowableCampaignsRepositoryAdapter(campaignInfo.getClientId()),
                        List.of(entityId));

        assertThat(campaignsByIdMap.keySet(), empty());
    }

    @Test
    public void uslugiSourceCampaign_AllCampaignsReturned() {
        var campaignInfo = steps.campaignSteps().createDefaultCampaign();
        testCampaignRepository.setSource(campaignInfo.getShard(), campaignInfo.getCampaignId(), CampaignSource.USLUGI);
        var entityId = entitySupplier.apply(steps, campaignInfo);

        var campaignsByIdMap = subObjectRetrieverSupplier
                .apply(campaignAccessCheckRepository).get(
                        campaignInfo.getShard(),
                        getApi5AccessibilityChecker().toAllCampaignsRepositoryAdapter(campaignInfo.getClientId()),
                        List.of(entityId));

        assertThat(campaignsByIdMap.keySet(), contains(entityId));
    }

    @Test
    public void uslugiSourceCampaign_AllowableCampaignsReturned_UslugiRequest() {
        var campaignInfo = steps.campaignSteps().createDefaultCampaign();
        testCampaignRepository.setSource(campaignInfo.getShard(), campaignInfo.getCampaignId(), CampaignSource.USLUGI);
        var entityId = entitySupplier.apply(steps, campaignInfo);

        var campaignsByIdMap = subObjectRetrieverSupplier
                .apply(campaignAccessCheckRepository).get(
                        campaignInfo.getShard(),
                        getApi5AccessibilityChecker(RequestSource.API_USLUGI)
                                .toAllowableCampaignsRepositoryAdapter(campaignInfo.getClientId()),
                        List.of(entityId));

        assertThat(campaignsByIdMap.keySet(), contains(entityId));
    }

    @Test
    public void uslugiSourceCampaign_AllCampaignsReturned_UslugiRequest() {
        var campaignInfo = steps.campaignSteps().createDefaultCampaign();
        testCampaignRepository.setSource(campaignInfo.getShard(), campaignInfo.getCampaignId(), CampaignSource.USLUGI);
        var entityId = entitySupplier.apply(steps, campaignInfo);

        var campaignsByIdMap = subObjectRetrieverSupplier
                .apply(campaignAccessCheckRepository).get(
                        campaignInfo.getShard(),
                        getApi5AccessibilityChecker(RequestSource.API_USLUGI)
                                .toAllCampaignsRepositoryAdapter(campaignInfo.getClientId()),
                        List.of(entityId));

        assertThat(campaignsByIdMap.keySet(), contains(entityId));
    }
}
