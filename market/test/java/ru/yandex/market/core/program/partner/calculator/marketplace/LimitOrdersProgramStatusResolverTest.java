package ru.yandex.market.core.program.partner.calculator.marketplace;

import java.util.Optional;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.abo.api.client.AboPublicRestClient;
import ru.yandex.market.abo.api.entity.order.limit.OrderLimitDTO;
import ru.yandex.market.abo.api.entity.order.limit.PublicOrderLimitReason;
import ru.yandex.market.core.abo.AboLimitedService;
import ru.yandex.market.core.business.BusinessService;
import ru.yandex.market.core.program.partner.model.AboLimitOrdersCheckStatus;
import ru.yandex.market.core.program.partner.model.NeedTestingState;
import ru.yandex.market.core.program.partner.model.ProgramArgs;
import ru.yandex.market.core.program.partner.model.ProgramStatus;
import ru.yandex.market.core.program.partner.model.ProgramSubStatus;
import ru.yandex.market.core.program.partner.model.Status;
import ru.yandex.market.core.program.partner.model.Substatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
class LimitOrdersProgramStatusResolverTest {

    static ThreadPoolExecutor executor;
    static AboPublicRestClient abo;
    static LimitOrdersProgramStatusResolver resolver;
    static BusinessService businessService;
    static AboLimitedService aboLimitedService;

    @BeforeAll
    static void setUp() {
        executor = new ThreadPoolExecutor(1, 1, Long.MAX_VALUE, TimeUnit.MILLISECONDS, new SynchronousQueue<>());
        abo = mock(AboPublicRestClient.class);
        aboLimitedService = new AboLimitedService(abo, businessService, executor, executor);
        resolver = new LimitOrdersProgramStatusResolver(aboLimitedService, () -> true);
    }

    @AfterAll
    static void tearDown() {
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
        abo = null;
    }

    public static Stream<Arguments> checkLogicData() {
        ProgramStatus.Builder restrictedStatus = ProgramStatus.builder()
                .status(Status.RESTRICTED)
                .addSubStatus(ProgramSubStatus.builder().code(Substatus.LIMIT_ORDERS).build())
                .needTestingState(NeedTestingState.NOT_REQUIRED)
                .enabled(true);
        OrderLimitDTO orderLimitDTO = new OrderLimitDTO(111, 100, PublicOrderLimitReason.NEWBIE, 1L, 1);
        return Stream.of(
                Arguments.of(ProgramArgs.builder().build(), orderLimitDTO, restrictedStatus),
                Arguments.of(ProgramArgs.builder().withAboLimitOrdersCheck(AboLimitOrdersCheckStatus.NOT_LIMITED).build(), orderLimitDTO, null),
                Arguments.of(ProgramArgs.builder().withAboLimitOrdersCheck(AboLimitOrdersCheckStatus.SKIP).build(),
                        orderLimitDTO, null),
                Arguments.of(ProgramArgs.builder().withAboLimitOrdersCheck(AboLimitOrdersCheckStatus.LIMITED).build()
                        , orderLimitDTO, restrictedStatus),
                Arguments.of(ProgramArgs.builder().withAboLimitOrdersCheck(AboLimitOrdersCheckStatus.UNKNOWN).build()
                        , orderLimitDTO, restrictedStatus),
                Arguments.of(ProgramArgs.builder().withAboLimitOrdersCheck(AboLimitOrdersCheckStatus.UNKNOWN).build()
                        , null, null)
        );
    }

    @AfterEach
    void afterEach() {
        reset(abo);
    }

    @Test
    void testRejectNow() {
        Future<?> busy = executor.submit(() -> { // Занимаем пул черти чем
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
//                 do nothing
            }
        });
        long origTaskCount = executor.getTaskCount();
        long start = System.currentTimeMillis();
        assertTrue(resolver.resolve(1, ProgramArgs.builder().build()).isEmpty()); // нет результата
        assertTrue(System.currentTimeMillis() - start < 500); // не ждал таймаут (если будет флапать, отключим)
        assertEquals(executor.getTaskCount(), origTaskCount); // в пул не попало задание

        busy.cancel(true); // не забываем грохнуть черти что
    }

    @Test
    void testOk() {
        when(abo.getOrderLimit(1)).thenReturn(new OrderLimitDTO(1, 100, PublicOrderLimitReason.NEWBIE, 1L, 1));

        long origTaskCount = executor.getTaskCount();
        assertTrue(resolver.resolve(1, ProgramArgs.builder().build()).isPresent()); // есть результат
        assertEquals(executor.getTaskCount(), origTaskCount + 1); // было задание
    }

    @ParameterizedTest
    @MethodSource("checkLogicData")
    void checkLogic(ProgramArgs programArgs, OrderLimitDTO aboReturnValue, ProgramStatus.Builder expected) {

        when(abo.getOrderLimit(111)).thenReturn(aboReturnValue);

        Optional<ProgramStatus.Builder> result = resolver.resolve(111L, programArgs);
        if (expected == null) {
            assertThat(result.isEmpty()).isTrue();
        } else {
            ProgramStatus.Builder actual = result.get();
            assertThat(actual.getStatus()).isEqualTo(expected.getStatus());
            assertThat(actual.getSubStatuses()).isEqualTo(expected.getSubStatuses());
        }
    }

}
