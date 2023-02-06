package ru.yandex.market.mbi.api.billing.client;

import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.mbi.api.billing.client.model.CurrentAndNextMonthPayoutFrequencyDTO;
import ru.yandex.market.mbi.api.billing.client.model.NettingTransitionInfoDTO;
import ru.yandex.market.mbi.api.billing.client.model.NettingTransitionStatus;
import ru.yandex.market.mbi.api.billing.client.model.PayoutFrequencyDTO;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


public class MbiBillingClientTest {

    private static final List<NettingTransitionInfoDTO> NETTING_TRANSITION =
            List.of(new NettingTransitionInfoDTO().partnerId(1L).status(NettingTransitionStatus.ENABLED));
    private static WireMockServer wm;
    private MbiBillingClient client = new MbiBillingClient.Builder().baseUrl("http://localhost:9000").build();

    @BeforeAll
    public static void setup() {
        wm = new WireMockServer(options().port(9000).withRootDirectory(Objects.requireNonNull(Util.getClassPathFile(
                "wiremock")).getAbsolutePath()));
        wm.start();
    }

    @AfterAll
    public static void tearDown() {
        wm.shutdown();
    }

    @DisplayName("Запрос выполнился с ошибкой. Сервер вернул код 400")
    @Test
    public void get400ErrorTest() {
        MbiBillingClientException thrown = assertThrows(MbiBillingClientException.class, () -> {
            List<Long> contractIds = Collections.singletonList(3L);
            client.getCurrentAndNextMonthPayoutFrequencies(contractIds);
        });
        assertEquals(400, thrown.getHttpErrorCode());
    }

    @DisplayName("Запрос выполнился с ошибкой. Сервер вернул код 500")
    @Test
    public void get500ErrorTest() {
        MbiBillingClientException thrown = assertThrows(MbiBillingClientException.class, () -> {
            List<Long> contractIds = Collections.singletonList(4L);
            client.getCurrentAndNextMonthPayoutFrequencies(contractIds);
        });
        assertEquals(500, thrown.getHttpErrorCode());
    }

    @DisplayName("Получение информации о частоте выплаты")
    @Test
    public void getGetCurrentAndNextFrequency() {
        List<Long> contractIds = Arrays.asList(1L, 2L);
        List<CurrentAndNextMonthPayoutFrequencyDTO> response =
                client.getCurrentAndNextMonthPayoutFrequencies(contractIds);

        assertEquals(2, contractIds.size());

        CurrentAndNextMonthPayoutFrequencyDTO firstFrequency = response.get(0);
        assertEquals(1L, firstFrequency.getContractId());
        assertEquals(PayoutFrequencyDTO.DAILY, firstFrequency.getCurrentMonthFrequency());
        assertEquals(PayoutFrequencyDTO.BIWEEKLY, firstFrequency.getNextMonthFrequency());
        assertEquals(true, firstFrequency.getIsDefaultCurrentMonthFrequency());

        CurrentAndNextMonthPayoutFrequencyDTO secondFrequency = response.get(1);
        assertEquals(2L, secondFrequency.getContractId());
        assertEquals(PayoutFrequencyDTO.WEEKLY, secondFrequency.getCurrentMonthFrequency());
        assertEquals(PayoutFrequencyDTO.DAILY, secondFrequency.getNextMonthFrequency());
        assertEquals(false, secondFrequency.getIsDefaultCurrentMonthFrequency());
    }

    @DisplayName("Сохранение информации о статусе взаимозачета в базу биллинга")
    @Test
    public void testSaveNettingTransition() {
        assertDoesNotThrow(() -> client.saveNettingTransition(NETTING_TRANSITION));
    }

    @DisplayName("Сервер не ответил вовремя.")
    @Test
    public void timeoutTestPayoutFrequency() {
        // given
        MbiBillingClient newClient = new MbiBillingClient.Builder()
                .baseUrl("http://localhost:9000")
                .readTimeout(5, TimeUnit.MILLISECONDS)
                .build();

        // then
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            List<Long> contractIds = Arrays.asList(1L, 2L);
            newClient.getCurrentAndNextMonthPayoutFrequencies(contractIds);
        });
        assertEquals(SocketTimeoutException.class, thrown.getCause().getClass());
        assertEquals("timeout", thrown.getCause().getMessage());
    }

    @DisplayName("Сервер не ответил вовремя.")
    @Test
    public void timeoutTestSaveNettingTransition() {
        // given
        MbiBillingClient billingClient = new MbiBillingClient.Builder()
                .baseUrl("http://localhost:9000")
                .readTimeout(5, TimeUnit.MILLISECONDS)
                .build();

        // then
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            billingClient.saveNettingTransition(NETTING_TRANSITION);
        });
        assertEquals(SocketTimeoutException.class, thrown.getCause().getClass());
        assertEquals("timeout", thrown.getCause().getMessage());
    }


}
