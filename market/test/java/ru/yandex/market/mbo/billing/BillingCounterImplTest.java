package ru.yandex.market.mbo.billing;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.dbselector.DbType;
import ru.yandex.market.mbo.billing.counter.AbstractOperationCounter;
import ru.yandex.market.mbo.billing.counter.BillingOperations;
import ru.yandex.market.mbo.billing.counter.BillingOperationsImpl;
import ru.yandex.market.mbo.billing.counter.PaidOperationLoader;
import ru.yandex.market.mbo.billing.counter.info.BillingOperationInfoWithoutPrice;
import ru.yandex.market.mbo.billing.tarif.Tarif;
import ru.yandex.market.mbo.billing.tarif.TarifManager;
import ru.yandex.market.mbo.billing.tarif.TarifProvider;
import ru.yandex.market.mbo.billing.tarif.TarifProviderImpl;
import ru.yandex.market.mbo.history.messaging.MessageWriter;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;

/**
 * @author amaslak
 */
@SuppressWarnings("checkstyle:magicNumber")
public class BillingCounterImplTest {

    private JdbcTemplate jdbcTemplate;
    private MessageWriter messageWriter;
    private TarifManager tarifManager;

    private BillingSessionManager billingSessionManager;
    private BillingCounterImpl billingCounter;
    private BillingOperations billingOperations;
    private BillingStatusUpdater billingStatusUpdater;
    private BillingCounterRegistry registry;

    @Before
    public void setUp() {
        jdbcTemplate = new JdbcTemplate(BillingDatabaseMock.getBillingDatasource());
        messageWriter = Mockito.mock(MessageWriter.class);
        tarifManager = Mockito.mock(TarifManager.class);
        billingStatusUpdater = Mockito.mock(BillingStatusUpdater.class);
        registry = Mockito.mock(BillingCounterRegistry.class);

        BillingSessionManagerImpl sessionManager = new BillingSessionManagerImpl(
            jdbcTemplate,
            jdbcTemplate,
            () -> DbType.ORACLE,
            messageWriter
        );

        this.billingSessionManager = sessionManager;

        TarifProvider tarifProvider = new TarifProviderImpl(
            new GregorianCalendar(2100, 6, 8),
            getTestOperationTarifs(),
            Collections.emptyMap());
        when(tarifManager.loadTarifs(Mockito.any())).thenReturn(tarifProvider);

        Mockito.doNothing().when(billingStatusUpdater).updateWithSuccess(Mockito.any());
        Mockito.doNothing().when(billingStatusUpdater).updateWithFailure(Mockito.any());

        BillingCounterImpl billingCounterImpl =
            new BillingCounterImpl(tarifManager, registry, messageWriter, billingStatusUpdater);

        this.billingCounter = billingCounterImpl;

        this.billingOperations = new BillingOperationsImpl(jdbcTemplate);
    }

    @Test
    public void testErrorBillingCounter() throws Throwable {
        try {
            PaidOperationLoader loaderWithException = (interval, tarifProvider) -> {
                throw new BillingCounterImplTestException();
            };
            when(registry.getLoaders()).thenReturn(Collections.singleton(loaderWithException));
            billingCounter.loadBilling(billingSessionManager);
        } catch (BillingException e) {
            // ожидаем BillingCounterImplTestException, если пришло что-то другое, то выкидываем исключение
            if (!(e.getCause() instanceof BillingCounterImplTestException)) {
                throw e.getCause();
            }
        }
    }

    @Test
    public void testErrorInOperations() throws Throwable {
        try {
            OperationCounterMock operationCounter = new OperationCounterMock(true);
            operationCounter.setBillingOperations(billingOperations);

            when(registry.getLoaders()).thenReturn(Collections.singleton(operationCounter));
            billingCounter.loadBilling(billingSessionManager);
        } catch (BillingException e) {
            // ожидаем BillingCounterImplTestException, если пришло что-то другое, то выкидываем исключение
            if (!(e.getCause() instanceof BillingCounterImplTestException)) {
                throw e.getCause();
            }
        }
    }

    @Test
    public void testSuccessStatusUpdated() {
        OperationCounterMock operationCounter = new OperationCounterMock(false);
        operationCounter.setBillingOperations(billingOperations);

        when(registry.getLoaders()).thenReturn(Collections.singleton(operationCounter));
        billingCounter.loadBilling(billingSessionManager);

        Mockito.verify(billingStatusUpdater, Mockito.times(1)).updateWithSuccess(Mockito.any());
        Mockito.verify(billingStatusUpdater, Mockito.times(0)).updateWithFailure(Mockito.any());
    }

    @Test
    public void testFailureStatusUpdated() throws Throwable {
        OperationCounterMock operationCounter = new OperationCounterMock(true);
        operationCounter.setBillingOperations(billingOperations);

        when(registry.getLoaders()).thenReturn(Collections.singleton(operationCounter));
        try {
            billingCounter.loadBilling(billingSessionManager);
        } catch (BillingException e) {
            // ожидаем BillingCounterImplTestException, если пришло что-то другое, то выкидываем исключение
            if (!(e.getCause() instanceof BillingCounterImplTestException)) {
                throw e.getCause();
            }
        }

        Mockito.verify(billingStatusUpdater, Mockito.times(0)).updateWithSuccess(Mockito.any());
        Mockito.verify(billingStatusUpdater, Mockito.times(1)).updateWithFailure(Mockito.any());
    }

    private Map<Integer, List<Tarif>> getTestOperationTarifs() {
        Calendar startDate = new GregorianCalendar(1970, 1, 1);
        Tarif tarif1 = new Tarif(new BigDecimal(10), startDate);
        Tarif tarif2 = new Tarif(new BigDecimal(10), startDate);

        return ImmutableMap.of(0, Collections.singletonList(tarif1),
            1, Collections.singletonList(tarif2));
    }

    private class OperationCounterMock extends AbstractOperationCounter {
        private boolean forceFailure;

        private OperationCounterMock(boolean forceFailure) {
            this.forceFailure = forceFailure;
        }

        @Override
        protected void doLoad(Pair<Calendar, Calendar> interval, TarifProvider tarifProvider) {
            if (forceFailure) {
                throw new BillingCounterImplTestException();
            } else {
                addOperation(
                    interval,
                    tarifProvider,
                    BillingOperationInfoWithoutPrice.BillingOperationInfoWithoutPriceBuilder.newBuilder()
                        .setUserId(1L)
                        .setGuruCategoryId(2L)
                        .setTime(Calendar.getInstance())
                        .setOperation(PaidAction.FILL_MODEL_CARD)
                        .setSourceId(1L).build()
                );
                addOperation(
                    interval,
                    tarifProvider,
                    BillingOperationInfoWithoutPrice.BillingOperationInfoWithoutPriceBuilder.newBuilder()
                        .setUserId(1L)
                        .setGuruCategoryId(2L)
                        .setTime(Calendar.getInstance())
                        .setOperation(PaidAction.CHECK_MODEL_CARD)
                        .setSourceId(1L).build()
                );
            }
        }
    }

    private class BillingCounterImplTestException extends RuntimeException {
    }
}
