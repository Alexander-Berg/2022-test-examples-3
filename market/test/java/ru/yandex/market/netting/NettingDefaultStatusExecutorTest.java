package ru.yandex.market.netting;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.api.billing.client.MbiBillingClient;
import ru.yandex.market.mbi.api.billing.client.model.NettingTransitionInfoDTO;
import ru.yandex.market.mbi.api.billing.client.model.NettingTransitionStatus;
import ru.yandex.market.shop.FunctionalTest;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@DbUnitDataSet(before = "csv/NettingDefaultStatusExecutorTest.before.csv")
public class NettingDefaultStatusExecutorTest extends FunctionalTest {

    @Autowired
    private MbiBillingClient mbiBillingClient;

    @Autowired
    private NettingDefaultStatusExecutor nettingDefaultStatusExecutor;

    @Test
    @DbUnitDataSet(after = "csv/NettingDefaultStatusExecutorTest.test.after.csv")
    void testDisabled() {
        nettingDefaultStatusExecutor.doJob(null);
    }

    @Test
    @DbUnitDataSet(before = "csv/NettingDefaultStatusExecutorTest.testEnabled.before.csv",
            after = "csv/NettingDefaultStatusExecutorTest.testEnabled.after.csv")
    void testEnabled() {
        nettingDefaultStatusExecutor.doJob(null);
    }

    @Test
    @DbUnitDataSet(before = "csv/NettingDefaultStatusExecutorTest.testBillingUpload.before.csv",
            after = "csv/NettingDefaultStatusExecutorTest.testBillingUpload.after.csv")
    void testBillingUpload() {
        nettingDefaultStatusExecutor.doJob(null);
        verify(mbiBillingClient).saveNettingTransition(Mockito.argThat(t -> t.containsAll(List.of(
                        buildDTO(774L, NettingTransitionStatus.ENABLED),
                        buildDTO(775L, NettingTransitionStatus.ENABLED)
                )
        )));

        verifyNoMoreInteractions(mbiBillingClient);
    }

    private NettingTransitionInfoDTO buildDTO(long partnerId, NettingTransitionStatus nettingTransitionStatus) {
        NettingTransitionInfoDTO nettingTransitionInfoDTO = new NettingTransitionInfoDTO();
        nettingTransitionInfoDTO.setPartnerId(partnerId);
        nettingTransitionInfoDTO.setStatus(nettingTransitionStatus);
        return nettingTransitionInfoDTO;
    }
}
