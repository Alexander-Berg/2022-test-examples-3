package ru.yandex.autotests.innerpochta.tests.screentests.Avatars;

import com.google.common.collect.Sets;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.yandex.xplat.testopithecus.MessageSpecBuilder;
import com.yandex.xplat.testopithecus.UserSpec;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.openqa.selenium.By;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.autotests.passport.api.core.rules.LogTestStartRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.Set;
import java.util.function.Consumer;

import static com.google.common.collect.ImmutableMap.of;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.openqa.selenium.By.cssSelector;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.FOLDER_ID;
import static ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager.touchScreenRulesManager;
import static ru.yandex.autotests.innerpochta.touch.data.FidsAndLids.TRASH_FOLDER;
import static ru.yandex.autotests.innerpochta.util.MailConst.DEV_NULL_EMAIL;
import static ru.yandex.autotests.innerpochta.util.MailConst.DEV_NULL_EMAIL_2;
import static ru.yandex.autotests.innerpochta.util.MailConst.USER_WITH_AVATAR_EMAIL;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.Utils.withWaitFor;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.INBOX;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TEMPLATE;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRASH;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_USER_NAME;

/**
 * @author oleshko
 */
@Aqua.Test
@Title("Тесты на аватарки в списке писем")
@Features(FeaturesConst.AVATARS)
@Stories(FeaturesConst.GENERAL)
@RunWith(DataProviderRunner.class)
public class AvatarInMessageListScreenTest {

    private static final Set<By> IGNORE_THIS = Sets.newHashSet(
        cssSelector(".ns-view-right-column-box")
    );

    private TouchScreenRulesManager rules = touchScreenRulesManager().withLock(AccLockRule.use().useTusAccount());
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule acc = rules.getLock();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static LogTestStartRule start = new LogTestStartRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createTouchRuleChain();

    @Before
    public void prepare() {
        String subj = getRandomString();
        stepsProd.user().apiSettingsSteps()
            .callWithListAndParams("Меняем имя юзера", of(SETTINGS_USER_NAME, "Винни Пух"));
        stepsProd.user().apiMessagesSteps()
            .addCcEmails(DEV_NULL_EMAIL).addBccEmails(DEV_NULL_EMAIL_2)
            .sendMailWithCcAndBcc(acc.firstAcc().getSelfEmail(), getRandomName(), "");
        stepsProd.user().apiSettingsSteps()
            .callWithListAndParams("Меняем имя юзера", of(SETTINGS_USER_NAME, ""));
        stepsProd.user().apiMessagesSteps().sendThread(acc.firstAcc(), getRandomName(), 2);
        stepsProd.user().apiMessagesSteps().sendMail(DEV_NULL_EMAIL, "", "");
        stepsProd.user().apiMessagesSteps().sendMail(USER_WITH_AVATAR_EMAIL, subj, "");
        assertThat(
            "Не все письма доставлены",
            stepsProd.user().apiMessagesSteps().getAllMessages(),
            withWaitFor(hasSize(3))
        );
        stepsProd.user().apiMessagesSteps().markAllMsgRead();
    }

    String[] emails = {
        "robot-pinkie-pie@yandex-team.ru",
        "reebok@ru-news.reebok.com",
        "noreply@em.kuponator.ru",
        "news@realty.yandex.ru",
        "noreply@rambler-co.ru",
        "online@booking-lufthansa.com",
    };

    @Test
    @Title("Аватарки в списке писем")
    @Description("Проверяем аватарки у одиночного письма в Черновиках, Шаблонах, Отправленных и Исходящих," +
        "треда во Входящих, аватарка адресата без имени и с именем")
    @TestCaseId("102")
    @DataProvider({"1", "4", "5", "6"})
    public void shouldSeeAvatarInMsgList(String num) {
        Consumer<InitStepsRule> act = st ->
            st.user().defaultSteps().shouldSee(st.pages().touch().messageList().messageBlock().avatar());

        stepsProd.user().apiMessagesSteps().prepareDraft("", "", "");
        stepsProd.user().apiMessagesSteps().prepareDraft(USER_WITH_AVATAR_EMAIL, "", "");
        stepsProd.user().apiMessagesSteps().sendMailWithSentTime(acc.firstAcc(), getRandomName(), "");
        parallelRun.withActions(act).withAcc(acc.firstAcc()).withUrlPath(FOLDER_ID.makeTouchUrlPart(num))
            .withAdditionalIgnoredElements(IGNORE_THIS).run();
    }

    @Test
    @Title("Аватарки в списке писем")
    @Description("Проверяем аватарки у письма в Шаблонах")
    @TestCaseId("102")
    public void shouldSeeAvatarInMsgList() {
        Consumer<InitStepsRule> act = st ->
            st.user().defaultSteps().shouldSee(st.pages().touch().messageList().messageBlock().avatar());

        stepsProd.user().apiMessagesSteps().createTemplateMessage(acc.firstAcc());
        stepsProd.user().apiMessagesSteps().createTemplateMessage(USER_WITH_AVATAR_EMAIL, "", "");
        String fid = stepsProd.user().apiFoldersSteps().getFolderBySymbol(TEMPLATE).getFid();
        parallelRun.withActions(act).withAcc(acc.firstAcc()).withUrlPath(FOLDER_ID.makeTouchUrlPart(fid))
            .withAdditionalIgnoredElements(IGNORE_THIS).run();
    }

    @Test
    @Title("Аватарки сервисов в списке писем")
    @Description("Проверяем аватарки у яндексоидов во внешнем мире аватарки и монограммы различных сервисов")
    @TestCaseId("115")
    public void shouldSeeServiceAvatarsInMsgList() {
        Consumer<InitStepsRule> act = st ->
            st.user().defaultSteps().shouldSee(st.pages().touch().messageList().messageBlock().avatar());

        stepsProd.user().apiMessagesSteps().deleteAllMessagesInFolder(
            stepsProd.user().apiFoldersSteps().getFolderBySymbol(INBOX)
        );
        for (String email : emails)
            stepsTest.user().imapSteps().connectByImap()
                .addMessage(
                    new MessageSpecBuilder().withDefaults()
                        .withSender(new UserSpec(email, email))
                        .withSubject(getRandomString())
                        .build()
                )
                .closeConnection();
        parallelRun.withActions(act).withAcc(acc.firstAcc()).withAdditionalIgnoredElements(IGNORE_THIS).run();
    }

    @Test
    @Title("Аватарка в отлупе о недоставке")
    @TestCaseId("686")
    public void shouldDaemonMail() {
        Consumer<InitStepsRule> act = st ->
            st.user().defaultSteps()
                .shouldSee(st.pages().touch().messageList().messageBlock().attachmentsInMessageList());

        stepsProd.user().apiMessagesSteps().sendMailWithNoSaveWithoutCheck(
            getRandomString() + "@ya.ru",
            getRandomName(),
            ""
        );
        parallelRun.withActions(act).withAcc(acc.firstAcc()).run();
    }

    @Test
    @Title("Аватарка в удалённых")
    @TestCaseId("113")
    public void shouldSeeAvatarsInTrash() {
        Consumer<InitStepsRule> act = st ->
            st.user().defaultSteps().shouldSee(st.pages().touch().messageList().messageBlock());

        stepsProd.user().apiMessagesSteps().moveAllMessagesFromFolderToFolder(INBOX, TRASH);
        parallelRun.withActions(act).withAcc(acc.firstAcc()).withUrlPath(FOLDER_ID.makeTouchUrlPart(TRASH_FOLDER)).run();
    }
}
