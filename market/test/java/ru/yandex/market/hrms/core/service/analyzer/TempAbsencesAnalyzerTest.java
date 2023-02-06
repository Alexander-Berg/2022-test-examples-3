package ru.yandex.market.hrms.core.service.analyzer;

import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.core.AbstractCoreTest;
import ru.yandex.market.hrms.core.domain.domain.repo.Domain;
import ru.yandex.market.hrms.core.service.outstaff.bot.OutstaffTelegramBot;
import ru.yandex.market.hrms.model.domain.DomainType;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DbUnitDataSet(before = "TempAbsencesAnalyzerTest.before.csv")
public class TempAbsencesAnalyzerTest extends AbstractCoreTest {

    private static final String TELEGRAM_BAD_MESSAGE = "Внимание. на %s возможен сбой при передаче данных биометрии в" +
            " Гермес, просьба не создавать тикеты на корректировку табеля в течении пары часов.";
    private static final String TELEGRAM_OK_MESSAGE = "Внимание. на Софьино сбой при передаче данных исправлен";

    @Autowired
    TempAbsencesAnalyzer tempAbsencesAnalyzer;

    @MockBean
    OutstaffTelegramBot tempAbsenceAnalyzerTelegramBot;

    @Test
    @DbUnitDataSet(before = "TempAbsenceAnalyzerTestBadStatus.before.csv")
    void analyzeBadStatusTest() throws TelegramApiException {
        LocalDateTime time = LocalDateTime.of(2022, 3, 31, 17, 0, 0);
        mockClock(time);
        tempAbsencesAnalyzer.analyzeAbsence(Domain.builder()
                .id(1L)
                .type(DomainType.FFC)
                .name("Софьино")
                .build(), time.atZone(ZoneId.of("Europe/Moscow")).toInstant());
        verify(tempAbsenceAnalyzerTelegramBot, times(1))
                .sendMessage("1232", TELEGRAM_BAD_MESSAGE.formatted("Софьино"));
    }

    @Test
    @DbUnitDataSet(before = "TempAbsenceAnalyzerTestOkStatus.before.csv")
    void analyzeOkStatusTest() throws TelegramApiException {
        LocalDateTime time = LocalDateTime.of(2022, 3, 31, 17, 0, 0);
        mockClock(time);
        tempAbsencesAnalyzer.analyzeAbsence(Domain.builder()
                .id(1L)
                .type(DomainType.FFC)
                .name("Софьино")
                .build(), time.atZone(ZoneId.of("Europe/Moscow")).toInstant());
        verify(tempAbsenceAnalyzerTelegramBot, times(1))
                .sendMessage("1232", TELEGRAM_OK_MESSAGE);
    }

    @Test
    @DbUnitDataSet(before = "TempAbsenceAnalyzerTestBadStatusSc.before.csv")
    void analyzeBadStatusTestSc() throws TelegramApiException {
        LocalDateTime time = LocalDateTime.of(2022, 3, 31, 20, 0, 0);
        mockClock(time);
        tempAbsencesAnalyzer.analyzeAbsence(Domain.builder()
                .id(49L)
                .type(DomainType.SC)
                .name("СЦ Новосибирск")
                .build(), time.atZone(ZoneId.of("Asia/Novosibirsk")).toInstant());
        verify(tempAbsenceAnalyzerTelegramBot, times(1))
                .sendMessage("1231", TELEGRAM_BAD_MESSAGE.formatted("СЦ Новосибирск"));
    }
}
