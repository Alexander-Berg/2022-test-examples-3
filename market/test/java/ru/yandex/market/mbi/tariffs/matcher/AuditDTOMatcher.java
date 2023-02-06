package ru.yandex.market.mbi.tariffs.matcher;

import java.time.OffsetDateTime;

import javax.annotation.ParametersAreNonnullByDefault;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

import ru.yandex.market.mbi.tariffs.model.AuditDTO;
import ru.yandex.market.mbi.util.MbiMatchers;

/**
 * Матчер для {@link ru.yandex.market.mbi.tariffs.model.AuditDTO}
 */
@ParametersAreNonnullByDefault
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
