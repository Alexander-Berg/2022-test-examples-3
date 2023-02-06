package ru.yandex.calendar.logic.sending.so;

import java.util.List;
import java.util.Optional;

import edu.emory.mathcs.backport.java.util.Collections;
import lombok.val;
import net.javacrumbs.jsonunit.assertj.JsonAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.calendar.boot.MicroCoreContextConfiguration;
import ru.yandex.calendar.logic.domain.PassportAuthDomains;
import ru.yandex.calendar.logic.domain.PassportAuthDomainsHolder;
import ru.yandex.calendar.logic.event.ActionSource;
import ru.yandex.calendar.micro.MicroCoreContext;
import ru.yandex.calendar.micro.so.Form;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.mail.micronaut.common.JsonMapper;
import ru.yandex.misc.env.EnvironmentType;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {SoCheckerClientConfiguration.class, MicroCoreContextConfiguration.class})
public class SoCheckerClientSmallTest {
    @Autowired
    private EnvironmentType environmentType;
    @Autowired
    private MicroCoreContext microCoreContext;

    private static final Form.FormFields EMPTY_FORM_FIELDS = new Form.FormFields(
            "",
            "",
            "",
            "",
            "",
            "",
            Collections.emptyList());

    @Test
    public void checkFormSerialization() {
        val location = "room1";
        val description = "event description";
        val summary = "event name";
        val id = "external id";
        val timeStart = "startTime";
        val timeEnd = "endTime";
        val participants = List.of("person1@example.com", "person2@example.com", "person3@example.com");

        val formFields = new Form.FormFields(location, description, summary, id, timeStart, timeEnd, participants);

        val formType = ActionSource.WEB;
        val action = "testAction";
        val subject = "Subject";
        val uid = 12345L;
        val realPath = "RealPathUrl";
        val form = SoChecker.constructForm(
                environmentType,
                formType,
                action,
                Optional.of(subject),
                new PassportUid(uid),
                Optional.of(realPath),
                formFields);

        val realJson =  microCoreContext.findBean(JsonMapper.class).toJson(form);

        assertThatJson(realJson).and(
                a -> a.node("environment").isEqualTo(environmentType.name()),
                a -> a.node("form_type").isEqualTo(formType.name()),
                a -> a.node("action").isEqualTo(action),
                a -> a.node("client_ip").isEqualTo(""),
                a -> a.node("subject").isEqualTo(subject),
                a -> a.node("form_author").isEqualTo(uid),
                a -> a.node("form_realpath").isEqualTo(realPath),
                a -> a.node("form_fields").and(
                        b -> hasValue(b.node("location"), location),
                        b -> hasValue(b.node("description"), description),
                        b -> hasValue(b.node("id"), id, "String", "automatic"),
                        b -> hasValue(b.node("summary"), summary),
                        b -> hasValue(b.node("time_start"), timeStart),
                        b -> hasValue(b.node("time_end"), timeEnd),
                        b -> hasListValue(b.node("participants"), participants)
                )
        );
    }

    @Test
    public void checkEmptyFormSerialization() {
        val formType = ActionSource.WEB;
        val action = "testAction";
        val subject = "Subject";
        val uid = 12345L;
        val realPath = "RealPathUrl";
        val form = SoChecker.constructForm(
                environmentType,
                formType,
                action,
                Optional.of(subject),
                new PassportUid(uid),
                Optional.of(realPath),
                EMPTY_FORM_FIELDS);

        val realJson =  microCoreContext.findBean(JsonMapper.class).toJson(form);

        assertThatJson(realJson).and(
                a -> a.node("environment").isEqualTo(environmentType.name()),
                a -> a.node("form_type").isEqualTo(formType.name()),
                a -> a.node("action").isEqualTo(action),
                a -> a.node("client_ip").isEqualTo(""),
                a -> a.node("subject").isEqualTo(subject),
                a -> a.node("form_author").isEqualTo(uid),
                a -> a.node("form_realpath").isEqualTo(realPath),
                a -> a.node("form_fields").and(
                        b -> hasValue(b.node("location"), ""),
                        b -> hasValue(b.node("description"), ""),
                        b -> hasValue(b.node("id"), "", "String", "automatic"),
                        b -> hasValue(b.node("summary"), ""),
                        b -> hasValue(b.node("time_start"), ""),
                        b -> hasValue(b.node("time_end"), ""),
                        b -> hasListValue(b.node("participants"), Collections.emptyList())
                )
        );
    }

    private void hasListValue(JsonAssert listField, List<String> listValue) {
        hasValue(listField, listValue, "List");
    }

    private void hasValue(JsonAssert field, String value) {
        hasValue(field, value, "String");
    }

    private <T> void hasValue(JsonAssert field, T value, String type) {
        hasValue(field, value, type,  "user");
    }

    private <T> void hasValue(JsonAssert field, T value, String type, String filledBy) {
        field.and(
                a -> a.node("type").isEqualTo(type),
                a -> a.node("value").isEqualTo(value),
                a -> a.node("filled_by").isEqualTo(filledBy)
        );
    }
}

@Configuration
class SoCheckerClientConfiguration {
    @Bean
    public EnvironmentType environmentType() {
        return EnvironmentType.TESTS;
    }

    @Bean
    public PassportAuthDomainsHolder passportAuthDomainsHolder() {
        val passportAuthDomainsHolder = mock(PassportAuthDomainsHolder.class);
        when(passportAuthDomainsHolder.getDomains()).thenReturn(PassportAuthDomains.BOTH);
        return passportAuthDomainsHolder;
    }
}
