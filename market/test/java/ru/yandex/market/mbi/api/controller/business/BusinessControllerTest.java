package ru.yandex.market.mbi.api.controller.business;

import Market.DataCamp.DataCampPromo;
import Market.DataCamp.SyncAPI.SyncGetPromo;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.offer.mapping.MboMappingServiceException;
import ru.yandex.market.mbi.api.client.entity.business.BusinessDto;
import ru.yandex.market.mbi.api.client.entity.business.BusinessListDto;
import ru.yandex.market.mbi.api.client.entity.business.CanMigrateVerdictDTO;
import ru.yandex.market.mbi.api.config.FunctionalTest;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.mbi.datacamp.stroller.model.PromoDatacampRequest;
import ru.yandex.market.mboc.http.MboMappings;
import ru.yandex.market.mboc.http.MboMappingsService;
import ru.yandex.market.mboc.http.SupplierOffer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Тесты для {@link ru.yandex.market.mbi.api.controller.BusinessController}
 *
 * @author Vadim Lyalin
 */
@DbUnitDataSet(before = "BusinessControllerTest.before.csv")
public class BusinessControllerTest extends FunctionalTest {

    @Autowired
    private MboMappingsService mboMappingsService;

    @Autowired
    @Qualifier("dataCampShopClient")
    private DataCampClient dataCampShopClient;


    @Test
    @DisplayName("Тест для ручки /business.")
    void testGetAllBusinesses() {
        BusinessListDto businesses = mbiApiClient.getAllBusinesses();
        assertThat(businesses.getBusinesses(),
                containsInAnyOrder(new BusinessDto(100, "Ромашка"), new BusinessDto(101, "Одуванчик"),
                        new BusinessDto(102, "Клевер"), new BusinessDto(103, "Люцерна")));
    }

    @Test
    @DisplayName("Тест для ручки /business/search.")
    void testGetBusinesses() {
        BusinessListDto businesses = mbiApiClient.getBusinesses(100500L, "рома", 5, null);
        assertThat(businesses.getBusinesses(),
                containsInAnyOrder(new BusinessDto(100, "Ромашка")));
    }

    @Deprecated
    @Test
    @DisplayName("Тест для ручки /business/search. Без оптимизации про пустой контакт")
    @DbUnitDataSet(before = "BusinessControllerTest.disabledEmptyContact.before.csv")
    void testGetBusinessesOld() {
        BusinessListDto businesses = mbiApiClient.getBusinesses(100500L, "рома", 5, null);
        assertThat(businesses.getBusinesses(),
                containsInAnyOrder(new BusinessDto(100, "Ромашка")));
    }

    @Test
    @DisplayName("Тест для ручки /business/{businessId}.")
    void testGetBusiness() {
        BusinessDto businesses = mbiApiClient.getBusiness(100);
        assertThat(businesses, equalTo(new BusinessDto(100, "Ромашка")));

        // несуществующий бизнес
        assertThrows(HttpClientErrorException.NotFound.class, () -> mbiApiClient.getBusiness(1));
    }

    @Test
    @DisplayName("Миграция не возможна. Причина: партнер не является магазином.")
    void canMigrate_no_partnerIsNotShop() {
        checkCanMigrate(105, 101, false, "Партнёр (105) не является магазином!");
    }

    @Test
    @DisplayName("Миграция не возможна. Причина: магазин не в ЕКате.")
    void canMigrate_no_shopIdNotInUC() {
        checkCanMigrate(104, 101, false, "Партнер (104) не в едином каталоге!");
    }

    @Test
    @Disabled
    @DisplayName("Миграция не возможна. Причина: один или оба бизнеса не существуют.")
    @DbUnitDataSet(before = "BusinessControllerTest.canMigrate.withMBOMappings.true.csv")
    void canMigrate_no_businessesNotExist() {
        checkCanMigrate(103, 105, false, "Один или несколько бизнесов не существуют [100, 105] !");
    }

    @Test
    @DisplayName("Миграция не возможна. Причина: один или оба бизнеса залочены.")
    void canMigrate_no_businessesLocked() {
        checkCanMigrate(103, 102, false, "Один или несколько бизнесов заблокированы другой миграцией [100, 102] !");
    }

    @Test
    @DisplayName("Миграция не возможна. " +
            "Причина: целевой бизнес задействован в другой незавершенной операции c другим partnerId."
    )
    @DbUnitDataSet(before = "BusinessControllerTest.canMigrate.businessEngagedInOtherNotFinishedMigration.fail.csv")
    void canMigrate_no_businessEngagedInOtherNotFinishedOperation() {
        checkCanMigrate(103, 101, false, "Бизнес (101) участвует в другой операции (e.g. миграции)!");
    }

    @Test
    @DisplayName("Миграция не возможна. Причина: есть маппинги в МБО.")
    void canMigrate_no_mappingsInMboExist() {
        when(mboMappingsService.searchOfferProcessingStatusesByShopId(any()))
                .thenReturn(MboMappings.SearchOfferProcessingStatusesResponse
                        .newBuilder()
                        .addOfferProcessingStatuses(
                                MboMappings.SearchOfferProcessingStatusesResponse.OfferProcessingStatusInfo
                                        .newBuilder()
                                        .setOfferProcessingStatus(SupplierOffer.OfferProcessingStatus.READY)
                                        .build())
                        .build());
        checkCanMigrate(103, 101, false, "Уже существует маппинг в MBO для shopId: 103!");
    }

    @Test
    @DisplayName("Миграция не возможна. Причина: ошибка при запросе маппингов из МБО.")
    void canMigrate_no_exceptionWhenRequestingMbo() throws MboMappingServiceException {
        when(mboMappingsService.searchOfferProcessingStatusesByShopId(any()))
                .thenReturn(MboMappings.SearchOfferProcessingStatusesResponse
                        .newBuilder()
                        .setStatus(MboMappings.SearchOfferProcessingStatusesResponse.Status.ERROR)
                        .build());
        checkCanMigrate(103, 101, false, "Исключение при запросе маппингов от MBO!");
    }

    @Test
    @DisplayName("Миграция не возможна. Последняя миграция была совсем недавно.")
    @DbUnitDataSet(before = {
            "BusinessControllerTest.canMigrateTime.fail.before.csv",
            "BusinessControllerTest.canMigrate.withPromos.true.csv"
    })
    void canMigrate_no_lessThanNMinutesAfterLastMigration() throws MboMappingServiceException {
        when(mboMappingsService.searchOfferProcessingStatusesByShopId(any()))
                .thenReturn(MboMappings.SearchOfferProcessingStatusesResponse
                        .newBuilder()
                        .setStatus(MboMappings.SearchOfferProcessingStatusesResponse.Status.OK)
                        .build());
        checkCanMigrate(103, 101, false, "Последняя миграция закончилась менее 10 минут назад!");
    }

    @Test
    @DisplayName("Миграция не возможна. У партнера есть промоакции.")
    @DbUnitDataSet(before = "BusinessControllerTest.canMigrate.withPromos.false.csv")
    void canMigrate_no_partnerBusinessHavePromos() {
        when(dataCampShopClient.getPromos(any(PromoDatacampRequest.class)))
                .thenReturn(SyncGetPromo.GetPromoBatchResponse.newBuilder()
                        .setPromos(DataCampPromo.PromoDescriptionBatch.newBuilder()
                                .addPromo(DataCampPromo.PromoDescription.newBuilder().build()).build()
                        )
                        .build()
                );
        checkCanMigrate(103, 101, false,
                "У партнера (103) в исходном бизнесе (100) есть промоакции в ЕОХе!"
        );
    }

    @Test
    @DisplayName("Миграция возможна. Последняя миграция была совсем давно.")
    @DbUnitDataSet(before = {
            "BusinessControllerTest.canMigrateTime.success.before.csv",
            "BusinessControllerTest.canMigrate.withPromos.true.csv"
    })
    void canMigrate_yes_lessThanNMinutesAfterLastMigration() throws MboMappingServiceException {
        when(mboMappingsService.searchOfferProcessingStatusesByShopId(any()))
                .thenReturn(MboMappings.SearchOfferProcessingStatusesResponse
                        .newBuilder()
                        .setStatus(MboMappings.SearchOfferProcessingStatusesResponse.Status.OK)
                        .build());
        checkCanMigrate(103, 101, true, null);
    }

    @Test
    @DisplayName("Миграция возможна. " +
            "Есть маппинги в МБО, но флаг business.migration.canMigrate.withMboMappings = true.")
    @DbUnitDataSet(before = {
            "BusinessControllerTest.canMigrate.withMBOMappings.true.csv",
            "BusinessControllerTest.canMigrate.withPromos.true.csv"
    })
    void canMigrate_yes_mappingsInMboExist_truenegative() {
        when(mboMappingsService.searchOfferProcessingStatusesByShopId(any()))
                .thenReturn(MboMappings.SearchOfferProcessingStatusesResponse
                        .newBuilder()
                        .addOfferProcessingStatuses(
                                MboMappings.SearchOfferProcessingStatusesResponse.OfferProcessingStatusInfo
                                        .newBuilder()
                                        .setOfferProcessingStatus(SupplierOffer.OfferProcessingStatus.READY)
                                        .build())
                        .build());
        checkCanMigrate(103, 101, true, null);
    }

    @Test
    @DisplayName("Миграция возможна. " + "Синим в ЕКате можно между бизнесами мигрировать, " +
            "если флаг business.migration.canMigrate.withMboMappings = true.")
    @DbUnitDataSet(before = {
            "BusinessControllerTest.canMigrate.withMBOMappings.true.csv",
            "BusinessControllerTest.canMigrate.withPromos.true.csv"
    })
    void canMigrateSupplierInUCat_withMboMappingsIsTrue_truenegative() {
        when(mboMappingsService.searchOfferProcessingStatusesByShopId(any()))
                .thenReturn(MboMappings.SearchOfferProcessingStatusesResponse
                        .newBuilder()
                        .setStatus(MboMappings.SearchOfferProcessingStatusesResponse.Status.OK)
                        .build());
        checkCanMigrate(105, 101, true, null);
    }


    @Test
    @DisplayName("Миграция возможна.")
    @DbUnitDataSet(before = {
            "BusinessControllerTest.canMigrate.withPromos.true.csv",
            "BusinessControllerTest.canMigrate.withMBOMappings.true.csv"
    })
    void canMigrate_yes() {
        checkCanMigrate(103, 101, true, null);
    }

    void checkCanMigrate(long shopId, long dstBusinessId, boolean expectedCanMigrate,
                         String expectedReason) {
        CanMigrateVerdictDTO expected = new CanMigrateVerdictDTO(expectedCanMigrate, expectedReason);
        CanMigrateVerdictDTO actual = mbiApiClient.canMigrate(shopId, 100, dstBusinessId);
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Миграция не возможна. Магазин уже в миграции.")
    @DbUnitDataSet(before = "BusinessControllerTest.canMigrateTime.alreadyInMigration.fail.before.csv")
    void canMigrate_no_shopAlreadyInMigration() throws MboMappingServiceException {
        when(mboMappingsService.searchOfferProcessingStatusesByShopId(any()))
                .thenReturn(MboMappings.SearchOfferProcessingStatusesResponse
                        .newBuilder()
                        .setStatus(MboMappings.SearchOfferProcessingStatusesResponse.Status.OK)
                        .build());
        checkCanMigrate(103, 101, false, "Партнер (103) уже находится в миграции!");
    }
}
