package ru.yandex.direct.oneshot.oneshots.attach_wallet_to_user_campaigns;

import java.math.BigDecimal;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.repository.TestCampaignRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.campaign.model0.Campaign;
import ru.yandex.direct.dbschema.ppc.enums.CampaignsArchived;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.oneshot.configuration.OneshotTest;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestCampaigns.newTextCampaign;
import static ru.yandex.direct.dbschema.ppc.Tables.CAMPAIGNS;
import static ru.yandex.direct.dbschema.ppc.Tables.CLIENTS_OPTIONS;

@OneshotTest
@RunWith(SpringRunner.class)
public class AttachWalletToClientCampaignsOneshotValidationTest {

    @Autowired
    private Steps steps;
    @Autowired
    private AttachWalletToClientCampaignsOneshot oneshot;
    @Autowired
    private TestCampaignRepository testCampaignRepository;
    @Autowired
    private DslContextProvider dslContextProvider;

    private ClientId clientId;
    private int shard;
    private Long walletId;
    private Long campaignId;
    private ClientInfo clientInfo;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        clientId = clientInfo.getClientId();
        shard = clientInfo.getShard();

        walletId = steps.campaignSteps().createWalletCampaign(clientInfo).getCampaignId();
        campaignId = steps.campaignSteps()
                .createCampaign(newTextCampaign(clientId, clientInfo.getUid()), clientInfo).getCampaignId();
    }

    @Test
    public void validate() {
        InputData inputData = new InputData()
                .withClientIds(singletonList(clientId.asLong()));

        ValidationResult<InputData, Defect> vr = oneshot.validate(inputData);
        assertThat(vr.hasAnyErrors()).describedAs("наличие ошибок в результате валидации")
                .isFalse();
    }

    @Test
    public void validate_WithoutInputData_HasErrors() {
        InputData inputData = new InputData();

        ValidationResult<InputData, Defect> vr = oneshot.validate(inputData);
        assertThat(vr.hasAnyErrors()).describedAs("наличие ошибок в результате валидации")
                .isTrue();
    }

    @Test
    public void validate_WithEmptyClientIdsList_HasErrors() {
        InputData inputData = new InputData()
                .withClientIds(emptyList());

        ValidationResult<InputData, Defect> vr = oneshot.validate(inputData);
        assertThat(vr.hasAnyErrors()).describedAs("наличие ошибок в результате валидации")
                .isTrue();
    }

    @Test
    public void validate_WithDuplicatedClientIds_HasErrors() {
        InputData inputData = new InputData()
                .withClientIds(List.of(clientId.asLong(), clientId.asLong()));

        ValidationResult<InputData, Defect> vr = oneshot.validate(inputData);
        assertThat(vr.hasAnyErrors()).describedAs("наличие ошибок в результате валидации")
                .isTrue();
    }

    @Test
    public void validate_WithNotValidClientId_HasErrors() {
        InputData inputData = new InputData()
                .withClientIds(singletonList(0L));

        ValidationResult<InputData, Defect> vr = oneshot.validate(inputData);
        assertThat(vr.hasAnyErrors()).describedAs("наличие ошибок в результате валидации")
                .isTrue();
    }

    @Test
    public void validate_WithNullalueInClientIdList_HasErrors() {
        InputData inputData = new InputData()
                .withClientIds(singletonList(null));

        ValidationResult<InputData, Defect> vr = oneshot.validate(inputData);
        assertThat(vr.hasAnyErrors()).describedAs("наличие ошибок в результате валидации")
                .isTrue();
    }

    /**
     * Когда кампания клиента уже прикреплена к кошельку
     */
    @Test
    public void validate_WithCampaignUnderWallet_HasErrors() {
        InputData inputData = new InputData()
                .withClientIds(singletonList(clientId.asLong()));

        attachCampaignToWallet(campaignId, walletId);

        ValidationResult<InputData, Defect> vr = oneshot.validate(inputData);
        assertThat(vr.hasAnyErrors()).describedAs("наличие ошибок в результате валидации")
                .isTrue();
    }

    /**
     * Когда все кампании клиента не прикреплены к кошельку
     */
    @Test
    public void validate_WithTwoCampaignsNotUnderWallet_NoErrors() {
        InputData inputData = new InputData()
                .withClientIds(singletonList(clientId.asLong()));

        steps.campaignSteps().createCampaign(newTextCampaign(clientId, clientInfo.getUid()), clientInfo);

        ValidationResult<InputData, Defect> vr = oneshot.validate(inputData);
        assertThat(vr.hasAnyErrors()).describedAs("наличие ошибок в результате валидации")
                .isFalse();
    }

    /**
     * Когда одна из нескольких кампаний клиента не прикреплена к кошельку
     */
    @Test
    public void validate_WithOneCampaignUnderWalletAndAnotherNot_NoErrors() {
        InputData inputData = new InputData()
                .withClientIds(singletonList(clientId.asLong()));

        steps.campaignSteps().createCampaignUnderWalletByCampaignType(
                CampaignType.TEXT, clientInfo, walletId, BigDecimal.ZERO);

        ValidationResult<InputData, Defect> vr = oneshot.validate(inputData);
        assertThat(vr.hasAnyErrors()).describedAs("наличие ошибок в результате валидации")
                .isFalse();
    }

    /**
     * Когда у клиента только одна пустая кампания (StatusEmpty = Yes)
     */
    @Test
    public void validate_WithEmptyCampaign_HasErrors() {
        InputData inputData = new InputData()
                .withClientIds(singletonList(clientId.asLong()));

        testCampaignRepository.setStatusEmpty(shard, campaignId, true);

        ValidationResult<InputData, Defect> vr = oneshot.validate(inputData);
        assertThat(vr.hasAnyErrors()).describedAs("наличие ошибок в результате валидации")
                .isTrue();
    }

    /**
     * Когда у клиента пустая кампания-кошелек (StatusEmpty = Yes)
     */
    @Test
    public void validate_WithEmptyWallet_HasErrors() {
        InputData inputData = new InputData()
                .withClientIds(singletonList(clientId.asLong()));

        testCampaignRepository.setStatusEmpty(shard, walletId, true);

        ValidationResult<InputData, Defect> vr = oneshot.validate(inputData);
        assertThat(vr.hasAnyErrors()).describedAs("наличие ошибок в результате валидации")
                .isTrue();
    }

    /**
     * Когда у клиента архивный кошелек (StatusArchive = Yes)
     */
    @Test
    public void validate_WithArchiveWallet_HasErrors() {
        InputData inputData = new InputData()
                .withClientIds(singletonList(clientId.asLong()));

        testCampaignRepository.setStatusArchive(shard, walletId, CampaignsArchived.Yes);

        ValidationResult<InputData, Defect> vr = oneshot.validate(inputData);
        assertThat(vr.hasAnyErrors()).describedAs("наличие ошибок в результате валидации")
                .isTrue();
    }

    /**
     * Когда у пользователя отключена возможность использовать ОС
     * (выставлен флаг clients_options.client_flags.create_without_wallet)
     */
    @Test
    public void validate_WhenUserDisabledSharedAccount_HasErrors() {
        InputData inputData = new InputData()
                .withClientIds(singletonList(clientId.asLong()));

        steps.campaignSteps().createCampaign(newTextCampaign(clientId, clientInfo.getUid()), clientInfo);

        dslContextProvider.ppc(shard).update(CLIENTS_OPTIONS)
                .set(CLIENTS_OPTIONS.CLIENT_FLAGS, "create_without_wallet")
                .where(CLIENTS_OPTIONS.CLIENT_ID.eq(clientId.asLong()))
                .execute();

        ValidationResult<InputData, Defect> vr = oneshot.validate(inputData);
        assertThat(vr.hasAnyErrors()).describedAs("наличие ошибок в результате валидации")
                .isTrue();
    }

    /**
     * Когда у клиента есть кампании с деньгами
     */
    @Test
    public void validate_WhenUserHasCampaignWithMoney_HasErrors() {
        InputData inputData = new InputData()
                .withClientIds(singletonList(clientId.asLong()));

        Campaign campaign = newTextCampaign(clientId, clientInfo.getUid());
        campaign.getBalanceInfo()
                .withSum(BigDecimal.TEN);
        steps.campaignSteps().createCampaign(campaign, clientInfo);

        ValidationResult<InputData, Defect> vr = oneshot.validate(inputData);
        assertThat(vr.hasAnyErrors()).describedAs("наличие ошибок в результате валидации")
                .isTrue();
    }

    /**
     * Когда у клиента есть кампании с деньгами но в остатке (с учетеом расходов) сумма равна 0
     */
    @Test
    public void validate_WhenUserHasCampaignWithoutRestOfSum_HasErrors() {
        InputData inputData = new InputData()
                .withClientIds(singletonList(clientId.asLong()));

        Campaign campaign = newTextCampaign(clientId, clientInfo.getUid());
        campaign.getBalanceInfo()
                .withSum(BigDecimal.TEN)
                .withSumSpent(BigDecimal.TEN);
        steps.campaignSteps().createCampaign(campaign, clientInfo);

        ValidationResult<InputData, Defect> vr = oneshot.validate(inputData);
        assertThat(vr.hasAnyErrors()).describedAs("наличие ошибок в результате валидации")
                .isFalse();
    }

    private void attachCampaignToWallet(Long campaignId, Long walletId) {
        dslContextProvider.ppc(shard)
                .update(CAMPAIGNS)
                .set(CAMPAIGNS.WALLET_CID, walletId)
                .where(CAMPAIGNS.CID.eq(campaignId))
                .execute();
    }
}
