package ru.yandex.market.psku.postprocessor.service.remapper;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.ir.autogeneration_api.http.service.MboMappingsServiceMock;
import ru.yandex.market.mboc.http.MboMappings;
import ru.yandex.market.mboc.http.MboMappingsService;
import ru.yandex.market.mboc.http.SupplierOffer;
import ru.yandex.market.psku.postprocessor.common.BaseDBTest;
import ru.yandex.market.psku.postprocessor.common.db.dao.PskuResultStorageDao;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.PskuStorageState;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.PskuResultStorage;

import java.sql.Timestamp;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

public class RemapperServiceTest extends BaseDBTest {
    private static final String MBOC_TEST_USER = "mbocTestUser";

    private static final int HID = 91491;
    private static final int SUPPLIER_ID = 100;
    private static final String SHOP_SKU1 = "SHOP_SKU1";
    private static final String SHOP_SKU2 = "SHOP_SKU2";
    private static final String SHOP_SKU3 = "SHOP_SKU3";
    private static final String SHOP_SKU4 = "SHOP_SKU4";
    private static final String SHOP_SKU5 = "SHOP_SKU5";
    private static final long EXISTING_MSKU_ID = 200501L;
    private static final long EXISTING_MSKU_ID2 = 200502L;
    private static final long EXISTING_PSKU_ID = 100501L;
    private static final long EXISTING_PSKU_ID3 = 200505L;
    private static final long EXISTING_PSKU_ID4 = 1234567L;
    private static final long NOT_EXISTING_PSKU_ID = 100502L;
    private static final String TEST_USER_LOGIN = "test_user";

    private MboMappingsServiceMock mboMappingsServiceMock = new MboMappingsServiceMock();
    private MboMappingsServiceMock mboMappingsServiceMockSpy = Mockito.spy(mboMappingsServiceMock);
    private RemapperService remapperService;

    @Captor
    ArgumentCaptor<MboMappings.UpdateMappingsRequest> updateMappingsRequestCaptor;

    @Autowired
    private PskuResultStorageDao pskuResultStorageDao;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        remapperService = new RemapperService(pskuResultStorageDao, mboMappingsServiceMock, MBOC_TEST_USER, 100);

        mboMappingsServiceMock.addMapping(HID, SUPPLIER_ID, SHOP_SKU1, EXISTING_PSKU_ID);
        mboMappingsServiceMock.addMapping(HID, SUPPLIER_ID, SHOP_SKU2, NOT_EXISTING_PSKU_ID);
        mboMappingsServiceMock.addMapping(HID, SUPPLIER_ID, SHOP_SKU3, EXISTING_PSKU_ID3);
        mboMappingsServiceMock.addUpdateMappingError(SUPPLIER_ID,
            SHOP_SKU2,
            "ERROR",
            MboMappings.ProviderProductInfoResponse.ErrorKind.SKU_NOT_EXISTS);

        mboMappingsServiceMock.addUpdateMappingError(SUPPLIER_ID,
            SHOP_SKU3,
            "ERROR",
            MboMappings.ProviderProductInfoResponse.ErrorKind.CONCURRENT_MODIFICATION);
    }

    @Test
    public void whenInvalidReceivedThenMarkNoMapping() {
        long pskuResultStorageId = createPskuResultStorage(100500L, 200500L);

        remapperService.doRemap();

        final PskuResultStorage modifiedPsku = pskuResultStorageDao.findById(pskuResultStorageId);
        assertThat(modifiedPsku.getState()).isEqualTo(PskuStorageState.NO_MAPPING);
    }

    @Test
    public void whenUserLoginNotPresentRobotLoginIsSent() {
        long pskuResultStorageId = createPskuResultStorage(EXISTING_PSKU_ID, EXISTING_MSKU_ID);

        remapperService = new RemapperService(pskuResultStorageDao, mboMappingsServiceMockSpy, MBOC_TEST_USER, 100);

        remapperService.doRemap();

        Mockito.verify(mboMappingsServiceMockSpy).updateMappings(updateMappingsRequestCaptor.capture());
        final PskuResultStorage modifiedPsku = pskuResultStorageDao.findById(pskuResultStorageId);
        assertThat(modifiedPsku.getState()).isEqualTo(PskuStorageState.REMAPPED);
        assertThat(modifiedPsku.getUserLogin()).isEqualTo(null);

        final long mappingSkuId = mboMappingsServiceMockSpy.getMappingSkuId(SUPPLIER_ID, SHOP_SKU1);
        assertThat(mappingSkuId).isEqualTo(EXISTING_MSKU_ID);

        assertThat(updateMappingsRequestCaptor.getValue().getRequestInfo().getUserLogin()).isEqualTo(MBOC_TEST_USER);
    }

    @Test
    public void whenUserLoginPresentItIsSent() {
        long pskuResultStorageId = createPskuResultStorageWithUserLogin(EXISTING_PSKU_ID, EXISTING_MSKU_ID);

        remapperService = new RemapperService(pskuResultStorageDao, mboMappingsServiceMockSpy, MBOC_TEST_USER, 100);

        remapperService.doRemap();

        Mockito.verify(mboMappingsServiceMockSpy).updateMappings(updateMappingsRequestCaptor.capture());
        final PskuResultStorage modifiedPsku = pskuResultStorageDao.findById(pskuResultStorageId);
        assertThat(modifiedPsku.getState()).isEqualTo(PskuStorageState.REMAPPED);
        assertThat(modifiedPsku.getUserLogin()).isEqualTo(TEST_USER_LOGIN);

        final long mappingSkuId = mboMappingsServiceMockSpy.getMappingSkuId(SUPPLIER_ID, SHOP_SKU1);
        assertThat(mappingSkuId).isEqualTo(EXISTING_MSKU_ID);

        assertThat(updateMappingsRequestCaptor.getValue().getRequestInfo().getUserLogin()).isEqualTo(TEST_USER_LOGIN);
    }

    @Test
    public void whenValidReceivedThenMarkRemaped() {
        long pskuResultStorageId = createPskuResultStorage(EXISTING_PSKU_ID, EXISTING_MSKU_ID);

        remapperService.doRemap();

        final PskuResultStorage modifiedPsku = pskuResultStorageDao.findById(pskuResultStorageId);
        assertThat(modifiedPsku.getState()).isEqualTo(PskuStorageState.REMAPPED);

        final long mappingSkuId = mboMappingsServiceMock.getMappingSkuId(SUPPLIER_ID, SHOP_SKU1);
        assertThat(mappingSkuId).isEqualTo(EXISTING_MSKU_ID);
    }

    @Test
    public void whenErrorNotExistsSluReceivedThenMarkMbocErrorAndErrorKind() {
        long pskuResultStorageId = createPskuResultStorage(NOT_EXISTING_PSKU_ID, EXISTING_MSKU_ID2);

        remapperService.doRemap();

        final PskuResultStorage modifiedPsku = pskuResultStorageDao.findById(pskuResultStorageId);

        assertThat(modifiedPsku.getState()).isEqualTo(PskuStorageState.MBOC_ERROR);
        assertThat(modifiedPsku.getErrorKinds()).isEqualTo("SKU_NOT_EXISTS");

        final long mappingSkuId = mboMappingsServiceMock.getMappingSkuId(SUPPLIER_ID, SHOP_SKU2);
        assertThat(mappingSkuId).isEqualTo(NOT_EXISTING_PSKU_ID);
    }

    @Test
    public void whenRealErrorFromMbocThenStayForRemapping() {
        long pskuResultStorageId = createPskuResultStorage(EXISTING_PSKU_ID3, EXISTING_MSKU_ID2);

        remapperService.doRemap();

        final PskuResultStorage modifiedPsku = pskuResultStorageDao.findById(pskuResultStorageId);

        assertThat(modifiedPsku.getState()).isEqualTo(PskuStorageState.FOR_REMAPPING);
        assertThat(modifiedPsku.getErrorKinds()).isNull();

        final long mappingSkuId = mboMappingsServiceMock.getMappingSkuId(SUPPLIER_ID, SHOP_SKU3);
        assertThat(mappingSkuId).isEqualTo(EXISTING_PSKU_ID3);
    }

    @Test
    public void multipleMappingstest() {
        MboMappingsService myMboMappingsServiceMock = Mockito.mock(MboMappingsService.class);
        RemapperService myRemapperService = new RemapperService(pskuResultStorageDao, myMboMappingsServiceMock, MBOC_TEST_USER, 100);

        // mock search
        SupplierOffer.Mapping mapping = SupplierOffer.Mapping.newBuilder()
                .setSkuId(EXISTING_PSKU_ID4)
                .setCategoryId(HID)
                .build();

        final MboMappings.SearchMappingsResponse.Builder builder = MboMappings.SearchMappingsResponse.newBuilder();
        builder.addOffers(SupplierOffer.Offer.newBuilder()
                .setSupplierId(SUPPLIER_ID)
                .setShopSkuId(SHOP_SKU4)
                .setMarketCategoryId(HID)
                .setTitle("title1")
                .setInternalProcessingStatus(SupplierOffer.Offer.InternalProcessingStatus.PROCESSED)
                .setBarcode("barcode")
                .setShopVendor("vendor")
                .setApprovedMapping(mapping)
                .build());
        builder.addOffers(SupplierOffer.Offer.newBuilder()
                .setSupplierId(SUPPLIER_ID)
                .setShopSkuId(SHOP_SKU5)
                .setMarketCategoryId(HID)
                .setTitle("title1")
                .setInternalProcessingStatus(SupplierOffer.Offer.InternalProcessingStatus.PROCESSED)
                .setBarcode("barcode")
                .setShopVendor("vendor")
                .setApprovedMapping(mapping)
                .build());
        Mockito.when(myMboMappingsServiceMock.searchBaseOfferMappingsByMarketSkuId(Mockito.any())).thenReturn(builder.build());

        // mock update
        MboMappings.ProviderProductInfoResponse.Builder updateResponse = MboMappings.ProviderProductInfoResponse.newBuilder();
        updateResponse.addResults(
                MboMappings.ProviderProductInfoResponse.ProductResult.newBuilder()
                        .setStatus(MboMappings.ProviderProductInfoResponse.Status.OK)
                        .build()
        );
        updateResponse.addResults(
                MboMappings.ProviderProductInfoResponse.ProductResult.newBuilder()
                        .setStatus(MboMappings.ProviderProductInfoResponse.Status.OK)
                        .build()
        );
        Mockito.when(myMboMappingsServiceMock.updateMappings(updateMappingsRequestCaptor.capture())).thenReturn(updateResponse.build());

        long pskuResultStorageId = createPskuResultStorage(EXISTING_PSKU_ID4, EXISTING_MSKU_ID2);

        myRemapperService.doRemap();

        final PskuResultStorage modifiedPsku = pskuResultStorageDao.findById(pskuResultStorageId);

        assertThat(modifiedPsku.getState()).isEqualTo(PskuStorageState.REMAPPED);
        assertThat(modifiedPsku.getErrorKinds()).isEmpty();

        MboMappings.UpdateMappingsRequest request = updateMappingsRequestCaptor.getValue();
        assertThat(request.getUpdatesCount()).isEqualTo(2);
        request.getUpdatesList().forEach(update -> assertThat(update.getMarketSkuId()).isEqualTo(EXISTING_MSKU_ID2));
    }

    private Long createPskuResultStorageWithUserLogin(long pskuId, long mskuId) {
        final PskuResultStorage psku = new PskuResultStorage();
        psku.setPskuId(pskuId);
        psku.setMskuMappedId(mskuId);
        psku.setCategoryId((long) HID);
        psku.setState(PskuStorageState.FOR_REMAPPING);
        psku.setUserLogin(TEST_USER_LOGIN);
        psku.setCreateTime(Timestamp.from(Instant.now()));
        pskuResultStorageDao.insert(psku);
        return psku.getId();
    }

    private Long createPskuResultStorage(long pskuId, long mskuId) {
        final PskuResultStorage psku = new PskuResultStorage();
        psku.setPskuId(pskuId);
        psku.setMskuMappedId(mskuId);
        psku.setCategoryId((long) HID);
        psku.setState(PskuStorageState.FOR_REMAPPING);
        psku.setCreateTime(Timestamp.from(Instant.now()));
        pskuResultStorageDao.insert(psku);
        return psku.getId();
    }
}