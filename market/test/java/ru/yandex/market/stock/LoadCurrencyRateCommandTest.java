package ru.yandex.market.stock;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.hamcrest.core.StringStartsWith;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.common.util.currency.CurrencyRate;
import ru.yandex.common.util.currency.RateSource;
import ru.yandex.common.util.date.DateUtil;
import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.billing.FunctionalTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Тесты для {@link LoadCurrencyRateCommand}.
 *
 * @author vbudnev
 */
@ExtendWith(MockitoExtension.class)
class LoadCurrencyRateCommandTest extends FunctionalTest {
    private static final Date DATE_2019_03_11 = DateUtil.asDate(LocalDate.of(2019, 3, 11));
    private static final Date DATE_2019_03_12 = DateUtil.asDate(LocalDate.of(2019, 3, 12));
    private static final Date DATE_2019_03_13 = DateUtil.asDate(LocalDate.of(2019, 3, 13));
    private static final LocalDateTime DATE_2019_03_17_124522 = LocalDateTime.of(2019, 3, 17, 12, 45, 22);
    private static final boolean CONFIRM_LOAD = true;
    private static final boolean CONFIRM_PERSIST = true;

    private static final CommandInvocation CMD_CALL_WITH_BAD_CURRENCY = new CommandInvocation(
            "nvm",
            new String[0],
            ImmutableMap.of(
                    LoadCurrencyRateCommand.ARG_CURRENCY, "USDaD",
                    LoadCurrencyRateCommand.ARG_EXCHANGE_DATE, "2019-03-05",
                    LoadCurrencyRateCommand.ARG_STORE_TIMESTAMP, "2019-02-03T12:45:22"
            )
    );

    private static final CommandInvocation CMD_CALL_WITH_BAD_TARGET_DATE = new CommandInvocation(
            "nvm",
            new String[0],
            ImmutableMap.of(
                    LoadCurrencyRateCommand.ARG_CURRENCY, "USD",
                    LoadCurrencyRateCommand.ARG_EXCHANGE_DATE, "2019-0305",
                    LoadCurrencyRateCommand.ARG_STORE_TIMESTAMP, "2019-02-03T12:45:22"
            )
    );

    private static final CommandInvocation CMD_CALL_WITH_BAD_TIMESTAMP = new CommandInvocation(
            "nvm",
            new String[0],
            ImmutableMap.of(
                    LoadCurrencyRateCommand.ARG_CURRENCY, "USD",
                    LoadCurrencyRateCommand.ARG_EXCHANGE_DATE, "2019-03-05",
                    LoadCurrencyRateCommand.ARG_STORE_TIMESTAMP, "2019-02-03 12:45:22"
            )
    );

    private static final CommandInvocation CMD_CALL_WITH_VALID_ARGS = new CommandInvocation(
            "nvm",
            new String[0],
            ImmutableMap.of(
                    LoadCurrencyRateCommand.ARG_CURRENCY, "USD",
                    LoadCurrencyRateCommand.ARG_EXCHANGE_DATE, "2019-03-12",
                    LoadCurrencyRateCommand.ARG_STORE_TIMESTAMP, "2019-03-17T12:45:22"
            )
    );

    private static final CommandInvocation CMD_CALL_WITHOUT_EXPLICIT_TS = new CommandInvocation(
            "nvm",
            new String[0],
            ImmutableMap.of(
                    LoadCurrencyRateCommand.ARG_CURRENCY, "USD",
                    LoadCurrencyRateCommand.ARG_EXCHANGE_DATE, "2019-03-12"
            )
    );

    private static final CurrencyRate RATE_20190312_CBRF_USD
            = new CurrencyRate(Currency.USD, RateSource.CBRF_DAILY, 1, BigDecimal.valueOf(12.1), DATE_2019_03_12);

    private static final List<CurrencyRate> RATES = ImmutableList.of(
            new CurrencyRate(Currency.USD, RateSource.CBRF_DAILY, 1, BigDecimal.valueOf(11.1), DATE_2019_03_11),
            RATE_20190312_CBRF_USD,
            new CurrencyRate(Currency.USD, RateSource.CBRF_DAILY, 1, BigDecimal.valueOf(13.1), DATE_2019_03_13),
            new CurrencyRate(Currency.EUR, RateSource.CBRF_DAILY, 1, BigDecimal.TEN, DATE_2019_03_12),
            new CurrencyRate(Currency.RUR, RateSource.YANDEX, 1, BigDecimal.valueOf(65.5), DATE_2019_03_12)
    );

    private static final List<CurrencyRate> BROKEN_RATES = ImmutableList.of(
            RATE_20190312_CBRF_USD,
            RATE_20190312_CBRF_USD
    );

    @Mock
    private Terminal terminal;
    @Mock
    private CurrencyRateLoaderService mockedCurrencyRateLoaderService;
    @Mock
    private HistoricalCurrencyRateLoader mockedHistoricalCurrencyRateLoader;
    @Captor
    private ArgumentCaptor<LocalDateTime> localDateTimeCaptor;
    @Autowired
    private LoadCurrencyRateCommand command;

    private LoadCurrencyRateCommand mockedCommand;

    private static Stream<Arguments> argsCheck() {
        return Stream.of(
                Arguments.of(
                        "Валюта указана неверно/не удалось найти",
                        CMD_CALL_WITH_BAD_CURRENCY,
                        IllegalArgumentException.class,
                        "Missing or incorrect currency"
                ),
                Arguments.of(
                        "Неверный формат для даты публикации курса валюты",
                        CMD_CALL_WITH_BAD_TARGET_DATE,
                        DateTimeParseException.class,
                        "Text '2019-0305' could not be parsed"
                ),
                Arguments.of(
                        "Неверный формат даты-времени для устновки timestamp при загрузке в базу",
                        CMD_CALL_WITH_BAD_TIMESTAMP,
                        DateTimeParseException.class,
                        "Text '2019-02-03 12:45:22' could not be parsed at"
                )
        );
    }

    @BeforeEach
    void beforeEach() {
        when(terminal.getWriter())
                .thenReturn(Mockito.mock(PrintWriter.class));
        mockedCommand = new LoadCurrencyRateCommand(mockedCurrencyRateLoaderService, mockedHistoricalCurrencyRateLoader);

    }

    @DisplayName("Проверка переданных аругментов")
    @MethodSource(value = "argsCheck")
    @ParameterizedTest(name = "{0}")
    void test_argsCheck(String descr,
                        CommandInvocation commandInvocation,
                        Class<Exception> exceptionClass,
                        String expectedErrMsg
    ) {
        final Exception ex = Assertions.assertThrows(
                exceptionClass,
                () -> command.executeCommand(commandInvocation, terminal)
        );
        assertThat(ex.getMessage(), StringStartsWith.startsWith(expectedErrMsg));
    }

    @DisplayName("Вызов без явного ts -> сохраненияем с trantime=now")
    @Test
    void test_defaultTimeStamp() {

        when(mockedHistoricalCurrencyRateLoader.load(
                eq(Currency.USD),
                eq(RateSource.CBRF_DAILY),
                eq(LocalDate.of(2019, 3, 12)))
        ).thenReturn(RATE_20190312_CBRF_USD);

        when(terminal.areYouSure())
                .thenReturn(CONFIRM_LOAD)
                .thenReturn(CONFIRM_PERSIST);

        final LocalDateTime now = LocalDateTime.now();
        final ZoneOffset offset = ZoneOffset.systemDefault().getRules().getOffset(Instant.now());

        mockedCommand.executeCommand(CMD_CALL_WITHOUT_EXPLICIT_TS, terminal);

        verify(mockedCurrencyRateLoaderService)
                .persistRate(eq(RATE_20190312_CBRF_USD), localDateTimeCaptor.capture());

        Mockito.verify(mockedHistoricalCurrencyRateLoader, times(1)).load(any(), any(), any());

        Mockito.verifyNoMoreInteractions(mockedCurrencyRateLoaderService);

        assertTrue(
                localDateTimeCaptor.getValue().toInstant(offset).toEpochMilli() -
                        now.toInstant(offset).toEpochMilli() < TimeUnit.SECONDS.toMillis(10),
                "Stored with now"
        );
    }

    /**
     * Проверяем, что:
     * - данные корретно фильтруются по типу валюты + дате курса + типу источника, и в итоге остается только одна запись
     * - вызывается метод сервиса для записи в хранилище с ожидаемым курсом
     */
    @DisplayName("Подгрузка, фильтрация и подтверждение на persist")
    @Test
    void test_generalWhenBothConfirmed() {
        when(mockedHistoricalCurrencyRateLoader.load(
                eq(Currency.USD),
                eq(RateSource.CBRF_DAILY),
                eq(LocalDate.of(2019, 3, 12)))
        ).thenReturn(RATE_20190312_CBRF_USD);

        when(terminal.areYouSure())
                .thenReturn(CONFIRM_LOAD)
                .thenReturn(CONFIRM_PERSIST);

        mockedCommand.executeCommand(CMD_CALL_WITH_VALID_ARGS, terminal);

        Mockito.verify(mockedHistoricalCurrencyRateLoader, times(1)).load(any(), any(), any());

        Mockito.verify(mockedCurrencyRateLoaderService)
                .persistRate(RATE_20190312_CBRF_USD, DATE_2019_03_17_124522);

        Mockito.verifyNoMoreInteractions(mockedCurrencyRateLoaderService);
    }

    /**
     * Ничего не сохраняем без подтверждения явного.
     */
    @DisplayName("Подгрузка, фильтрация но нет подтверждения на persist")
    @Test
    void test_persistNotConfirmed() {
        when(mockedHistoricalCurrencyRateLoader.load(
                eq(Currency.USD),
                eq(RateSource.CBRF_DAILY),
                eq(LocalDate.of(2019, 3, 12)))
        ).thenReturn(RATE_20190312_CBRF_USD);

        when(terminal.areYouSure())
                .thenReturn(CONFIRM_LOAD)
                .thenReturn(!CONFIRM_PERSIST);

        mockedCommand.executeCommand(CMD_CALL_WITH_VALID_ARGS, terminal);

        Mockito.verify(mockedHistoricalCurrencyRateLoader, times(1)).load(any(), any(), any());

        Mockito.verifyNoMoreInteractions(mockedCurrencyRateLoaderService);
    }

    @DisplayName("Нет подтверждения на load и на persist")
    @Test
    void test_loadNotConfirmed() {
        when(mockedHistoricalCurrencyRateLoader.load(
                eq(Currency.USD),
                eq(RateSource.CBRF_DAILY),
                eq(LocalDate.of(2019, 3, 12)))
        ).thenReturn(RATE_20190312_CBRF_USD);

        when(terminal.areYouSure())
                .thenReturn(!CONFIRM_LOAD).thenReturn(!CONFIRM_PERSIST);

        mockedCommand.executeCommand(CMD_CALL_WITH_VALID_ARGS, terminal);

        Mockito.verifyZeroInteractions(mockedCurrencyRateLoaderService);
        Mockito.verifyZeroInteractions(mockedHistoricalCurrencyRateLoader);
    }
}