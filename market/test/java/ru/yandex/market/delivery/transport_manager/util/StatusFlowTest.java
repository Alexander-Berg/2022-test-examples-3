package ru.yandex.market.delivery.transport_manager.util;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.ibatis.jdbc.RuntimeSqlException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.delivery.transport_manager.domain.HasStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.Status;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnit;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitType;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.Register;
import ru.yandex.market.delivery.transport_manager.domain.enums.RegisterStatus;
import ru.yandex.market.delivery.transport_manager.domain.enums.RegisterType;
import ru.yandex.market.delivery.transport_manager.repository.StatusFlowMapper;
import ru.yandex.market.delivery.transport_manager.repository.mappers.TransportationUnitMapper;
import ru.yandex.market.delivery.transport_manager.repository.mappers.register.RegisterMapper;
import ru.yandex.market.delivery.transport_manager.service.StatusHistoryService;
import ru.yandex.market.delivery.transport_manager.service.status_flow.flow.StatusFlowDependencies;
import ru.yandex.market.delivery.transport_manager.service.status_flow.flow.StatusFlowFactory;
import ru.yandex.market.delivery.transport_manager.service.status_flow.lambda.Function;
import ru.yandex.market.delivery.transport_manager.service.status_flow.lambda.Supplier;

import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class StatusFlowTest {

    private static final long ID = 1L;

    private StatusFlowMapper<SampleStatus> statusFlowMapper;
    private StatusFlowDependencies<SampleStatus, SampleEntity> sampleStatusFlow;
    private TransactionTemplate transactionTemplate;
    private StatusHistoryService statusHistoryService;
    private StatusFlowFactory statusFlowFactory;
    private TransportationUnitMapper unitMapper;
    private RegisterMapper registerMapper;

    @BeforeEach
    void setUp() {
        statusFlowMapper = mock(StatusFlowMapper.class);
        transactionTemplate = mock(TransactionTemplate.class);
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });
        unitMapper = mock(TransportationUnitMapper.class);
        registerMapper = mock(RegisterMapper.class);

        statusHistoryService = Mockito.mock(StatusHistoryService.class);
        statusFlowFactory = new StatusFlowFactory(
            transactionTemplate,
            statusHistoryService,
            null,
            unitMapper,
            null,
            registerMapper,
            null,
            null,
            null,
            null
        );

        sampleStatusFlow = statusFlowFactory.of(statusFlowMapper, SampleEntity.class);

        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(statusFlowMapper, statusHistoryService);
    }

    @DisplayName("Успешное выполнение с проверкой статуса")
    @Test
    public void ok() {
        when(statusFlowMapper.getStatus(1L)).thenReturn(SampleStatus.NEW);
        when(statusFlowMapper.switchStatusReturningCount(1L, SampleStatus.NEW, SampleStatus.DONE))
            .thenReturn(1);
        when(statusHistoryService.supports(SampleEntity.class)).thenReturn(true);
        AtomicBoolean initialOperationsPerformed = new AtomicBoolean(false);
        AtomicBoolean savingPerformed = new AtomicBoolean(false);

        Optional<Integer> value = sampleStatusFlow
            .id(1L)
            .fromStatus(SampleStatus.NEW)
            .successStatus(SampleStatus.DONE)
            .longRunnigOperation(() -> {
                initialOperationsPerformed.set(true);
                return true;
            })
            .performTransactionalResultSaving(v -> {
                savingPerformed.set(v);
                return 0;
            });

        Assertions.assertEquals(Optional.of(0), value);
        Assertions.assertTrue(initialOperationsPerformed.get());
        Assertions.assertTrue(savingPerformed.get());

        verify(statusFlowMapper).getStatus(eq(1L));
        verify(statusFlowMapper).switchStatusReturningCount(
            eq(1L),
            eq(SampleStatus.NEW),
            eq(SampleStatus.DONE)
        );
        verify(statusHistoryService).supports(SampleEntity.class);
        verify(statusHistoryService)
            .submitStatusChangeInfo(1L, SampleEntity.class, null, SampleStatus.DONE, SampleStatus.NEW);
    }

    @DisplayName("Успешное выполнение без проверки изначального статуса")
    @Test
    public void okNoInitialStatusCheck() {
        when(statusFlowMapper.getStatus(1L)).thenReturn(SampleStatus.NEW);
        when(statusFlowMapper.switchStatusReturningCount(1L, SampleStatus.NEW, SampleStatus.DONE))
            .thenReturn(1);
        when(statusHistoryService.supports(SampleEntity.class)).thenReturn(true);
        AtomicBoolean initialOperationsPerformed = new AtomicBoolean(false);
        AtomicBoolean savingPerformed = new AtomicBoolean(false);

        Optional<Integer> value = sampleStatusFlow
            .id(1L)
            .fromStatus((SampleStatus) null)
            .successStatus(SampleStatus.DONE)
            .longRunnigOperation(() -> {
                initialOperationsPerformed.set(true);
                return true;
            })
            .performTransactionalResultSaving(v -> {
                savingPerformed.set(v);
                return 0;
            });

        Assertions.assertEquals(Optional.of(0), value);
        Assertions.assertTrue(initialOperationsPerformed.get());
        Assertions.assertTrue(savingPerformed.get());

        verify(statusFlowMapper).getStatus(eq(1L));
        verify(statusFlowMapper).switchStatusReturningCount(
            eq(1L),
            eq(SampleStatus.NEW),
            eq(SampleStatus.DONE)
        );
        verify(statusHistoryService).supports(SampleEntity.class);
        verify(statusHistoryService)
            .submitStatusChangeInfo(1L, SampleEntity.class, null, SampleStatus.DONE, SampleStatus.NEW);
    }

    @DisplayName("Пропускаем обработку, если статус уже успешный и выставлен флаг")
    @Test
    public void skipSuccessStatus() {
        when(statusFlowMapper.getStatus(1L)).thenReturn(SampleStatus.DONE);
        AtomicBoolean initialOperationsPerformed = new AtomicBoolean(false);
        AtomicBoolean savingPerformed = new AtomicBoolean(false);

        Optional<Integer> value = sampleStatusFlow
            .id(1L)
            .fromStatus(SampleStatus.NEW)
            .successStatus(SampleStatus.DONE)
            .skipSuccessStatus(true)
            .longRunnigOperation(() -> {
                initialOperationsPerformed.set(true);
                return true;
            })
            .performTransactionalResultSaving(v -> {
                savingPerformed.set(v);
                return 0;
            });

        Assertions.assertEquals(Optional.empty(), value);
        Assertions.assertFalse(initialOperationsPerformed.get());
        Assertions.assertFalse(savingPerformed.get());

        verify(statusFlowMapper).getStatus(eq(1L));
    }

    @DisplayName("Успешное выполнение без финальной проверки статуса")
    @Test
    public void okNoFinalStatusCheck() {
        when(statusFlowMapper.getStatus(1L)).thenReturn(SampleStatus.NEW);
        when(statusFlowMapper.switchStatusReturningCount(1L, null, SampleStatus.DONE))
            .thenReturn(1);
        when(statusHistoryService.supports(SampleEntity.class)).thenReturn(true);
        AtomicBoolean initialOperationsPerformed = new AtomicBoolean(false);
        AtomicBoolean savingPerformed = new AtomicBoolean(false);

        Optional<Integer> value = sampleStatusFlow
            .id(1L)
            .fromStatus(SampleStatus.NEW)
            .successStatus(SampleStatus.DONE)
            .checkInitialStatusNotChangedAfterLongRunningOperationCompleted(false)
            .longRunnigOperation(() -> {
                initialOperationsPerformed.set(true);
                return true;
            })
            .performTransactionalResultSaving(v -> {
                savingPerformed.set(v);
                return 0;
            });

        Assertions.assertEquals(Optional.of(0), value);
        Assertions.assertTrue(initialOperationsPerformed.get());
        Assertions.assertTrue(savingPerformed.get());

        verify(statusFlowMapper).getStatus(eq(1L));
        verify(statusFlowMapper).switchStatusReturningCount(
            eq(1L),
            isNull(),
            eq(SampleStatus.DONE)
        );
        verify(statusHistoryService).supports(SampleEntity.class);
        verify(statusHistoryService)
            .submitStatusChangeInfo(1L, SampleEntity.class, null, SampleStatus.DONE, SampleStatus.NEW);
    }

    @DisplayName("Успешное выполнение с промежуточным статусом")
    @Test
    public void okWithProcessingStatus() {
        when(statusFlowMapper.getStatus(1L)).thenReturn(SampleStatus.NEW);
        when(statusFlowMapper.switchStatusReturningCount(1L, SampleStatus.NEW, SampleStatus.PROCESSING))
            .thenReturn(1);
        when(statusFlowMapper.switchStatusReturningCount(1L, SampleStatus.PROCESSING, SampleStatus.DONE))
            .thenReturn(1);
        when(statusHistoryService.supports(SampleEntity.class)).thenReturn(true);
        AtomicBoolean initialOperationsPerformed = new AtomicBoolean(false);
        AtomicBoolean savingPerformed = new AtomicBoolean(false);

        Optional<Integer> value = sampleStatusFlow
            .id(1L)
            .fromStatus(SampleStatus.NEW)
            .processingStatus(SampleStatus.PROCESSING)
            .successStatus(SampleStatus.DONE)
            .longRunnigOperation(() -> {
                initialOperationsPerformed.set(true);
                return true;
            })
            .performTransactionalResultSaving(v -> {
                savingPerformed.set(v);
                return 0;
            });

        Assertions.assertEquals(Optional.of(0), value);
        Assertions.assertTrue(initialOperationsPerformed.get());
        Assertions.assertTrue(savingPerformed.get());

        verify(statusFlowMapper).getStatus(eq(1L));
        verify(statusFlowMapper).switchStatusReturningCount(
            eq(1L),
            eq(SampleStatus.NEW),
            eq(SampleStatus.PROCESSING)
        );
        verify(statusFlowMapper).switchStatusReturningCount(
            eq(1L),
            eq(SampleStatus.PROCESSING),
            eq(SampleStatus.DONE)
        );
        verify(statusHistoryService).supports(SampleEntity.class);
        verify(statusHistoryService)
            .submitStatusChangeInfo(1L, SampleEntity.class, null, SampleStatus.DONE, SampleStatus.NEW);
    }

    @DisplayName("Успешное выполнение без переключения статуса")
    @Test
    public void okNoStatusSwitch() {
        when(statusFlowMapper.getStatus(1L)).thenReturn(SampleStatus.NEW);

        AtomicBoolean initialOperationsPerformed = new AtomicBoolean(false);
        AtomicBoolean savingPerformed = new AtomicBoolean(false);

        Optional<Integer> value = sampleStatusFlow
            .id(1L)
            .longRunnigOperation(() -> {
                initialOperationsPerformed.set(true);
                return true;
            })
            .performTransactionalResultSaving(v -> {
                savingPerformed.set(v);
                return 0;
            });

        Assertions.assertEquals(Optional.of(0), value);
        Assertions.assertTrue(initialOperationsPerformed.get());
        Assertions.assertTrue(savingPerformed.get());

        verify(statusFlowMapper).getStatus(eq(1L));
    }

    @DisplayName("Успешное выполнение без переключения статуса с промежуточным статусом")
    @Test
    public void okNoStatusSwitchWithProcessingStatus() {
        when(statusFlowMapper.getStatus(1L)).thenReturn(SampleStatus.NEW);
        when(statusFlowMapper.switchStatusReturningCount(1L, SampleStatus.NEW, SampleStatus.PROCESSING))
            .thenReturn(1);
        when(statusFlowMapper.switchStatusReturningCount(1L, SampleStatus.PROCESSING, SampleStatus.NEW))
            .thenReturn(1);

        AtomicBoolean initialOperationsPerformed = new AtomicBoolean(false);
        AtomicBoolean savingPerformed = new AtomicBoolean(false);

        Optional<Integer> value = sampleStatusFlow
            .id(1L)
            .processingStatus(SampleStatus.PROCESSING)
            .longRunnigOperation(() -> {
                initialOperationsPerformed.set(true);
                return true;
            })
            .performTransactionalResultSaving(v -> {
                savingPerformed.set(v);
                return 0;
            });

        Assertions.assertEquals(Optional.of(0), value);
        Assertions.assertTrue(initialOperationsPerformed.get());
        Assertions.assertTrue(savingPerformed.get());

        verify(statusFlowMapper).switchStatusReturningCount(
            eq(1L),
            eq(SampleStatus.NEW),
            eq(SampleStatus.PROCESSING)
        );
        verify(statusFlowMapper).switchStatusReturningCount(
            eq(1L),
            eq(SampleStatus.PROCESSING),
            eq(SampleStatus.NEW)
        );

        verify(statusFlowMapper).getStatus(eq(1L));
    }

    @DisplayName("Успешное выполнение с ручным переключением статуса с промежуточным статусом")
    @Test
    public void okManualSwitchWithProcessingStatus() {
        when(statusFlowMapper.getStatus(1L)).thenReturn(SampleStatus.NEW);
        when(statusFlowMapper.switchStatusReturningCount(1L, SampleStatus.NEW, SampleStatus.PROCESSING))
            .thenReturn(1);
        when(statusFlowMapper.switchStatusReturningCount(0L, SampleStatus.PROCESSING, SampleStatus.NEW))
            .thenReturn(1);

        AtomicBoolean initialOperationsPerformed = new AtomicBoolean(false);
        AtomicBoolean savingPerformed = new AtomicBoolean(false);

        Optional<Integer> value = sampleStatusFlow
            .id(1L)
            .processingStatus(SampleStatus.PROCESSING)
            .longRunnigOperation(() -> {
                initialOperationsPerformed.set(true);
                return true;
            })
            .performTransactionalResultSaving(v -> {
                savingPerformed.set(v);
                return 0;
            });

        Assertions.assertEquals(Optional.of(0), value);
        Assertions.assertTrue(initialOperationsPerformed.get());
        Assertions.assertTrue(savingPerformed.get());

        verify(statusFlowMapper).switchStatusReturningCount(
            eq(1L),
            eq(SampleStatus.NEW),
            eq(SampleStatus.PROCESSING)
        );
        verify(statusFlowMapper).switchStatusReturningCount(
            eq(1L),
            eq(SampleStatus.PROCESSING),
            eq(SampleStatus.NEW)
        );

        verify(statusFlowMapper).getStatus(eq(1L));
    }

    @DisplayName("Статус сразу не подходящий - кидаем исключение")
    @Test
    public void incorrectInitialStatus() {
        when(statusFlowMapper.getStatus(1L)).thenReturn(SampleStatus.DONE);
        when(statusFlowMapper.switchStatusReturningCount(1L, SampleStatus.NEW, SampleStatus.DONE))
            .thenReturn(1);
        AtomicBoolean initialOperationsPerformed = new AtomicBoolean(false);
        AtomicBoolean savingPerformed = new AtomicBoolean(false);

        Assertions.assertThrows(
            IllegalStateException.class,
            () -> sampleStatusFlow.
                id(1L)
                .fromStatus(SampleStatus.NEW)
                .successStatus(SampleStatus.DONE)
                .longRunnigOperation(() -> {
                    initialOperationsPerformed.set(true);
                    return true;
                })
                .performTransactionalResultSaving(v -> {
                    savingPerformed.set(v);
                    return 0;
                })
        );

        Assertions.assertFalse(initialOperationsPerformed.get());
        Assertions.assertFalse(savingPerformed.get());

        verify(statusFlowMapper).getStatus(eq(1L));
    }

    @DisplayName("Эксепшн при проверке статуса в начале")
    @Test
    public void initialStatusCheckerTransactionException() {
        when(statusFlowMapper.getStatus(1L)).thenThrow(RuntimeSqlException.class);
        AtomicBoolean initialOperationsPerformed = new AtomicBoolean(false);
        AtomicBoolean savingPerformed = new AtomicBoolean(false);

        Assertions.assertThrows(
            RuntimeSqlException.class,
            () -> sampleStatusFlow
                .id(1L)
                .fromStatus(SampleStatus.NEW)
                .processingStatus(SampleStatus.PROCESSING)
                .successStatus(SampleStatus.DONE)
                .longRunnigOperation(() -> {
                    initialOperationsPerformed.set(true);
                    return true;
                })
                .performTransactionalResultSaving(v -> {
                    savingPerformed.set(v);
                    return 0;
                })
        );

        Assertions.assertFalse(initialOperationsPerformed.get());
        Assertions.assertFalse(savingPerformed.get());

        verify(statusFlowMapper).getStatus(eq(1L));
    }

    @DisplayName("Эксепшн при выполнении \"длинной\" операции вне транзакции")
    @Test
    public void initialOperationException() {
        when(statusFlowMapper.getStatus(1L)).thenReturn(SampleStatus.NEW);
        when(statusFlowMapper.switchStatusReturningCount(1L, SampleStatus.NEW, SampleStatus.DONE))
            .thenReturn(1);
        AtomicBoolean initialOperationsPerformed = new AtomicBoolean(false);
        AtomicBoolean savingPerformed = new AtomicBoolean(false);

        Assertions.assertThrows(
            IOException.class,
            () -> sampleStatusFlow
                .id(1L)
                .fromStatus(SampleStatus.NEW)
                .successStatus(SampleStatus.DONE)
                .longRunnigOperation((Supplier<Boolean, IOException>) () -> {
                    initialOperationsPerformed.set(true);
                    throw new IOException();
                })
                .performTransactionalResultSaving(v -> {
                    savingPerformed.set(v);
                    return 0;
                })
        );

        Assertions.assertTrue(initialOperationsPerformed.get());
        Assertions.assertFalse(savingPerformed.get());

        verify(statusFlowMapper).getStatus(eq(1L));
        verifyNoMoreInteractions(statusFlowMapper);
    }

    @DisplayName("Эксепшн при выполнении \"длинной\" операции вне транзакции с промежуточным статусом")
    @Test
    public void initialOperationExceptionWithProcessingStstua() {
        when(statusFlowMapper.getStatus(1L)).thenReturn(SampleStatus.NEW);
        when(statusFlowMapper.switchStatusReturningCount(1L, SampleStatus.NEW, SampleStatus.PROCESSING))
            .thenReturn(1);
        when(statusFlowMapper.switchStatusReturningCount(1L, SampleStatus.PROCESSING, SampleStatus.NEW))
            .thenReturn(1);
        AtomicBoolean initialOperationsPerformed = new AtomicBoolean(false);
        AtomicBoolean savingPerformed = new AtomicBoolean(false);

        Assertions.assertThrows(
            IOException.class,
            () -> sampleStatusFlow
                .id(1L)
                .fromStatus(SampleStatus.NEW)
                .processingStatus(SampleStatus.PROCESSING)
                .successStatus(SampleStatus.DONE)
                .longRunnigOperation((Supplier<Boolean, IOException>) () -> {
                    initialOperationsPerformed.set(true);
                    throw new IOException();
                })
                .performTransactionalResultSaving(v -> {
                    savingPerformed.set(v);
                    return 0;
                })
        );

        Assertions.assertTrue(initialOperationsPerformed.get());
        Assertions.assertFalse(savingPerformed.get());

        verify(statusFlowMapper).getStatus(eq(1L));
        verify(statusFlowMapper).switchStatusReturningCount(
            eq(1L),
            eq(SampleStatus.NEW),
            eq(SampleStatus.PROCESSING)
        );
        verify(statusFlowMapper).switchStatusReturningCount(
            eq(1L),
            eq(SampleStatus.PROCESSING),
            eq(SampleStatus.NEW)
        );
        verifyNoMoreInteractions(statusFlowMapper);
    }

    @DisplayName("Эксепшн при сохранении")
    @Test
    public void savingException() {
        when(statusFlowMapper.getStatus(1L)).thenReturn(SampleStatus.NEW);

        AtomicBoolean initialOperationsPerformed = new AtomicBoolean(false);
        AtomicBoolean performed = new AtomicBoolean(false);

        Assertions.assertThrows(
            IOException.class,
            () -> sampleStatusFlow
                .id(1L)
                .fromStatus(SampleStatus.NEW)
                .successStatus(SampleStatus.DONE)
                .longRunnigOperation(() -> {
                    initialOperationsPerformed.set(true);
                    return true;
                })
                .performTransactionalResultSaving((Function<Boolean, Object, IOException>) v -> {
                    performed.set(v);
                    throw new IOException("Some error");
                })
        );

        Assertions.assertTrue(initialOperationsPerformed.get());
        Assertions.assertTrue(performed.get());

        verify(statusFlowMapper).getStatus(eq(1L));
        verifyNoMoreInteractions(statusFlowMapper);
    }

    @DisplayName("Эксепшн при сохранении с промежуточным статусом")
    @Test
    public void savingExceptionWithProcessingStatus() {
        when(statusFlowMapper.getStatus(1L)).thenReturn(SampleStatus.NEW);
        when(statusFlowMapper.switchStatusReturningCount(1L, SampleStatus.NEW, SampleStatus.PROCESSING))
            .thenReturn(1);
        when(statusFlowMapper.switchStatusReturningCount(1L, SampleStatus.PROCESSING, SampleStatus.NEW))
            .thenReturn(1);

        AtomicBoolean initialOperationsPerformed = new AtomicBoolean(false);
        AtomicBoolean performed = new AtomicBoolean(false);

        Assertions.assertThrows(
            IOException.class,
            () -> sampleStatusFlow
                .id(1L)
                .fromStatus(SampleStatus.NEW)
                .processingStatus(SampleStatus.PROCESSING)
                .successStatus(SampleStatus.DONE)
                .longRunnigOperation(() -> {
                    initialOperationsPerformed.set(true);
                    return true;
                })
                .performTransactionalResultSaving((Function<Boolean, Object, IOException>) v -> {
                    performed.set(v);
                    throw new IOException("Some error");
                })
        );

        Assertions.assertTrue(initialOperationsPerformed.get());
        Assertions.assertTrue(performed.get());

        verify(statusFlowMapper).getStatus(eq(1L));
        verify(statusFlowMapper).switchStatusReturningCount(
            eq(1L),
            eq(SampleStatus.NEW),
            eq(SampleStatus.PROCESSING)
        );
        verify(statusFlowMapper).switchStatusReturningCount(
            eq(1L),
            eq(SampleStatus.PROCESSING),
            eq(SampleStatus.NEW)
        );
        verifyNoMoreInteractions(statusFlowMapper);
    }

    @DisplayName("При смене статуса оказывается, что он не подходит")
    @Test
    public void illegalStatusOnStatusChange() {
        when(statusFlowMapper.getStatus(1L)).thenReturn(SampleStatus.NEW);
        when(statusFlowMapper.switchStatusReturningCount(1L, SampleStatus.NEW, SampleStatus.DONE)).thenReturn(0);

        AtomicBoolean initialOperationsPerformed = new AtomicBoolean(false);
        AtomicBoolean performed = new AtomicBoolean(false);

        Assertions.assertThrows(
            IllegalStateException.class,
            () -> sampleStatusFlow
                .id(1L)
                .fromStatus(SampleStatus.NEW)
                .successStatus(SampleStatus.DONE)
                .longRunnigOperation(() -> {
                    initialOperationsPerformed.set(true);
                    return true;
                })
                .performTransactionalResultSaving(v -> {
                    performed.set(v);
                    return 0;
                })
        );

        Assertions.assertTrue(initialOperationsPerformed.get());
        Assertions.assertTrue(performed.get());

        verify(statusFlowMapper).getStatus(eq(1L));
        verify(statusFlowMapper).switchStatusReturningCount(
            eq(1L),
            eq(SampleStatus.NEW),
            eq(SampleStatus.DONE)
        );
        verifyNoMoreInteractions(statusFlowMapper);
    }

    @DisplayName("Эксепшн при смене статуса")
    @Test
    public void exceptionOnStatusChange() {
        when(statusFlowMapper.getStatus(1L)).thenReturn(SampleStatus.NEW);
        when(statusFlowMapper.switchStatusReturningCount(1L, SampleStatus.NEW, SampleStatus.DONE))
            .thenThrow(RuntimeSqlException.class);

        AtomicBoolean initialOperationsPerformed = new AtomicBoolean(false);
        AtomicBoolean performed = new AtomicBoolean(false);

        Assertions.assertThrows(
            RuntimeSqlException.class,
            () -> sampleStatusFlow
                .id(1L)
                .fromStatus(SampleStatus.NEW)
                .successStatus(SampleStatus.DONE)
                .longRunnigOperation(() -> {
                    initialOperationsPerformed.set(true);
                    return true;
                })
                .performTransactionalResultSaving(v -> {
                        performed.set(v);
                        return 0;
                    }
                )
        );

        Assertions.assertTrue(initialOperationsPerformed.get());
        Assertions.assertTrue(performed.get());

        verify(statusFlowMapper).getStatus(eq(1L));
        verify(statusFlowMapper).switchStatusReturningCount(
            eq(1L),
            eq(SampleStatus.NEW),
            eq(SampleStatus.DONE)
        );
        verifyNoMoreInteractions(statusFlowMapper);
    }

    @DisplayName("Эксепшн при смене статуса с промежуточным статусом")
    @Test
    public void exceptionOnStatusChangeWithProcessingStatus() {
        when(statusFlowMapper.getStatus(1L)).thenReturn(SampleStatus.NEW);
        when(statusFlowMapper.switchStatusReturningCount(1L, SampleStatus.NEW, SampleStatus.PROCESSING))
            .thenReturn(1);
        when(statusFlowMapper.switchStatusReturningCount(1L, SampleStatus.PROCESSING, SampleStatus.DONE))
            .thenThrow(RuntimeSqlException.class);
        when(statusFlowMapper.switchStatusReturningCount(1L, SampleStatus.PROCESSING, SampleStatus.NEW))
            .thenReturn(1);

        AtomicBoolean initialOperationsPerformed = new AtomicBoolean(false);
        AtomicBoolean performed = new AtomicBoolean(false);

        Assertions.assertThrows(
            RuntimeSqlException.class,
            () -> sampleStatusFlow
                .id(1L)
                .fromStatus(SampleStatus.NEW)
                .processingStatus(SampleStatus.PROCESSING)
                .successStatus(SampleStatus.DONE)
                .longRunnigOperation(() -> {
                    initialOperationsPerformed.set(true);
                    return true;
                })
                .performTransactionalResultSaving(v -> {
                        performed.set(v);
                        return 0;
                    }
                )
        );

        Assertions.assertTrue(initialOperationsPerformed.get());
        Assertions.assertTrue(performed.get());

        verify(statusFlowMapper).getStatus(eq(1L));
        verify(statusFlowMapper).switchStatusReturningCount(
            eq(1L),
            eq(SampleStatus.NEW),
            eq(SampleStatus.PROCESSING)
        );
        verify(statusFlowMapper).switchStatusReturningCount(
            eq(1L),
            eq(SampleStatus.PROCESSING),
            eq(SampleStatus.DONE)
        );
        verify(statusFlowMapper).switchStatusReturningCount(
            eq(1L),
            eq(SampleStatus.PROCESSING),
            eq(SampleStatus.NEW)
        );
        verifyNoMoreInteractions(statusFlowMapper);
    }

    @DisplayName("Эксепшн при существующей транзакции")
    @Test
    public void existingTx() {
        when(statusFlowMapper.getStatus(1L)).thenReturn(SampleStatus.NEW);
        when(statusFlowMapper.switchStatusReturningCount(1L, SampleStatus.NEW, SampleStatus.DONE))
            .thenReturn(1);
        when(statusHistoryService.supports(SampleEntity.class)).thenReturn(true);
        AtomicBoolean initialOperationsPerformed = new AtomicBoolean(false);
        AtomicBoolean savingPerformed = new AtomicBoolean(false);

        TransactionSynchronizationManager.setActualTransactionActive(true);

        Assertions.assertThrows(
            IllegalStateException.class,
            () -> sampleStatusFlow
                .id(1L)
                .fromStatus(SampleStatus.NEW)
                .successStatus(SampleStatus.DONE)
                .longRunnigOperation(() -> {
                    initialOperationsPerformed.set(true);
                    return true;
                })
                .performTransactionalResultSaving(v -> {
                    savingPerformed.set(v);
                    return 0;
                })
        );

        Assertions.assertFalse(initialOperationsPerformed.get());
        Assertions.assertFalse(savingPerformed.get());
    }

    @DisplayName("Проверка на заполнение поля subtype для записи истории у TransportationUnit")
    @Test
    public void checkUnitSubtype() {
        mockMapper(
            unitMapper, TransportationUnit.class, ID, TransportationUnitStatus.NEW, TransportationUnitStatus.SENT
        );

        TransportationUnit outbound = new TransportationUnit().setId(ID).setType(TransportationUnitType.OUTBOUND);

        statusFlowFactory.transportationUnit(outbound)
            .fromStatus(TransportationUnitStatus.NEW)
            .successStatus(TransportationUnitStatus.SENT)
            .perform();

        verify(statusHistoryService).supports(TransportationUnit.class);
        verify(statusHistoryService).submitStatusChangeInfo(
            ID, TransportationUnit.class, "OUTBOUND", TransportationUnitStatus.SENT, TransportationUnitStatus.NEW
        );
    }

    @DisplayName("Проверка на заполнение поля subtype для записи истории у Register")
    @Test
    public void checkRegisterSubtype() {
        mockMapper(
            registerMapper, Register.class, ID, RegisterStatus.NEW, RegisterStatus.SENT
        );

        Register register = new Register().setId(ID).setType(RegisterType.PLAN);

        statusFlowFactory.register(register)
            .fromStatus(RegisterStatus.NEW)
            .successStatus(RegisterStatus.SENT)
            .perform();

        verify(statusHistoryService).supports(Register.class);
        verify(statusHistoryService).submitStatusChangeInfo(
            1L, Register.class, "PLAN", RegisterStatus.SENT, RegisterStatus.NEW
        );
    }

    private <S extends Status> void mockMapper(StatusFlowMapper<S> mapper, Class<?> clazz, long id, S from, S to) {
        when(mapper.getStatus(id)).thenReturn(from);
        when(mapper.switchStatusReturningCount(id, from, to))
            .thenReturn(1);
        when(statusHistoryService.supports(clazz)).thenReturn(true);
    }

    static class SampleEntity implements HasStatus<SampleStatus> {
        private final SampleStatus status = SampleStatus.NEW;

        public SampleStatus getStatus() {
            return status;
        }
    }

    enum SampleStatus implements Status {
        NEW,
        PROCESSING,
        DONE,
    }
}
