package ru.yandex.market.mboc.common.services.migration.category;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import io.grpc.stub.StreamObserver;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ru.yandex.market.mboc.common.availability.msku.MskuRepository;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.enums.ContentProcessingFreezeStatus;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.enums.MigrationModelChangeSource;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.enums.MigrationModelStatus;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.MigrationModel;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.Msku;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.offers.repository.MigrationModelRepository;
import ru.yandex.market.mboc.http.CategoryMigration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

public class CategoryMigrationServiceImplTest {
    private MigrationModelRepository migrationModelRepository;
    private MskuRepository mskuRepository;

    private CategoryMigrationServiceImpl categoryMigrationService;

    @Before
    public void setUp() {
        migrationModelRepository = Mockito.mock(MigrationModelRepository.class);
        mskuRepository = Mockito.mock(MskuRepository.class);

        var transactionHelperMock = new TransactionHelper() {
            @Override
            public <T> T doInTransaction(TransactionCallback<T> callback) {
                return callback.doInTransaction(null);
            }

            @Override
            public void doInTransactionVoid(Consumer<TransactionStatus> callback) {
                callback.accept(null);
            }
        };

        categoryMigrationService = new CategoryMigrationServiceImpl(
            migrationModelRepository,
            mskuRepository,
            transactionHelperMock
        );
    }

    @Test
    public void lockOk() {
        when(mskuRepository.findAllByModelIds(anyList()))
            .thenReturn(List.of(new Msku()
                    .setDeleted(false)
                    .setParentModelId(1L)
                )
            );
        when(migrationModelRepository.findByIds(anyList()))
            .thenReturn(List.of());
        when(migrationModelRepository.save(anyList()))
            .thenAnswer(pararms -> {
                var migrations = (List<MigrationModel>) pararms.getArgument(0);
                var migrationModel = migrations.get(0);
                assertEquals(migrationModel.getModelId().longValue(), 1L);
                assertEquals(migrationModel.getStatus(), MigrationModelStatus.IN_PROCESS);
                assertEquals(migrationModel.getChangeSource(), MigrationModelChangeSource.ORCHESTRATOR);
                assertEquals(migrationModel.getContentProcessingFreezeStatus(),
                    ContentProcessingFreezeStatus.FORBIDDEN);
                return migrations;
            });

        var streamObserver = getStreamObserverForType(
            CategoryMigration.LockMigrationModelsResponse.newBuilder().build(),
            resp -> assertTrue(resp.getMigrationResultsList().stream()
                .allMatch(migrationModelResult -> migrationModelResult.getStatus()
                    .equals(CategoryMigration.MigrationModelResult.Status.OK)))
        );

        categoryMigrationService.lock(
            CategoryMigration.LockMigrationModelsRequest.newBuilder()
                .setMigrationData(CategoryMigration.MigrationData.newBuilder()
                    .addAllModelIds(List.of(1L))
                    .build())
                .build(),
            streamObserver
        );
    }

    @Test
    public void unlockOk() {
        when(mskuRepository.findAllByModelIds(anyList()))
            .thenReturn(List.of(new Msku()
                    .setDeleted(false)
                    .setParentModelId(1L)
                )
            );
        when(migrationModelRepository.findByModelIds(anyList(), anyBoolean()))
            .thenReturn(List.of(
                new MigrationModel()
                    .setChangeSource(MigrationModelChangeSource.CLASSIFICATION)
                    .setStatus(MigrationModelStatus.IN_PROCESS)
                    .setSourceOfferId(100L)
                    .setId(111L)
                    .setModelId(1L)
                    .setContentProcessingFreezeStatus(ContentProcessingFreezeStatus.FORBIDDEN)
            ));
        when(migrationModelRepository.save(anyList()))
            .thenAnswer(params -> {
                var migrations = (List<MigrationModel>) params.getArgument(0);
                var migrationModel = migrations.get(0);
                assertEquals(migrationModel.getModelId().longValue(), 1L);
                assertEquals(migrationModel.getStatus(), MigrationModelStatus.PROCESSED);
                assertEquals(migrationModel.getContentProcessingFreezeStatus(),
                    ContentProcessingFreezeStatus.FORBIDDEN);
                return migrations;
            });
        // models without resend to ag should not delete
        when(migrationModelRepository.deleteByModelIds(anyCollection()))
            .thenAnswer(params -> {
                var modelIds = (Collection<Long>) params.getArgument(0);
                assertTrue(modelIds.isEmpty());
                return 0;
            });

        var streamObserver = getStreamObserverForType(
            CategoryMigration.UnlockMigrationModelsResponse.newBuilder().build(),
            resp -> assertTrue(resp.getMigrationResultsList().stream()
                .allMatch(migrationModelResult -> migrationModelResult.getStatus()
                    .equals(CategoryMigration.MigrationModelResult.Status.OK)))
        );

        categoryMigrationService.unlock(
            CategoryMigration.UnlockMigrationModelsRequest.newBuilder()
                .setMigrationData(CategoryMigration.MigrationData.newBuilder()
                    .addAllModelIds(List.of(1L))
                    .build())
                .build(),
            streamObserver
        );
    }

    @Test
    public void unlockContentProcessingOk() {
        final var migrationModel = new MigrationModel()
            .setChangeSource(MigrationModelChangeSource.CLASSIFICATION)
            .setStatus(MigrationModelStatus.IN_PROCESS)
            .setSourceOfferId(100L)
            .setId(111L)
            .setModelId(1L)
            .setContentProcessingFreezeStatus(ContentProcessingFreezeStatus.FORBIDDEN);
        when(mskuRepository.findAllByModelIds(anyList()))
            .thenReturn(List.of(new Msku()
                    .setDeleted(false)
                    .setParentModelId(1L)
                )
            );
        when(migrationModelRepository.findByModelIds(anyList(), anyBoolean()))
            .thenReturn(List.of(
                migrationModel
            ));
        when(migrationModelRepository.save(anyList()))
            .thenAnswer(pararms -> {
                var migrations = (List<MigrationModel>) pararms.getArgument(0);
                var migrationModel1 = migrations.get(0);
                assertEquals(migrationModel1.getModelId().longValue(), 1L);
                assertEquals(migrationModel1.getStatus(), MigrationModelStatus.IN_PROCESS);
                assertEquals(migrationModel1.getContentProcessingFreezeStatus(), ContentProcessingFreezeStatus.ALLOWED);
                return migrations;
            });

        var streamObserver = getStreamObserverForType(
            CategoryMigration.UnlockContentProcessingResponse.newBuilder().build(),
            resp -> assertTrue(resp.getMigrationResultsList().stream()
                .allMatch(migrationModelResult -> migrationModelResult.getStatus()
                    .equals(CategoryMigration.MigrationModelResult.Status.OK)))
        );

        categoryMigrationService.unlockContentProcessing(
            CategoryMigration.UnlockContentProcessingRequest.newBuilder()
                .setMigrationData(CategoryMigration.MigrationData.newBuilder()
                    .addAllModelIds(List.of(1L))
                    .build())
                .build(),
            streamObserver
        );
    }

    private <T> StreamObserver<T> getStreamObserverForType(T type, Consumer<T> onNext) {
        return new StreamObserver<>() {
            @Override
            public void onNext(T value) {
                onNext.accept(value);
            }

            @Override
            public void onError(Throwable t) {
                t.printStackTrace();
            }

            @Override
            public void onCompleted() {

            }
        };
    }
}
