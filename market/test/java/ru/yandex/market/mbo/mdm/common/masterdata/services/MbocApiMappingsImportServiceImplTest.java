package ru.yandex.market.mbo.mdm.common.masterdata.services;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.mbo.mdm.common.masterdata.model.MappingCacheDao;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.http.MboMappings;
import ru.yandex.market.mboc.http.MboMappingsService;
import ru.yandex.market.mboc.http.SupplierOffer;

@RunWith(MockitoJUnitRunner.class)
public class MbocApiMappingsImportServiceImplTest {
    private static final ShopSkuKey KEY = new ShopSkuKey(32534, "dfsg");
    private static final long MSKU = 70056435L;
    private static final Instant TIMESTAMP = Instant.now();
    private MbocApiMappingsImportServiceImpl service;
    @Mock
    private MboMappingsService mboMappingsService;

    @Before
    public void setup() {
        service = new MbocApiMappingsImportServiceImpl(
            mboMappingsService, null, null, null, null, new SupplierConverterServiceMock(), null
        );
    }

    @Test
    public void whenNoOffersFromKiThenReturnNothing() {
        Mockito.when(mboMappingsService.searchMappingsByMarketSkuId(Mockito.any()))
            .thenReturn(MboMappings.SearchMappingsResponse.newBuilder()
                .build());

        List<MappingCacheDao> found = service.getOffersFromMbocApi(Set.of(MSKU));
        Assertions.assertThat(found).isEmpty();
    }

    @Test
    public void whenNoMappingsFromKiThenReturnNothing() {
        expectFromMboc(mboMappingsService, offer(null, null, null));

        List<MappingCacheDao> found = service.getOffersFromMbocApi(Set.of(MSKU));
        Assertions.assertThat(found).isEmpty();
    }

    @Test
    public void whenUnsupportedMappingsFromKiThenReturnNothing() {
        expectFromMboc(mboMappingsService, offer(null, null, mapping(MSKU))); // supplier mapping

        List<MappingCacheDao> found = service.getOffersFromMbocApi(Set.of(MSKU));
        Assertions.assertThat(found).isEmpty();
    }

    @Test
    public void whenSuggestOnlyWithRequestedMskuThenReturnIt() {
        expectFromMboc(mboMappingsService, offer(null, mapping(MSKU), null)); // suggest mapping

        List<MappingCacheDao> found = service.getOffersFromMbocApi(Set.of(MSKU));
        Assertions.assertThat(found).containsExactlyInAnyOrder(new MappingCacheDao()
            .setMappingKind(MappingCacheDao.MappingKind.SUGGESTED)
            .setShopSkuKey(KEY)
            .setMskuId(MSKU)
            .setCategoryId(0)
            .setMbocTimestamp(TIMESTAMP)
            .setVersionTimestamp(TIMESTAMP)
            .setUpdateStamp(TIMESTAMP.toEpochMilli())
            .setMappingSource(MappingCacheDao.MappingSource.MBOC_API)
        );
    }

    @Test
    public void whenSuggestOnlyWithWrongMskuThenReturnNothing() {
        expectFromMboc(mboMappingsService, offer(null, mapping(3456345), mapping(MSKU))); // supplier ok, suggest isn't

        List<MappingCacheDao> found = service.getOffersFromMbocApi(Set.of(MSKU));
        Assertions.assertThat(found).isEmpty();
    }

    @Test
    public void whenApprovedOnlyWithRequestedMskuThenReturnIt() {
        expectFromMboc(mboMappingsService, offer(mapping(MSKU), null, null)); // approved mapping

        List<MappingCacheDao> found = service.getOffersFromMbocApi(Set.of(MSKU));
        Assertions.assertThat(found).containsExactlyInAnyOrder(new MappingCacheDao()
            .setMappingKind(MappingCacheDao.MappingKind.APPROVED)
            .setShopSkuKey(KEY)
            .setMskuId(MSKU)
            .setCategoryId(0)
            .setMbocTimestamp(TIMESTAMP)
            .setVersionTimestamp(TIMESTAMP)
            .setUpdateStamp(TIMESTAMP.toEpochMilli())
            .setMappingSource(MappingCacheDao.MappingSource.MBOC_API)
        );
    }

    @Test
    public void whenApprovedOnlyWithWrongMskuThenReturnNothing() {
        expectFromMboc(mboMappingsService, offer(mapping(3456345), null, null)); // sugg ok, approved isn't

        List<MappingCacheDao> found = service.getOffersFromMbocApi(Set.of(MSKU));
        Assertions.assertThat(found).isEmpty();
    }

    @Test
    public void whenBothWithRequestedMskuOnUnsupportedThenReturnNothing() {
        expectFromMboc(mboMappingsService, offer(mapping(3456345), mapping(56764745), mapping(MSKU))); // supp ok

        List<MappingCacheDao> found = service.getOffersFromMbocApi(Set.of(MSKU));
        Assertions.assertThat(found).isEmpty();
    }

    @Test
    public void whenBothWithRequestedMskuOnSuggestThenReturnNothing() {
        expectFromMboc(mboMappingsService, offer(mapping(3456345), mapping(MSKU), null)); // sugg ok, approved isn't

        List<MappingCacheDao> found = service.getOffersFromMbocApi(Set.of(MSKU));
        Assertions.assertThat(found).isEmpty();
    }

    @Test
    public void whenBothWithRequestedMskuOnApprovedThenReturnApproved() {
        expectFromMboc(mboMappingsService, offer(mapping(MSKU), mapping(5343634), mapping(654556))); // approved is ok

        List<MappingCacheDao> found = service.getOffersFromMbocApi(Set.of(MSKU));
        Assertions.assertThat(found).containsExactlyInAnyOrder(new MappingCacheDao()
            .setMappingKind(MappingCacheDao.MappingKind.APPROVED)
            .setShopSkuKey(KEY)
            .setMskuId(MSKU)
            .setCategoryId(0)
            .setMbocTimestamp(TIMESTAMP)
            .setVersionTimestamp(TIMESTAMP)
            .setUpdateStamp(TIMESTAMP.toEpochMilli())
            .setMappingSource(MappingCacheDao.MappingSource.MBOC_API)
        );
    }

    private static void expectFromMboc(MboMappingsService mboMappingsService, SupplierOffer.Offer offer) {
        Mockito.when(mboMappingsService.searchMappingsByMarketSkuId(Mockito.any()))
            .thenReturn(MboMappings.SearchMappingsResponse.newBuilder()
                .addOffers(offer)
                .build());
    }

    private static SupplierOffer.Mapping mapping(long mskuId) {
        return SupplierOffer.Mapping.newBuilder()
            .setCategoryId(0L)
            .setSkuId(mskuId)
            .setTimestamp(TIMESTAMP.toEpochMilli())
            .build();
    }

    private static SupplierOffer.Offer offer(
        @Nullable SupplierOffer.Mapping approved,
        @Nullable SupplierOffer.Mapping suggested,
        @Nullable SupplierOffer.Mapping supplier
    ) {
        var builder = SupplierOffer.Offer.newBuilder();
        if (approved != null) {
            builder.setApprovedMapping(approved);
        }
        if (suggested != null) {
            builder.setSuggestMapping(suggested);
        }
        if (supplier != null) {
            builder.setSupplierMapping(supplier);
        }
        builder.setSupplierId(KEY.getSupplierId());
        builder.setShopSkuId(KEY.getShopSku());
        return builder.build();
    }
}
