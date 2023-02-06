package ru.yandex.market.delivery.command;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.Optional;

import io.github.benas.randombeans.api.EnhancedRandom;
import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerStatus;
import ru.yandex.market.shop.FunctionalTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@DbUnitDataSet(before = "LmsCommandTest.before.csv")
@ExtendWith(MockitoExtension.class)
class LmsCommandTest extends FunctionalTest {

    private static final Long LMS_PARTNER_ID = 1002L;
    private static final Long MBI_PARTNER_ID = 103L;
    private static final Long MBI_PARTNER_ID_WITHOUT_LMS_PARTNER = 104L;
    private static final Long MBI_PARTNER_ID_FAILED_FEATURE = 105L;
    private static final Long LMS_PARTNER_ID_FAILED_FEATURE = 1005L;

    private static final Long MBI_DBS_PARTNER_IN_TESTING = 110L;
    private static final Long LMS_DBS_PARTNER_IN_TESTING = 1010L;

    private static final Long MBI_DBS_PARTNER_FAILED = 112L;
    private static final Long LMS_DBS_PARTNER_FAILED = 1012L;

    @Mock
    private Terminal terminal;

    @Autowired
    LmsCommand lmsCommand;

    @Autowired
    LMSClient lmsClient;

    private static CommandInvocation commandInvocation(String... args) {
        return new CommandInvocation("lms-cli", args, Collections.emptyMap());
    }

    @BeforeEach
    void beforeEach() {
        when(terminal.getWriter()).thenReturn(Mockito.mock(PrintWriter.class));
    }

    @Test
    @DisplayName("Тест на получение логистического партнера по его id.")
    void testGetPartner() {
        when(lmsClient.getPartner(LMS_PARTNER_ID)).thenReturn(
                Optional.of(EnhancedRandom.random(PartnerResponse.PartnerResponseBuilder.class).build()));
        CommandInvocation commandInvocation = commandInvocation("get-partner", LMS_PARTNER_ID.toString());
        lmsCommand.executeCommand(commandInvocation, terminal);
        Mockito.verify(lmsClient, times(1)).getPartner(LMS_PARTNER_ID);
    }

    @Test
    @DisplayName("Тест на получение логистического партнера по id mbi партнера.")
    void testGetPartnerByMbiPartner() {
        when(lmsClient.getPartner(LMS_PARTNER_ID)).thenReturn(
                Optional.of(EnhancedRandom.random(PartnerResponse.PartnerResponseBuilder.class).build()));
        CommandInvocation commandInvocation =
                commandInvocation("get-partner-by-mbi-partner", MBI_PARTNER_ID.toString());
        lmsCommand.executeCommand(commandInvocation, terminal);
        Mockito.verify(lmsClient, times(1)).getPartner(LMS_PARTNER_ID);
    }

    @Test
    @DisplayName("Тест на возникновение ошибки при запросе партнера без связки со складом.")
    void testGetPartnerByMbiPartnerWithoutLmsPartner() {
        CommandInvocation commandInvocation =
                commandInvocation("get-partner-by-mbi-partner", MBI_PARTNER_ID_WITHOUT_LMS_PARTNER.toString());
        lmsCommand.executeCommand(commandInvocation, terminal);
        Mockito.verify(lmsClient, never()).getPartner(any());
    }

    @Test
    @DisplayName("Тест на обновление статуса логистического партнера в active по id партнера mbi.")
    void testUpdatePartnerStatusToActive() {
        PartnerResponse getPartnerResp = EnhancedRandom.random(PartnerResponse.PartnerResponseBuilder.class)
                .id(LMS_PARTNER_ID)
                .status(PartnerStatus.INACTIVE)
                .build();
        when(lmsClient.getPartner(LMS_PARTNER_ID)).thenReturn(Optional.of(getPartnerResp));
        when(lmsClient.changePartnerStatus(LMS_PARTNER_ID, PartnerStatus.ACTIVE)).thenReturn(getPartnerResp);

        CommandInvocation commandInvocation = commandInvocation("update-dropship-partner-status", MBI_PARTNER_ID.toString());
        lmsCommand.executeCommand(commandInvocation, terminal);

        Mockito.verify(lmsClient, times(1)).getPartner(LMS_PARTNER_ID);
        Mockito.verify(lmsClient).changePartnerStatus(
                ArgumentMatchers.argThat(parnterId -> parnterId.equals(LMS_PARTNER_ID)),
                ArgumentMatchers.argThat(status -> status == PartnerStatus.ACTIVE));
    }

    @Test
    @DisplayName("Тест на обновление статуса логистического партнера в inactive по id партнера mbi.")
    void testUpdatePartnerStatusToInactive() {
        PartnerResponse getPartnerResp = EnhancedRandom.random(PartnerResponse.PartnerResponseBuilder.class)
                .id(LMS_PARTNER_ID_FAILED_FEATURE)
                .build();
        when(lmsClient.getPartner(LMS_PARTNER_ID_FAILED_FEATURE)).thenReturn(Optional.of(getPartnerResp));
        when(lmsClient.changePartnerStatus(LMS_PARTNER_ID_FAILED_FEATURE, PartnerStatus.INACTIVE)).thenReturn(getPartnerResp);

        CommandInvocation commandInvocation =
                commandInvocation("update-dropship-partner-status", MBI_PARTNER_ID_FAILED_FEATURE.toString());
        lmsCommand.executeCommand(commandInvocation, terminal);

        Mockito.verify(lmsClient, times(1)).getPartner(LMS_PARTNER_ID_FAILED_FEATURE);
        Mockito.verify(lmsClient).changePartnerStatus(
                ArgumentMatchers.argThat(parnterId -> parnterId.equals(LMS_PARTNER_ID_FAILED_FEATURE)),
                ArgumentMatchers.argThat(status -> status == PartnerStatus.INACTIVE));
    }

    @Test
    @DisplayName("Тест на обновление статуса выключеного логистического DBS партнера в testing, т.к. фид в ПШ")
    void testUpdateDbsPartnerStatusIsInPlaneshift() {
        PartnerResponse getPartnerResp = EnhancedRandom.random(PartnerResponse.PartnerResponseBuilder.class)
                .id(LMS_DBS_PARTNER_IN_TESTING)
                .build();
        Mockito.when(lmsClient.getPartner(LMS_DBS_PARTNER_IN_TESTING)).thenReturn(Optional.of(getPartnerResp));
        Mockito.when(lmsClient.changePartnerStatus(LMS_DBS_PARTNER_IN_TESTING, PartnerStatus.TESTING))
                .thenReturn(getPartnerResp);

        CommandInvocation commandInvocation =
                commandInvocation("update-dbs-partner-status", MBI_DBS_PARTNER_IN_TESTING.toString());
        lmsCommand.executeCommand(commandInvocation, terminal);

        Mockito.verify(lmsClient, times(1)).getPartner(LMS_DBS_PARTNER_IN_TESTING);
        Mockito.verify(lmsClient).changePartnerStatus(
                ArgumentMatchers.argThat(parnterId -> parnterId.equals(LMS_DBS_PARTNER_IN_TESTING)),
                ArgumentMatchers.argThat(status -> status == PartnerStatus.TESTING));
    }

    @Test
    @DisplayName("Тест на обновление статуса выключеного логистического DBS партнера в inactive")
    void testUpdateDbsPartnerStatusInactive() {
        PartnerResponse getPartnerResp = EnhancedRandom.random(PartnerResponse.PartnerResponseBuilder.class)
                .id(LMS_DBS_PARTNER_FAILED)
                .build();
        Mockito.when(lmsClient.getPartner(LMS_DBS_PARTNER_FAILED)).thenReturn(Optional.of(getPartnerResp));
        Mockito.when(lmsClient.changePartnerStatus(LMS_DBS_PARTNER_FAILED, PartnerStatus.INACTIVE))
                .thenReturn(getPartnerResp);

        CommandInvocation commandInvocation =
                commandInvocation("update-dropship-partner-status", MBI_DBS_PARTNER_FAILED.toString());
        lmsCommand.executeCommand(commandInvocation, terminal);

        Mockito.verify(lmsClient, times(1)).getPartner(LMS_DBS_PARTNER_FAILED);
        Mockito.verify(lmsClient).changePartnerStatus(
                ArgumentMatchers.argThat(parnterId -> parnterId.equals(LMS_DBS_PARTNER_FAILED)),
                ArgumentMatchers.argThat(status -> status == PartnerStatus.INACTIVE));
    }
}
