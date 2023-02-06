package ru.yandex.direct.core.entity.promocodes.service;

import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.i18n.Language;
import ru.yandex.direct.rbac.RbacRepType;
import ru.yandex.direct.sender.YandexSenderTemplateParams;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@CoreTest
@RunWith(Parameterized.class)
public class PromocodesTearOffMailSenderServicePrepareParamsForEmailTest {
    private static final String EMAIL = "test-dzhadikov-user@yandex.ru";
    private static final String FIO = "Вася Пупкин";

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private Steps steps;
    @Autowired
    private PromocodesTearOffMailSenderService senderService;

    private long campaignId;
    private UserInfo userInfo;

    @Parameter
    public Language lang;

    @Parameters(name = "{index}: lang = {0}")
    public static Object[] getLanguage() {
        return Language.values();
    }

    @Before
    public void prepareData() {
        User chiefUser = new User()
                .withRepType(RbacRepType.CHIEF)
                .withEmail(EMAIL)
                .withFio(FIO)
                .withLang(lang)
                .withCanManagePricePackages(false)
                .withCanApprovePricePackages(false);

        userInfo = steps.userSteps().createUser(chiefUser);
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveCampaign(userInfo.getClientInfo());
        campaignId = campaignInfo.getCampaignId();
    }

    @Test
    public void checkValidParams() {
        User user = senderService.getChiefByCampaignId(campaignId);
        YandexSenderTemplateParams params = senderService.prepareParamsForEmail(user);

        assertEquals(EMAIL, params.getToEmail());
        assertEquals(FIO, params.getArgs().get(PromocodesTearOffMailSenderService.NAME));
        assertEquals(userInfo.getClientId().toString(),
                params.getArgs().get(PromocodesTearOffMailSenderService.CLIENT_ID));
        assertTrue(StringUtils.isNotEmpty(params.getCampaignSlug()));
    }
}
