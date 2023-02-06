package ru.yandex.autotests.innerpochta.tests;

import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.yandex.xplat.common.YSDate;
import com.yandex.xplat.testopithecus.AttachmentSpec;
import com.yandex.xplat.testopithecus.MessageSpecBuilder;
import com.yandex.xplat.testopithecus.UserSpec;
import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.steps.ImapSteps;
import ru.yandex.autotests.innerpochta.steps.beans.message.Message;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static ru.yandex.autotests.innerpochta.rules.resources.RemoveAllMessagesRule.removeAllMessages;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.INBOX;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.SENT;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRASH;

@Aqua.Test
@Title("Генератор презентабельного ящика MAILPRODUCT-608")
@Features(FeaturesConst.OTHER)
@Tag(FeaturesConst.OTHER)
@Stories(FeaturesConst.OTHER)
@RunWith(DataProviderRunner.class)
public class BeautifulMailboxGenerator extends BaseTest {
    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();
    private final String ATTACH_BASE64 = "ZXhhbXBsZQ==";
    private final String IMPORTANT_SUBJECT = "Проверь табличку";
    private final UserSpec MASLOVA = new UserSpec("yandex-team-mailt-315@yandex.ru", "Маслова Виктория");
    private final UserSpec POLIVANOV = new UserSpec("yandex-team-mailt-316@yandex.ru", "Поливанов Кирилл");
    private final UserSpec RIBAKOVA = new UserSpec("yandex-team-mailt-317@yandex.ru", "Рыбакова Майя");
    private final UserSpec FEDOTOVA = new UserSpec("yandex-team-mailt-318@yandex.ru", "Федотова Кристина");
    private final UserSpec GRACHEV = new UserSpec("yandex-team-mailt-319@yandex.ru", "Грачев Дмитрий");
    private final UserSpec SIMAKIN = new UserSpec("yandex-team-mailt-320@yandex.ru", "Симакин Дмитрий");
    private final UserSpec OGURTSOV = new UserSpec("yandex-team-mailt-321@yandex.ru", "Огурцов Иван");
    private final UserSpec MIHAILOVA = new UserSpec("yandex-team-mailt-322@yandex.ru", "Михайлова Олеся");
    private final UserSpec ME = new UserSpec("yandex-team-mailt-324@yandex.ru", "Игорь Пахомов");
    private AccLockRule lock = AccLockRule.use().className();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);
    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(removeAllMessages(() -> user, INBOX, SENT, TRASH));

    @Before
    public void logIn() {
    }

    @Test
    @Title("Генерируем красивый ящик")
    public void generateBeautifulMailbox() {
        DateTimeFormatter dateFormat = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        LocalDateTime date = LocalDateTime.now();
        ImapSteps imapConnection = user.imapSteps().connectByImap();
        for (int i = 0; i < 5; i++)
            imapConnection.addMessage(
                new MessageSpecBuilder().withDefaults()
                    .withSubject("Подписанный акт")
                    .withSender(MASLOVA)
                    .withTextBody("Давайте согласуем последнюю версию акта, заказчики ждут")
                    .withTimestamp(new YSDate(dateFormat.format(date.minusMinutes(i + 1)) + "Z"))
                    .addReceiver(ME)
                    .build()
            );
        imapConnection.addMessage(
            new MessageSpecBuilder().withDefaults()
                .withSubject("Адженда встречи с аналитиками")
                .withSender(POLIVANOV)
                .withTextBody(
                    "Итак, по результатам встречи с отделом аналитики решили: 1) Проводить кампанию в этом году в" +
                        " Цветочках")
                .withTimestamp(new YSDate(dateFormat.format(date.minusHours(1)) + "Z"))
                .addReceiver(ME)
                .build()
        );
        imapConnection.addMessage(
            new MessageSpecBuilder().withDefaults()
                .withSubject("Договор на согласование")
                .withSender(RIBAKOVA)
                .withTextBody("Добрый день! Посмотрите, пожалуйста, договор. Жду правок или согласования")
                .withTimestamp(new YSDate(dateFormat.format(date.minusHours(2)) + "Z"))
                .addAttachments(Collections.singletonList(new AttachmentSpec(
                        "Договор№178-123.pdf",
                        "application/octet-stream",
                        ATTACH_BASE64
                    ))
                )
                .addReceiver(ME)
                .build()
        );
        for (int i = 0; i < 3; i++)
            imapConnection.addMessage(
                new MessageSpecBuilder().withDefaults()
                    .withSubject("Статус проекта")
                    .withSender(FEDOTOVA)
                    .withTextBody(
                        "Обсудили и согласовали промежуточные результаты и статус проекта по разработке новых видов " +
                            "котиков и пёсиков")
                    .withTimestamp(new YSDate(dateFormat.format(date.minusHours(3).minusMinutes(i)) + "Z"))
                    .addReceiver(ME)
                    .build()
            );
        for (int i = 0; i < 4; i++)
            imapConnection.addMessage(
                new MessageSpecBuilder().withDefaults()
                    .withSubject("Согласование бюджета")
                    .withSender(GRACHEV)
                    .withTextBody(
                        "Коллеги, в третьем столбце страницы с расходами мы не учли затраты на разрешение " +
                            "строительства норки для лисы")
                    .withTimestamp(new YSDate(dateFormat.format(date.minusHours(4).minusMinutes(i)) + "Z"))
                    .addReceiver(ME)
                    .build()
            );
        imapConnection.addMessage(
            new MessageSpecBuilder().withDefaults()
                .withSubject("Планирование")
                .withSender(RIBAKOVA)
                .withTextBody(
                    "Планируем неделю: Команда дизайна продолжает заниматься новой рекламной кампанией, маркетинг " +
                        "решает вопрос с подарками для наших инвесторов")
                .withTimestamp(new YSDate(dateFormat.format(date.minusHours(5)) + "Z"))
                .addReceiver(ME)
                .build()
        );
        for (int i = 0; i < 11; i++)
            imapConnection.addMessage(
                new MessageSpecBuilder().withDefaults()
                    .withSubject("Планы на квартал")
                    .withSender(SIMAKIN)
                    .withTextBody("Вот скорректированные планы, давайте обсуждать")
                    .withTimestamp(new YSDate(dateFormat.format(date.minusHours(6).minusMinutes(i)) + "Z"))
                    .addReceiver(ME)
                    .build()
            );
        imapConnection.addMessage(
            new MessageSpecBuilder().withDefaults()
                .withSubject("Обсудить стратегию")
                .withSender(OGURTSOV)
                .withTextBody(
                    "Привет! Хочу обсудить с тобой маркетинговую стратегию на квартал, когда будешь готов, сообщи " +
                        "пожалуйста мне название своего любимого коктейля")
                .withTimestamp(new YSDate(dateFormat.format(date.minusHours(7)) + "Z"))
                .addReceiver(ME)
                .build()
        );
        imapConnection.addMessage(
            new MessageSpecBuilder().withDefaults()
                .withSubject(IMPORTANT_SUBJECT)
                .withSender(GRACHEV)
                .withTextBody(
                    "Привет! Проверь пожалуйста всё ли корректно, готов отправлять заказчику")
                .withTimestamp(new YSDate(dateFormat.format(date.minusHours(8)) + "Z"))
                .addAttachments(Arrays.asList(new AttachmentSpec(
                    "table_v3.xlsx",
                    "application/octet-stream",
                    ATTACH_BASE64
                )))
                .addReceiver(ME)
                .build()
        );
        imapConnection.addMessage(
            new MessageSpecBuilder().withDefaults()
                .withSubject("Собеседование кандидата")
                .withSender(MIHAILOVA)
                .withTextBody(
                    "Завтра придет Кирилл, о котором я тебе рассказывала. Присоединишься к нам в 11.00 в переговорке " +
                        "\"Лисий нос\"?")
                .withTimestamp(new YSDate(dateFormat.format(date.minusHours(9)) + "Z"))
                .addReceiver(ME)
                .build()
        );
        imapConnection.addMessage(
            new MessageSpecBuilder().withDefaults()
                .withSubject("Встреча с маркетингом")
                .withSender(POLIVANOV)
                .withTextBody(
                    "Напоминаю, что сегодня состоится встреча с маркетингом, на которой будем обсуждать новую шубу " +
                        "начальника, всем быть.")
                .withTimestamp(new YSDate(dateFormat.format(date.minusHours(10)) + "Z"))
                .addReceiver(ME)
                .build()
        );
        imapConnection.closeConnection();

        user.apiMessagesSteps().markAllMsgRead();
        List<Message> allMessages = user.apiMessagesSteps().getAllMessages();
        allMessages.stream()
            .filter(msg -> Long.parseLong(msg.getDate().getTimestamp()) > date.minusHours(2).toEpochSecond(
                ZoneOffset.ofHours(3)) * 1000
            ).forEach(msg -> user.apiMessagesSteps().markLetterUnRead(msg));
        allMessages.stream()
            .filter(msg -> msg.getSubject().equals(IMPORTANT_SUBJECT))
            .forEach(msg -> user.apiLabelsSteps().markImportant(msg));
    }
}
