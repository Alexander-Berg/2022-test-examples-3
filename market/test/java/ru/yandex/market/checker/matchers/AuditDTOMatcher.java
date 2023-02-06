package ru.yandex.market.checker.matchers;

import java.time.OffsetDateTime;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

import ru.yandex.market.checker.api.model.AuditDTO;
import ru.yandex.market.mbi.util.MbiMatchers;

public class AuditDTOMatcher {
    public static Matcher<AuditDTO> hasAllFields(AuditDTO auditDTO) {
        return Matchers.allOf(
                hasLogin(auditDTO.getLogin()),
                hasMessage(auditDTO.getMessage()),
                hasTime(auditDTO.getTime())
        );
    }

    public static Matcher<AuditDTO> hasLogin(String expectedValue) {
        return MbiMatchers.<AuditDTO>newAllOfBuilder()
                .add(AuditDTO::getLogin, expectedValue, "login")
                .build();
    }

    public static Matcher<AuditDTO> hasMessage(String expectedValue) {
        return MbiMatchers.<AuditDTO>newAllOfBuilder()
                .add(AuditDTO::getMessage, expectedValue, "message")
                .build();
    }

    public static Matcher<AuditDTO> hasTime(OffsetDateTime expectedValue) {
        return MbiMatchers.<AuditDTO>newAllOfBuilder()
                .add(AuditDTO::getTime, expectedValue, "time")
                .build();
    }
}
