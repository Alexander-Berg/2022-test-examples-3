package ru.yandex.direct.intapi.entity.user.service;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.common.TranslationService;
import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignOpts;
import ru.yandex.direct.core.entity.campaign.model.SmsFlag;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.client.model.Client;
import ru.yandex.direct.core.entity.client.repository.ClientRepository;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.dbutil.sharding.ShardKey;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.i18n.Language;
import ru.yandex.direct.intapi.configuration.IntApiTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.utils.FunctionalUtils.listToSet;

@IntApiTest
@RunWith(SpringJUnit4ClassRunner.class)
public class SmsHelperTest {

    @Autowired
    private TranslationService translationService;
    @Autowired
    private ShardHelper shardHelper;
    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private CampaignRepository campaignRepository;
    @Autowired
    private FeatureService featureService;
    @Autowired
    private UserService userService;

    @Autowired
    private Steps steps;

    private CampaignInfo textCampaignInfoNonDynamic;
    private CampaignInfo textCampaignInfoNonDynamicTouch;
    private CampaignInfo textCampaignInfoNonDynamicWallet;
    private CampaignInfo textCampaignInfoNonDynamicWalletTouchCampaign;
    private CampaignInfo textCampaignInfoNonDynamicWalletTouchClientCampaign;

    private CampaignInfo textCampaignInfoDynamic;
    private CampaignInfo textCampaignInfoDynamicTouch;

    @Before
    public void setUp() {
        textCampaignInfoNonDynamic = steps.campaignSteps().createActiveTextCampaign();

        textCampaignInfoNonDynamicTouch = steps.campaignSteps().createActiveTextCampaign();
        steps.campaignSteps().setCampaignProperty(textCampaignInfoNonDynamicTouch,
                Campaign.OPTS, EnumSet.of(CampaignOpts.IS_TOUCH)
        );

        textCampaignInfoNonDynamicWallet = steps.campaignSteps().createWalletCampaign(
                steps.clientSteps().createDefaultClient());

        textCampaignInfoNonDynamicWalletTouchCampaign = steps.campaignSteps().createWalletCampaign(
                steps.clientSteps().createDefaultClient());
        steps.campaignSteps().setCampaignProperty(textCampaignInfoNonDynamicWalletTouchCampaign,
                Campaign.OPTS, EnumSet.of(CampaignOpts.IS_TOUCH)
        );

       textCampaignInfoNonDynamicWalletTouchClientCampaign = steps.campaignSteps().createWalletCampaign(
               steps.clientSteps().createDefaultClient());
       steps.clientSteps().setClientProperty(textCampaignInfoNonDynamicWalletTouchClientCampaign.getClientInfo(),
               Client.IS_TOUCH, Boolean.TRUE);

       textCampaignInfoDynamic = steps.campaignSteps().createActiveTextCampaign();
       steps.featureSteps().addClientFeature(textCampaignInfoDynamic.getClientId(),
               FeatureName.USE_DYNAMIC_THRESHOLD_FOR_SEND_ORDER_WARNINGS, true);

       textCampaignInfoDynamicTouch = steps.campaignSteps().createActiveTextCampaign();
       steps.campaignSteps().setCampaignProperty(textCampaignInfoDynamicTouch,
               Campaign.OPTS, EnumSet.of(CampaignOpts.IS_TOUCH)
       );
       steps.featureSteps().addClientFeature(textCampaignInfoDynamicTouch.getClientId(),
               FeatureName.USE_DYNAMIC_THRESHOLD_FOR_SEND_ORDER_WARNINGS, true);
    }

    private List<Campaign> resolveCampaigns(Collection<Long> campaignsIds) {
        return shardHelper.groupByShard(campaignsIds, ShardKey.CID)
                .stream()
                .map(e -> campaignRepository.getCampaigns(e.getKey(), listToSet(e.getValue())))
                .flatMap(Collection::stream).toList();
    }

    @Test
    public void testIsTouch() throws Exception {
        final List<Long> campaignsIds = Arrays.asList(
                textCampaignInfoNonDynamic.getCampaignId(),
                textCampaignInfoNonDynamicTouch.getCampaignId(),
                textCampaignInfoNonDynamicWallet.getCampaignId(),
                textCampaignInfoNonDynamicWalletTouchCampaign.getCampaignId(),
                textCampaignInfoNonDynamicWalletTouchClientCampaign.getCampaignId(),
                textCampaignInfoDynamic.getCampaignId(),
                textCampaignInfoDynamicTouch.getCampaignId()
        );

        final Set<Long> targetTouchRepositories = Set.of(
                textCampaignInfoNonDynamicTouch.getCampaignId(),
                textCampaignInfoNonDynamicWalletTouchCampaign.getCampaignId(),
                textCampaignInfoNonDynamicWalletTouchClientCampaign.getCampaignId(),
                textCampaignInfoDynamicTouch.getCampaignId()
        );

        List<Campaign> campaigns = resolveCampaigns(campaignsIds);
        SmsHelper smsHelper = new SmsHelper(translationService, shardHelper, clientRepository);

        Set<Long> touchCampaignsIds = smsHelper.getTouchCampaignsIds(campaigns);

        assertThat(touchCampaignsIds).isEqualTo(targetTouchRepositories);
    }


    SmsHelper.UserWarningSms createWarningSms(CampaignInfo campaignInfo) {
        SmsHelper smsHelper = new SmsHelper(translationService, shardHelper, clientRepository);
        User user = userService.getUser(campaignInfo.getUid());
        Campaign campaign = resolveCampaigns(Collections.singletonList(campaignInfo.getCampaignId())).get(0);

        boolean useDynamicThreshold = featureService.isEnabledForClientId(ClientId.fromLong(campaign.getClientId()),
                FeatureName.USE_DYNAMIC_THRESHOLD_FOR_SEND_ORDER_WARNINGS);

        boolean isTouchCampaign = smsHelper.getTouchCampaignsIds(Collections.singletonList(campaign))
                .contains(campaignInfo.getCampaignId());
        return smsHelper.createSmsMessage(user, campaign, isTouchCampaign, useDynamicThreshold);
    }

    @Test
    public void testCreateSmsMessage() throws Exception {
        final Language language = Language.RU;
        final Locale locale = Locale.forLanguageTag(language.getLangString());
        ActiveOrdersMoneyOutSmsTranslations instance = ActiveOrdersMoneyOutSmsTranslations.INSTANCE;

        final SmsHelper.UserWarningSms touchSms = new SmsHelper.UserWarningSms(
                translationService.translate(instance.activeOrdersMoneyOutTouch(), locale),
                SmsFlag.ACTIVE_ORDERS_MONEY_OUT_TOUCH_SMS);

        final SmsHelper.UserWarningSms walletSms = new SmsHelper.UserWarningSms(
                translationService.translate(instance.activeOrdersMoneyOutWallet(), locale),
                SmsFlag.ACTIVE_ORDERS_MONEY_OUT_SMS);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(createWarningSms(textCampaignInfoNonDynamic))
                .as("Обычные кампании без фичи динамического порога")
                .isEqualTo(new SmsHelper.UserWarningSms(
                        translationService.translate(instance.activeOrdersMoneyOut(
                                textCampaignInfoNonDynamic.getCampaignId()), locale
                        ), SmsFlag.ACTIVE_ORDERS_MONEY_OUT_SMS));

        soft.assertThat(createWarningSms(textCampaignInfoNonDynamicTouch))
                .as("Обычные тачёвые кампании без фичи динамического порога")
                .isEqualTo(touchSms);
        soft.assertThat(createWarningSms(textCampaignInfoNonDynamicWallet))
                .as("Кампании с кошельком без фичи динамического порога")
                .isEqualTo(walletSms);
        soft.assertThat(createWarningSms(textCampaignInfoNonDynamicWalletTouchCampaign))
                .as("Тачёвые кампании с кошельком без фичи динамического порога с флагом тачёвости " +
                        "на кампании")
                .isEqualTo(touchSms);
        soft.assertThat(createWarningSms(textCampaignInfoNonDynamicWalletTouchClientCampaign))
                .as("Тачёвые кампании с кошельком без фичи динамического порога с флагом тачёвости " +
                        "на клиенте")
                .isEqualTo(touchSms);

        soft.assertThat(createWarningSms(textCampaignInfoDynamic))
                .as("Обычные кампании с фичей динамического порога")
                .isEqualTo(new SmsHelper.UserWarningSms(
                        translationService.translate(instance.activeOrdersMoneyOutCampaignStopped(
                                userService.getUser(textCampaignInfoDynamic.getUid()).getLogin()),
                                locale),
                        SmsFlag.ACTIVE_ORDERS_MONEY_OUT_CAMPAIGN_STOPPED_SMS));

        soft.assertThat(createWarningSms(textCampaignInfoDynamicTouch))
                .as("Тачёвые кампании с фичей динамического порога")
                .isEqualTo(new SmsHelper.UserWarningSms(
                translationService.translate(instance.activeOrdersMoneyOutCampaignStoppedTouch(
                        userService.getUser(textCampaignInfoDynamicTouch.getUid()).getLogin()),
                        locale),
                SmsFlag.ACTIVE_ORDERS_MONEY_OUT_CAMPAIGN_STOPPED_TOUCH_SMS));
        soft.assertAll();
    }
}
