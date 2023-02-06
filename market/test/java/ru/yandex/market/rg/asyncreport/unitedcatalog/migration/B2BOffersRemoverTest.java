package ru.yandex.market.rg.asyncreport.unitedcatalog.migration;

import Market.DataCamp.DataCampUnitedOffer;
import Market.DataCamp.SyncAPI.OffersBatch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.ProtoTestUtil;
import ru.yandex.market.core.asyncreport.model.unitedcatalog.CopyOffersParams;
import ru.yandex.market.core.asyncreport.util.ParamsUtils;
import ru.yandex.market.mbi.datacamp.model.search.SearchBusinessOffersResult;
import ru.yandex.market.rg.asyncreport.unitedcatalog.migration.delete.B2BOffersRemover;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.times;

@DbUnitDataSet(before = "B2BOffersRemoverTest.before.csv")
public class B2BOffersRemoverTest extends AbstractMigrationTaskProcessorTest {
    private static final long PARTNER_ID = 101L;
    private static final long ANOTHER_PARTNER_ID = 10957705L;
    private static final long SOURCE_BUSINESS = 99L;
    private static final long TARGET_BUSINESS = 98L;

    B2BOffersRemover b2BOffersRemover;

    static Stream<Arguments> copyOffersParamsArgs() {
        return Stream.of(
                Arguments.of(new CopyOffersParams(SOURCE_BUSINESS, TARGET_BUSINESS, PARTNER_ID), false),
                Arguments.of(new CopyOffersParams(SOURCE_BUSINESS, TARGET_BUSINESS, PARTNER_ID), true) // with retry
        );
    }

    @BeforeEach
    void setUp() {
        if (b2BOffersRemover == null) {
            b2BOffersRemover = new B2BOffersRemover(
                    reportsMdsStorage, dataCampMigrationClient, reportsService, migrationTaskStateService,
                    assortmentReportWriteService, mboMappingService, offerConversionService,
                    clock, environmentService
            );
        }
    }

    @ParameterizedTest
    @MethodSource("copyOffersParamsArgs")
    void removeOnlyService(CopyOffersParams copyOffersParams, boolean retry) {
        //given
        DataCampUnitedOffer.UnitedOffer sourceOffer = ProtoTestUtil.getProtoMessageByJson(
                DataCampUnitedOffer.UnitedOffer.class,
                "UnitedOfferWithTwoServices.json",
                getClass()
        );
        Mockito.when(dataCampMigrationClient.searchBusinessOffers(any()))
                .thenReturn(SearchBusinessOffersResult.builder().setOffers(List.of(sourceOffer)).build())
                .thenReturn(SearchBusinessOffersResult.builder().setOffers(retry ? List.of(sourceOffer) : List.of()).build());
        willReturn(OffersBatch.UnitedOffersBatchResponse.newBuilder().build())
                .given(dataCampMigrationClient).changeBusinessUnitedOffers(any(), any(), any());
        //when
        b2BOffersRemover.generate("1", copyOffersParams);

        //then
        then(dataCampMigrationClient).should(times(2)).searchBusinessOffers(any());
        //noinspection unchecked
        ArgumentCaptor<Collection<DataCampUnitedOffer.UnitedOffer>> captor = ArgumentCaptor.forClass(Collection.class);
        then(dataCampMigrationClient).should().changeBusinessUnitedOffers(eq(SOURCE_BUSINESS), eq(PARTNER_ID),
                captor.capture());
        assertEquals(1, captor.getValue().size());
        DataCampUnitedOffer.UnitedOffer unitedOffer = captor.getValue().iterator().next();
        //не трогали базовую
        assertFalse(unitedOffer.getBasic().getStatus().getRemoved().getFlag());
        //поудаляли сервисные
        assertTrue(unitedOffer.getServiceMap().get((int) PARTNER_ID).getStatus().getRemoved().getFlag());
        assertNull(unitedOffer.getServiceMap().get((int) ANOTHER_PARTNER_ID));
        assertTrue(unitedOffer.getActualMap().get((int) PARTNER_ID)
                .getWarehouseMap().values().stream().allMatch(o -> o.getStatus().getRemoved().getFlag()));
        //передали TS
        assertTrue(unitedOffer.getServiceMap().get((int) PARTNER_ID).getStatus().getRemoved().hasMeta());

        if (retry) {
            //проверяем что в случае если сервисная не удалилась мы сделали ретрай
            Mockito.verify(reportsService, times(1)).requestReportGeneration(any());
        } else {
            Mockito.verify(reportsService, times(0)).requestReportGeneration(any());
        }
    }

    @ParameterizedTest
    @MethodSource("copyOffersParamsArgs")
    void removeServiceWithBasic(CopyOffersParams copyOffersParams, boolean retry) {
        //given
        long partnerId = 101L;
        DataCampUnitedOffer.UnitedOffer sourceOffer = ProtoTestUtil.getProtoMessageByJson(
                DataCampUnitedOffer.UnitedOffer.class,
                "UnitedOfferWithOneService.json",
                getClass()
        );
        Mockito.when(dataCampMigrationClient.searchBusinessOffers(any()))
                .thenReturn(SearchBusinessOffersResult.builder().setOffers(List.of(sourceOffer)).build())
                .thenReturn(SearchBusinessOffersResult.builder().setOffers(retry ? List.of(sourceOffer) : List.of()).build());
        willReturn(OffersBatch.UnitedOffersBatchResponse.newBuilder().build())
                .given(dataCampMigrationClient).changeBusinessUnitedOffers(any(), any(), any());

        //when
        b2BOffersRemover.generate("1", copyOffersParams);

        //then
        then(dataCampMigrationClient).should(times(2)).searchBusinessOffers(any());
        //noinspection unchecked
        ArgumentCaptor<Collection<DataCampUnitedOffer.UnitedOffer>> captor = ArgumentCaptor.forClass(Collection.class);
        then(dataCampMigrationClient).should().changeBusinessUnitedOffers(eq(SOURCE_BUSINESS), eq(partnerId),
                captor.capture());
        assertEquals(1, captor.getValue().size());
        DataCampUnitedOffer.UnitedOffer unitedOffer = captor.getValue().iterator().next();
        //Удалили базовую
        assertTrue(unitedOffer.getBasic().getStatus().getRemoved().getFlag(), "Basic part should be removed");
        //передали TS для базовой
        assertTrue(unitedOffer.getBasic().getStatus().getRemoved().hasMeta());
        //поудаляли сервисные
        assertTrue(unitedOffer.getServiceMap().get((int) partnerId).getStatus().getRemoved().getFlag());
        assertTrue(unitedOffer.getActualMap().get((int) partnerId)
                .getWarehouseMap().values().stream().allMatch(o -> o.getStatus().getRemoved().getFlag()));

        if (retry) {
            //проверяем что в случае если сервисная не удалилась мы сделали ретрай
            Mockito.verify(reportsService, times(1)).requestReportGeneration(any());
        } else {
            Mockito.verify(reportsService, times(0)).requestReportGeneration(any());
        }
    }

    @ParameterizedTest
    @MethodSource("copyOffersParamsArgs")
    @DisplayName("Не трогаем оффер с чужой сервисной")
    void doNotRemoveWithAnotherService(CopyOffersParams copyOffersParams, boolean retry) {
        //given
        long partnerId = 101L;
        DataCampUnitedOffer.UnitedOffer anotherOffer = ProtoTestUtil.getProtoMessageByJson(
                DataCampUnitedOffer.UnitedOffer.class,
                "UnitedOfferWithAnotherService.json",
                getClass()
        );
        Mockito.when(dataCampMigrationClient.searchBusinessOffers(any()))
                .thenReturn(SearchBusinessOffersResult.builder().setOffers(List.of(anotherOffer)).build())
                .thenReturn(SearchBusinessOffersResult.builder().setOffers(List.of()).build());
        willReturn(OffersBatch.UnitedOffersBatchResponse.newBuilder().build())
                .given(dataCampMigrationClient).changeBusinessUnitedOffers(any(), any(), any());

        //when
        b2BOffersRemover.generate("1", copyOffersParams);

        //then
        then(dataCampMigrationClient).should(times(2)).searchBusinessOffers(any());
        //noinspection unchecked
        ArgumentCaptor<Collection<DataCampUnitedOffer.UnitedOffer>> captor = ArgumentCaptor.forClass(Collection.class);
        then(dataCampMigrationClient).should().changeBusinessUnitedOffers(eq(SOURCE_BUSINESS), eq(partnerId),
                captor.capture());
        assertEquals(1, captor.getValue().size());
        DataCampUnitedOffer.UnitedOffer unitedOffer = captor.getValue().iterator().next();
        //Не трогали базовую
        assertFalse(unitedOffer.getBasic().getStatus().getRemoved().getFlag(), "Basic part should be removed");
        //не трогали сервисные
        assertTrue(unitedOffer.getServiceMap()
                .values()
                .stream()
                .noneMatch(service -> service.getStatus().getRemoved().getFlag()));
        assertTrue(unitedOffer.getActualMap()
                .values()
                .stream()
                .flatMap(actual -> actual.getWarehouseMap().values().stream())
                .noneMatch(w -> w.getStatus().getRemoved().getFlag()));

        Mockito.verify(reportsService, times(0)).requestReportGeneration(any());
    }

    @Test
    void testParamsDeserialization() {
        Map<String, Object> params = Map.of("entityId", 100,
                "pendingTimeSec", 100,
                "retryNum", 1,
                "sourceBusinessId", 1000,
                "targetBusinessId", 1000);
        CopyOffersParams copyOffersParams = ParamsUtils.convertToParams(
                params,
                b2BOffersRemover.getParamsType());
        assertEquals(copyOffersParams.getPartnerId(), 100);
        assertEquals(copyOffersParams.getRetryNum(), 1);
        assertEquals(copyOffersParams.getSourceBusinessId(), 1000);
        assertEquals(copyOffersParams.getTargetBusinessId(), 1000);
    }

    @Test
    void testParamsDeserializationWithoutRetries() {
        Map<String, Object> params = Map.of("entityId", 100,
                "pendingTimeSec", 100,
                "sourceBusinessId", 1000,
                "targetBusinessId", 1000);
        CopyOffersParams copyOffersParams = ParamsUtils.convertToParams(
                params,
                b2BOffersRemover.getParamsType());
        assertEquals(copyOffersParams.getPartnerId(), 100);
        assertEquals(copyOffersParams.getRetryNum(), 0);
        assertEquals(copyOffersParams.getSourceBusinessId(), 1000);
        assertEquals(copyOffersParams.getTargetBusinessId(), 1000);
    }
}
