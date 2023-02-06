package ru.yandex.autotests.direct.httpclient.banners.phrases.ajaxapplyrejectcorrections;

import java.util.List;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;

import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.commons.phrase.Phrase;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.steps.base.DirectCmdStepsException;
import ru.yandex.autotests.direct.httpclient.CocaineSteps;
import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CSRFToken;
import ru.yandex.autotests.direct.httpclient.data.phrases.ajaxapplyrejectcorrections.AjaxApplyRejectCorrectionsParameters;
import ru.yandex.autotests.direct.httpclient.data.phrases.ajaxapplyrejectcorrections.AjaxApplyRejectCorrectionsPhrase;
import ru.yandex.autotests.direct.httpclient.data.phrases.ajaxapplyrejectcorrections.AjaxApplyRejectCorrectionsPhrasesBean;
import ru.yandex.autotests.direct.httpclient.util.PropertyLoader;
import ru.yandex.autotests.directapi.model.User;

import static java.util.stream.Collectors.toList;

/**
 * Created by shmykov on 27.05.15.
 * TESTIRT-4999
 */
public abstract class AjaxApplyRejectCorrectionsTestBase {

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    protected static CSRFToken csrfToken;
    protected final String CLIENT_LOGIN = "at-direct-b-ajaxapplyreject";
    @Rule
    public DirectCmdRule cmdRule;

    public BannersRule bannersRule;
    protected AjaxApplyRejectCorrectionsPhrasesBean jsonPhrases;
    protected AjaxApplyRejectCorrectionsParameters requestParams;
    protected PropertyLoader<AjaxApplyRejectCorrectionsPhrase> loader;
    protected DirectResponse response;


    public AjaxApplyRejectCorrectionsTestBase(List<Phrase> phraseList)
    {
        bannersRule = new TextBannersRule().overrideGroupTemplate(new Group().withPhrases(phraseList))
                .withUlogin(CLIENT_LOGIN);
        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    }

    @Before
    public void before() {
        loader = new PropertyLoader<>(AjaxApplyRejectCorrectionsPhrase.class);
        csrfToken = CocaineSteps.getCsrfTokenFromCocaine(User.get(CLIENT_LOGIN).getPassportUID());

        jsonPhrases = new AjaxApplyRejectCorrectionsPhrasesBean();
        requestParams = new AjaxApplyRejectCorrectionsParameters();
        requestParams.setUlogin(CLIENT_LOGIN);
        requestParams.setCid(String.valueOf(bannersRule.getCampaignId()));
        requestParams.setJsonPhrases(jsonPhrases);
        setPhrasesInRequest();
        cmdRule.oldSteps().onPassport().authoriseAs(CLIENT_LOGIN, User.get(CLIENT_LOGIN).getPassword());
    }

    protected List<String> getPhrases() {
        return cmdRule.cmdSteps().campaignSteps()
                .getCampaign(CLIENT_LOGIN, bannersRule.getCampaignId()).getGroups().stream()
                .filter(group -> group.getBanners().stream()
                        .map(Banner::getBid)
                        .anyMatch(bid -> bid.equals(bannersRule.getBannerId()))
                )
                .findFirst()
                .orElseThrow(() -> new DirectCmdStepsException("Ожидаемый баннер не найден")).getPhrases().stream()
                .map(phrase -> phrase.getPhrase())
                .collect(toList());
    }

    protected abstract void setPhrasesInRequest();
}
