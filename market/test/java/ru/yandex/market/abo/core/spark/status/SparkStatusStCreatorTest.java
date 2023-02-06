package ru.yandex.market.abo.core.spark.status;

import java.util.stream.Stream;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.abo.core.spark.model.SparkCheckResult;
import ru.yandex.market.abo.core.spark.model.SparkCheckShop;
import ru.yandex.market.abo.core.startrek.StartrekTicketManager;
import ru.yandex.market.abo.core.startrek.model.StartrekTicketFactory;
import ru.yandex.market.abo.core.startrek.model.StartrekTicketReason;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 04.08.2020
 */
class SparkStatusStCreatorTest {

    @InjectMocks
    private SparkStatusStCreator stTicketCreator;

    @Mock
    private StartrekTicketManager startrekTicketManager;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
    }

    @ParameterizedTest
    @CsvSource({"true, 1", "false, 0"})
    void createStTicketHasNoNewTicket(boolean hasNoNewTickets, int expectedNewTicketsCount) {
        when(startrekTicketManager.hasNoTickets(anyLong(), any(StartrekTicketReason.class)))
                .thenReturn(hasNoNewTickets);

        stTicketCreator.createStTicketForSupplier(buildResultStatistics());

        verify(startrekTicketManager, times(expectedNewTicketsCount)).createTicket(any(StartrekTicketFactory.class));
    }

    @ParameterizedTest(name = "createStTicketHasAnyProblemResult_{index}")
    @MethodSource("createStTicketHasAnyProblemResultSource")
    void createStTicketHasAnyProblemResult(Multimap<SparkCheckResult, SparkCheckShop> resultStatistic,
                                              int expectedNewTicketsCount) {
        when(startrekTicketManager.hasNoTickets(anyLong(), any(StartrekTicketReason.class)))
                .thenReturn(true);

        stTicketCreator.createStTicketForSupplier(resultStatistic);

        verify(startrekTicketManager, times(expectedNewTicketsCount)).createTicket(any(StartrekTicketFactory.class));
    }

    private static Multimap<SparkCheckResult, SparkCheckShop> buildResultStatistics() {
        Multimap<SparkCheckResult, SparkCheckShop> resultStatistics = ArrayListMultimap.create();
        resultStatistics.put(SparkCheckResult.WRONG_OGRN, new SparkCheckShop(123L, "123456789012", "Поставщик"));
        return resultStatistics;
    }

    private static Stream<Arguments> createStTicketHasAnyProblemResultSource() {
        return Stream.of(
                Arguments.of(buildResultStatistics(), 1),
                Arguments.of(ArrayListMultimap.create(), 0)
        );
    }
}
