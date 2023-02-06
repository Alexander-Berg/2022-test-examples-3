package ru.yandex.calendar.logic.sending.so;

import java.util.Collections;
import java.util.Optional;

import lombok.SneakyThrows;
import lombok.val;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.calendar.frontend.webNew.WebNewTestBase;
import ru.yandex.calendar.logic.event.ActionSource;
import ru.yandex.calendar.micro.so.Form;
import ru.yandex.calendar.micro.MicroCoreContext;
import ru.yandex.calendar.micro.so.SoCheckClient;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.env.EnvironmentType;

import static org.assertj.core.api.Assertions.assertThat;

public class SoCheckerClientTest extends WebNewTestBase {
    private static final String SPAM_EVENT_NAME = "XJS*C4JDBQADN1.NSBN3*2IDNEN*GTUBE-STANDARD-ANTI-UBE-TEST-EMAIL*C.34X";
    private static final String NOT_SPAM_EVENT_NAME = "Ordinary event";
    @Autowired
    private EnvironmentType environmentType;
    @Autowired
    private MicroCoreContext microCoreContext;

    private Form getForm(String name) {
        val formField = new Form.FormFields("", "", "", "", "", "", Collections.emptyList());
        return SoChecker.constructForm(environmentType, ActionSource.WEB, "test", Optional.of(name), new PassportUid(123), Optional.empty(), formField);
    }

    @Test
    @SneakyThrows
    public void checkSpamWithRequest() {
        val soCheckerClient = microCoreContext.findBean(SoCheckClient.class);
        assertThat(soCheckerClient.checkForm("CALENDAR", "id", "formId", getForm(SPAM_EVENT_NAME)).get()).isTrue();
    }

    @Test
    @SneakyThrows
    public void checkNotSpamWithRequest() {
        val soCheckerClient = microCoreContext.findBean(SoCheckClient.class);
        assertThat(soCheckerClient.checkForm("CALENDAR", "id", "formId", getForm(NOT_SPAM_EVENT_NAME)).get()).isFalse();
    }
}
