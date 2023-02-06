package ru.yandex.market.hrms.core.service.outstaff.telegram;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.core.AbstractCoreTest;
import ru.yandex.market.hrms.core.service.outstaff.bot.OutstaffTelegramBot;
import ru.yandex.market.hrms.core.service.outstaff.bot.OutstaffTelegramReportService;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class OutstaffTelegramReportServiceTest extends AbstractCoreTest {

    private static final String TELEGRAM_MESSAGE = """
                                        <b>#Склад_Софьино 2022-03-03</b>

                                        <b> last_name1 first_name1 mid_name1</b>
                                          Первый вход в оперзону: ,
                                          Последний выход: ,
                                          Первая операция 2022-03-03 00:00,
                                          Последняя операция 2022-03-04 00:00

                                              """;

    @Autowired
    private OutstaffTelegramReportService outstaffTelegramReportService;

    @MockBean
    private OutstaffTelegramBot outstaffTelegramBot;

    @Test
    @DbUnitDataSet(before = "OutstaffTelegramReportServiceTest.before.csv")
    @DbUnitDataSet(before = "Stats.before.csv")
    public void shouldCreateCorrectReport() throws TelegramApiException {
        mockClock(LocalDate.of(2022, 3, 3));
        outstaffTelegramReportService.sendReports();
        verify(outstaffTelegramBot, times(1))
                .sendMessage("249654183", TELEGRAM_MESSAGE);
    }
}
