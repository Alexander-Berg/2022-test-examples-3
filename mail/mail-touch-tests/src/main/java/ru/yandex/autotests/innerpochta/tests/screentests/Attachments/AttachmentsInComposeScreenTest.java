package ru.yandex.autotests.innerpochta.tests.screentests.Attachments;

import com.google.common.collect.Sets;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.openqa.selenium.By;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.annotations.DoTestOnlyForEnvironment;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.Set;
import java.util.function.Consumer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.openqa.selenium.By.cssSelector;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.COMPOSE;
import static ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager.touchScreenRulesManager;
import static ru.yandex.autotests.innerpochta.util.MailConst.IMAGE_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.MailConst.PDF_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.MailConst.XIVA_TIMEOUT;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.TouchTestsIgnoredElements.IGNORED_ELEMENTS;
import static ru.yandex.autotests.innerpochta.util.Utils.withWaitFor;

/**
 * @author oleshko
 */

@Aqua.Test
@Title("Скриночные тесты на аттачи в композе")
@Features(FeaturesConst.ATTACHES)
@Stories(FeaturesConst.GENERAL)
@RunWith(DataProviderRunner.class)
public class AttachmentsInComposeScreenTest {

    private static final Set<By> IGNORE_THIS = Sets.newHashSet(
        cssSelector(".compose-AttachmentItem-ProgressIndicator"),
        cssSelector(".ComposeAttachmentsLoadingProgress")
    );

    private TouchScreenRulesManager rules = touchScreenRulesManager().withLock(AccLockRule.use().useTusAccount());
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule acc = rules.getLock();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createTouchRuleChain();

    @Before
    public void prep() {
        stepsProd.user().apiMessagesSteps().sendMailWithAttachmentsAndHTMLBody(
            acc.firstAcc().getSelfEmail(),
            Utils.getRandomString(),
            Utils.getRandomString(),
            IMAGE_ATTACHMENT,
            PDF_ATTACHMENT
        );
    }

    @Test
    @Title("Сравниваем список аттачей с диска")
    @TestCaseId("429")
    public void shouldSeeDiskAttachments() {
        Consumer<InitStepsRule> act = st -> {
            st.user().touchSteps().switchToComposeIframe();
            st.user().defaultSteps().clicksOn(st.pages().touch().composeIframe().header().clip())
                .shouldSee(st.pages().touch().composeIframe().attachFilesPopup())
                .clicksOn(st.pages().touch().composeIframe().attachFilesPopup().fromDisk())
                .shouldSee(st.pages().touch().composeIframe().diskAttachmentsPage().attachBtn());
        };
        parallelRun.withActions(act).withAcc(acc.firstAcc()).withUrlPath(COMPOSE.makeTouchUrlPart()).run();
    }

    @Test
    @Title("Сравниваем список аттачей из почты")
    @TestCaseId("430")
    public void shouldSeeMailAttachments() {
        Consumer<InitStepsRule> act = st -> {
            st.user().touchSteps().switchToComposeIframe();
            st.user().defaultSteps().clicksOn(st.pages().touch().composeIframe().header().clip())
                .shouldSee(st.pages().touch().composeIframe().attachFilesPopup())
                .clicksOn(st.pages().touch().composeIframe().attachFilesPopup().fromMail())
                .shouldSee(st.pages().touch().composeIframe().diskAttachmentsPage().attachBtn());
        };
        parallelRun.withActions(act).withAcc(acc.firstAcc()).withUrlPath(COMPOSE.makeTouchUrlPart())
            .withIgnoredElements(IGNORED_ELEMENTS).run();
    }

    @Test
    @Title("Сравниваем список аттачей из почты")
    @TestCaseId("430")
    public void shouldSeeMailAttachmentsInFolder() {
        Consumer<InitStepsRule> act = st -> {
            st.user().touchSteps().switchToComposeIframe();
            st.user().defaultSteps().clicksOn(st.pages().touch().composeIframe().header().clip())
                .shouldSee(st.pages().touch().composeIframe().attachFilesPopup())
                .clicksOn(st.pages().touch().composeIframe().attachFilesPopup().fromMail())
                .clicksOn(st.pages().touch().composeIframe().diskAttachmentsPage().attachments().get(1))
                .shouldSee(st.pages().touch().composeIframe().diskAttachmentsPage().attachment());
        };
        parallelRun.withActions(act).withAcc(acc.firstAcc()).withUrlPath(COMPOSE.makeTouchUrlPart())
            .withIgnoredElements(IGNORED_ELEMENTS).run();
    }

    @Test
    @Title("Добавление аттачей в композ")
    @TestCaseId("128")
    public void shouldAttachInCompose() {
        Consumer<InitStepsRule> act = st -> {
            st.user().touchSteps().switchToComposeIframe();
            st.user().defaultSteps().clicksOn(
                st.pages().touch().composeIframe().header().clip(),
                st.pages().touch().composeIframe().attachFilesPopup().fromDisk()
            )
                .turnTrue(st.pages().touch().composeIframe().diskAttachmentsPage().checkbox().get(1))
                .clicksOn(st.pages().touch().composeIframe().diskAttachmentsPage().attachBtn())
                .shouldSee(st.pages().touch().composeIframe().attachments().uploadedAttachment());
        };
        parallelRun.withActions(act).withAcc(acc.firstAcc())
            .withUrlPath(COMPOSE.makeTouchUrlPart()).withIgnoredElements(IGNORED_ELEMENTS).run();
    }

    @Test
    @Title("Несколько рядов аттачей в композе")
    @TestCaseId("777")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldScrollAttachmentsInCompose() {
        Consumer<InitStepsRule> act = st -> {
            st.user().touchSteps().switchToComposeIframe();
            addDiskAttaches(st, 1);
        };
        parallelRun.withActions(act).withAcc(acc.firstAcc()).withUrlPath(COMPOSE.makeTouchUrlPart())
            .withIgnoredElements(IGNORE_THIS).run();
    }

    @Test
    @Title("Несколько рядов аттачей в композе")
    @TestCaseId("777")
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldScrollAttachmentsInComposeTablet() {
        Consumer<InitStepsRule> act = st -> {
            st.user().touchSteps().switchToComposeIframe();
            addDiskAttaches(st, 1);
        };
        parallelRun.withActions(act).withAcc(acc.firstAcc()).withUrlPath(COMPOSE.makeTouchUrlPart())
            .withIgnoredElements(IGNORE_THIS).run();
    }

    @Test
    @Title("Сравниваем попап прикрепления аттачей")
    @TestCaseId("1389")
    public void shouldSeeAttachFilesPopup() {
        Consumer<InitStepsRule> act = st -> {
            st.user().touchSteps().switchToComposeIframe();
            st.user().defaultSteps().clicksOn(st.pages().touch().composeIframe().header().clip())
                .shouldSee(st.pages().touch().composeIframe().attachFilesPopup());
        };
        parallelRun.withActions(act).withAcc(acc.firstAcc()).withUrlPath(COMPOSE.makeTouchUrlPart()).run();
    }

    @Step("Добавляем в композ все аттачи с диска")
    private void addDiskAttaches(InitStepsRule st, int i) {
        st.user().defaultSteps().clicksOn(st.pages().touch().composeIframe().header().clip())
            .shouldSee(st.pages().touch().composeIframe().attachFilesPopup())
            .clicksOn(st.pages().touch().composeIframe().attachFilesPopup().fromDisk())
            .shouldSee(st.pages().touch().composeIframe().diskAttachmentsPage().attachment());
        int num = st.pages().touch().composeIframe().diskAttachmentsPage().attachments().size() - 1;
        int j;
        for (j = 1; j <= num; j++) {
            st.user().defaultSteps()
                .clicksOn(st.pages().touch().composeIframe().diskAttachmentsPage().attachments().get(j));
        }
        st.user().defaultSteps().clicksOn(st.pages().touch().composeIframe().diskAttachmentsPage().attachBtn());
        assertThat(
            "Не все аттачи загрузились",
            st.pages().touch().composeIframe().attachments().uploadedAttachment(),
            withWaitFor(hasSize((num - 1) * i), XIVA_TIMEOUT)
        );
    }
}
