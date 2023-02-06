package ru.yandex.direct.jobs.campaign.paused.daybudget;


import java.util.List;
import java.util.Map;

import org.apache.commons.validator.routines.EmailValidator;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.common.TranslationService;
import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.eventlog.service.EventLogService;
import ru.yandex.direct.core.entity.notification.repository.SmsQueueRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.i18n.Language;
import ru.yandex.direct.jobs.configuration.JobsTest;
import ru.yandex.direct.sender.YandexSenderClient;
import ru.yandex.direct.sender.YandexSenderTemplateParams;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.direct.jobs.campaign.paused.daybudget.PausedByDayBudgetSenderService.CAMPAIGN_ID_KEY;
import static ru.yandex.direct.jobs.campaign.paused.daybudget.PausedByDayBudgetSenderService.CLIENT_ID_KEY;
import static ru.yandex.direct.jobs.campaign.paused.daybudget.PausedByDayBudgetSenderService.LOGIN_KEY;
import static ru.yandex.direct.jobs.campaign.paused.daybudget.PausedByDayBudgetSenderService.MENTION_CID_KEY;
import static ru.yandex.direct.jobs.campaign.paused.daybudget.PausedByDayBudgetSenderService.MENTION_CID_VALUE;
import static ru.yandex.direct.jobs.campaign.paused.daybudget.PausedByDayBudgetSenderService.NOT_MENTION_CID_VALUE;


@JobsTest
@ExtendWith(SpringExtension.class)
class PausedByDayBudgetSenderServiceTest {

    private PausedByDayBudgetSenderService senderService;

    @Autowired
    private UserService userService;

    @Autowired
    private PausedByDayBudgetCampaignsWarningsMailTemplateResolver templateResolver;

    @Autowired
    private EventLogService eventLogService;

    @Autowired
    private SmsQueueRepository smsQueueRepository;

    private YandexSenderClient senderClient;

    @Autowired
    private TranslationService translationService;

    @Autowired
    private Steps steps;

    @Autowired
    private CampaignRepository campaignRepository;

    @BeforeEach
    public void setUp() {
        senderClient = mock(YandexSenderClient.class);
        translationService = mock(TranslationService.class);

        senderService = new PausedByDayBudgetSenderService(userService, senderClient, templateResolver,
                eventLogService, smsQueueRepository, translationService);
    }

    public static Object[][] emailSendingParameters() {
        return new Object[][]{
                {"RU, invalid",
                        Language.RU, "wrongEmail"},
                {"RU, valid",
                        Language.RU, "nice.email@yandex-team.ru"},
                {"EN, valid",
                        Language.EN, "nicer@yandex.ru"},
                {"TR, valid",
                        Language.TR, "best@mail.ru"}
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("emailSendingParameters")
    void testEmailSending(String description, Language lang, String email) {
        var clientInfo = steps.clientSteps().createDefaultClient();
        steps.userSteps().setUserProperty(clientInfo.getChiefUserInfo(), User.EMAIL, email);
        steps.userSteps().setUserProperty(clientInfo.getChiefUserInfo(), User.LANG, lang);

        CampaignInfo headCampaignInfo = steps.campaignSteps().createActiveCampaign(clientInfo);
        Campaign campaign = PausedByDayBudgetTestUtils.campaignsByInfos(
                        clientInfo.getShard(),
                        campaignRepository,
                        List.of(headCampaignInfo))
                .get(0);


        List<Boolean> needMentionCids = List.of(false, true);
        for (Boolean needMentionCid : needMentionCids) {
            var result = senderService.sendMail(campaign, needMentionCid);

            if (!EmailValidator.getInstance().isValid(email)) {
                assertThat(result).isEqualTo(PausedByDayBudgetSenderService.MailSendingResult.ERROR);
                return;
            }
        }

        var softAssertions = new SoftAssertions();
        var argument = ArgumentCaptor.forClass(YandexSenderTemplateParams.class);
        verify(senderClient, times(needMentionCids.size())).sendTemplate(argument.capture(), any());

        for (int i = 0; i < needMentionCids.size(); i++) {
            YandexSenderTemplateParams expectedTemplateParams = new YandexSenderTemplateParams.Builder()
                    .withToEmail(email)
                    .withCampaignSlug(templateResolver.getTemplateByLanguage(lang))
                    .withArgs(
                            Map.ofEntries(
                                    Map.entry(CLIENT_ID_KEY, clientInfo.getClientId().toString()),
                                    Map.entry(LOGIN_KEY, clientInfo.getChiefUserInfo().getLogin()),
                                    Map.entry(CAMPAIGN_ID_KEY, campaign.getId().toString()),
                                    Map.entry(MENTION_CID_KEY, needMentionCids.get(i) ?
                                            MENTION_CID_VALUE :
                                            NOT_MENTION_CID_VALUE)))
                    .build();

            softAssertions.assertThat(argument.getAllValues().get(i)).isEqualToIgnoringNullFields(expectedTemplateParams);
        }
        softAssertions.assertAll();
    }
}
