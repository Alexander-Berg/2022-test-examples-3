package ru.yandex.direct.core.entity.campaign.service.validation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.bs.export.queue.model.BsExportQueueInfo;
import ru.yandex.direct.core.entity.bs.export.queue.repository.BsExportQueueRepository;
import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertThat;
import static org.mockito.MockitoAnnotations.initMocks;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefects.campaignNotFound;
import static ru.yandex.direct.core.testing.data.TestBsExportQueueRecords.yesterdayRecordWithoutStat;
import static ru.yandex.direct.core.testing.data.TestCampaigns.emptyBalanceInfo;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.unableToDelete;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class DeleteCampaignValidationServiceTest {

    @Autowired
    private Steps steps;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private BsExportQueueRepository queueRepository;

    @Autowired
    private DeleteCampaignValidationService deleteCampaignValidationService;

    private Campaign newTextCampaign;

    private Campaign activeTextCampaign;

    private long operatorUid;
    private ClientId clientId;
    private int shard;
    private ClientInfo clientInfo;

    @Before
    public void before() {
        initMocks(this);

        clientInfo = steps.clientSteps().createDefaultClient();

        ru.yandex.direct.core.testing.steps.campaign.model0.Campaign campaignTest1 =
                TestCampaigns.newTextCampaign(null, null);

        ru.yandex.direct.core.testing.steps.campaign.model0.Campaign campaignTest2 =
                TestCampaigns.activeTextCampaign(null, null);

        CampaignInfo campaignInfo1 = steps.campaignSteps().createCampaign(campaignTest1, clientInfo);
        CampaignInfo campaignInfo2 = steps.campaignSteps().createCampaign(campaignTest2, clientInfo);

        operatorUid = campaignInfo1.getUid();
        clientId = campaignInfo1.getClientId();
        shard = campaignInfo1.getShard();

        newTextCampaign = getCampaign(shard, campaignInfo1.getCampaignId());
        activeTextCampaign = getCampaign(shard, campaignInfo2.getCampaignId());
    }

    @Test
    public void validate_OneValidItem_ResultIsExpected() {
        List<Long> campaignIds = List.of(newTextCampaign.getId());

        ValidationResult<List<Long>, Defect> vr = deleteCampaignValidationService.validate(campaignIds, operatorUid,
                clientId, shard);

        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validate_DeleteDeletedCampaign_Error() {
        List<Long> campaignIds = List.of(newTextCampaign.getId());

        campaignRepository.setStatusEmptyToYes(shard, campaignIds);

        ValidationResult<List<Long>, Defect> vr = deleteCampaignValidationService.validate(campaignIds, operatorUid,
                clientId, shard);

        assertThat(vr, hasDefectDefinitionWith(validationError(path(index(0)), campaignNotFound())));
    }

    @Test
    public void validate_DeleteActiveCampaign_Error() {
        List<Long> campaignIds = List.of(activeTextCampaign.getId());

        ValidationResult<List<Long>, Defect> vr = deleteCampaignValidationService.validate(campaignIds, operatorUid,
                clientId, shard);

        assertThat(vr, hasDefectDefinitionWith(validationError(path(index(0)), unableToDelete())));

    }

    @Test
    public void validate_AnotherClientCampaign_Error() {
        List<Long> campaignIds = List.of(newTextCampaign.getId());

        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();

        ValidationResult<List<Long>, Defect> vr = deleteCampaignValidationService.validate(campaignIds,
                clientInfo.getUid(), clientInfo.getClientId(), clientInfo.getShard());

        assertThat(vr, hasDefectDefinitionWith(validationError(path(index(0)), campaignNotFound())));
    }

    @Test
    public void validate_SumToPay_NonZero_Error() {
        ru.yandex.direct.core.testing.steps.campaign.model0.Campaign campaignTest =
                TestCampaigns.newTextCampaign(null, null)
                        .withBalanceInfo(emptyBalanceInfo(CurrencyCode.RUB)
                                .withSumToPay(BigDecimal.valueOf(50_000L).setScale(6, RoundingMode.DOWN)));

        CampaignInfo campaignInfo = steps.campaignSteps().createCampaign(campaignTest, clientInfo);
        List<Long> campaignIds = List.of(campaignInfo.getCampaignId());

        ValidationResult<List<Long>, Defect> vr = deleteCampaignValidationService.validate(campaignIds, operatorUid,
                clientId, shard);

        assertThat(vr, hasDefectDefinitionWith(validationError(path(index(0)), unableToDelete())));
    }

    @Test
    public void validate_CampaignSum_NonZero_Error() {
        ru.yandex.direct.core.testing.steps.campaign.model0.Campaign campaignTest =
                TestCampaigns.newTextCampaign(null, null)
                        .withBalanceInfo(emptyBalanceInfo(CurrencyCode.RUB)
                                .withSum(BigDecimal.valueOf(50_000L).setScale(6, RoundingMode.DOWN)));

        CampaignInfo campaignInfo = steps.campaignSteps().createCampaign(campaignTest, clientInfo);
        List<Long> campaignIds = List.of(campaignInfo.getCampaignId());

        ValidationResult<List<Long>, Defect> vr = deleteCampaignValidationService.validate(campaignIds, operatorUid,
                clientId, shard);

        assertThat(vr, hasDefectDefinitionWith(validationError(path(index(0)), unableToDelete())));
    }

    @Test
    public void validate_CampaignSum_EmptyWallet_Success() {
        ru.yandex.direct.core.testing.steps.campaign.model0.Campaign walletCampaign =
                TestCampaigns.newTextCampaign(null, null)
                        .withBalanceInfo(emptyBalanceInfo(CurrencyCode.RUB));
        Long walletId = steps.campaignSteps().createCampaign(walletCampaign, clientInfo).getCampaignId();

        ru.yandex.direct.core.testing.steps.campaign.model0.Campaign campaignTest =
                TestCampaigns.newTextCampaign(null, null)
                        .withBalanceInfo(emptyBalanceInfo(CurrencyCode.RUB)
                                .withWalletCid(walletId));

        CampaignInfo campaignInfo = steps.campaignSteps().createCampaign(campaignTest, clientInfo);
        List<Long> campaignIds = List.of(campaignInfo.getCampaignId());

        ValidationResult<List<Long>, Defect> vr = deleteCampaignValidationService.validate(campaignIds, operatorUid,
                clientId, shard);

        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validate_CampaignSum_NotEmptyWallet_Success() {
        ru.yandex.direct.core.testing.steps.campaign.model0.Campaign walletCampaign =
                TestCampaigns.newTextCampaign(null, null)
                        .withBalanceInfo(emptyBalanceInfo(CurrencyCode.RUB)
                                .withSum(BigDecimal.valueOf(50_000L).setScale(6, RoundingMode.DOWN)));
        Long walletId = steps.campaignSteps().createCampaign(walletCampaign, clientInfo).getCampaignId();

        ru.yandex.direct.core.testing.steps.campaign.model0.Campaign campaignTest =
                TestCampaigns.newTextCampaign(null, null)
                        .withBalanceInfo(emptyBalanceInfo(CurrencyCode.RUB)
                                .withWalletCid(walletId));

        CampaignInfo campaignInfo = steps.campaignSteps().createCampaign(campaignTest, clientInfo);
        List<Long> campaignIds = List.of(campaignInfo.getCampaignId());

        ValidationResult<List<Long>, Defect> vr = deleteCampaignValidationService.validate(campaignIds, operatorUid,
                clientId, shard);

        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validate_SumLast_NonZero_Error() {
        ru.yandex.direct.core.testing.steps.campaign.model0.Campaign campaignTest =
                TestCampaigns.newTextCampaign(null, null)
                        .withBalanceInfo(emptyBalanceInfo(CurrencyCode.RUB)
                                .withSumLast(BigDecimal.valueOf(50_000L).setScale(6, RoundingMode.DOWN)));

        CampaignInfo campaignInfo = steps.campaignSteps().createCampaign(campaignTest, clientInfo);
        List<Long> campaignIds = List.of(campaignInfo.getCampaignId());

        ValidationResult<List<Long>, Defect> vr = deleteCampaignValidationService.validate(campaignIds, operatorUid,
                clientId, shard);

        assertThat(vr, hasDefectDefinitionWith(validationError(path(index(0)), unableToDelete())));
    }

    @Test
    public void validate_ConvertedToYndxFixed_Error() {
        ru.yandex.direct.core.testing.steps.campaign.model0.Campaign campaignTest =
                TestCampaigns.newTextCampaign(null, null)
                        .withBalanceInfo(emptyBalanceInfo(CurrencyCode.RUB)
                                .withCurrencyConverted(true)
                                .withCurrency(CurrencyCode.YND_FIXED));

        CampaignInfo campaignInfo = steps.campaignSteps().createCampaign(campaignTest, clientInfo);
        List<Long> campaignIds = List.of(campaignInfo.getCampaignId());

        ValidationResult<List<Long>, Defect> vr = deleteCampaignValidationService.validate(campaignIds, operatorUid,
                clientId, shard);

        assertThat(vr, hasDefectDefinitionWith(validationError(path(index(0)), unableToDelete())));
    }

    @Test
    public void validate_ConvertedToEur_Success() {
        ru.yandex.direct.core.testing.steps.campaign.model0.Campaign campaignTest =
                TestCampaigns.newTextCampaign(null, null)
                        .withBalanceInfo(emptyBalanceInfo(CurrencyCode.RUB)
                                .withCurrencyConverted(true)
                                .withCurrency(CurrencyCode.EUR));

        CampaignInfo campaignInfo = steps.campaignSteps().createCampaign(campaignTest, clientInfo);
        List<Long> campaignIds = List.of(campaignInfo.getCampaignId());

        ValidationResult<List<Long>, Defect> vr = deleteCampaignValidationService.validate(campaignIds, operatorUid,
                clientId, shard);

        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validate_NotConverted_Success() {
        ru.yandex.direct.core.testing.steps.campaign.model0.Campaign campaignTest =
                TestCampaigns.newTextCampaign(null, null)
                        .withBalanceInfo(emptyBalanceInfo(CurrencyCode.RUB)
                                .withCurrencyConverted(false));

        CampaignInfo campaignInfo = steps.campaignSteps().createCampaign(campaignTest, clientInfo);
        List<Long> campaignIds = List.of(campaignInfo.getCampaignId());

        ValidationResult<List<Long>, Defect> vr = deleteCampaignValidationService.validate(campaignIds, operatorUid,
                clientId, shard);

        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validate_CampaignInBsQueue_Error() {
        List<Long> campaignIds = List.of(newTextCampaign.getId());

        BsExportQueueInfo queueRecord = yesterdayRecordWithoutStat(campaignIds.get(0));
        queueRepository.insertRecord(shard, queueRecord);

        ValidationResult<List<Long>, Defect> vr = deleteCampaignValidationService.validate(campaignIds, operatorUid,
                clientId, shard);

        assertThat(vr, hasDefectDefinitionWith(validationError(path(index(0)), unableToDelete())));
    }

    private Campaign getCampaign(int shard, long id) {
        return campaignRepository.getCampaigns(shard, singletonList(id)).iterator().next();
    }
}
