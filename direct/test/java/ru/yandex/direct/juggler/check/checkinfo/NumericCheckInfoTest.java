package ru.yandex.direct.juggler.check.checkinfo;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.ansiblejuggler.PlaybookBuilder;
import ru.yandex.direct.ansiblejuggler.model.notifications.NotificationMethod;
import ru.yandex.direct.juggler.JugglerStatus;
import ru.yandex.direct.juggler.check.DirectNumericCheck;
import ru.yandex.direct.juggler.check.annotation.JugglerCheck;
import ru.yandex.direct.juggler.check.annotation.OnChangeNotification;
import ru.yandex.direct.juggler.check.model.CheckTag;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static ru.yandex.direct.juggler.check.model.NotificationRecipient.CHAT_INTERNAL_SYSTEMS_MONITORING;

public class NumericCheckInfoTest {
    private static final String ENV_NAME = "some_env";
    private static final String SERVICE_NAME = "service.name";
    private static final List<String> RAW_SERVICE_NAMES = ImmutableList.<String>builder()
            .add("service.name.taragam")
            .add("pew.service.name.taragam")
            .add("pew.service.pewpew")
            .build();
    private static final String HOST = "ya.ru";

    @JugglerCheck(ttl = @JugglerCheck.Duration(hours = 4),
            tags = {CheckTag.GROUP_INTERNAL_SYSTEMS},
            notifications = {
                    @OnChangeNotification(recipient = CHAT_INTERNAL_SYSTEMS_MONITORING,
                            status = {JugglerStatus.OK, JugglerStatus.CRIT},
                            method = NotificationMethod.TELEGRAM),
            })
    private static class SomeClass {
    }

    @Mock
    private DirectNumericCheck check;

    @Mock
    private PlaybookBuilder playbookBuilder;

    @Mock
    private ru.yandex.direct.ansiblejuggler.model.checks.JugglerCheck jugglerCheck;

    private NumericCheckInfo checkInfo;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);

        doReturn(jugglerCheck)
                .when(playbookBuilder).addNumericMultiServicesCheck(any(), any(), any(), any());

        doReturn(SERVICE_NAME)
                .when(check).getServiceName();
        doReturn(RAW_SERVICE_NAMES)
                .when(check).getRawServiceNames();
        doReturn(HOST)
                .when(check).getHost();

        JugglerCheck annotation = SomeClass.class.getAnnotation(JugglerCheck.class);
        checkInfo = new NumericCheckInfo(annotation, check, ENV_NAME);
    }

    @Test
    public void testNumericCheckFields() {
        SoftAssertions soft = new SoftAssertions();

        soft.assertThat(checkInfo.getServiceName())
                .isEqualTo(SERVICE_NAME + "." + ENV_NAME);
        soft.assertThat(checkInfo.getNameWithEnv("somename", ENV_NAME))
                .isEqualTo("somename." + ENV_NAME);

        soft.assertAll();
    }

    @Test
    public void testPlaybookBuilder() {
        checkInfo.addCheckToPlaybook(playbookBuilder);

        verify(playbookBuilder)
                .addNumericMultiServicesCheck(
                        eq(HOST),
                        eq(RAW_SERVICE_NAMES.stream()
                                .map(n -> n + "." + ENV_NAME)
                                .collect(Collectors.toList())),
                        eq(SERVICE_NAME + "." + ENV_NAME),
                        eq(Duration.ofHours(4)));
        verify(jugglerCheck)
                .withTag(eq(CheckTag.GROUP_INTERNAL_SYSTEMS.getName()));
        verify(jugglerCheck)
                .withNotification(any());
    }
}
