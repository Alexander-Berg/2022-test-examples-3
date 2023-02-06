package ru.yandex.autotests.innerpochta.steps;

import com.yandex.xplat.common.YSDate;
import com.yandex.xplat.testopithecus.DefaultFolderName;
import com.yandex.xplat.testopithecus.MailAccountSpec;
import com.yandex.xplat.testopithecus.MessageSpec;
import com.yandex.xplat.testopithecus.MessageSpecBuilder;
import kotlin.Unit;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.testopithecus.pal.DefaultImap;
import ru.yandex.qatools.allure.annotations.Step;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.FALSE;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRUE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_ENABLE_IMAP;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_ENABLE_IMAP_AUTH_PLAIN;

public class ImapSteps {

    public RestAssuredAuthRule auth;
    private DefaultImap imap;
    private AllureStepStorage user;

    ImapSteps(AllureStepStorage user) {
        this.user = user;
    }

    public ImapSteps withAuth(RestAssuredAuthRule auth) {
        this.auth = auth;
        return this;
    }

    public ImapSteps connectByImap() {
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем IMAP",
            of(
                SETTINGS_PARAM_ENABLE_IMAP, TRUE,
                SETTINGS_PARAM_ENABLE_IMAP_AUTH_PLAIN, TRUE
            )
        );

        imap = new DefaultImap(new MailAccountSpec(
            auth.getLogin(),
            auth.getPassword(),
            "imap.yandex.ru"
        ));
        imap.connect(error -> {
            return Unit.INSTANCE;
        });
        return this;
    }

    @Step("Добавялем письмо через IMAP")
    public ImapSteps addMessage(MessageSpec message) {
        imap.appendMessage(
            DefaultFolderName.getInbox(),
            message,
            error -> {
                return Unit.INSTANCE;
            }
        );
        return this;
    }

    @Step("Добавялем письмо из eml через IMAP")
    public ImapSteps addMessage(String fileName) {
        imap.appendMessage(
            DefaultFolderName.getInbox(),
            user.defaultSteps().getAttachPath(fileName),
            error -> {
                return Unit.INSTANCE;
            }
        );
        return this;
    }

    public void closeConnection() {
        imap.disconnect(error -> {
            return Unit.INSTANCE;
        });
        user.apiSettingsSteps().callWithListAndParams(
            "Выключаем IMAP",
            of(
                SETTINGS_PARAM_ENABLE_IMAP, FALSE,
                SETTINGS_PARAM_ENABLE_IMAP_AUTH_PLAIN, FALSE
            )
        );
    }

    @Step("Кладём в ящик пиьсмо за нужную дату")
    public ImapSteps addMessage(int month, int year, int day) {
        DateTimeFormatter dateFormat = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        LocalDateTime date = LocalDateTime.now();
        connectByImap()
            .addMessage(
                new MessageSpecBuilder().withDefaults()
                    .withTimestamp(new YSDate(dateFormat.format(
                        date.withMonth(month).withYear(year).withDayOfMonth(day)
                    ) + "Z"))
                    .withSubject(getRandomString())
                    .build()
            )
            .closeConnection();
        return this;
    }
}
