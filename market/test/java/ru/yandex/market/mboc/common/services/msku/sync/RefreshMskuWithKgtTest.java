package ru.yandex.market.mboc.common.services.msku.sync;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;

import javax.annotation.Resource;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.MboAudit;
import ru.yandex.market.mbo.http.MboAudit.EntityType;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.availability.msku.MskuRepository;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.Msku;
import ru.yandex.market.mboc.common.msku.CargoType;
import ru.yandex.market.mboc.common.msku.KnownMboParams;
import ru.yandex.market.mboc.common.msku.TestUtils;
import ru.yandex.market.mboc.common.offers.repository.MboAuditServiceMock;
import ru.yandex.market.mboc.common.repo.bindings.proto.ByteArrayToModelStorageParameterValueConverter;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.mbo.http.MboAudit.EntityType.MODEL_PARAM;
import static ru.yandex.market.mbo.http.MboAudit.EntityType.PARTNER_PARAM;
import static ru.yandex.market.mbo.http.MboAudit.EntityType.PARTNER_SKU_PARAM;
import static ru.yandex.market.mbo.http.MboAudit.EntityType.SKU_PARAM;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @since 15.11.2019
 */
@SuppressWarnings("checkstyle:magicnumber")
public class RefreshMskuWithKgtTest extends BaseDbTestClass {

    @Autowired
    private MskuRepository mskuRepository;
    @Resource
    private StorageKeyValueService storageKeyValueService;

    private ImportMskuService importMskuService;
    private MboAuditServiceMock mboAuditService;
    private RefreshMskuWithKgt refreshMskuWithKgt;

    private static Msku msku(long mskuId, Long... cargoTypes) {
        return TestUtils.newMsku(mskuId)
            .setCargoTypeLmsIds(cargoTypes);
    }

    private static Msku msku(long mskuId, List<ModelStorage.ParameterValue> values, Long... cargoTypes) {
        return TestUtils.newMsku(mskuId)
            .setCargoTypeLmsIds(cargoTypes)
            .setParameterValuesProto(ByteArrayToModelStorageParameterValueConverter.INSTANCE.to(values));
    }

    private static ModelStorage.ParameterValue bool(String xslName, boolean value) {
        ModelStorage.ParameterValue.Builder builder = ModelStorage.ParameterValue.newBuilder();
        builder.setParamId(1);
        builder.setXslName(xslName);
        builder.setTypeId(ModelStorage.ParameterValueType.BOOLEAN_VALUE);
        builder.setValueType(MboParameters.ValueType.BOOLEAN);
        builder.setModificationDate(System.currentTimeMillis());
        builder.setBoolValue(value);
        return builder.build();
    }

    private static MboAudit.MboAction action(EntityType type, long mskuId, CargoType cargoType, boolean value,
                                             Instant date) {
        return action(type, mskuId, cargoType.mboXslName(), value, date);
    }

    private static MboAudit.MboAction action(EntityType type, long mskuId, String cargoType, boolean value,
                                             Instant date) {
        return MboAudit.MboAction.newBuilder()
            .setEntityId(mskuId)
            .setActionType(MboAudit.ActionType.UPDATE)
            .setEntityType(type)
            .setNewValue(String.valueOf(value))
            .setPropertyName(cargoType)
            .setDate(date.toEpochMilli())
            .build();
    }

    private static MboAudit.WriteActionsRequest request(MboAudit.MboAction... action) {
        return MboAudit.WriteActionsRequest.newBuilder()
            .addAllActions(List.of(action))
            .build();
    }

    @Before
    public void setUp() {
        importMskuService = Mockito.mock(ImportMskuService.class);
        mboAuditService = new MboAuditServiceMock();
        refreshMskuWithKgt = new RefreshMskuWithKgt(
            importMskuService,
            mboAuditService,
            storageKeyValueService,
            mskuRepository
        );
    }

    @Test
    public void emptyRun() {
        refreshMskuWithKgt.run();
    }

    @Test
    public void insertNew() {
        Instant start = Instant.now().minus(1, ChronoUnit.DAYS);
        storageKeyValueService.putValue(RefreshMskuWithKgt.LAST_SYNCHRONIZED_TS, start);

        mboAuditService.writeActions(request(
            action(MODEL_PARAM, 101, CargoType.HEAVY_GOOD, true, start.plus(1, ChronoUnit.MINUTES)),
            action(MODEL_PARAM, 102, CargoType.HEAVY_GOOD, false, start.plus(2, ChronoUnit.MINUTES))
        ));

        refreshMskuWithKgt.run();

        Collection<Long> mskuToRefresh = getMskuToRefresh();
        assertThat(mskuToRefresh).containsExactlyInAnyOrder(101L, 102L);
    }

    @Test
    public void insertNewExpirDate() {
        Instant start = Instant.now().minus(1, ChronoUnit.DAYS);
        storageKeyValueService.putValue(RefreshMskuWithKgt.LAST_SYNCHRONIZED_TS, start);

        mboAuditService.writeActions(request(
            action(MODEL_PARAM, 101, KnownMboParams.EXPIR_DATE.mboXslName(), true, start.plus(1, ChronoUnit.MINUTES)),
            action(MODEL_PARAM, 102, KnownMboParams.EXPIR_DATE.mboXslName(), false, start.plus(2, ChronoUnit.MINUTES))
        ));

        refreshMskuWithKgt.run();

        Collection<Long> mskuToRefresh = getMskuToRefresh();
        assertThat(mskuToRefresh).containsExactlyInAnyOrder(101L, 102L);
    }

    @Test
    public void batching() {
        Instant start = Instant.now().minus(1, ChronoUnit.DAYS);
        storageKeyValueService.putValue(RefreshMskuWithKgt.LAST_SYNCHRONIZED_TS, start);

        mboAuditService.writeActions(request(
            action(MODEL_PARAM, 101, CargoType.HEAVY_GOOD, true, start.plus(1, ChronoUnit.MINUTES)),
            action(SKU_PARAM, 102, CargoType.HEAVY_GOOD, false, start.plus(2, ChronoUnit.MINUTES)),
            action(PARTNER_PARAM, 103, CargoType.HEAVY_GOOD, true, start.plus(3, ChronoUnit.MINUTES)),
            action(PARTNER_SKU_PARAM, 104, CargoType.HEAVY_GOOD, false, start.plus(4, ChronoUnit.MINUTES)),
            action(MODEL_PARAM, 105, CargoType.HEAVY_GOOD, true, start.plus(7, ChronoUnit.MINUTES))
        ));

        refreshMskuWithKgt.setBatchSize(3);
        refreshMskuWithKgt.run();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<Long>> syncRequest = ArgumentCaptor.forClass(Collection.class);
        verify(importMskuService, times(2)).syncWithMbo(syncRequest.capture());

        assertThat(syncRequest.getAllValues()).hasSize(2);
        assertThat(syncRequest.getAllValues().get(0)).containsOnly(101L, 102L, 103L);
        assertThat(syncRequest.getAllValues().get(1)).containsOnly(104L, 105L);
    }

    @Test
    public void collectChangesFromManyTypes() {
        Instant start = Instant.now().minus(1, ChronoUnit.DAYS);
        storageKeyValueService.putValue(RefreshMskuWithKgt.LAST_SYNCHRONIZED_TS, start);

        mboAuditService.writeActions(request(
            action(MODEL_PARAM, 101, CargoType.HEAVY_GOOD, true, start.plus(1, ChronoUnit.MINUTES)),
            action(SKU_PARAM, 102, CargoType.HEAVY_GOOD, false, start.plus(2, ChronoUnit.MINUTES)),
            action(PARTNER_PARAM, 103, CargoType.HEAVY_GOOD, true, start.plus(3, ChronoUnit.MINUTES)),
            action(PARTNER_SKU_PARAM, 104, CargoType.HEAVY_GOOD, false, start.plus(4, ChronoUnit.MINUTES))
        ));

        refreshMskuWithKgt.run();

        Collection<Long> mskuToRefresh = getMskuToRefresh();
        assertThat(mskuToRefresh).containsExactlyInAnyOrder(101L, 102L, 103L, 104L);
    }

    @Test
    public void notSyncIfNotChanged() {
        Instant start = Instant.now().minus(1, ChronoUnit.DAYS);
        storageKeyValueService.putValue(RefreshMskuWithKgt.LAST_SYNCHRONIZED_TS, start);

        mboAuditService.writeActions(request(
            action(MODEL_PARAM, 101, CargoType.HEAVY_GOOD, true, start.plus(1, ChronoUnit.MINUTES)),
            action(MODEL_PARAM, 102, CargoType.HEAVY_GOOD, false, start.plus(2, ChronoUnit.MINUTES)),
            action(MODEL_PARAM, 103, KnownMboParams.EXPIR_DATE.mboXslName(), false, start.plus(2, ChronoUnit.MINUTES))
        ));

        mskuRepository.save(
            msku(101, List.of(bool(KnownMboParams.EXPIR_DATE.mboXslName(), false)), CargoType.HEAVY_GOOD.lmsId()),
            msku(102, List.of(bool(KnownMboParams.EXPIR_DATE.mboXslName(), false)), CargoType.HEAVY_GOOD.lmsId()),
            msku(103, List.of(bool(KnownMboParams.EXPIR_DATE.mboXslName(), false)), CargoType.HEAVY_GOOD.lmsId())
        );

        refreshMskuWithKgt.run();

        Collection<Long> mskuToRefresh = getMskuToRefresh();
        assertThat(mskuToRefresh).containsExactlyInAnyOrder(102L);
    }

    @Test
    public void useLastModifiedTime() {
        Instant start = Instant.now().minus(1, ChronoUnit.DAYS);
        storageKeyValueService.putValue(RefreshMskuWithKgt.LAST_SYNCHRONIZED_TS, start);

        mboAuditService.writeActions(request(
            action(MODEL_PARAM, 101, CargoType.HEAVY_GOOD, true, start.plus(1, ChronoUnit.MINUTES)),
            action(MODEL_PARAM, 102, CargoType.HEAVY_GOOD, false, start.plus(2, ChronoUnit.MINUTES))
        ));

        refreshMskuWithKgt.run();

        mskuRepository.save(
            msku(102)
        );

        mboAuditService.writeActions(request(
            action(MODEL_PARAM, 103, KnownMboParams.EXPIR_DATE.mboXslName(), false, start.plus(2, ChronoUnit.MINUTES)),
            action(MODEL_PARAM, 104, CargoType.HEAVY_GOOD, false, start.plus(3, ChronoUnit.MINUTES))
        ));

        refreshMskuWithKgt.run();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<Long>> syncRequest = ArgumentCaptor.forClass(Collection.class);
        verify(importMskuService, times(2)).syncWithMbo(syncRequest.capture());

        List<Collection<Long>> refreshIds = syncRequest.getAllValues();
        assertThat(refreshIds.get(0)).containsExactlyInAnyOrder(101L, 102L);
        assertThat(refreshIds.get(1)).containsExactlyInAnyOrder(103L, 104L);
    }

    @Test
    public void differentProperties() {
        Instant start = Instant.now().minus(1, ChronoUnit.DAYS);
        storageKeyValueService.putValue(RefreshMskuWithKgt.LAST_SYNCHRONIZED_TS, start);

        mboAuditService.writeActions(request(
            action(SKU_PARAM, 101, CargoType.HEAVY_GOOD, true, start.plus(1, ChronoUnit.MINUTES)),
            action(SKU_PARAM, 102, CargoType.HEAVY_GOOD, false, start.plus(2, ChronoUnit.MINUTES)),
            action(SKU_PARAM, 103, CargoType.HEAVY_GOOD20, false, start.plus(2, ChronoUnit.MINUTES)),
            action(SKU_PARAM, 104, CargoType.HEAVY_GOOD20, true, start.plus(3, ChronoUnit.MINUTES)),
            action(SKU_PARAM, 105, "cargoType310", true, start.plus(4, ChronoUnit.MINUTES)),

            action(SKU_PARAM, 1000, CargoType.HEAVY_GOOD, true, start.plus(5, ChronoUnit.MINUTES)),
            action(SKU_PARAM, 1000, CargoType.HEAVY_GOOD20, true, start.plus(5, ChronoUnit.MINUTES)),
            action(SKU_PARAM, 1001, CargoType.HEAVY_GOOD, false, start.plus(6, ChronoUnit.MINUTES)),
            action(SKU_PARAM, 1001, CargoType.HEAVY_GOOD20, true, start.plus(6, ChronoUnit.MINUTES)),
            action(SKU_PARAM, 1002, CargoType.HEAVY_GOOD, true, start.plus(7, ChronoUnit.MINUTES)),
            action(SKU_PARAM, 1002, CargoType.HEAVY_GOOD20, false, start.plus(7, ChronoUnit.MINUTES)),
            action(SKU_PARAM, 1003, CargoType.HEAVY_GOOD, false, start.plus(8, ChronoUnit.MINUTES)),
            action(SKU_PARAM, 1003, CargoType.HEAVY_GOOD20, false, start.plus(8, ChronoUnit.MINUTES)),

            action(SKU_PARAM, 1010, CargoType.HEAVY_GOOD, true, start.plus(9, ChronoUnit.MINUTES)),
            action(SKU_PARAM, 1010, CargoType.HEAVY_GOOD20, true, start.plus(9, ChronoUnit.MINUTES)),
            action(SKU_PARAM, 1010, CargoType.HEAVY_GOOD, false, start.plus(10, ChronoUnit.MINUTES)),
            action(SKU_PARAM, 1010, CargoType.HEAVY_GOOD20, false, start.plus(10, ChronoUnit.MINUTES)),
            action(SKU_PARAM, 1010, KnownMboParams.EXPIR_DATE.mboXslName(), false, start.plus(10, ChronoUnit.MINUTES))
        ));

        mskuRepository.save(
            msku(101L),
            msku(102L),
            msku(103L),
            msku(104L),
            msku(105L),
            msku(1000L),
            msku(1001L),
            msku(1002L),
            msku(1003L),
            msku(1010L, List.of(bool(KnownMboParams.EXPIR_DATE.mboXslName(), false)))
        );

        refreshMskuWithKgt.run();

        Collection<Long> mskuToRefresh = getMskuToRefresh();
        assertThat(mskuToRefresh).containsExactlyInAnyOrder(101L, 104L, 1000L, 1001L, 1002L);
    }

    @Test
    public void nothingToSyncDoesntCrash() {
        Instant start = Instant.now().minus(1, ChronoUnit.DAYS);
        storageKeyValueService.putValue(RefreshMskuWithKgt.LAST_SYNCHRONIZED_TS, start);

        mboAuditService.writeActions(request(
            action(MODEL_PARAM, 101, CargoType.HEAVY_GOOD, true, start.plus(1, ChronoUnit.MINUTES))
        ));

        mskuRepository.save(
            msku(101, CargoType.HEAVY_GOOD.lmsId())
        );

        refreshMskuWithKgt.run();

        Collection<Long> mskuToRefresh = getMskuToRefresh();
        assertThat(mskuToRefresh).isEmpty();
    }

    @SuppressWarnings("unchecked")
    private Collection<Long> getMskuToRefresh() {
        ArgumentCaptor<Collection<Long>> syncRequest = ArgumentCaptor.forClass(Collection.class);
        verify(importMskuService).syncWithMbo(syncRequest.capture());
        return syncRequest.getValue();
    }
}
