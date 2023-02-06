// CHECKSTYLE:OFF
package ru.yandex.market.mboc.app.proto;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import kotlin.Pair;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.MboAudit;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mboc.common.assertions.MbocAssertions;
import ru.yandex.market.mboc.common.dict.MbocSupplierType;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.masterdata.model.MasterData;
import ru.yandex.market.mboc.common.masterdata.model.MasterDataFromMdiConverter;
import ru.yandex.market.mboc.common.masterdata.model.MbocBaseProtoConverter;
import ru.yandex.market.mboc.common.masterdata.services.category.MboTimeUnitAliasesService;
import ru.yandex.market.mboc.common.offers.model.AntiMapping;
import ru.yandex.market.mboc.common.offers.model.BusinessSkuKey;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.model.OfferContent;
import ru.yandex.market.mboc.common.offers.repository.AntiMappingRepository;
import ru.yandex.market.mboc.common.offers.repository.search.OffersFilter;
import ru.yandex.market.mboc.common.services.category.models.Category;
import ru.yandex.market.mboc.common.services.modelstorage.ParamsUtils;
import ru.yandex.market.mboc.common.services.modelstorage.models.Model;
import ru.yandex.market.mboc.common.services.proto.AddProductInfoError;
import ru.yandex.market.mboc.common.services.proto.AddProductInfoListener;
import ru.yandex.market.mboc.common.services.proto.datacamp.AdditionalData;
import ru.yandex.market.mboc.common.services.proto.datacamp.DatacampContext;
import ru.yandex.market.mboc.common.utils.DateTimeUtils;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;
import ru.yandex.market.mboc.http.MboMappings;
import ru.yandex.market.mboc.http.MboMappings.ProductUpdateRequestInfo.ChangeSource;
import ru.yandex.market.mboc.http.MboMappings.ProviderProductInfoResponse;
import ru.yandex.market.mboc.http.MboMappings.ProviderProductInfoResponse.ErrorKind;
import ru.yandex.market.mdm.http.MasterDataProto.MasterDataInfo;
import ru.yandex.market.mdm.http.MasterDataProto.ProviderProductMasterData;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static ru.yandex.market.mboc.common.offers.model.Offer.MappingConfidence.CONTENT;
import static ru.yandex.market.mboc.common.offers.model.Offer.MappingConfidence.PARTNER;
import static ru.yandex.market.mboc.common.offers.model.Offer.MappingConfidence.PARTNER_SELF;
import static ru.yandex.market.mboc.common.offers.model.Offer.PartnerMappingModerationDecision.APPROVE;
import static ru.yandex.market.mboc.common.offers.model.Offer.PartnerMappingModerationDecision.DENY;
import static ru.yandex.market.mboc.common.utils.MbocConstants.MBO_MAPPINGS_SERVICE_DEFAULT_USER;
import static ru.yandex.market.mboc.common.utils.OfferTestUtils.TEST_MODEL_ID;
import static ru.yandex.market.mboc.common.utils.OfferTestUtils.TEST_SKU_ID;
import static ru.yandex.market.mboc.common.utils.OfferTestUtils.TEST_SUPPLIER_ID;

/**
 * @author yuramalinov
 * @created 05.07.18
 */
@SuppressWarnings({"checkstyle:MagicNumber", "JavaDoc"})
public class AddProductInfoHelperServiceTest extends AddProductInfoHelperServiceTestBase {

    @Before
    public void setUpTest() {
        modelServiceMock.addModel(new Model()
            .setId(OfferTestUtils.TEST_SKU_ID)
            .setCategoryId(OfferTestUtils.TEST_CATEGORY_INFO_ID)
            .setTitle("Operator quality sku")
            .setModelType(Model.ModelType.SKU)
            .setPublishedOnBlueMarket(true)
            .setModelQuality(Model.ModelQuality.OPERATOR));

        modelServiceMock.addModel(new Model()
            .setId(OfferTestUtils.TEST_MODEL_ID)
            .setCategoryId(OfferTestUtils.TEST_CATEGORY_INFO_ID)
            .setTitle("Operator quality model")
            .setModelType(Model.ModelType.GURU)
            .setModelQuality(Model.ModelQuality.OPERATOR));
    }

    @Test
    public void testCorrectInsert() {
        // TODO: Как будем сохранять логин пользователя?
        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo().build())
                .addProviderProductInfo(blueProductInfo().build())
                .build(), true);

        assertEquals(ProviderProductInfoResponse.Status.OK, response.getStatus());
        assertEquals(ProviderProductInfoResponse.Status.OK, response.getResults(0).getStatus());

        List<Offer> offers = offerRepository.findAll();
        assertThat(offers).hasSize(1);
        Offer offer = offers.get(0);
        assertEquals(BLUE_SHOP_ID, offer.getBusinessId());
        assertEquals("ShopSku", offer.getShopSku());
        assertEquals("Title", offer.getTitle());
        assertEquals("ShopCategoryName", offer.getShopCategoryName());
        assertEquals("Description", offer.extractOfferContent().getDescription());
        assertEquals("Vendor", offer.getVendor());
        assertEquals("VendorCode", offer.getVendorCode());
        assertEquals("123,321", offer.getBarCode());
        assertEquals(MODEL_ID, offer.getContentSkuMapping().getMappingId());
        assertEquals(MODEL_ID, offer.getApprovedSkuMapping().getMappingId());
        assertEquals(Offer.AcceptanceStatus.NEW, offer.getAcceptanceStatus());
        assertEquals(Offer.ProcessingStatus.OPEN, offer.getProcessingStatus());
    }

    @Test
    public void testSupplierMappingChangeCommentIsRecorded() {
        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo().setChangeSource(ChangeSource.SUPPLIER).build())
                .addProviderProductInfo(
                    blueProductInfo().setMappingChangeReasonComment("because").setMarketSkuId(MODEL_ID).build())
                .build(), true);

        assertEquals(ProviderProductInfoResponse.Status.OK, response.getStatus());

        Offer offer = getSingleCurrentOffer();
        assertNotNull(offer.getSupplierSkuMapping());
        assertEquals("because", offer.getSupplierSkuMappingChangeReason());
    }

    @Test
    public void testSupplierMappingToPsku10() {
        Offer existingOffer = commonBlueOffer()
            .updateApprovedSkuMapping(new Offer.Mapping(MODEL_ID, DateTimeUtils.dateTimeNow()), CONTENT);
        String supplierSkuMappingId = existingOffer.getSupplierSkuIdStr();

        offerRepository.insertOffer(existingOffer);

        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo().setChangeSource(ChangeSource.SUPPLIER).build())
                .addProviderProductInfo(
                    blueProductInfo().setMarketSkuId(PSKU10_MODEL_ID).build())
                .build(), true);

        assertEquals(ProviderProductInfoResponse.Status.OK, response.getStatus());
        assertEquals(1, response.getResultsCount());
        Offer offer = offerRepository.findAll().get(0);
        assertThat(offer.getShopSku()).isEqualTo(existingOffer.getShopSku());
        assertThat(offer.getSupplierSkuIdStr()).isEqualTo(supplierSkuMappingId);
    }

    @Test
    public void testSupplierMappingToFastSku() {
        Offer existingOffer = commonBlueOffer()
            .updateApprovedSkuMapping(new Offer.Mapping(MODEL_ID, DateTimeUtils.dateTimeNow()), CONTENT);
        String supplierSkuMappingId = existingOffer.getSupplierSkuIdStr();

        offerRepository.insertOffer(existingOffer);

        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo().setChangeSource(ChangeSource.SUPPLIER).build())
                .addProviderProductInfo(
                    blueProductInfo().setMarketSkuId(FAST_SKU_MODEL_ID).build())
                .build(), true);

        assertEquals(ProviderProductInfoResponse.Status.OK, response.getStatus());
        assertEquals(1, response.getResultsCount());
        Offer offer = offerRepository.findAll().get(0);
        assertThat(offer.getShopSku()).isEqualTo(existingOffer.getShopSku());
        assertThat(offer.getSupplierSkuIdStr()).isEqualTo(supplierSkuMappingId);
    }

    @Test
    public void testSupplierMappingToPsku20Allowed() {
        Offer existingOffer = commonBlueOffer()
            .updateApprovedSkuMapping(new Offer.Mapping(MODEL_ID, DateTimeUtils.dateTimeNow()), CONTENT);

        offerRepository.insertOffer(existingOffer);

        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo().setChangeSource(ChangeSource.SUPPLIER).build())
                .addProviderProductInfo(
                    blueProductInfo().setMarketSkuId(PSKU20_MODEL_ID).build())
                .build(), true);

        assertEquals(ProviderProductInfoResponse.Status.OK, response.getStatus());
        assertEquals(1, response.getResultsCount());

        Offer offer = offerRepository.findAll().get(0);
        assertThat(offer.getShopSku()).isEqualTo(existingOffer.getShopSku());
        assertThat(offer.getSupplierSkuIdStr()).isEqualTo(String.valueOf(PSKU20_MODEL_ID));
    }

    @Test
    public void testSupplierMappingForOfferWithNullApproved() {
        Offer existingOffer = commonBlueOffer()
            .updateApprovedSkuMapping(null, null);
        String supplierSkuMappingId = existingOffer.getSupplierSkuIdStr();

        offerRepository.insertOffer(existingOffer);

        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo().setChangeSource(ChangeSource.SUPPLIER).build())
                .addProviderProductInfo(
                    blueProductInfo().setMarketSkuId(PSKU10_MODEL_ID).build())
                .build(), true);

        assertEquals(ProviderProductInfoResponse.Status.OK, response.getStatus());
        assertEquals(1, response.getResultsCount());

        Offer offer = offerRepository.findAll().get(0);
        assertThat(offer.getShopSku()).isEqualTo(existingOffer.getShopSku());
        assertThat(offer.getSupplierSkuIdStr()).isEqualTo(supplierSkuMappingId);
    }

    @Test
    public void testSkipWrongShopSku() {
        String wrongShopSku = "**12_;/";
        List<AddProductInfoError> errors = new ArrayList<>();
        ProviderProductInfoResponse productResult = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo().build())
                .addProviderProductInfo(blueProductInfo().setShopSkuId(wrongShopSku).build())
                .build(), DatacampContext.builder().build(), (productInfoOrder, error) -> {
                errors.add(error);
            });

        assertThat(ProviderProductInfoResponse.Status.OK).isEqualTo(productResult.getStatus());
        assertThat(productResult.getResults(0).getStatus()).isEqualTo(ProviderProductInfoResponse.Status.ERROR);
        assertThat(errors.get(0).isRecoverableError()).isFalse();
        assertThat(offerRepository.findAll()).isEmpty();
    }

    @Test
    public void testValidateCorrectSupplierMapping() {
        Offer existingOffer = commonBlueOffer()
            .updateApprovedSkuMapping(new Offer.Mapping(PSKU10_MODEL_ID, DateTimeUtils.dateTimeNow()), CONTENT);

        offerRepository.insertOffer(existingOffer);

        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo().setChangeSource(ChangeSource.SUPPLIER).build())
                .addProviderProductInfo(
                    blueProductInfo().setMarketSkuId(PSKU10_MODEL_ID).build())
                .build(), true);

        assertEquals(ProviderProductInfoResponse.Status.OK, response.getStatus());
        assertEquals(1, response.getResultsCount());
    }

    @Test
    public void testAlertWhenSupplierMappingChangeCommentIsNotUsed() {
        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo().setChangeSource(ChangeSource.SUPPLIER).build())
                .addProviderProductInfo(
                    blueProductInfo().clearMarketSkuId().setMappingChangeReasonComment("because").build())
                .build(), true);

        assertEquals(ProviderProductInfoResponse.Status.ERROR, response.getStatus());
        // Это ошибка при разработке, можно не кодировать
        validateHasError(response.getResults(0), ErrorKind.OTHER);
        assertEquals("Provided mapping_change_reason_comment is not used because no supplier mapping is provided",
            response.getResults(0).getErrors(0).getMessage());
    }

    @Test
    public void testMultipleUrls() {
        //no urls
        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo().build())
                .addProviderProductInfo(blueProductInfo().setMarketSkuId(MODEL_ID).build())
                .build(), true);

        assertEquals(ProviderProductInfoResponse.Status.OK, response.getStatus());
        List<Offer> offers = offerRepository.findAll();
        assertThat(offers).hasSize(1);
        Offer offer = offers.get(0);
        assertTrue(offer.extractOfferContent().getUrls().isEmpty());

        //add some invalid urls on update
        response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo().build())
                .addProviderProductInfo(blueProductInfo()
                    .setMarketSkuId(MODEL_ID)
                    .addAllUrl(Arrays.asList(
                        "ftp://www.wrong.pro/tocol",
                        "1",
                        "https://www."))
                    .build())
                .build(), true);
        assertEquals(ProviderProductInfoResponse.Status.ERROR, response.getStatus());
        assertEquals(1, response.getResultsCount());
        ProviderProductInfoResponse.ProductResult result = response.getResults(0);
        validateHasError(result, ErrorKind.OTHER);
        assertEquals(3, result.getErrorsCount());
        response.getResults(0).getErrorsList().forEach(error ->
            assertThat(error.getMessage()).containsIgnoringCase("не является валидным URL"));

        //add some urls on update
        response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo().build())
                .addProviderProductInfo(blueProductInfo()
                    .setMarketSkuId(MODEL_ID)
                    .addAllUrl(Arrays.asList(
                        "https://www.yandex.ru",
                        "http://dogcats.ru/11/кириллица-в-урле-чтоб-её",
                        "https://ящеры.рф/pages/144932/showinfo.php?id=12&fullsize=1",
                        "https://ящеры.рф/кириллица-в-урле-чтоб-её"
                    )).build())
                .build(), true);

        assertEquals(ProviderProductInfoResponse.Status.OK, response.getStatus());
        offers = offerRepository.findAll();
        assertThat(offers).hasSize(1);
        offer = offers.get(0);
        assertEquals(offer.getId(), offers.get(0).getId()); //same offer updated
        assertThat(offer.extractOfferContent().getUrls()).containsExactlyInAnyOrder(
            "https://www.yandex.ru",
            "http://dogcats.ru/11/кириллица-в-урле-чтоб-её",
            "https://ящеры.рф/pages/144932/showinfo.php?id=12&fullsize=1",
            "https://ящеры.рф/кириллица-в-урле-чтоб-её");
    }

    @Test
    public void testLegacyCall() {
        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .addProviderProductInfo(blueProductInfo())
                .build(), true);

        assertEquals(ProviderProductInfoResponse.Status.OK, response.getStatus());
    }

    @Test
    public void testCorrectUpdate() {
        Offer original = new Offer()
            .setBusinessId(BLUE_SHOP_ID)
            .setShopSku("ShopSku")
            .setTitle("Before")
            .setShopCategoryName("Before")
            .setIsOfferContentPresent(true)
            .storeOfferContent(OfferContent.initEmptyContent());

        offerRepository.insertOffer(original);
        original = offerRepository.getOfferById(original.getId());

        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo()
                    .build())
                .addProviderProductInfo(blueProductInfo().build())
                .build(), true);

        assertEquals(ProviderProductInfoResponse.Status.OK, response.getStatus());

        List<Offer> offers = offerRepository.findAll();
        assertThat(offers).hasSize(1);
        Offer offer = offers.get(0);
        assertEquals(original.getId(), offer.getId());
        assertThat(offer.getLastVersion()).isGreaterThan(original.getLastVersion());
        assertEquals(BLUE_SHOP_ID, offer.getBusinessId());
        assertEquals("ShopSku", offer.getShopSku());
        assertEquals("Title", offer.getTitle());
        assertEquals("ShopCategoryName", offer.getShopCategoryName());
        assertEquals("Description", offer.extractOfferContent().getDescription());
        assertEquals("Vendor", offer.getVendor());
        assertEquals("VendorCode", offer.getVendorCode());
        assertEquals("123,321", offer.getBarCode());
        assertEquals(MODEL_ID, offer.getContentSkuMapping().getMappingId());
        assertEquals(MODEL_ID, offer.getApprovedSkuMapping().getMappingId());
        assertEquals(Offer.AcceptanceStatus.NEW, offer.getAcceptanceStatus());
        assertEquals(Offer.ProcessingStatus.OPEN, offer.getProcessingStatus());
    }

    @Test
    public void willNotDeleteNullApproveMapping() {
        Offer original = new Offer()
            .setBusinessId(BLUE_SHOP_ID)
            .setShopSku("ShopSku")
            .setTitle("Before")
            .setShopCategoryName("Before")
            .setIsOfferContentPresent(true)
            .storeOfferContent(OfferContent.initEmptyContent());

        offerRepository.insertOffer(original);
        original = offerRepository.getOfferById(original.getId());

        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo()
                    .setChangeSource(ChangeSource.SUPPLIER)
                    .build())
                .addProviderProductInfo(
                    blueProductInfo()
                        .setMarketSkuId(0)
                        .build()
                )
                .build(), true);

        assertEquals(ProviderProductInfoResponse.Status.OK, response.getStatus());

        List<Offer> offers = offerRepository.findAll();
        assertThat(offers).hasSize(1);
        Offer offer = offers.get(0);
        assertEquals(original.getId(), offer.getId());
        assertThat(offer.getLastVersion()).isGreaterThan(original.getLastVersion());
        assertEquals(BLUE_SHOP_ID, offer.getBusinessId());
        assertEquals("ShopSku", offer.getShopSku());
        assertEquals("Title", offer.getTitle());
        assertEquals("ShopCategoryName", offer.getShopCategoryName());
        assertEquals("Description", offer.extractOfferContent().getDescription());
        assertEquals("Vendor", offer.getVendor());
        assertEquals("VendorCode", offer.getVendorCode());
        assertEquals("123,321", offer.getBarCode());
        assertNull(offer.getContentSkuMapping());
        assertNull(offer.getApprovedSkuMapping());
        assertEquals(Offer.ProcessingStatus.OPEN, offer.getProcessingStatus());
    }

    @Test
    public void testCorrectUpdateToEmptyBarcodeVendocodeDescription() {
        Offer original = new Offer()
            .setBusinessId(BLUE_SHOP_ID)
            .setShopSku("ShopSku")
            .setTitle("Before")
            .setShopCategoryName("Before")
            .storeOfferContent(OfferContent.builder()
                .description("test-description")
                .build())
            .setBarCode("123,234")
            .setVendorCode("test-vendor");

        offerRepository.insertOffer(original);
        original = offerRepository.getOfferById(original.getId());

        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo()
                    .build())
                .addProviderProductInfo(blueProductInfo()
                    .clearDescription()
                    .clearVendorCode()
                    .clearBarcode()
                    .setRemoveBarcodes(true)
                    .setRemoveVendorCode(true)
                    .setRemoveDescription(true)
                    .build())
                .build(), true);

        assertEquals(ProviderProductInfoResponse.Status.OK, response.getStatus());

        List<Offer> offers = offerRepository.findAll();
        assertThat(offers).hasSize(1);
        Offer offer = offers.get(0);
        assertEquals(original.getId(), offer.getId());
        assertThat(offer.getLastVersion()).isGreaterThan(original.getLastVersion());
        assertEquals(BLUE_SHOP_ID, offer.getBusinessId());
        assertNull(offer.extractOfferContent().getDescription());
        assertNull(offer.getVendorCode());
        assertNull(offer.getBarCode());
    }

    @Test
    public void testCorrectUpdateApprovedSkuConfidenceContent() {
        Offer original = new Offer()
            .setBusinessId(BLUE_SHOP_ID)
            .setShopSku("ShopSku")
            .setTitle("Before")
            .setShopCategoryName("Before")
            .storeOfferContent(OfferContent.builder().description("test-description").build())
            .setBarCode("123,234")
            .setVendorCode("test-vendor");

        offerRepository.insertOffer(original);
        original = offerRepository.getOfferById(original.getId());

        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo()
                    .setChangeSource(ChangeSource.CONTENT)
                    .build())
                .addProviderProductInfo(blueProductInfo()
                    .build())
                .build(), true);

        assertEquals(ProviderProductInfoResponse.Status.OK, response.getStatus());

        List<Offer> offers = offerRepository.findAll();
        assertThat(offers).hasSize(1);
        Offer offer = offers.get(0);
        assertEquals(original.getId(), offer.getId());
        assertThat(offer.getLastVersion()).isGreaterThan(original.getLastVersion());
        assertEquals(BLUE_SHOP_ID, offer.getBusinessId());
        assertNotNull(offer.getApprovedSkuMapping());
        assertEquals(Offer.MappingConfidence.CONTENT, offer.getApprovedSkuMappingConfidence());
    }

    @Test
    public void testCorrectUpdateApprovedSkuConfidenceAuto() {
        Offer original = new Offer()
            .setBusinessId(BLUE_SHOP_ID)
            .setShopSku("ShopSku")
            .setTitle("Before")
            .setShopCategoryName("Before")
            .storeOfferContent(OfferContent.builder()
                .description("test-description")
                .build())
            .setBarCode("123,234")
            .setCategoryIdForTests(99L, Offer.BindingKind.APPROVED)
            .setVendorCode("test-vendor");

        offerRepository.insertOffer(original);
        original = offerRepository.getOfferById(original.getId());

        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo()
                    .setChangeSource(ChangeSource.AUTO)
                    .build())
                .addProviderProductInfo(blueProductInfo()
                    .build())
                .build(), true);

        assertEquals(ProviderProductInfoResponse.Status.OK, response.getStatus());

        Offer offer = getSingleCurrentOffer();
        assertEquals(original.getId(), offer.getId());
        assertThat(offer.getLastVersion()).isGreaterThan(original.getLastVersion());
        assertEquals(BLUE_SHOP_ID, offer.getBusinessId());
        assertNotNull(offer.getApprovedSkuMapping());
        assertEquals(Offer.MappingConfidence.PARTNER_SELF, offer.getApprovedSkuMappingConfidence());
    }

    @Test
    public void testRequiredFieldsShopIdShopSkuId() {
        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo()
                    .build())
                .addProviderProductInfo(blueProductInfo()
                    .clearShopSkuId()
                    .clearShopId()
                    .build())
                .build(), true);

        assertEquals(ProviderProductInfoResponse.Status.ERROR, response.getStatus());
        assertThat(offerRepository.findAll()).hasSize(0);
        assertEquals(1, response.getResultsCount());
        validateHasError(response.getResults(0), ErrorKind.NO_REQUIRED_FIELDS);
    }

    @Test
    public void testRequiredFieldsTitleShopCategoryName() {
        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo()
                    .build())
                .addProviderProductInfo(blueProductInfo()
                    .setTitle("")
                    .setShopCategoryName("")
                    .build())
                .build(), true);

        assertEquals(ProviderProductInfoResponse.Status.ERROR, response.getStatus());
        assertThat(offerRepository.findAll()).hasSize(0);
        assertEquals(1, response.getResultsCount());
        validateHasError(response.getResults(0), ErrorKind.NO_REQUIRED_FIELDS);
    }

    @Test
    public void testVersionVerificationWontFailIfUpdateOffer() {
        Offer original = new Offer()
            .setBusinessId(BLUE_SHOP_ID)
            .setShopSku("ShopSku")
            .setTitle("Before")
            .setShopCategoryName("Before")
            .setIsOfferContentPresent(true)
            .storeOfferContent(OfferContent.initEmptyContent());

        offerRepository.insertOffer(original);
        original = offerRepository.getOfferById(original.getId());

        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo()
                    .build())
                .addProviderProductInfo(blueProductInfo()
                    .setVerifyLastVersion(original.getLastVersion())
                    .build())
                .build(), true);

        assertEquals(ProviderProductInfoResponse.Status.OK, response.getStatus());
    }

    @Test
    public void testVersionVerificationFails() {
        Offer original = new Offer()
            .setBusinessId(BLUE_SHOP_ID)
            .setShopSku("ShopSku")
            .setTitle("Before")
            .setShopCategoryName("Before")
            .setIsOfferContentPresent(true)
            .storeOfferContent(OfferContent.initEmptyContent());

        offerRepository.insertOffer(original);
        original = offerRepository.getOfferById(original.getId());

        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo()
                    .build())
                .addProviderProductInfo(blueProductInfo()
                    .setVerifyLastVersion(original.getLastVersion() - 1)
                    .build())
                .build(), true);

        assertEquals(ProviderProductInfoResponse.Status.ERROR, response.getStatus());
        validateHasError(response.getResults(0), ErrorKind.CONCURRENT_MODIFICATION);
    }

    @Test
    public void testVersionVerificationWontFailIfAddNewOfferWithVersionEqualsZero() {
        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo()
                    .build())
                .addProviderProductInfo(blueProductInfo()
                    .setVerifyLastVersion(0)
                    .build())
                .build(), true);

        assertEquals(ProviderProductInfoResponse.Status.OK, response.getStatus());
    }

    /**
     * If requests contains "new offer" with VerifyLastVersion == 0, but offer storage already contains that offer,
     * than save should fail with CONCURRENT_MODIFICATION error.
     */
    @Test
    public void testVersionVerificationWillFailIfNewOfferMatchesExistentOne() {
        Offer original = new Offer()
            .setBusinessId(BLUE_SHOP_ID)
            .setShopSku("ShopSku")
            .setTitle("Before")
            .setShopCategoryName("Before")
            .setIsOfferContentPresent(true)
            .storeOfferContent(OfferContent.initEmptyContent());
        offerRepository.insertOffer(original);

        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo()
                    .build())
                .addProviderProductInfo(blueProductInfo()
                    .setVerifyLastVersion(0)
                    .build())
                .build(), true);

        assertEquals(ProviderProductInfoResponse.Status.ERROR, response.getStatus());
        validateHasError(response.getResults(0), ErrorKind.CONCURRENT_MODIFICATION);
    }

    @Test
    public void testCheckSkuPublished() {
        modelServiceMock.getModel(MODEL_ID).map(model -> model.setPublishedOnBlueMarket(false));

        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo()
                    .build())
                .addProviderProductInfo(blueProductInfo().build())
                .build(), true);

        assertEquals(ProviderProductInfoResponse.Status.ERROR, response.getStatus());
        validateHasError(response.getResults(0), ErrorKind.SKU_NOT_PUBLISHED);
    }

    @Test
    public void testCheckSkuExists() {
        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo()
                    .build())
                .addProviderProductInfo(blueProductInfo()
                    .setMarketSkuId(MODEL_ID + 666)
                    .build())
                .build(), true);

        assertEquals(ProviderProductInfoResponse.Status.ERROR, response.getStatus());
        validateHasError(response.getResults(0), ErrorKind.SKU_NOT_EXISTS);
    }

    @Test
    public void testMappingExistenceVerificationFailsApproved() {
        Offer original = new Offer()
            .setBusinessId(BLUE_SHOP_ID)
            .setShopSku("ShopSku")
            .setTitle("Before")
            .setShopCategoryName("Before")
            .setCategoryIdForTests(99L, Offer.BindingKind.APPROVED)
            .setIsOfferContentPresent(true)
            .storeOfferContent(OfferContent.initEmptyContent())
            .updateApprovedSkuMapping(new Offer.Mapping(123, DateTimeUtils.dateTimeNow()), CONTENT);

        offerRepository.insertOffer(original);

        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo()
                    .setVerifyNoApprovedMapping(true)
                    .build())
                .addProviderProductInfo(blueProductInfo().build())
                .build(), true);

        assertEquals(ProviderProductInfoResponse.Status.ERROR, response.getStatus());
        validateHasError(response.getResults(0), ErrorKind.HAS_APPROVED_MAPPING);
    }

    @Test
    public void testMappingExistenceVerificationSuccessWithDeletedApprovedMapping() {
        Offer original = new Offer()
            .setBusinessId(BLUE_SHOP_ID)
            .setShopSku("ShopSku")
            .setTitle("Before")
            .setShopCategoryName("Before")
            .setCategoryIdForTests(99L, Offer.BindingKind.APPROVED)
            .setIsOfferContentPresent(true)
            .storeOfferContent(OfferContent.initEmptyContent())
            .updateApprovedSkuMapping(new Offer.Mapping(0, DateTimeUtils.dateTimeNow()),
                null);

        offerRepository.insertOffer(original);

        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo()
                    .setVerifyNoApprovedMapping(true)
                    .build())
                .addProviderProductInfo(blueProductInfo().build())
                .build(), true);

        assertEquals(ProviderProductInfoResponse.Status.OK, response.getStatus());
    }

    @Test
    public void testAcceptIfOnlyApprovedMappingIsPresent() {
        Offer original = new Offer()
            .setBusinessId(BLUE_SHOP_ID)
            .setShopSku("ShopSku")
            .setTitle("Title")
            .setShopCategoryName("ShopCategoryName")
            .storeOfferContent(OfferContent.builder()
                .description("Description")
                .build())
            .setVendor("Vendor")
            .setVendorCode("VendorCode")
            .setBarCode("123,321")
            .setCategoryIdForTests(99L, Offer.BindingKind.APPROVED)
            .updateApprovedSkuMapping(new Offer.Mapping(MODEL_ID, DateTimeUtils.dateTimeNow()), CONTENT)
            .setServiceOffers(List.of(new Offer.ServiceOffer(BLUE_SHOP_ID).setSupplierType(MbocSupplierType.BUSINESS)));

        offerRepository.insertOffer(original);

        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo()
                    .setChangeSource(ChangeSource.SUPPLIER)
                    .build())
                .addProviderProductInfo(blueProductInfo().build())
                .build(), true);

        assertEquals(ProviderProductInfoResponse.Status.OK, response.getStatus());

        Offer after = offerRepository.getOfferById(original.getId());
        // Auto confirm same mapping
        assertEquals(Offer.MappingStatus.ACCEPTED, after.getSupplierSkuMappingStatus());
    }

    @Test
    public void testAcceptedEvenIfContentMappingIsDifferentButApprovedIsOk() {
        Offer original = new Offer()
            .setBusinessId(BLUE_SHOP_ID)
            .setShopSku("ShopSku")
            .setTitle("Title")
            .setShopCategoryName("ShopCategoryName")
            .storeOfferContent(OfferContent.builder()
                .description("Description")
                .build())
            .setVendor("Vendor")
            .setVendorCode("VendorCode")
            .setBarCode("123,321")
            .updateApprovedSkuMapping(new Offer.Mapping(MODEL_ID, DateTimeUtils.dateTimeNow()), PARTNER)
            .setCategoryIdForTests(99L, Offer.BindingKind.APPROVED)
            .setApprovedSkuMappingConfidence(Offer.MappingConfidence.CONTENT)
            .setContentSkuMapping(new Offer.Mapping(123, DateTimeUtils.dateTimeNow())) // different
            .setServiceOffers(List.of(new Offer.ServiceOffer(BLUE_SHOP_ID).setSupplierType(MbocSupplierType.BUSINESS)));

        offerRepository.insertOffer(original);

        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo()
                    .setChangeSource(ChangeSource.SUPPLIER)
                    .build())
                .addProviderProductInfo(blueProductInfo().build())
                .build(), true);

        assertEquals(ProviderProductInfoResponse.Status.OK, response.getStatus());

        Offer after = offerRepository.getOfferById(original.getId());
        // Auto confirm same mapping
        assertEquals(Offer.MappingStatus.ACCEPTED, after.getSupplierSkuMappingStatus());
    }

    @Test
    public void testMappingExistenceVerificationFailsSupplier() {
        Offer original = commonBlueOffer();

        offerRepository.insertOffer(original);

        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo()
                    .setVerifyNoSupplierMapping(true)
                    .build())
                .addProviderProductInfo(blueProductInfo().build())
                .build(), true);

        assertEquals(ProviderProductInfoResponse.Status.ERROR, response.getStatus());
        validateHasError(response.getResults(0), ErrorKind.HAS_SUPPLIER_MAPPING);
    }

    @Test
    public void testMappingExistenceVerificationSuccessWithDeletedSupplierMapping() {
        Offer original = commonBlueOffer()
            .setSupplierSkuMapping(new Offer.Mapping(0, DateTimeUtils.dateTimeNow()));

        offerRepository.insertOffer(original);

        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo()
                    .setVerifyNoSupplierMapping(true)
                    .build())
                .addProviderProductInfo(blueProductInfo().build())
                .build(), true);

        assertEquals(ProviderProductInfoResponse.Status.OK, response.getStatus());
    }

    @Test
    public void testUpdateFallsIfNoOffer() {
        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo().build())
                .addProviderProductInfo(blueProductInfo().build())
                .build(), false);

        assertEquals(ProviderProductInfoResponse.Status.ERROR, response.getStatus());
        validateHasError(response.getResults(0), ErrorKind.OFFER_DOESNT_EXIST);
    }

    @Test
    public void testComplexRequest() {
        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo().build())
                .addProviderProductInfo(blueProductInfo().build())
                .addProviderProductInfo(blueProductInfo().setShopSkuId("BAA D SKU").build())
                .build(), true);

        assertEquals(ProviderProductInfoResponse.Status.ERROR, response.getStatus());
        assertEquals(ProviderProductInfoResponse.Status.ERROR, response.getResults(0).getStatus());
        validateHasError(response.getResults(0), ErrorKind.OK_NOT_SAVED_DUE_TO_OTHER_ERRORS);

        assertEquals(ProviderProductInfoResponse.Status.ERROR, response.getResults(1).getStatus());
        validateHasError(response.getResults(1), ErrorKind.WRONG_SHOP_SKU);
    }

    @Test
    public void testUpdateToZeroSupplier() {
        categoryCachingService.addCategory(CATEGORY_ID);
        categoryKnowledgeService.addCategory(CATEGORY_ID);
        MboMappings.ProviderProductInfoRequest request = MboMappings.ProviderProductInfoRequest.newBuilder()
            .setRequestInfo(commonRequestInfo()
                .setChangeSource(ChangeSource.SUPPLIER)
                .build())
            .addProviderProductInfo(blueProductInfo().setMarketSkuId(0).build())
            .build();

        // old pipeline case
        Offer original = commonBlueOffer()
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.PROCESSED);
        offerRepository.insertOffer(original);
        Offer offer = offerRepository.getOfferById(original.getId());
        assertEquals(123L, offer.getSupplierSkuMapping().getMappingId());
        assertEquals(Offer.ProcessingStatus.PROCESSED, offer.getProcessingStatus());

        ProviderProductInfoResponse response = service.addProductInfo(request, true);
        assertEquals(ProviderProductInfoResponse.Status.OK, response.getStatus());

        offer = offerRepository.getOfferById(original.getId());
        assertEquals(0, offer.getSupplierSkuMapping().getMappingId());
        assertEquals(Offer.MappingStatus.NONE, offer.getSupplierSkuMappingStatus());
        assertEquals(Offer.ProcessingStatus.PROCESSED, offer.getProcessingStatus());

        // old pipeline in moderation case
        original = commonBlueOffer()
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_MODERATION)
            .setBindingKind(Offer.BindingKind.APPROVED)
            .setLastVersion(offer.getLastVersion());
        offerRepository.updateOffer(original.setId(offer.getId()));
        offer = offerRepository.getOfferById(original.getId());
        assertEquals(123L, offer.getSupplierSkuMapping().getMappingId());
        assertEquals(Offer.ProcessingStatus.IN_MODERATION, offer.getProcessingStatus());

        response = service.addProductInfo(request, true);
        assertEquals(ProviderProductInfoResponse.Status.OK, response.getStatus());

        offer = offerRepository.getOfferById(original.getId());
        assertEquals(0, offer.getSupplierSkuMapping().getMappingId());
        assertEquals(Offer.MappingStatus.NONE, offer.getSupplierSkuMappingStatus());
        assertEquals(Offer.ProcessingStatus.IN_PROCESS, offer.getProcessingStatus());

        // Должно ли это писаться в YT сразу или через модерацию - надо решить (пока будет сразу, пока заглушка)]

        // new pipeline case
        supplierRepository.update(new Supplier(BLUE_SHOP_ID, "LoL").setNewContentPipeline(true));
        supplierService.invalidateCache();
        original = commonBlueOffer()
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.PROCESSED)
            .setProcessingCounter(offer.getProcessingCounter())
            .setLastVersion(offer.getLastVersion());
        offerRepository.updateOffer(original.setId(offer.getId()));
        offer = offerRepository.getOfferById(original.getId());
        assertEquals(123L, offer.getSupplierSkuMapping().getMappingId());
        assertEquals(Offer.ProcessingStatus.PROCESSED, offer.getProcessingStatus());

        response = service.addProductInfo(request, true);
        assertEquals(ProviderProductInfoResponse.Status.OK, response.getStatus());

        offer = offerRepository.getOfferById(original.getId());
        assertEquals(0, offer.getSupplierSkuMapping().getMappingId());
        assertEquals(Offer.MappingStatus.NONE, offer.getSupplierSkuMappingStatus());
        assertEquals(Offer.ProcessingStatus.PROCESSED, offer.getProcessingStatus());

        // new pipeline in moderation Good Content case
        categoryCachingService.setCategoryAcceptGoodContent(CATEGORY_ID, true);
        supplierRepository.update(new Supplier(BLUE_SHOP_ID, "LoL").setNewContentPipeline(true));
        supplierService.invalidateCache();
        original = commonBlueOffer()
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_MODERATION)
            .setProcessingCounter(offer.getProcessingCounter())
            .setBindingKind(Offer.BindingKind.APPROVED)
            .setLastVersion(offer.getLastVersion());
        offerRepository.updateOffer(original.setId(offer.getId()));
        offer = offerRepository.getOfferById(original.getId());
        assertEquals(123L, offer.getSupplierSkuMapping().getMappingId());
        assertEquals(Offer.ProcessingStatus.IN_MODERATION, offer.getProcessingStatus());

        response = service.addProductInfo(request, true);
        assertEquals(ProviderProductInfoResponse.Status.OK, response.getStatus());

        offer = offerRepository.getOfferById(original.getId());
        assertEquals(0, offer.getSupplierSkuMapping().getMappingId());
        assertEquals(Offer.MappingStatus.NONE, offer.getSupplierSkuMappingStatus());
        assertEquals(Offer.ProcessingStatus.NEED_CONTENT, offer.getProcessingStatus());

        // new pipeline in moderation not Good Content case
        categoryCachingService.setCategoryAcceptGoodContent(CATEGORY_ID, false);
        supplierRepository.update(new Supplier(BLUE_SHOP_ID, "LoL").setNewContentPipeline(true));
        supplierService.invalidateCache();
        original = commonBlueOffer()
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_MODERATION)
            .setProcessingCounter(offer.getProcessingCounter())
            .setBindingKind(Offer.BindingKind.APPROVED)
            .setLastVersion(offer.getLastVersion());
        offerRepository.updateOffer(original.setId(offer.getId()));
        offer = offerRepository.getOfferById(original.getId());
        assertEquals(123L, offer.getSupplierSkuMapping().getMappingId());
        assertEquals(Offer.ProcessingStatus.IN_MODERATION, offer.getProcessingStatus());

        response = service.addProductInfo(request, true);
        assertEquals(ProviderProductInfoResponse.Status.OK, response.getStatus());

        offer = offerRepository.getOfferById(original.getId());
        assertEquals(0, offer.getSupplierSkuMapping().getMappingId());
        assertEquals(Offer.MappingStatus.NONE, offer.getSupplierSkuMappingStatus());
        assertEquals(Offer.ProcessingStatus.IN_PROCESS, offer.getProcessingStatus());

        // new pipeline to be in moderation Good Content case
        categoryCachingService.setCategoryAcceptGoodContent(CATEGORY_ID, true);
        supplierRepository.update(new Supplier(BLUE_SHOP_ID, "LoL").setNewContentPipeline(true));
        original = commonBlueOffer()
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_MODERATION)
            .setProcessingCounter(offer.getProcessingCounter())
            .setBindingKind(Offer.BindingKind.APPROVED)
            .setLastVersion(offer.getLastVersion());
        offerRepository.updateOffer(original.setId(offer.getId()));
        offer = offerRepository.getOfferById(original.getId());
        assertEquals(123L, offer.getSupplierSkuMapping().getMappingId());
        assertEquals(Offer.ProcessingStatus.IN_MODERATION, offer.getProcessingStatus());

        response = service.addProductInfo(request, true);
        assertEquals(ProviderProductInfoResponse.Status.OK, response.getStatus());

        offer = offerRepository.getOfferById(original.getId());
        assertEquals(0, offer.getSupplierSkuMapping().getMappingId());
        assertEquals(Offer.MappingStatus.NONE, offer.getSupplierSkuMappingStatus());
        assertEquals(Offer.ProcessingStatus.NEED_CONTENT, offer.getProcessingStatus());

        // new pipeline to be in moderation not Good Content case
        categoryCachingService.setCategoryAcceptGoodContent(CATEGORY_ID, false);
        supplierRepository.update(new Supplier(BLUE_SHOP_ID, "LoL").setNewContentPipeline(true));
        original = commonBlueOffer()
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_MODERATION)
            .setProcessingCounter(offer.getProcessingCounter())
            .setBindingKind(Offer.BindingKind.APPROVED)
            .setLastVersion(offer.getLastVersion());
        offerRepository.updateOffer(original.setId(offer.getId()));
        offer = offerRepository.getOfferById(original.getId());
        assertEquals(123L, offer.getSupplierSkuMapping().getMappingId());
        assertEquals(Offer.ProcessingStatus.IN_MODERATION, offer.getProcessingStatus());

        response = service.addProductInfo(request, true);
        assertEquals(ProviderProductInfoResponse.Status.OK, response.getStatus());

        offer = offerRepository.getOfferById(original.getId());
        assertEquals(0, offer.getSupplierSkuMapping().getMappingId());
        assertEquals(Offer.MappingStatus.NONE, offer.getSupplierSkuMappingStatus());
        assertEquals(Offer.ProcessingStatus.IN_PROCESS, offer.getProcessingStatus());
        categoryCachingService.removeCategory(CATEGORY_ID);
    }

    @Test
    public void testCreateWithZeroSupplier() {
        categoryCachingService.addCategory(CATEGORY_ID);
        MboMappings.ProviderProductInfoRequest request = MboMappings.ProviderProductInfoRequest.newBuilder()
            .setRequestInfo(commonRequestInfo()
                .setChangeSource(ChangeSource.SUPPLIER)
                .build())
            .addProviderProductInfo(blueProductInfo().setMarketSkuId(0).build())
            .build();

        // old pipeline case
        ProviderProductInfoResponse response = service.addProductInfo(request, true);
        assertEquals(ProviderProductInfoResponse.Status.OK, response.getStatus());

        List<Offer> offers = offerRepository.findAll();
        assertThat(offers).hasSize(1);
        Offer offer = offers.get(0);
        assertThat(offer.getSupplierSkuMapping().getMappingId()).isZero();
        assertThat(offer.getSupplierSkuMappingStatus()).isEqualTo(Offer.MappingStatus.NONE);
        assertThat(offer.getApprovedSkuMapping()).isNull();
        assertThat(offer.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.OPEN);
        offerRepository.deleteAllInTest();

        // new pipeline case
        supplierRepository.update(new Supplier(BLUE_SHOP_ID, "LoL").setNewContentPipeline(true));

        response = service.addProductInfo(request, true);
        assertEquals(ProviderProductInfoResponse.Status.OK, response.getStatus());

        offers = offerRepository.findAll();
        assertThat(offers).hasSize(1);
        offer = offers.get(0);
        assertThat(offer.getSupplierSkuMapping().getMappingId()).isZero();
        assertThat(offer.getSupplierSkuMappingStatus()).isEqualTo(Offer.MappingStatus.NONE);
        assertThat(offer.getApprovedSkuMapping()).isNull();
        assertThat(offer.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.OPEN);
        offerRepository.deleteAllInTest();

        // new pipeline Good Content case
        categoryCachingService.setCategoryAcceptGoodContent(CATEGORY_ID, true);
        supplierRepository.update(new Supplier(BLUE_SHOP_ID, "LoL").setNewContentPipeline(true));

        response = service.addProductInfo(request, true);
        assertEquals(ProviderProductInfoResponse.Status.OK, response.getStatus());

        offers = offerRepository.findAll();
        assertThat(offers).hasSize(1);
        offer = offers.get(0);
        assertThat(offer.getSupplierSkuMapping().getMappingId()).isZero();
        assertThat(offer.getSupplierSkuMappingStatus()).isEqualTo(Offer.MappingStatus.NONE);
        assertThat(offer.getApprovedSkuMapping()).isNull();
        assertThat(offer.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.OPEN);
        offerRepository.deleteAllInTest();

        categoryCachingService.removeCategory(CATEGORY_ID);
    }

    @Test
    public void testUpdateToZeroContent() {
        Offer original = commonBlueOffer();
        original.setContentSkuMapping(original.getSupplierSkuMapping());

        offerRepository.insertOffer(original);
        Offer offer = offerRepository.getOfferById(original.getId());
        assertEquals(123L, offer.getSupplierSkuMapping().getMappingId());

        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo()
                    .setChangeSource(ChangeSource.CONTENT)
                    .build())
                .addProviderProductInfo(blueProductInfo().setMarketSkuId(0).build())
                .build(), true);

        assertEquals(ProviderProductInfoResponse.Status.OK, response.getStatus());

        offer = offerRepository.getOfferById(original.getId());
        assertEquals(123, offer.getSupplierSkuMapping().getMappingId()); // Не меняется
        assertEquals(0, offer.getContentSkuMapping().getMappingId());
        assertEquals(0, offer.getApprovedSkuMapping().getMappingId());
        assertNotEquals(Offer.ProcessingStatus.OPEN, offer.getProcessingStatus());
    }

    @Test
    public void testContentAddWhiteModelNotFoundError() {
        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo()
                    .setChangeSource(ChangeSource.CONTENT)
                    .build())
                .addProviderProductInfo(whiteProductInfo()
                    .setMappingType(MboMappings.MappingType.PRICE_COMPARISION)
                    .setMarketModelId(NONEXISING_MODEL_ID)
                    .build())
                .build(), true);

        assertEquals(ProviderProductInfoResponse.Status.ERROR, response.getStatus());
        validateHasError(response.getResults(0), ErrorKind.MODEL_NOT_FOUND);
        assertThat(response.getMessage()).contains("Model not exists");
        assertThat(response.getResults(0).getErrors(0).getMessage())
            .contains("Model not exists");
    }

    @Test
    public void testContentAddWhiteCategoryNotFoundError() {
        categoryCachingService.enableAuto(false);

        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo()
                    .setChangeSource(ChangeSource.CONTENT)
                    .build())
                .addProviderProductInfo(whiteProductInfo()
                    .setMappingType(MboMappings.MappingType.PRICE_COMPARISION)
                    .setMarketCategoryId((int) CATEGORY_ID)
                    .build())
                .build(), true);

        assertEquals(ProviderProductInfoResponse.Status.ERROR, response.getStatus());
        validateHasError(response.getResults(0), ErrorKind.CATEGORY_NOT_FOUND);
        assertThat(response.getMessage()).contains("Category not exists");
        assertThat(response.getResults(0).getErrors(0).getMessage())
            .contains("Category not exists");
    }

    @Test
    public void testSupplierAddWhiteModelNotFoundIgnored() {
        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo()
                    .setChangeSource(ChangeSource.SUPPLIER)
                    .build())
                .addProviderProductInfo(whiteProductInfo()
                    .setMappingType(MboMappings.MappingType.PRICE_COMPARISION)
                    .setMarketModelId(NONEXISING_MODEL_ID)
                    .build())
                .build(), true);

        assertEquals(ProviderProductInfoResponse.Status.OK, response.getStatus());
    }

    @Test
    public void testSupplierAddWhiteCategoryNotFoundIgnored() {
        categoryCachingService.enableAuto(false);

        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo()
                    .setChangeSource(ChangeSource.SUPPLIER)
                    .build())
                .addProviderProductInfo(whiteProductInfo()
                    .setMappingType(MboMappings.MappingType.PRICE_COMPARISION)
                    .setMarketCategoryId((int) CATEGORY_ID)
                    .build())
                .build(), true);

        assertEquals(ProviderProductInfoResponse.Status.OK, response.getStatus());
    }

    @Test
    public void testWhiteWrongOfferId() {
        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo()
                    .setChangeSource(ChangeSource.CONTENT)
                    .build())
                .addProviderProductInfo(whiteProductInfo()
                    .setShopSkuId(StringUtils.repeat('1', 81)) // 80 is maximum length for white offer id
                    .setMappingType(MboMappings.MappingType.PRICE_COMPARISION)
                    .setMarketCategoryId((int) CATEGORY_ID)
                    .build())
                .build(), true);

        assertEquals(ProviderProductInfoResponse.Status.ERROR, response.getStatus());
        validateHasError(response.getResults(0), ErrorKind.WRONG_SHOP_SKU);
    }

    @Test
    public void testAddWhiteModelIdOffer() {
        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo()
                    .setChangeSource(ChangeSource.CONTENT)
                    .build())
                .addProviderProductInfo(whiteProductInfo()
                    .setMappingType(MboMappings.MappingType.PRICE_COMPARISION)
                    .setMarketModelId(MODEL_ID)
                    .build())
                .build(), true);

        assertEquals(ProviderProductInfoResponse.Status.OK, response.getStatus());

        List<Offer> offers = offerRepository.findOffers(new OffersFilter());
        MbocAssertions.assertThat(offers).hasSize(1).first()
            .hasModelId(MODEL_ID)
            .doesNotHaveMappedCategoryId()
            .hasMappingDestination(Offer.MappingDestination.WHITE)
            .hasMappingsEqualAndNotNull(Offer.MappingType.CONTENT, Offer.MappingType.APPROVED);
    }

    @Test
    public void testSupplierAddsWhiteCategoryIdOffer() {
        List<Offer> offers = testAddWhiteCategoryIdOffer(ChangeSource.SUPPLIER);
        MbocAssertions.assertThat(offers).hasSize(1).first()
            .hasBindingKind(Offer.BindingKind.SUPPLIER)
            .hasSupplierCategoryId(CATEGORY_ID)
            .hasMappedCategoryId(CATEGORY_ID);
    }

    @Test
    public void testContentAddsWhiteCategoryIdOffer() {
        List<Offer> offers = testAddWhiteCategoryIdOffer(ChangeSource.CONTENT);
        MbocAssertions.assertThat(offers).hasSize(1).first()
            .hasBindingKind(Offer.BindingKind.APPROVED)
            .doesNotHaveSupplierCategoryId()
            .hasMappedCategoryId(CATEGORY_ID)
            .hasMappingsEqualAndNotNull(Offer.MappingType.CONTENT, Offer.MappingType.APPROVED);
    }

    public List<Offer> testAddWhiteCategoryIdOffer(ChangeSource changeSource) {
        mockSuggestedCategoryEnrichment(CATEGORY_ID_2);

        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo()
                    .setChangeSource(changeSource)
                    .build())
                .addProviderProductInfo(whiteProductInfo()
                    .setMappingType(MboMappings.MappingType.PRICE_COMPARISION)
                    .setMarketCategoryId((int) CATEGORY_ID)
                    .build())
                .build(), true, true);

        assertEquals(ProviderProductInfoResponse.Status.OK, response.getStatus());

        List<Offer> offers = offerRepository.findOffers(new OffersFilter());
        MbocAssertions.assertThat(offers).hasSize(1).first()
            .hasCategoryId(1)
            .hasMappingDestination(Offer.MappingDestination.WHITE)
            .hasMappingsEqualAndNotNull(Offer.MappingType.CONTENT, Offer.MappingType.APPROVED);
        return offers;
    }

    @Test
    public void testSimpleWhiteOffer() {
        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo().setChangeSource(ChangeSource.CONTENT).build())
                .addProviderProductInfo(MboMappings.ProviderProductInfo.newBuilder()
                    .setShopId(WHITE_SHOP_ID)
                    .setTitle("Тайтл")
                    .setShopCategoryName("Категория")
                    .setShopSkuId("ShopSku")
                    .setMappingType(MboMappings.MappingType.PRICE_COMPARISION)
                    .setMarketCategoryId((int) CATEGORY_ID)
                    .build())
                .build(), true);

        assertEquals(ProviderProductInfoResponse.Status.OK, response.getStatus());

        List<Offer> offers = offerRepository.findOffers(new OffersFilter());
        MbocAssertions.assertThat(offers)
            .hasSize(1)
            .first()
            .satisfies(o -> assertThat(o.getShopSku()).isEqualTo("ShopSku"));
    }

    @Test
    public void testUpdateEmptyWhiteOffer() {
        Offer offer = new Offer()
            .setBusinessId(WHITE_SHOP_ID)
            .setShopSku("ShopSku")
            .setTitle("Тайтл")
            .setShopCategoryName("ShopCategoryName")
            .setIsOfferContentPresent(true)
            .storeOfferContent(OfferContent.initEmptyContent())
            .setOfferDestination(Offer.MappingDestination.WHITE)
            .setMappingDestination(Offer.MappingDestination.WHITE);
        offerRepository.insertOffer(offer);

        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo()
                    .setChangeSource(ChangeSource.CONTENT)
                    .build())
                .addProviderProductInfo(whiteProductInfo()
                    .setMappingType(MboMappings.MappingType.PRICE_COMPARISION)
                    .setMarketCategoryId((int) CATEGORY_ID)
                    .build())
                .build(), true);

        assertEquals(ProviderProductInfoResponse.Status.OK, response.getStatus());

        List<Offer> offers = offerRepository.findOffers(new OffersFilter().setFetchOfferContent(true));
        MbocAssertions.assertThat(offers).hasSize(1).first()
            .hasCategoryId(CATEGORY_ID)
            .hasBindingKind(Offer.BindingKind.APPROVED)
            .doesNotHaveSupplierCategoryId()
            .hasMappedCategoryId(CATEGORY_ID)
            .hasTitle("Title")
            .hasShopCategoryName("ShopCategoryName")
            .hasDescription("Description")
            .hasVendor("Vendor")
            .hasVendorCode("VendorCode")
            .hasBarcodes("123", "321")
            .hasMappingDestination(Offer.MappingDestination.WHITE)
            .hasMappingsEqualAndNotNull(Offer.MappingType.CONTENT, Offer.MappingType.APPROVED);
    }

    @Test
    public void testClearWhiteOffer() {
        Offer offer = new Offer()
            .setBusinessId(WHITE_SHOP_ID)
            .setShopSku("ShopSku")
            .setCategoryIdForTests(100L, Offer.BindingKind.SUGGESTED)
            .setVendor("My vendor")
            .setModelId(100L)
            .setTitle("Тайтл")
            .setShopCategoryName("Категория")
            .setContentSkuMapping(new Offer.Mapping(100, DateTimeUtils.dateTimeNow()))
            .setIsOfferContentPresent(true)
            .storeOfferContent(OfferContent.initEmptyContent())
            .setOfferDestination(Offer.MappingDestination.WHITE)
            .setMappingDestination(Offer.MappingDestination.WHITE);
        offerRepository.insertOffer(offer);

        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo()
                    .setChangeSource(ChangeSource.CONTENT)
                    .build())
                .addProviderProductInfo(whiteProductInfo()
                    .setMappingType(MboMappings.MappingType.PRICE_COMPARISION)
                    .setMarketCategoryId(200)
                    .clearMarketModelId()
                    .setMarketSkuId(0)
                    .build())
                .build(), true);

        assertEquals(ProviderProductInfoResponse.Status.OK, response.getStatus());

        List<Offer> offers = offerRepository.findOffers(new OffersFilter());
        MbocAssertions.assertThat(offers).hasSize(1).first()
            .hasCategoryId(200)
            .hasMappedCategoryId(200)
            .hasBindingKind(Offer.BindingKind.APPROVED)
            .doesNotHaveSupplierCategoryId()
            .hasContentMapping(0)
            .hasMappingsEqualAndNotNull(Offer.MappingType.CONTENT, Offer.MappingType.APPROVED);
    }

    @Test
    public void testSupplerCategoryMappingSuggestedCategory() {
        mockSuggestedCategoryEnrichment(CATEGORY_ID_2);
        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo()
                    .setChangeSource(ChangeSource.SUPPLIER)
                    .build())
                .addProviderProductInfo(whiteProductInfo()
                    .setMappingType(MboMappings.MappingType.PRICE_COMPARISION)
                    .setMarketCategoryId((int) CATEGORY_ID)
                    .build())
                .build(), true, true);

        assertEquals(ProviderProductInfoResponse.Status.OK, response.getStatus());

        List<Offer> offers = offerRepository.findOffers(new OffersFilter());
        Assertions.assertThat(offers)
            .hasSize(1)
            .allSatisfy(offer -> {
                assertTrue(Offer.Mapping.mappingEqual(offer.getSupplierSkuMapping(), offer.getApprovedSkuMapping()));
                assertThat(offer.getCategoryId()).isEqualTo(CATEGORY_ID);
                assertThat(offer.getMappedCategoryId()).isEqualTo(CATEGORY_ID);
                assertThat(offer.getMappedCategoryConfidence()).isEqualTo(Offer.MappingConfidence.PARTNER);
                assertThat(offer.getMappingDestination()).isEqualTo(Offer.MappingDestination.WHITE);
            });
    }

    @Test
    public void testSupplerCategoryMappingApprovedCategory() {
        mockApprovedCategoryEnrichment(CATEGORY_ID_2);
        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo()
                    .setChangeSource(ChangeSource.SUPPLIER)
                    .build())
                .addProviderProductInfo(whiteProductInfo()
                    .setMappingType(MboMappings.MappingType.PRICE_COMPARISION)
                    .setMarketCategoryId((int) CATEGORY_ID)
                    .build())
                .build(), true, true);

        assertEquals(ProviderProductInfoResponse.Status.OK, response.getStatus());

        List<Offer> offers = offerRepository.findOffers(new OffersFilter());
        Assertions.assertThat(offers)
            .hasSize(1)
            .allSatisfy(offer -> {
                assertTrue(Offer.Mapping.mappingEqual(offer.getSupplierSkuMapping(), offer.getApprovedSkuMapping()));
                assertThat(offer.getCategoryId()).isEqualTo(CATEGORY_ID);
                assertThat(offer.getMappedCategoryId()).isEqualTo(CATEGORY_ID);
                assertThat(offer.getMappedCategoryConfidence()).isEqualTo(PARTNER);
                assertThat(offer.getMappingDestination()).isEqualTo(Offer.MappingDestination.WHITE);
            });
    }

    @Test
    public void testSupplerCategoryMappingMatching() {
        mockMatchingEnrichment(CATEGORY_ID_2, MODEL_ID);
        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo()
                    .setChangeSource(ChangeSource.SUPPLIER)
                    .build())
                .addProviderProductInfo(whiteProductInfo()
                    .setMappingType(MboMappings.MappingType.PRICE_COMPARISION)
                    .setMarketCategoryId((int) CATEGORY_ID)
                    .build())
                .build(), true, true);

        assertEquals(ProviderProductInfoResponse.Status.OK, response.getStatus());

        List<Offer> offers = offerRepository.findOffers(new OffersFilter());
        Assertions.assertThat(offers)
            .hasSize(1)
            .allSatisfy(offer -> {
                assertTrue(Offer.Mapping.mappingEqual(offer.getSupplierSkuMapping(), offer.getApprovedSkuMapping()));
                assertThat(offer.getCategoryId()).isEqualTo(CATEGORY_ID_2);
                assertThat(offer.getModelId()).isEqualTo(MODEL_ID);
                assertThat(offer.getMappedCategoryId()).isNull();
                assertThat(offer.getMappedCategoryConfidence()).isNull();
                assertThat(offer.getMappingDestination()).isEqualTo(Offer.MappingDestination.WHITE);
            });
    }

    @Test
    public void testSupplierModelMappingSuggestedCategory() {
        mockSuggestedCategoryEnrichment(CATEGORY_ID_2);
        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo()
                    .setChangeSource(ChangeSource.SUPPLIER)
                    .build())
                .addProviderProductInfo(whiteProductInfo()
                    .setMappingType(MboMappings.MappingType.PRICE_COMPARISION)
                    .setMarketModelId(MODEL_ID)
                    .build())
                .build(), true, true);

        assertEquals(ProviderProductInfoResponse.Status.OK, response.getStatus());

        List<Offer> offers = offerRepository.findOffers(new OffersFilter());
        Assertions.assertThat(offers)
            .hasSize(1)
            .allSatisfy(offer -> {
                assertThat(offer.getCategoryId()).isEqualTo(CATEGORY_ID);
                assertThat(offer.getMappedCategoryId()).isNull();
                assertThat(offer.getMappedCategoryConfidence()).isNull();
                assertThat(offer.getModelId()).isEqualTo(MODEL_ID);
                assertThat(offer.getMappedModelId()).isEqualTo(MODEL_ID);
                assertThat(offer.getMappedModelConfidence()).isEqualTo(Offer.MappingConfidence.PARTNER);
                assertThat(offer.getMappingDestination()).isEqualTo(Offer.MappingDestination.WHITE);
            });
    }

    @Test
    public void testSupplierModelMappingApprovedCategory() {
        mockApprovedCategoryEnrichment(CATEGORY_ID_2);
        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo()
                    .setChangeSource(ChangeSource.SUPPLIER)
                    .build())
                .addProviderProductInfo(whiteProductInfo()
                    .setMappingType(MboMappings.MappingType.PRICE_COMPARISION)
                    .setMarketModelId(MODEL_ID)
                    .build())
                .build(), true, true);

        assertEquals(ProviderProductInfoResponse.Status.OK, response.getStatus());

        List<Offer> offers = offerRepository.findOffers(new OffersFilter());
        Assertions.assertThat(offers)
            .hasSize(1)
            .allSatisfy(offer -> {
                assertThat(offer.getCategoryId()).isEqualTo(CATEGORY_ID_2);
                assertThat(offer.getMappedCategoryId()).isNull();
                assertThat(offer.getMappedCategoryConfidence()).isNull();
                assertThat(offer.getModelId()).isNull();
                assertThat(offer.getMappedModelId()).isNull();
                assertThat(offer.getMappedModelConfidence()).isNull();
                assertThat(offer.getMappingDestination()).isEqualTo(Offer.MappingDestination.WHITE);
            });
    }

    @Test
    public void testSupplierModelMappingApprovedCategorySame() {
        mockApprovedCategoryEnrichment(CATEGORY_ID);
        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo()
                    .setChangeSource(ChangeSource.SUPPLIER)
                    .build())
                .addProviderProductInfo(whiteProductInfo()
                    .setMappingType(MboMappings.MappingType.PRICE_COMPARISION)
                    .setMarketModelId(MODEL_ID)
                    .build())
                .build(), true, true);

        assertEquals(ProviderProductInfoResponse.Status.OK, response.getStatus());

        List<Offer> offers = offerRepository.findOffers(new OffersFilter());
        Assertions.assertThat(offers)
            .hasSize(1)
            .allSatisfy(offer -> {
                assertThat(offer.getCategoryId()).isEqualTo(CATEGORY_ID);
                assertThat(offer.getMappedCategoryId()).isNull();
                assertThat(offer.getMappedCategoryConfidence()).isNull();
                assertThat(offer.getModelId()).isEqualTo(MODEL_ID);
                assertThat(offer.getMappedModelId()).isEqualTo(MODEL_ID);
                assertThat(offer.getMappedModelConfidence()).isEqualTo(Offer.MappingConfidence.PARTNER);
                assertThat(offer.getMappingDestination()).isEqualTo(Offer.MappingDestination.WHITE);
            });
    }

    @Test
    public void testSupplierModelMappingOverExistingSupplier() {
        Offer before = commonWhiteOffer()
            .setCategoryIdForTests(1L, Offer.BindingKind.APPROVED)
            .setModelId(MODEL_ID)
            .setMappedModelId(MODEL_ID)
            .setMappedModelConfidence(Offer.MappingConfidence.PARTNER);

        offerRepository.insertOffer(before);

        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo()
                    .setChangeSource(ChangeSource.SUPPLIER)
                    .build())
                .addProviderProductInfo(whiteProductInfo()
                    .setMappingType(MboMappings.MappingType.PRICE_COMPARISION)
                    .setMarketModelId(MODEL_ID_2)
                    .build())
                .build(), true, true);

        assertEquals(ProviderProductInfoResponse.Status.OK, response.getStatus());

        List<Offer> offers = offerRepository.findOffers(new OffersFilter());
        Assertions.assertThat(offers)
            .hasSize(1)
            .allSatisfy(offer -> {
                assertThat(offer.getCategoryId()).isEqualTo(1L);
                assertThat(offer.getMappedCategoryId()).isNull();
                assertThat(offer.getMappedCategoryConfidence()).isNull();
                assertThat(offer.getModelId()).isEqualTo(MODEL_ID_2);
                assertThat(offer.getMappedModelId()).isEqualTo(MODEL_ID_2);
                assertThat(offer.getMappedModelConfidence()).isEqualTo(Offer.MappingConfidence.PARTNER);
                assertThat(offer.getMappingDestination()).isEqualTo(Offer.MappingDestination.WHITE);
            });
    }

    @Test
    public void testContentAddWhiteUploadToYtMarker() {
        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo()
                    .setChangeSource(ChangeSource.CONTENT)
                    .build())
                .addProviderProductInfo(whiteProductInfo()
                    .setMappingType(MboMappings.MappingType.PRICE_COMPARISION)
                    .setMarketCategoryId((int) CATEGORY_ID)
                    .build())
                .build(), true);

        assertEquals(ProviderProductInfoResponse.Status.OK, response.getStatus());

        List<Offer> offers = offerRepository.findOffers(new OffersFilter());
        Assertions.assertThat(offers)
            .hasSize(1)
            .allSatisfy(offer -> {
                assertTrue(Offer.Mapping.mappingEqual(offer.getContentSkuMapping(), offer.getApprovedSkuMapping()));
                assertThat(offer.getMappingDestination()).isEqualTo(Offer.MappingDestination.WHITE);
            });
    }

    @Test
    public void testUserIsTrackedToAuditInCaseItIsSetForContent() {
        service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo()
                    .setChangeSource(ChangeSource.CONTENT)
                    .setUserLogin("the-user")
                    .setAuditSource(MboAudit.Source.YANG_TASK)
                    .setAuditSourceId("10001000")
                    .build())
                .addProviderProductInfo(blueProductInfo().setMarketSkuId(MODEL_ID).build())
                .build(), true);

        List<Offer> offers = offerRepository.findAll();
        assertThat(offers).hasSize(1);
        Offer offer = offers.get(0);
        assertThat(auditServiceMock.getStaffLogins()).containsExactly("the-user");
        assertThat(offer.getMappingModifiedBy()).contains("the-user");
        assertThat(auditServiceMock.getSources()).containsEntry(MboAudit.Source.YANG_TASK, "10001000");
    }

    @Test
    public void testContentChangesArentAllowedWithoutUserLogin() {
        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo()
                    .setChangeSource(ChangeSource.CONTENT)
                    .clearUserLogin()
                    .setAuditSource(MboAudit.Source.YANG_TASK)
                    .setAuditSourceId("10001000")
                    .build())
                .addProviderProductInfo(blueProductInfo().setMarketSkuId(0).build())
                .build(), true);

        assertThat(response.getStatus()).isEqualTo(ProviderProductInfoResponse.Status.ERROR);
        assertThat(response.getMessage()).isEqualTo("ChangeSource == CONTENT but no user_login is provided");
        assertThat(auditServiceMock.getSources()).doesNotContainEntry(MboAudit.Source.YANG_TASK, "10001000");
    }

    @Test
    public void testUserIsTrackedToAuditInCaseItIsSetForSupplier() {
        service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo()
                    .setChangeSource(ChangeSource.SUPPLIER)
                    .setUserLogin("the-user")
                    .setAuditSource(MboAudit.Source.YANG_TASK)
                    .setAuditSourceId("10001000")
                    .build())
                .addProviderProductInfo(blueProductInfo().setMarketSkuId(0).build())
                .build(), true);

        List<Offer> offers = offerRepository.findAll();
        assertThat(offers).hasSize(1);
        Offer offer = offers.get(0);
        assertThat(auditServiceMock.getStaffLogins()).containsExactly("the-user");
        assertThat(offer.getMappingModifiedBy()).isNullOrEmpty();
        assertThat(auditServiceMock.getSources()).containsEntry(MboAudit.Source.YANG_TASK, "10001000");
    }

    @Test
    public void testUserIsTrackedToAuditInCaseItIsNotSetForSupplier() {
        service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo()
                    .setChangeSource(ChangeSource.SUPPLIER)
                    .clearUserLogin()
                    .setAuditSource(MboAudit.Source.YANG_TASK)
                    .setAuditSourceId("10001000")
                    .build())
                .addProviderProductInfo(blueProductInfo().setMarketSkuId(0).build())
                .build(), true);

        List<Offer> offers = offerRepository.findAll();
        assertThat(offers).hasSize(1);
        Offer offer = offers.get(0);
        assertThat(auditServiceMock.getStaffLogins()).containsExactly(MBO_MAPPINGS_SERVICE_DEFAULT_USER);
        assertThat(offer.getMappingModifiedBy()).isNullOrEmpty();
        assertThat(auditServiceMock.getSources()).containsEntry(MboAudit.Source.YANG_TASK, "10001000");
    }

    @Test
    public void testIncorrectShopId() {
        int wrongSupplierId = 666;
        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo()
                    .setChangeSource(ChangeSource.SUPPLIER)
                    .clearUserLogin()
                    .build())
                .addProviderProductInfo(blueProductInfo()
                    .setShopId(wrongSupplierId) // WRONG
                    .build())
                .build(), true);

        assertThat(response.getStatus()).isEqualTo(ProviderProductInfoResponse.Status.ERROR);
        assertThat(response.getMessage()).contains("Supplier with id=666 not found.");
        assertThat(response.getResults(0).getErrors(0).getMessage())
            .contains("Supplier with id=666 not found.");
        validateHasError(response.getResults(0), ErrorKind.WRONG_SHOP_ID);
    }

    @Test
    public void testSaveMasterDataForBlueOfferInCaseItCorrect() {
        MasterDataInfo masterDataInfo = masterData().build();
        MboMappings.ProviderProductInfo offerInfo = blueProductInfo()
            .setMasterDataInfo(masterDataInfo)
            .build();

        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo().build())
                .addProviderProductInfo(offerInfo)
                .build(), true);

        assertThat(response.getStatus()).isEqualTo(ProviderProductInfoResponse.Status.OK);
        assertThat(response.getResultsList())
            .extracting(ProviderProductInfoResponse.ProductResult::getStatus)
            .containsExactly(ProviderProductInfoResponse.Status.OK);

        List<MasterData> masterDataList = masterDataServiceMock.getSskuMasterData().values().stream()
            .map(MbocBaseProtoConverter::protoToPojo)
            .collect(Collectors.toList());

        assertThat(masterDataList).hasSize(1);
        MasterData masterData = masterDataList.get(0);

        MboTimeUnitAliasesService timeUnitAliasesService = Mockito.mock(MboTimeUnitAliasesService.class);
        MasterDataFromMdiConverter masterDataFromMdiConverter = new MasterDataFromMdiConverter(timeUnitAliasesService);
        assertSoftly(softly -> {
            softly.assertThat(masterData.getSupplierId()).isEqualTo(offerInfo.getShopId());
            softly.assertThat(masterData.getShopSku()).isEqualTo(offerInfo.getShopSkuId());
            softly.assertThat(masterData)
                .isEqualToIgnoringGivenFields(
                    masterDataFromMdiConverter.convertMdiToMasterData(
                        Collections.singleton(Maps.immutableEntry(masterData.getShopSkuKey(), masterDataInfo))).get(0),
                    "supplierId", "shopSku", "categoryId"
                );
        });
    }

    @Test
    public void testDoNotSaveMasterDataForWhiteOffer() {
        MasterDataInfo masterDataInfo = masterData().build();
        MboMappings.ProviderProductInfo offerInfo = whiteProductInfo()
            .setMarketSkuId(MODEL_ID)
            .setMasterDataInfo(masterDataInfo)
            .build();

        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo().build())
                .addProviderProductInfo(offerInfo)
                .build(), true);

        assertThat(response.getStatus()).isEqualTo(ProviderProductInfoResponse.Status.OK);
        assertThat(response.getResultsList())
            .extracting(ProviderProductInfoResponse.ProductResult::getStatus)
            .containsExactly(ProviderProductInfoResponse.Status.OK);

        List<MasterData> masterDataList = masterDataServiceMock.getSskuMasterData().values().stream()
            .map(MbocBaseProtoConverter::protoToPojo)
            .collect(Collectors.toList());
        assertThat(masterDataList).hasSize(0);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testDoNotFailRequestForWhiteOfferInCaseMasterDataIsInvalid() {
        MasterDataInfo masterDataInfo = masterData()
            .setShelfLife(INVALID_SHELF_LIFE)
            .setProviderProductMasterData(providerProductMasterData()
                .clearManufacturerCountry()
                .setMinShipment(INVALID_MIN_SHIPMENT)
            ).build();
        MboMappings.ProviderProductInfo offerInfo = whiteProductInfo()
            .setMarketSkuId(MODEL_ID)
            .setMasterDataInfo(masterDataInfo)
            .build();

        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo().build())
                .addProviderProductInfo(offerInfo)
                .build(), true);

        assertThat(response.getStatus()).isEqualTo(ProviderProductInfoResponse.Status.OK);
        assertThat(response.getResultsList())
            .extracting(ProviderProductInfoResponse.ProductResult::getStatus)
            .containsExactly(ProviderProductInfoResponse.Status.OK);

        List<MasterData> masterDataList = masterDataServiceMock.getSskuMasterData().values().stream()
            .map(MbocBaseProtoConverter::protoToPojo)
            .collect(Collectors.toList());
        assertThat(masterDataList).hasSize(0);
    }

    @Test
    public void testSaveMasterDataInCaseNonRequiredFieldsAreMissing() {
        MasterDataInfo masterDataInfo = MasterDataInfo.newBuilder()
            .setProviderProductMasterData(ProviderProductMasterData.newBuilder()
                .addManufacturerCountry(VALID_COUNTRY)
                .build())
            .build();
        MboMappings.ProviderProductInfo offerInfo = blueProductInfo()
            .setMasterDataInfo(masterDataInfo)
            .build();

        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo().build())
                .addProviderProductInfo(offerInfo)
                .build(), true);

        assertThat(response.getStatus()).isEqualTo(ProviderProductInfoResponse.Status.OK);
        assertThat(response.getResultsList())
            .extracting(ProviderProductInfoResponse.ProductResult::getStatus)
            .containsExactly(ProviderProductInfoResponse.Status.OK);

        List<MasterData> masterDataList = masterDataServiceMock.getSskuMasterData().values().stream()
            .map(MbocBaseProtoConverter::protoToPojo)
            .collect(Collectors.toList());
        assertThat(masterDataList).hasSize(1);
        MasterData masterData = masterDataList.get(0);

        MboTimeUnitAliasesService timeUnitAliasesService = Mockito.mock(MboTimeUnitAliasesService.class);
        MasterDataFromMdiConverter masterDataFromMdiConverter = new MasterDataFromMdiConverter(timeUnitAliasesService);
        assertSoftly(softly -> {
            softly.assertThat(masterData.getSupplierId()).isEqualTo(offerInfo.getShopId());
            softly.assertThat(masterData.getShopSku()).isEqualTo(offerInfo.getShopSkuId());
            softly.assertThat(masterData)
                .isEqualToIgnoringGivenFields(
                    masterDataFromMdiConverter.convertMdiToMasterData(
                        Collections.singleton(Maps.immutableEntry(masterData.getShopSkuKey(), masterDataInfo))).get(0),
                    "supplierId", "shopSku", "categoryId"
                );
        });
    }

    @Test
    public void testFailRequestInCaseOfSomeValidationError() {
        MasterDataInfo masterDataInfo = masterData()
            .build();
        MboMappings.ProviderProductInfo offerInfo = blueProductInfo()
            .setMasterDataInfo(masterDataInfo)
            .build();

        masterDataServiceMock.setSaveErrors(Collections.singletonList(validationError(
            offerInfo,
            "some.error",
            "Ошибка валидации: {{message}}",
            "{\"message\":\"Пример сообщения об ошибке testFailRequestInCaseOfSomeValidationError\"}"
        )));

        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo().build())
                .addProviderProductInfo(offerInfo)
                .build(), true);

        assertThat(response.getResultsList()).hasSize(1);

        assertSoftly(softly -> {
            softly.assertThat(response.getStatus()).isEqualTo(ProviderProductInfoResponse.Status.ERROR);
            validateHasError(response.getResults(0), ErrorKind.MASTER_DATA_VALIDATION);
            softly.assertThat(response.getResults(0).getErrorsCount()).isEqualTo(1);
            softly.assertThat(response.getMessage())
                .contains("Пример сообщения об ошибке testFailRequestInCaseOfSomeValidationError");
        });
    }

    @Test
    public void testMasterDataNotSavedNorValidatedForFmcgOffers() {
        supplierRepository.insert(OfferTestUtils.fmcgSupplier());

        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo().setChangeSource(ChangeSource.SUPPLIER).build())
                .addProviderProductInfo(fmcgProductInfoWithoutMD().setMasterDataInfo(masterData()))
                .addProviderProductInfo(fmcgProductInfoWithoutMD().setShopSkuId("other-sku-without-md").build())
                .build(), true);
        assertEquals(ProviderProductInfoResponse.Status.OK, response.getStatus());
        List<Offer> offers = offerRepository.findAll();
        assertThat(offers).hasSize(2);

        List<MasterData> masterDataList = masterDataServiceMock.getSskuMasterData().values().stream()
            .map(MbocBaseProtoConverter::protoToPojo)
            .collect(Collectors.toList());

        assertThat(masterDataList).isEmpty();
    }

    @Test
    public void testMasterDataNotSavedNorValidatedIfDisabled() {
        Supplier supplier = supplierRepository.insert(OfferTestUtils.simpleSupplier()
            .setId(1234)
            .setNewContentPipeline(true)
            .setDisableMdm(true));

        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo().setChangeSource(ChangeSource.SUPPLIER).build())
                .addProviderProductInfo(fmcgProductInfoWithoutMD()
                    .setShopId(supplier.getId())
                    .setMasterDataInfo(masterData()))
                .addProviderProductInfo(fmcgProductInfoWithoutMD()
                    .setShopId(supplier.getId())
                    .setShopSkuId("other-sku-without-md"))
                .build(), true);
        assertEquals(ProviderProductInfoResponse.Status.OK, response.getStatus());
        List<Offer> offers = offerRepository.findAll();
        assertThat(offers).hasSize(2);

        List<MasterData> masterDataList = masterDataServiceMock.getSskuMasterData().values().stream()
            .map(MbocBaseProtoConverter::protoToPojo)
            .collect(Collectors.toList());

        assertThat(masterDataList).isEmpty();
    }

    @Test
    public void testOfferProcessingStatusNotChangedInOldPipeline() {
        // white shop old pipe
        ProviderProductInfoResponse responseWhite = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo().setChangeSource(ChangeSource.SUPPLIER).build())
                .addProviderProductInfo(
                    whiteProductInfo().setMarketSkuId(MODEL_ID).build())
                .build(), true, true);
        assertEquals(ProviderProductInfoResponse.Status.OK, responseWhite.getStatus());
        // blue shop old pipe
        ProviderProductInfoResponse responseBlue = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo().setChangeSource(ChangeSource.SUPPLIER).build())
                .addProviderProductInfo(
                    blueProductInfo().setMarketSkuId(MODEL_ID).build())
                .build(), true, true);
        assertEquals(ProviderProductInfoResponse.Status.OK, responseBlue.getStatus());

        List<Offer> offers = offerRepository.findAll();
        assertThat(offers).hasSize(2);
        offers.forEach(offer -> {
            assertNotNull(offer.getSupplierSkuMapping());
            assertEquals(Offer.ProcessingStatus.OPEN, offer.getProcessingStatus());
        });
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void testOfferProcessingStatusChangedInNewPipeline() {
        supplierRepository.update(new Supplier(42, "blue")
            .setType(MbocSupplierType.THIRD_PARTY)
            .setNewContentPipeline(true));
        supplierRepository.update(new Supplier(4242, "white")
            .setType(MbocSupplierType.MARKET_SHOP)
            .setNewContentPipeline(true));
        categoryCachingService.addCategories(new Category().setCategoryId(CATEGORY_ID).setHasKnowledge(true));
        categoryKnowledgeService.addCategory(CATEGORY_ID);

        mockSuggestedCategoryEnrichment(CATEGORY_ID);

        // white shop new pipe
        ProviderProductInfoResponse responseWhite = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo().setChangeSource(ChangeSource.SUPPLIER).build())
                .addProviderProductInfo(
                    whiteProductInfo().setMarketSkuId(MODEL_ID).build())
                .build(), true, true);
        assertEquals(ProviderProductInfoResponse.Status.OK, responseWhite.getStatus());
        // blue shop new pipe
        ProviderProductInfoResponse responseBlue = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo().setChangeSource(ChangeSource.SUPPLIER).build())
                .addProviderProductInfo(
                    blueProductInfo().setMarketSkuId(MODEL_ID).setMarketCategoryId((int) CATEGORY_ID).build())
                .build(), true, true);
        assertEquals(ProviderProductInfoResponse.Status.OK, responseBlue.getStatus());


        List<Offer> offers = offerRepository.findAll();
        assertThat(offers).hasSize(2);
        assertNotNull(offers.get(0).getSupplierSkuMapping());
        assertEquals(Offer.ProcessingStatus.OPEN, offers.get(0).getProcessingStatus());
        assertNotNull(offers.get(1).getSupplierSkuMapping());
        assertEquals(Offer.ProcessingStatus.IN_CLASSIFICATION, offers.get(1).getProcessingStatus());
    }

    @Test
    public void testMappingsApprovedForFmcgSupplier() {
        supplierRepository.insert(OfferTestUtils.fmcgSupplier());

        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo().setChangeSource(ChangeSource.SUPPLIER).build())
                .addProviderProductInfo(fmcgProductInfoWithoutMD())
                .build(), true);
        assertEquals(ProviderProductInfoResponse.Status.OK, response.getStatus());

        List<Offer> offers = offerRepository.findAll();
        assertThat(offers).hasSize(1);
        Offer offer = offers.get(0);
        assertThat(offer.hasSupplierSkuMapping()).isTrue();
        assertThat(offer.getSupplierSkuMappingStatus()).isEqualTo(Offer.MappingStatus.ACCEPTED);
        assertThat(offer.hasApprovedSkuMapping()).isTrue();
        assertThat(offer.getBindingKind()).isEqualTo(Offer.BindingKind.SUGGESTED);
        assertThat(offer.getAcceptanceStatus()).isEqualTo(Offer.AcceptanceStatus.OK);
        assertThat(offer.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.PROCESSED);
    }

    @Test
    public void testUpdateToNegativeOneSupplier() {
        MboMappings.ProviderProductInfoRequest request = MboMappings.ProviderProductInfoRequest.newBuilder()
            .setRequestInfo(commonRequestInfo()
                .setChangeSource(ChangeSource.SUPPLIER)
                .build())
            .addProviderProductInfo(blueProductInfo().setMarketSkuId(-1).build())
            .build();
        Offer original = commonBlueOffer().setCategoryIdForTests(1L, Offer.BindingKind.APPROVED);

        offerRepository.insertOffer(original);
        Offer offer = offerRepository.getOfferById(original.getId());
        assertEquals(123L, offer.getSupplierSkuMapping().getMappingId());

        //  supplier must be new content
        supplierRepository.update(new Supplier(BLUE_SHOP_ID, "LoL").setNewContentPipeline(false));
        supplierService.invalidateCache();
        Assertions.assertThatThrownBy(() -> service.addProductInfo(request, true))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("-1 argument allowed only for new pipeline suppliers");

        //  offer must be in NEED_INFO status
        supplierRepository.update(new Supplier(BLUE_SHOP_ID, "LoL").setNewContentPipeline(true));
        supplierService.invalidateCache();
        Assertions.assertThatThrownBy(() -> service.addProductInfo(request, true))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("-1 argument allowed only for NEED_INFO, NEED_MAPPING or NEED_CONTENT offers");

        //  correct call
        supplierRepository.update(new Supplier(BLUE_SHOP_ID, "LoL").setNewContentPipeline(true));
        supplierService.invalidateCache();
        offerRepository.updateOffer(offer.updateProcessingStatusIfValid(Offer.ProcessingStatus.NEED_INFO));
        ProviderProductInfoResponse res = service.addProductInfo(request, true);

        assertEquals(ProviderProductInfoResponse.Status.OK, res.getStatus());
        offer = offerRepository.getOfferById(original.getId());
        assertNull(offer.getSupplierSkuMapping());
        assertEquals(Offer.MappingStatus.NONE, offer.getSupplierSkuMappingStatus());
        assertEquals(Offer.ProcessingStatus.NEED_CONTENT, offer.getProcessingStatus());
    }

    @Test
    public void testOffersLeaveNeedContentWhenCategoryWithoutGoodContent() {
        categoryCachingService.setGoodContentDefault(false);
        MboMappings.ProviderProductInfoRequest request = MboMappings.ProviderProductInfoRequest.newBuilder()
            .setRequestInfo(commonRequestInfo()
                .setChangeSource(ChangeSource.SUPPLIER)
                .build())
            .addProviderProductInfo(blueProductInfo().setMarketSkuId(-1).build())
            .build();
        Offer original = commonBlueOffer().setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED);

        offerRepository.insertOffer(original);
        categoryKnowledgeService.addCategory(1L);
        Offer offer = offerRepository.getOfferById(original.getId());
        assertEquals(123L, offer.getSupplierSkuMapping().getMappingId());

        //  supplier must be new content
        //  offer must be in NEED_CONTENT status
        supplierRepository.update(new Supplier(BLUE_SHOP_ID, "LoL").setNewContentPipeline(true));
        offerRepository.updateOffer(offer.updateProcessingStatusIfValid(Offer.ProcessingStatus.NEED_CONTENT));
        ProviderProductInfoResponse res = service.addProductInfo(request, true);

        assertEquals(ProviderProductInfoResponse.Status.OK, res.getStatus());
        offer = offerRepository.getOfferById(original.getId());
        assertNull(offer.getSupplierSkuMapping());
        assertEquals(Offer.MappingStatus.NONE, offer.getSupplierSkuMappingStatus());
        assertEquals(Offer.ProcessingStatus.IN_CLASSIFICATION, offer.getProcessingStatus());
    }

    @Test
    public void testOfferDoesNotLeaveNeedContentIfOfferNotChanged() {
        categoryCachingService.addCategory(CATEGORY_ID);
        categoryCachingService.setCategoryHasKnowledge(CATEGORY_ID, true);
        categoryCachingService.setCategoryAcceptGoodContent(CATEGORY_ID, true);
        categoryKnowledgeService.addCategory(CATEGORY_ID);
        MboMappings.ProviderProductInfoRequest request = MboMappings.ProviderProductInfoRequest.newBuilder()
            .setRequestInfo(commonRequestInfo()
                .setChangeSource(ChangeSource.SUPPLIER)
                .build())
            .addProviderProductInfo(blueProductInfo()
                .clearMarketModelId()
                .clearMarketSkuId()
                .build())
            .build();
        Offer original = commonBlueOffer()
            .setBindingKind(Offer.BindingKind.APPROVED)
            .setReprocessRequested(true)
            .setSupplierSkuMapping(null)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.NEED_CONTENT);

        offerRepository.insertOffer(original);

        supplierRepository.update(new Supplier(BLUE_SHOP_ID, "LoL").setNewContentPipeline(true));

        ProviderProductInfoResponse res = service.addProductInfo(request, true);
        assertEquals(ProviderProductInfoResponse.Status.OK, res.getStatus());

        Offer offer = offerRepository.getOfferById(original.getId());
        assertEquals(Offer.ProcessingStatus.NEED_CONTENT, offer.getProcessingStatus());
    }

    @Test
    public void testUpdateToNegativeOneSupplierFromNeedMappingStatus() {
        MboMappings.ProviderProductInfoRequest request = MboMappings.ProviderProductInfoRequest.newBuilder()
            .setRequestInfo(commonRequestInfo()
                .setChangeSource(ChangeSource.SUPPLIER)
                .build())
            .addProviderProductInfo(blueProductInfo().setMarketSkuId(-1).build())
            .build();
        Offer original = commonBlueOffer().setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED);

        offerRepository.insertOffer(original);
        Offer offer = offerRepository.getOfferById(original.getId());
        assertEquals(123L, offer.getSupplierSkuMapping().getMappingId());

        //  supplier must be new content
        //  offer must be in NEED_CONTENT status
        supplierRepository.update(new Supplier(BLUE_SHOP_ID, "LoL").setNewContentPipeline(true));
        supplierService.invalidateCache();
        offerRepository.updateOffer(offer.updateProcessingStatusIfValid(Offer.ProcessingStatus.NEED_CONTENT));
        ProviderProductInfoResponse res = service.addProductInfo(request, true);

        assertEquals(ProviderProductInfoResponse.Status.OK, res.getStatus());
        offer = offerRepository.getOfferById(original.getId());
        assertNull(offer.getSupplierSkuMapping());
        assertEquals(Offer.MappingStatus.NONE, offer.getSupplierSkuMappingStatus());
        assertEquals(Offer.ProcessingStatus.IN_CLASSIFICATION, offer.getProcessingStatus());
    }

    @Test
    public void testUpdateToNegativeOneContent() {
        Offer original = commonBlueOffer();
        original.setContentSkuMapping(original.getSupplierSkuMapping());

        offerRepository.insertOffer(original);
        Offer offer = offerRepository.getOfferById(original.getId());
        assertEquals(123L, offer.getSupplierSkuMapping().getMappingId());

        Assertions.assertThatThrownBy(() -> service.addProductInfo(
                MboMappings.ProviderProductInfoRequest.newBuilder()
                    .setRequestInfo(commonRequestInfo()
                        .setChangeSource(ChangeSource.CONTENT)
                        .build())
                    .addProviderProductInfo(blueProductInfo().setMarketSkuId(-1).build())
                    .build(), true)).isInstanceOf(IllegalArgumentException.class)
            .hasMessage("-1 not allowed as content mapping id");
    }

    @Test
    public void testDoProductInfoUsesEnrichmentFlag() {
        ProviderProductInfoResponse response;
        // false - no call
        response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo()
                    .setChangeSource(ChangeSource.SUPPLIER)
                    .build())
                .addProviderProductInfo(blueProductInfo()
                    .setMappingType(MboMappings.MappingType.REAL_SUPPLIER)
                    .build())
                .build(), true, false);

        assertEquals(ProviderProductInfoResponse.Status.OK, response.getStatus());
        assertEquals(response.getResults(0).getStatus(), ProviderProductInfoResponse.Status.OK);
        Mockito.verify(offersEnrichmentService, Mockito.times(0))
            .enrichOffers(anyList(), Mockito.anyBoolean(),
                Mockito.anyMap());

        //true - should enrich offers
        response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo()
                    .setChangeSource(ChangeSource.SUPPLIER)
                    .build())
                .addProviderProductInfo(blueProductInfo()
                    .setMappingType(MboMappings.MappingType.REAL_SUPPLIER)
                    .build())
                .build(), true, true);
        assertEquals(ProviderProductInfoResponse.Status.OK, response.getStatus());
        assertEquals(response.getResults(0).getStatus(), ProviderProductInfoResponse.Status.OK);
        Mockito.verify(offersEnrichmentService, Mockito.times(1))
            .enrichOffers(anyList(), Mockito.anyBoolean(),
                Mockito.anyMap());
    }

    @Test
    public void shouldHaveSuccessResponseIfEnrichmentFailed() {
        Mockito.doThrow(new RuntimeException())
            .when(offersEnrichmentService)
            .enrichOffers(anyList(), Mockito.anyBoolean(),
                Mockito.anyMap());
        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo()
                    .setChangeSource(ChangeSource.SUPPLIER)
                    .build())
                .addProviderProductInfo(blueProductInfo()
                    .setMappingType(MboMappings.MappingType.REAL_SUPPLIER)
                    .build())
                .build(), true, true);
        assertEquals(ProviderProductInfoResponse.Status.OK, response.getStatus());
        assertEquals(response.getResults(0).getStatus(), ProviderProductInfoResponse.Status.OK);
        Mockito.verify(offersEnrichmentService, Mockito.times(1))
            .enrichOffers(anyList(), Mockito.anyBoolean(),
                Mockito.anyMap());
    }

    @Test
    public void testSupplierAddsCategoryIdWithConfidentClassification() {
        doAnswer(i -> {
            List<Offer> offers = i.getArgument(0);
            offers.forEach(o -> o.setCategoryIdForTests(CATEGORY_ID, Offer.BindingKind.APPROVED)
                .setAutomaticClassification(true));
            return null;
        }).when(offersEnrichmentService).enrichOffers(anyList(), Mockito.anyBoolean(),
            Mockito.anyMap());

        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo()
                    .setChangeSource(ChangeSource.SUPPLIER)
                    .build())
                .addProviderProductInfo(whiteProductInfo()
                    .setMarketCategoryId((int) CATEGORY_ID_2)
                    .build())
                .build(), true, true);

        assertEquals(ProviderProductInfoResponse.Status.OK, response.getStatus());

        List<Offer> offers = offerRepository.findOffers(new OffersFilter());
        MbocAssertions.assertThat(offers).hasSize(1).first()
            .hasCategoryId(2)
            .hasBindingKind(Offer.BindingKind.SUPPLIER)
            .hasSupplierCategoryId(CATEGORY_ID_2)
            .hasMappedCategoryId(CATEGORY_ID_2)
            .hasMappingDestination(Offer.MappingDestination.WHITE)
            .hasMappingsEqualAndNotNull(Offer.MappingType.CONTENT, Offer.MappingType.APPROVED);
    }

    @Test
    public void changedMappingsServiceCalledWhenCorrectType() {
        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo()
                    .setChangeType(MboMappings.ProductUpdateRequestInfo.ChangeType.UNKNOWN)
                    .build())
                .addProviderProductInfo(blueProductInfo().build())
                .build(), true);

        Assertions.assertThat(response.getStatus()).isEqualTo(ProviderProductInfoResponse.Status.OK);

        response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo()
                    .setChangeType(MboMappings.ProductUpdateRequestInfo.ChangeType.MOVE_MAPPINGS)
                    .build())
                .addProviderProductInfo(blueProductInfo().build())
                .build(), true);

        Assertions.assertThat(response.getStatus()).isEqualTo(ProviderProductInfoResponse.Status.OK);
    }

    @Test
    public void changedMappingsServiceDoesNotFailWithoutApprovedMapping() {
        Offer original = commonBlueOffer();
        offerRepository.insertOffer(original);

        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo()
                    .setChangeType(MboMappings.ProductUpdateRequestInfo.ChangeType.MOVE_MAPPINGS)
                    .build())
                .addProviderProductInfo(blueProductInfo().build())
                .build(), true);

        Assertions.assertThat(response.getStatus()).isEqualTo(ProviderProductInfoResponse.Status.OK);
    }

    @Test
    public void processedAutoOffersSetToAutoProcessedState() {
        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo()
                    .setChangeSource(ChangeSource.AUTO)
                    .build())
                .addProviderProductInfo(blueProductInfo().build())
                .build(), true);

        Assertions.assertThat(response.getStatus()).isEqualTo(ProviderProductInfoResponse.Status.OK);

        List<Offer> offers = offerRepository.findAll();
        assertThat(offers).hasSize(1);
        assertNotNull(offers.get(0).getApprovedSkuMapping());

        Assertions.assertThat(offers.get(0).getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.AUTO_PROCESSED);
        Assertions.assertThat(offers.get(0).getApprovedSkuMappingConfidence()).isEqualTo(PARTNER_SELF);
        Assertions.assertThat(offers.get(0).getAcceptanceStatus()).isEqualTo(Offer.AcceptanceStatus.OK);
    }

    @Test
    public void betterContentSetsContentConfidence() {
        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo()
                    .setChangeSource(ChangeSource.BETTER)
                    .build())
                .addProviderProductInfo(blueProductInfo().build())
                .build(), true);

        Assertions.assertThat(response.getStatus()).isEqualTo(ProviderProductInfoResponse.Status.OK);

        List<Offer> offers = offerRepository.findAll();
        assertThat(offers).hasSize(1);
        assertNotNull(offers.get(0).getApprovedSkuMapping());

        Assertions.assertThat(offers.get(0).getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.AUTO_PROCESSED);
        Assertions.assertThat(offers.get(0).getApprovedSkuMappingConfidence()).isEqualTo(CONTENT);
        Assertions.assertThat(offers.get(0).getAcceptanceStatus()).isEqualTo(Offer.AcceptanceStatus.OK);
    }

    @Test
    public void notProcessedAutoOffersNotSetToAutoProcessedState() {
        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo()
                    .setChangeSource(ChangeSource.AUTO)
                    .build())
                .addProviderProductInfo(whiteProductInfo().setMarketCategoryId((int) CATEGORY_ID).build())
                .build(), true);

        Assertions.assertThat(response.getStatus()).isEqualTo(ProviderProductInfoResponse.Status.OK);

        List<Offer> offers = offerRepository.findAll();
        assertThat(offers).hasSize(1);
        assertNull(offers.get(0).getContentSkuMapping());

        Assertions.assertThat(offers.get(0).getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.OPEN);
        Assertions.assertThat(offers.get(0).getAcceptanceStatus()).isEqualTo(Offer.AcceptanceStatus.NEW);
    }

    @Test
    public void testUpdateMappingsDoesntChangeApprovedTs() {
        Offer offer = commonBlueOffer()
            .updateApprovedSkuMapping(new Offer.Mapping(MODEL_ID, LocalDateTime.parse("2017-10-28T10:15:20")), CONTENT);
        offerRepository.insertOffers(offer);

        MboMappings.ProductUpdateRequestInfo requestInfo = MboMappings.ProductUpdateRequestInfo.newBuilder()
            .setChangeSource(MboMappings.ProductUpdateRequestInfo.ChangeSource.CONTENT)
            .setUserLogin("test")
            .build();
        MboMappings.ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(requestInfo)
                .addProviderProductInfo(blueProductInfo()
                    .setMarketSkuId(MODEL_ID)
                    .build())
                .build(), false);

        Assertions.assertThat(response.getStatus()).isEqualTo(MboMappings.ProviderProductInfoResponse.Status.OK);

        offer = offerRepository.getOfferById(offer.getId());
        Assertions.assertThat(offer.getApprovedSkuMapping().getTimestamp())
            .isEqualTo(LocalDateTime.parse("2017-10-28T10:15:20"));
    }

    @Test
    public void testSupplierMappingToNonOperatorQualityModelIgnored() {
        mockNonOperatorQualityModel(TEST_SKU_ID);

        Offer existingOffer = commonBlueOffer()
            .setSupplierSkuMapping(null);
        offerRepository.insertOffer(existingOffer);

        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo().setChangeSource(ChangeSource.SUPPLIER).build())
                .addProviderProductInfo(
                    blueProductInfo().setMarketSkuId(TEST_SKU_ID).build())
                .build(), true);

        assertEquals(ProviderProductInfoResponse.Status.OK, response.getStatus());
        assertEquals(1, response.getResultsCount());

        Offer savedOffer = offerRepository.getOfferById(existingOffer.getId());
        Assertions.assertThat(savedOffer.getSupplierSkuMapping()).isNull();
    }

    @Test
    public void testDeduplicationMappingToNonOperatorQualityModelFailsWithError() {
        mockNonOperatorQualityModel(TEST_SKU_ID);

        Offer existingOffer = commonBlueOffer()
            .setSupplierSkuMapping(null);
        offerRepository.insertOffer(existingOffer);

        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo()
                    .setChangeSource(ChangeSource.CONTENT)
                    .setChangeType(MboMappings.ProductUpdateRequestInfo.ChangeType.DEDUPLICATION)
                    .build())
                .addProviderProductInfo(
                    blueProductInfo().setMarketSkuId(TEST_SKU_ID).build())
                .build(), true);

        assertEquals(ProviderProductInfoResponse.Status.ERROR, response.getStatus());
        assertEquals(1, response.getResultsCount());

        Offer savedOffer = offerRepository.getOfferById(existingOffer.getId());
        Assertions.assertThat(savedOffer.getContentSkuMapping()).isNull();
        Assertions.assertThat(savedOffer.getApprovedSkuMapping()).isNull();
    }

    @Test
    public void testBetterMappingToNonOperatorQualityModelFailsWithError() {
        mockNonOperatorQualityModel(TEST_SKU_ID);

        Offer existingOffer = commonBlueOffer()
            .setSupplierSkuMapping(null);
        offerRepository.insertOffer(existingOffer);

        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo()
                    .setChangeSource(ChangeSource.BETTER)
                    .build())
                .addProviderProductInfo(
                    blueProductInfo().setMarketSkuId(TEST_SKU_ID).build())
                .build(), true);

        assertEquals(ProviderProductInfoResponse.Status.ERROR, response.getStatus());
        assertEquals(1, response.getResultsCount());

        Offer savedOffer = offerRepository.getOfferById(existingOffer.getId());
        Assertions.assertThat(savedOffer.getContentSkuMapping()).isNull();
        Assertions.assertThat(savedOffer.getApprovedSkuMapping()).isNull();
    }

    @Test
    public void testSupplierMappingToNotMatchingFastSkuIgnored() {
        Offer existingOffer = commonBlueOffer()
            .setSupplierSkuMapping(null);
        offerRepository.insertOffer(existingOffer);

        mockFastSku(TEST_SKU_ID, existingOffer.getBusinessId() + 1);

        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo()
                    .setChangeSource(ChangeSource.SUPPLIER)
                    .build())
                .addProviderProductInfo(
                    blueProductInfo().setMarketSkuId(TEST_SKU_ID).build())
                .build(), true);

        assertEquals(ProviderProductInfoResponse.Status.OK, response.getStatus());
        assertEquals(1, response.getResultsCount());

        Offer savedOffer = offerRepository.getOfferById(existingOffer.getId());
        Assertions.assertThat(savedOffer.getSupplierSkuMapping()).isNull();
    }

    @Test
    public void testDeduplicationMappingToNotMatchingFastSkuFailsWithError() {
        Offer existingOffer = commonBlueOffer()
            .setSupplierSkuMapping(null);
        offerRepository.insertOffer(existingOffer);

        mockFastSku(TEST_SKU_ID, existingOffer.getBusinessId() + 1);

        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo()
                    .setChangeSource(ChangeSource.CONTENT)
                    .setChangeType(MboMappings.ProductUpdateRequestInfo.ChangeType.DEDUPLICATION)
                    .build())
                .addProviderProductInfo(
                    blueProductInfo().setMarketSkuId(TEST_SKU_ID).build())
                .build(), true);

        assertEquals(ProviderProductInfoResponse.Status.ERROR, response.getStatus());
        assertEquals(1, response.getResultsCount());

        Offer savedOffer = offerRepository.getOfferById(existingOffer.getId());
        Assertions.assertThat(savedOffer.getContentSkuMapping()).isNull();
        Assertions.assertThat(savedOffer.getApprovedSkuMapping()).isNull();
    }

    @Test
    public void testBetterMappingToNotMatchingFastSkuFailsWithError() {
        Offer existingOffer = commonBlueOffer()
            .setSupplierSkuMapping(null);
        offerRepository.insertOffer(existingOffer);

        mockFastSku(TEST_SKU_ID, existingOffer.getBusinessId() + 1);

        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo()
                    .setChangeSource(ChangeSource.BETTER)
                    .build())
                .addProviderProductInfo(
                    blueProductInfo().setMarketSkuId(TEST_SKU_ID).build())
                .build(), true);

        assertEquals(ProviderProductInfoResponse.Status.ERROR, response.getStatus());
        assertEquals(1, response.getResultsCount());

        Offer savedOffer = offerRepository.getOfferById(existingOffer.getId());
        Assertions.assertThat(savedOffer.getContentSkuMapping()).isNull();
        Assertions.assertThat(savedOffer.getApprovedSkuMapping()).isNull();
    }

    @Test
    public void testOfferOnBusiness() {
        MboMappings.ProviderProductInfo.Builder productInfo = blueProductInfo()
            .setShopId(OfferTestUtils.BIZ_ID_SUPPLIER);

        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo())
                .addProviderProductInfo(productInfo.setMasterDataInfo(masterData()))
                .build(), true);

        assertEquals(ProviderProductInfoResponse.Status.OK, response.getStatus());
        assertEquals(1, response.getResultsCount());

        List<Offer> offers = offerRepository.findAll();
        assertThat(offers).hasSize(1);
        Offer offer = offers.get(0);

        assertEquals(productInfo.getShopId(), offer.getBusinessId());
        assertEquals(productInfo.getTitle(), offer.getTitle());
        List<Offer.ServiceOffer> serviceOffers = offer.getServiceOffers();
        assertThat(serviceOffers).hasSize(0);

        long offerId = offer.getId();

        //test update
        String newTitle = "new title";
        response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo())
                .addProviderProductInfo(productInfo
                    .setMasterDataInfo(masterData())
                    .setTitle(newTitle))
                .build(), true);

        assertEquals(ProviderProductInfoResponse.Status.OK, response.getStatus());
        assertEquals(1, response.getResultsCount());

        offer = offerRepository.getOfferById(offerId);

        assertEquals(productInfo.getShopId(), offer.getBusinessId());
        assertEquals(newTitle, offer.getTitle());
    }

    @Test
    public void testOfferOnLinkedToBusiness() {
        int supplierId = OfferTestUtils.BIZ_ID_SUPPLIER + 1;
        Supplier supplier3p = new Supplier(supplierId, "biz child")
            .setType(MbocSupplierType.THIRD_PARTY)
            .setBusinessId(OfferTestUtils.BIZ_ID_SUPPLIER);
        supplierRepository.insert(supplier3p);

        MboMappings.ProviderProductInfo.Builder productInfo = blueProductInfo().setShopId(supplierId);

        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo())
                .addProviderProductInfo(productInfo.setMasterDataInfo(masterData()))
                .build(), true);

        assertEquals(ProviderProductInfoResponse.Status.OK, response.getStatus());
        assertEquals(1, response.getResultsCount());

        List<Offer> offers = offerRepository.findAll();
        assertThat(offers).hasSize(1);
        Offer offer = offers.get(0);

        // base offer is on businessId
        assertEquals(OfferTestUtils.BIZ_ID_SUPPLIER, offer.getBusinessId());
        List<Offer.ServiceOffer> serviceOffers = offer.getServiceOffers();
        assertThat(serviceOffers).hasSize(1);
        assertThat(serviceOffers).extracting(Offer.ServiceOffer::getSupplierId).containsExactly(supplierId);

        long offerId = offer.getId();

        //test update
        String newTitle = "new title";
        response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo())
                .addProviderProductInfo(productInfo
                    .setMasterDataInfo(masterData())
                    .setTitle(newTitle))
                .build(), true);

        assertEquals(ProviderProductInfoResponse.Status.OK, response.getStatus());
        assertEquals(1, response.getResultsCount());

        offer = offerRepository.getOfferById(offerId);

        assertEquals(OfferTestUtils.BIZ_ID_SUPPLIER, offer.getBusinessId());
        assertEquals(newTitle, offer.getTitle());
    }

    @Test
    public void testOfferOnLinkedToBusinessShouldNotCreateNewServiceOffer() {
        int supplierId = OfferTestUtils.BIZ_ID_SUPPLIER + 1;
        Supplier supplier3p = new Supplier(supplierId, "biz child")
            .setType(MbocSupplierType.THIRD_PARTY)
            .setBusinessId(OfferTestUtils.BIZ_ID_SUPPLIER);
        supplierRepository.insert(supplier3p);

        Offer existingOffer = commonBlueOffer()
            .setBusinessId(OfferTestUtils.BIZ_ID_SUPPLIER)
            .setServiceOffers(List.of())
            .addNewServiceOfferIfNotExistsForTests(supplier3p);
        List<Offer.ServiceOffer> serviceOffers = existingOffer.getServiceOffers();
        assertThat(serviceOffers).hasSize(1);
        offerRepository.insertOffer(existingOffer);

        MboMappings.ProviderProductInfo.Builder productInfo = blueProductInfo().setShopId(supplier3p.getId());

        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo())
                .addProviderProductInfo(productInfo.setMasterDataInfo(masterData()))
                .build(), true);

        assertEquals(ProviderProductInfoResponse.Status.OK, response.getStatus());
        assertEquals(1, response.getResultsCount());

        List<Offer> offers = offerRepository.findAll();
        assertThat(offers).hasSize(1);
        Offer offer = offers.get(0);

        // base offer is on businessId
        assertEquals(OfferTestUtils.BIZ_ID_SUPPLIER, offer.getBusinessId());
        serviceOffers = offer.getServiceOffers();
        assertThat(serviceOffers).hasSize(1);
        assertThat(serviceOffers).extracting(Offer.ServiceOffer::getSupplierId)
            .containsExactly(supplierId);
    }

    @Test
    public void testCreateNewServiceInvokesMappingReUploading() {
        Supplier firstSupplierWhite = new Supplier(WHITE_SHOP_ID + 1, "biz child")
            .setType(MbocSupplierType.MARKET_SHOP)
            .setBusinessId(OfferTestUtils.BIZ_ID_SUPPLIER);
        Supplier secondSupplierWhite = new Supplier(WHITE_SHOP_ID + 2, "biz child")
            .setType(MbocSupplierType.MARKET_SHOP)
            .setBusinessId(OfferTestUtils.BIZ_ID_SUPPLIER);
        supplierRepository.insertBatch(firstSupplierWhite, secondSupplierWhite);

        MboMappings.ProviderProductInfo.Builder productInfo = whiteProductInfo().setShopId(firstSupplierWhite.getId());

        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo())
                .addProviderProductInfo(productInfo.setMasterDataInfo(masterData()))
                .build(), true);

        assertEquals(ProviderProductInfoResponse.Status.OK, response.getStatus());
        assertEquals(1, response.getResultsCount());

        List<Offer> allOffers = offerRepository.findAll();
        assertThat(allOffers).hasSize(1);
        Offer existingOffer = allOffers.get(0);

        AntiMapping antiMapping = new AntiMapping()
            .setOfferId(existingOffer.getId())
            .setNotModelId(TEST_MODEL_ID)
            .setNotSkuId(TEST_SKU_ID)
            .setUploadRequestTs(Instant.now())
            .setUploadStamp(1L);
        antiMappingRepository.insert(antiMapping);

        List<Offer.ServiceOffer> serviceOffers = existingOffer.getServiceOffers();
        assertThat(serviceOffers).hasSize(1);

        existingOffer
            .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.APPROVED)
            .setMappedCategoryId(OfferTestUtils.TEST_CATEGORY_INFO_ID, CONTENT);
        markAsUploaded(existingOffer);
        offerRepository.updateOffers(existingOffer);

        productInfo = whiteProductInfo().setShopId(secondSupplierWhite.getId());

        response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo())
                .addProviderProductInfo(productInfo.setMasterDataInfo(masterData()))
                .build(), true);

        assertEquals(ProviderProductInfoResponse.Status.OK, response.getStatus());
        assertEquals(1, response.getResultsCount());

        allOffers = offerRepository.findAll();
        assertThat(allOffers).hasSize(1);
        existingOffer = allOffers.get(0);

        // base offer is on businessId
        assertEquals(OfferTestUtils.BIZ_ID_SUPPLIER, existingOffer.getBusinessId());
        serviceOffers = existingOffer.getServiceOffers();
        assertThat(serviceOffers).hasSize(2);
        assertThat(serviceOffers).extracting(Offer.ServiceOffer::getSupplierId)
            .containsExactlyInAnyOrder(firstSupplierWhite.getId(), secondSupplierWhite.getId());

        List<AntiMapping> offerAntiMappings = antiMappingRepository.findByFilter(AntiMappingRepository.newFilter()
            .setOfferIds(existingOffer.getId()));
        assertThat(offerAntiMappings)
            .allMatch(am -> am.getUploadStamp() == null);
    }

    @Test
    public void testCreateNewServiceOfferOnExistingOffer() {
        int supplierId = OfferTestUtils.BIZ_ID_SUPPLIER + 1;
        Supplier supplier3p = new Supplier(supplierId, "biz child")
            .setType(MbocSupplierType.THIRD_PARTY)
            .setBusinessId(OfferTestUtils.BIZ_ID_SUPPLIER);
        supplierRepository.insert(supplier3p);

        Offer existingOffer = commonBlueOffer()
            .setBusinessId(OfferTestUtils.BIZ_ID_SUPPLIER);

        List<Offer.ServiceOffer> serviceOffers = existingOffer.getServiceOffers();
        assertThat(serviceOffers).hasSize(1);
        offerRepository.insertOffer(existingOffer);

        MboMappings.ProviderProductInfo.Builder productInfo = blueProductInfo().setShopId(supplier3p.getId());

        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo())
                .addProviderProductInfo(productInfo.setMasterDataInfo(masterData()))
                .build(), true);

        assertEquals(ProviderProductInfoResponse.Status.OK, response.getStatus());
        assertEquals(1, response.getResultsCount());

        List<Offer> offers = offerRepository.findAll();
        assertThat(offers).hasSize(1);
        Offer offer = offers.get(0);

        // base offer is on businessId
        assertEquals(OfferTestUtils.BIZ_ID_SUPPLIER, offer.getBusinessId());
        serviceOffers = offer.getServiceOffers();
        assertThat(serviceOffers).hasSize(2);
        assertThat(serviceOffers).extracting(Offer.ServiceOffer::getSupplierId)
            .containsExactlyInAnyOrder(supplierId, TEST_SUPPLIER_ID);
    }

    @Test
    public void testOfferOnLinkedToBusinessSameBaseOffer() {
        int supplierId = OfferTestUtils.BIZ_ID_SUPPLIER + 1;
        Supplier supplier3p = new Supplier(supplierId, "biz child")
            .setType(MbocSupplierType.THIRD_PARTY)
            .setBusinessId(OfferTestUtils.BIZ_ID_SUPPLIER);
        int supplierId2 = OfferTestUtils.BIZ_ID_SUPPLIER + 2;
        Supplier supplier3p2 = new Supplier(supplierId2, "biz child")
            .setType(MbocSupplierType.THIRD_PARTY)
            .setBusinessId(OfferTestUtils.BIZ_ID_SUPPLIER);
        supplierRepository.insertBatch(supplier3p, supplier3p2);

        Offer existingOffer = commonBlueOffer()
            .setBusinessId(OfferTestUtils.BIZ_ID_SUPPLIER)
            .setServiceOffers(List.of())
            .addNewServiceOfferIfNotExistsForTests(supplier3p)
            .addNewServiceOfferIfNotExistsForTests(supplier3p2);
        offerRepository.insertOffer(existingOffer);

        MboMappings.ProviderProductInfo.Builder productInfo = blueProductInfo()
            .setShopId(supplier3p.getId())
            .setTitle("new title 1");
        MboMappings.ProviderProductInfo.Builder productInfo2 = blueProductInfo()
            .setShopId(supplier3p2.getId())
            .setTitle("new title 2");

        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo())
                .addProviderProductInfo(productInfo.setMasterDataInfo(masterData()))
                .addProviderProductInfo(productInfo2.setMasterDataInfo(masterData()))
                .build(), true);

        assertEquals(ProviderProductInfoResponse.Status.ERROR, response.getStatus());
        assertEquals(2, response.getResultsCount());

        List<Offer> offers = offerRepository.findAll();
        assertThat(offers).hasSize(1);
        Offer offer = offers.get(0);
        // title is not changed
        assertEquals(existingOffer.getTitle(), offer.getTitle());

        assertEquals(ProviderProductInfoResponse.Status.ERROR, response.getResults(0).getStatus());
        assertEquals(ErrorKind.OK_NOT_SAVED_DUE_TO_OTHER_ERRORS,
            response.getResults(0).getErrors(0).getErrorKind());

        assertEquals(ProviderProductInfoResponse.Status.ERROR, response.getResults(1).getStatus());
        assertEquals(ErrorKind.DUPLICATE_SHOP_SKU,
            response.getResults(1).getErrors(0).getErrorKind());

    }

    @Test
    public void testWhiteMappingsSetToBusiness() {
        int supplierId = OfferTestUtils.BIZ_ID_SUPPLIER + 1;
        Supplier supplierMs = new Supplier(supplierId, "biz child")
            .setType(MbocSupplierType.MARKET_SHOP)
            .setMbiBusinessId(OfferTestUtils.BIZ_ID_SUPPLIER);
        supplierRepository.insertBatch(supplierMs);

        Offer existingSupplierOffer = commonWhiteOffer()
            .setShopSku("someOffer")
            .setBusinessId(supplierId)
            .setServiceOffers(List.of())
            .setMappedCategoryId(OfferTestUtils.TEST_CATEGORY_INFO_ID, CONTENT)
            .setMappedModelId(OfferTestUtils.TEST_MODEL_ID, CONTENT)
            .updateApprovedSkuMapping(OfferTestUtils.mapping(OfferTestUtils.TEST_SKU_ID), CONTENT)
            .addNewServiceOfferIfNotExistsForTests(supplierMs);
        Offer existingBusinessOffer = commonWhiteOffer()
            .setShopSku("someOffer")
            .setBusinessId(OfferTestUtils.BIZ_ID_SUPPLIER)
            .setServiceOffers(List.of())
            .addNewServiceOfferIfNotExistsForTests(supplierMs);
        offerRepository.insertOffers(existingSupplierOffer, existingBusinessOffer);

        MboMappings.ProviderProductInfo.Builder productInfo = whiteProductInfo()
            .setShopId(supplierId)
            .setShopSkuId("someOffer")
            .setMarketCategoryId((int) OfferTestUtils.TEST_CATEGORY_INFO_ID)
            .setMarketModelId(OfferTestUtils.TEST_MODEL_ID)
            .setMarketSkuId(OfferTestUtils.TEST_SKU_ID);

        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo())
                .addProviderProductInfo(productInfo.setMasterDataInfo(masterData()))
                .build(), true);

        assertEquals(ProviderProductInfoResponse.Status.OK, response.getStatus());
        assertEquals(1, response.getResultsCount());

        List<Offer> offers = offerRepository.findAll();
        assertThat(offers).hasSize(1);
        Offer offer = offers.get(0);
        // mapping is set to business offer
        assertEquals(OfferTestUtils.BIZ_ID_SUPPLIER, offer.getBusinessId());
        assertEquals(OfferTestUtils.TEST_CATEGORY_INFO_ID, offer.getMappedCategoryId().longValue());
        assertEquals(OfferTestUtils.TEST_SKU_ID, offer.getApprovedSkuMapping().getMappingId());
    }

    @Test
    public void testWhiteMappingMergedToBusinessOffer() {
        int supplierId = OfferTestUtils.BIZ_ID_SUPPLIER + 1;
        Supplier supplierMs = new Supplier(supplierId, "biz child")
            .setType(MbocSupplierType.MARKET_SHOP)
            .setMbiBusinessId(OfferTestUtils.BIZ_ID_SUPPLIER);
        supplierRepository.insertBatch(supplierMs);

        Offer existingSupplierOffer = commonWhiteOffer()
            .setShopSku("someOffer")
            .setBusinessId(supplierId)
            .setServiceOffers(List.of())
            .setMappedCategoryId(OfferTestUtils.TEST_CATEGORY_INFO_ID, CONTENT)
            .setMappedModelId(OfferTestUtils.TEST_MODEL_ID, CONTENT)
            .updateApprovedSkuMapping(OfferTestUtils.mapping(OfferTestUtils.TEST_SKU_ID), CONTENT)
            .addNewServiceOfferIfNotExistsForTests(supplierMs);
        offerRepository.insertOffers(existingSupplierOffer);

        MboMappings.ProviderProductInfo.Builder productInfo = whiteProductInfo()
            .setShopId(OfferTestUtils.BIZ_ID_SUPPLIER)
            .setShopSkuId("someOffer");

        Map<Integer, Supplier> supplierMap = supplierRepository
            .findByIdsAsMap(ImmutableSet.of(OfferTestUtils.BIZ_ID_SUPPLIER, supplierId));
        DatacampContext datacampContext = DatacampContext.builder()
            .processDataCampData(true)
            .suppliers(supplierMap)
            .offersAdditionalData(ImmutableMap.of(
                new BusinessSkuKey(OfferTestUtils.BIZ_ID_SUPPLIER, "someOffer"),
                AdditionalData.builder()
                    .dataCampContentVersion(1L)
                    .supplierIds(ImmutableSet.of(supplierId))
                    .build()
            ))
            .build();
        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo())
                .addProviderProductInfo(productInfo.setMasterDataInfo(masterData()))
                .build(),
            datacampContext, AddProductInfoListener.NOOP
        );

        assertEquals(ProviderProductInfoResponse.Status.OK, response.getStatus());
        assertEquals(1, response.getResultsCount());

        List<Offer> offers = offerRepository.findAll();
        assertThat(offers).hasSize(1);
        Offer offer = offers.get(0);
        // mapping is set to business offer
        assertEquals(OfferTestUtils.BIZ_ID_SUPPLIER, offer.getBusinessId());
        assertEquals(OfferTestUtils.TEST_CATEGORY_INFO_ID, offer.getMappedCategoryId().longValue());
        assertEquals(OfferTestUtils.TEST_MODEL_ID, offer.getMappedModelId().longValue());
        assertEquals(OfferTestUtils.TEST_SKU_ID, offer.getApprovedSkuMapping().getMappingId());
    }

    @Test
    public void testWillRemoveMarkedToRemoveServiceOffer() {
        int supplierId = OfferTestUtils.BIZ_ID_SUPPLIER + 1;
        Supplier supplierMs = new Supplier(supplierId, "biz child")
            .setType(MbocSupplierType.MARKET_SHOP)
            .setMbiBusinessId(OfferTestUtils.BIZ_ID_SUPPLIER);
        int supplierIdToRemove = OfferTestUtils.BIZ_ID_SUPPLIER + 2;
        Supplier supplierMsToRemove = new Supplier(supplierIdToRemove, "biz child1")
            .setType(MbocSupplierType.MARKET_SHOP)
            .setMbiBusinessId(OfferTestUtils.BIZ_ID_SUPPLIER);
        supplierRepository.insertBatch(supplierMs, supplierMsToRemove);

        Offer offer = OfferTestUtils.simpleOkOffer()
            .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.SUGGESTED)
            .setShopSku("someOffer")
            .setBusinessId(OfferTestUtils.BIZ_ID_SUPPLIER)
            .setServiceOffers(List.of())
            .setMappedModelId(TEST_MODEL_ID, CONTENT)
            .updateApprovedSkuMapping(OfferTestUtils.mapping(TEST_SKU_ID), CONTENT)
            .addNewServiceOfferIfNotExistsForTests(supplierMs, supplierMsToRemove);

        var response = baseInitRemoveServiceOfferTest(
            offer,
            Map.of(offer.getBusinessSkuKey(), new HashSet<>(List.of(supplierIdToRemove)))
        );
        assertEquals(ProviderProductInfoResponse.Status.OK, response.getStatus());
        assertEquals(1, response.getResultsCount());

        var postProcessOffer = offerRepository.findOfferByBusinessSkuKey(offer.getBusinessSkuKey());

        assertEquals(offer.getServiceOffers().size() - 1, postProcessOffer.getServiceOffers().size());
        assertTrue(
            postProcessOffer.getServiceOffers().stream()
                .noneMatch(so -> Objects.equals(so.getSupplierId(), supplierIdToRemove))
        );
        assertTrue(
            postProcessOffer.getServiceOffers().stream()
                .anyMatch(so -> Objects.equals(so.getSupplierId(), supplierId))
        );
    }

    @Test
    public void testDoesNotRequireBarcodeWhenOfferHasNoCrossdockOrFulfillmentParts() {
        Supplier business = OfferTestUtils.businessSupplier()
            .setFulfillment(true)
            .setCrossdock(true);
        Supplier ffSupplier = OfferTestUtils.blueSupplierUnderBiz1()
            .setFulfillment(true);
        Supplier cdSupplier = OfferTestUtils.blueSupplierUnderBiz1()
            .setCrossdock(true);
        Supplier dsbsSupplier = OfferTestUtils.dsbsSupplierUnderBiz()
            .setFulfillment(false)
            .setCrossdock(false);

        deleteInsertSuppliers(business, ffSupplier, cdSupplier, dsbsSupplier);

        Offer existingOffer = commonBlueOffer()
            .setVendorId(1)
            .setBusinessId(business.getId())
            .setServiceOffers(Collections.emptyList())
            .addNewServiceOfferIfNotExistsForTests(dsbsSupplier);

        offerRepository.insertOffer(existingOffer);

        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo().setChangeSource(ChangeSource.SUPPLIER).build())
                .addProviderProductInfo(blueProductInfo()
                    .setShopId(dsbsSupplier.getId())
                    .clearBarcode())
                .build(), true);

        assertEquals(ProviderProductInfoResponse.Status.OK, response.getStatus());
        assertEquals(1, response.getResultsCount());
        assertEquals(0, response.getResults(0).getErrorsCount());
    }

    @Test
    public void testDoesNotRequiredAtLeastOneGtinBarcodeDropship() {
        deleteInsertSuppliers(OfferTestUtils.dropshipSupplier());

        Offer existingOffer = commonBlueOffer()
            .setVendorId(1)
            .updateApprovedSkuMapping(new Offer.Mapping(MODEL_ID, DateTimeUtils.dateTimeNow()), CONTENT);

        offerRepository.insertOffer(existingOffer);

        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo().setChangeSource(ChangeSource.SUPPLIER).build())
                .addProviderProductInfo(blueProductInfo())
                .build(), true);

        assertEquals(ProviderProductInfoResponse.Status.OK, response.getStatus());
        assertEquals(1, response.getResultsCount());
        assertEquals(0, response.getResults(0).getErrorsCount());
    }

    @Test
    public void testDoesNotRequiredAtLeastOneBarcodeDropship() {
        deleteInsertSuppliers(OfferTestUtils.dropshipSupplier());

        Offer existingOffer = commonBlueOffer()
            .setVendorId(1)
            .updateApprovedSkuMapping(new Offer.Mapping(MODEL_ID, DateTimeUtils.dateTimeNow()), CONTENT);

        offerRepository.insertOffer(existingOffer);

        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo().setChangeSource(ChangeSource.SUPPLIER).build())
                .addProviderProductInfo(blueProductInfo())
                .build(), true);

        assertEquals(ProviderProductInfoResponse.Status.OK, response.getStatus());
        assertEquals(1, response.getResultsCount());
        assertEquals(0, response.getResults(0).getErrorsCount());
    }

    @Test
    public void testDoesNotRequiredAtLeastOneGtinBarcodeDropshipBySeller() {
        deleteInsertSuppliers(OfferTestUtils.dropshipBySellerSupplier());

        Offer existingOffer = commonBlueOffer()
            .setVendorId(1)
            .updateApprovedSkuMapping(new Offer.Mapping(MODEL_ID, DateTimeUtils.dateTimeNow()), CONTENT);

        offerRepository.insertOffer(existingOffer);

        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo().setChangeSource(ChangeSource.SUPPLIER).build())
                .addProviderProductInfo(blueProductInfo())
                .build(), true);

        assertEquals(ProviderProductInfoResponse.Status.OK, response.getStatus());
        assertEquals(1, response.getResultsCount());
        assertEquals(0, response.getResults(0).getErrorsCount());
    }

    @Test
    public void testDoesNotRequiredAtLeastOneBarcodeDropshipBySeller() {
        deleteInsertSuppliers(OfferTestUtils.dropshipBySellerSupplier());

        Offer existingOffer = commonBlueOffer()
            .setVendorId(1)
            .updateApprovedSkuMapping(new Offer.Mapping(MODEL_ID, DateTimeUtils.dateTimeNow()), CONTENT);

        offerRepository.insertOffer(existingOffer);

        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo().setChangeSource(ChangeSource.SUPPLIER).build())
                .addProviderProductInfo(blueProductInfo())
                .build(), true);

        assertEquals(ProviderProductInfoResponse.Status.OK, response.getStatus());
        assertEquals(1, response.getResultsCount());
        assertEquals(0, response.getResults(0).getErrorsCount());
    }

    @Test
    public void testDoesNotRequiredAtLeastOneGtinBarcodeClickAndCollect() {
        deleteInsertSuppliers(OfferTestUtils.clickAndCollectSupplier());

        Offer existingOffer = commonBlueOffer()
            .setVendorId(1)
            .updateApprovedSkuMapping(new Offer.Mapping(MODEL_ID, DateTimeUtils.dateTimeNow()), CONTENT);

        offerRepository.insertOffer(existingOffer);

        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo().setChangeSource(ChangeSource.SUPPLIER).build())
                .addProviderProductInfo(blueProductInfo())
                .build(), true);

        assertEquals(ProviderProductInfoResponse.Status.OK, response.getStatus());
        assertEquals(1, response.getResultsCount());
        assertEquals(0, response.getResults(0).getErrorsCount());
    }

    @Test
    public void testDoesNotRequiredAtLeastOneBarcodeClickAndCollect() {
        deleteInsertSuppliers(OfferTestUtils.clickAndCollectSupplier());

        Offer existingOffer = commonBlueOffer()
            .setVendorId(1)
            .updateApprovedSkuMapping(new Offer.Mapping(MODEL_ID, DateTimeUtils.dateTimeNow()), CONTENT);

        offerRepository.insertOffer(existingOffer);

        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo().setChangeSource(ChangeSource.SUPPLIER).build())
                .addProviderProductInfo(blueProductInfo())
                .build(), true);

        assertEquals(ProviderProductInfoResponse.Status.OK, response.getStatus());
        assertEquals(1, response.getResultsCount());
        assertEquals(0, response.getResults(0).getErrorsCount());
    }

    @Test
    public void testRequiredAtLeastOneGtinBarcodeFulfillmentWithoutErrors() {
        deleteInsertSuppliers(OfferTestUtils.fulfillmentSupplier());

        Offer existingOffer = commonBlueOffer()
            .setVendorId(1)
            .updateApprovedSkuMapping(new Offer.Mapping(MODEL_ID, DateTimeUtils.dateTimeNow()), CONTENT);

        offerRepository.insertOffer(existingOffer);

        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo().setChangeSource(ChangeSource.SUPPLIER).build())
                .addProviderProductInfo(blueProductInfo().addBarcode("899121530013513485"))
                .build(), true);

        assertEquals(ProviderProductInfoResponse.Status.OK, response.getStatus());
        assertEquals(1, response.getResultsCount());
        assertEquals(0, response.getResults(0).getErrorsCount());
    }

    @Test
    public void testRequiredAtLeastOneGtinBarcodeCrossdockWithoutErrors() {
        deleteInsertSuppliers(OfferTestUtils.crossdockSupplier());

        Offer existingOffer = commonBlueOffer()
            .setVendorId(1)
            .updateApprovedSkuMapping(new Offer.Mapping(MODEL_ID, DateTimeUtils.dateTimeNow()), CONTENT);

        offerRepository.insertOffer(existingOffer);

        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo().setChangeSource(ChangeSource.SUPPLIER).build())
                .addProviderProductInfo(blueProductInfo().addBarcode("899121530013513485"))
                .build(), true);

        assertEquals(ProviderProductInfoResponse.Status.OK, response.getStatus());
        assertEquals(1, response.getResultsCount());
        assertEquals(0, response.getResults(0).getErrorsCount());
    }

    private void markAsUploaded(Offer... offers) {
        LocalDateTime uploadDoneTs = LocalDateTime.now();
        offerRepository.populateUploadToYtStamps(List.of(offers));
    }

    @Test
    public void testNotReadingExistingModels() {
        Offer existingOffer = commonBlueOffer()
            .setMappedModelId(TEST_MODEL_ID)
            .updateApprovedSkuMapping(null, null);

        offerRepository.insertOffer(existingOffer);

        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo().setChangeSource(ChangeSource.SUPPLIER).build())
                .addProviderProductInfo(
                    blueProductInfo().setMarketModelId(TEST_MODEL_ID).build())
                .build(), true);

        assertEquals(ProviderProductInfoResponse.Status.OK, response.getStatus());
        assertEquals(1, response.getResultsCount());
        Mockito.verify(modelServiceMock, Mockito.never()).getModelsFromMboOnly(Mockito.any());
    }

    @Test
    public void testNotReadingModelsAsSku() {
        Offer existingOffer = commonBlueOffer()
            .updateApprovedSkuMapping(null, null);

        offerRepository.insertOffer(existingOffer);

        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo().setChangeSource(ChangeSource.SUPPLIER).build())
                .addProviderProductInfo(
                    blueProductInfo().setMarketModelId(MODEL_ID).build())
                .build(), true);

        assertEquals(ProviderProductInfoResponse.Status.OK, response.getStatus());
        assertEquals(1, response.getResultsCount());
        Mockito.verify(modelServiceMock, Mockito.never()).getModelsFromMboOnly(Mockito.any());

        existingOffer = getSingleCurrentOffer();
        assertEquals(MODEL_ID, (long) existingOffer.getMappedModelId());
    }

    @Test
    public void testReadingModels() {
        Offer existingOffer = commonBlueOffer()
            .updateApprovedSkuMapping(null, null);

        offerRepository.insertOffer(existingOffer);

        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo().setChangeSource(ChangeSource.SUPPLIER).build())
                .addProviderProductInfo(
                    blueProductInfo().setMarketModelId(TEST_MODEL_ID).build())
                .build(), true);

        assertEquals(ProviderProductInfoResponse.Status.OK, response.getStatus());
        assertEquals(1, response.getResultsCount());
        Collection<Long> ids = new HashSet<>();
        ids.add(TEST_MODEL_ID);
        Mockito.verify(modelServiceMock).getModelsFromMboOnly(Mockito.eq(ids));
        existingOffer = getSingleCurrentOffer();
        assertEquals(TEST_MODEL_ID, (long) existingOffer.getMappedModelId());
    }

    @Test
    public void testSupplierChangedMappingSentToReSort() {
        Offer existingOffer = commonBlueOffer()
            .updateApprovedSkuMapping(new Offer.Mapping(MODEL_ID, DateTimeUtils.dateTimeNow()), CONTENT);

        offerRepository.insertOffer(existingOffer);

        ProviderProductInfoResponse response = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo().setChangeSource(ChangeSource.SUPPLIER).build())
                .addProviderProductInfo(
                    blueProductInfo().setMarketSkuId(PSKU20_MODEL_ID).build())
                .build(), true);

        assertEquals(ProviderProductInfoResponse.Status.OK, response.getStatus());
        assertEquals(1, response.getResultsCount());

        Offer offer = offerRepository.findAll().get(0);
        assertThat(offer.getShopSku()).isEqualTo(existingOffer.getShopSku());
        assertThat(offer.getSupplierSkuIdStr()).isEqualTo(String.valueOf(PSKU20_MODEL_ID));
        assertThat(offer.getSupplierSkuMappingStatus()).isEqualTo(Offer.MappingStatus.RE_SORT);
        assertThat(offer.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.IN_RE_SORT);
    }

    private ProviderProductInfoResponse baseInitRemoveServiceOfferTest(
        Offer offer, Map<BusinessSkuKey, Set<Integer>> serviceOffersToRemove) {
        offerRepository.insertOffers(offer);
        var preProcessOffer = offerRepository.findOfferByBusinessSkuKeyWithContent(offer.getBusinessSkuKey());

        MboMappings.ProviderProductInfo.Builder productInfo = whiteProductInfo()
            .setShopId(OfferTestUtils.BIZ_ID_SUPPLIER)
            .setShopSkuId("someOffer");

        var serviceOfferIds = offer.getServiceOffers().stream()
            .map(Offer.ServiceOffer::getSupplierId)
            .collect(Collectors.toSet());
        var suppliersIds = new HashSet<>(serviceOfferIds);
        suppliersIds.add(offer.getBusinessId());

        Map<Integer, Supplier> supplierMap = supplierRepository.findByIdsAsMap(suppliersIds);

        DatacampContext datacampContext = DatacampContext.builder()
            .processDataCampData(true)
            .suppliers(supplierMap)
            .existingOffers(Map.of(preProcessOffer.getBusinessSkuKey(), preProcessOffer))
            .offersAdditionalData(ImmutableMap.of(
                new BusinessSkuKey(OfferTestUtils.BIZ_ID_SUPPLIER, "someOffer"),
                AdditionalData.builder()
                    .dataCampContentVersion(1L)
                    .supplierIds(serviceOfferIds)
                    .build()
            ))
            .serviceOffersToRemove(serviceOffersToRemove)
            .build();

        return service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo())
                .addProviderProductInfo(productInfo.setMasterDataInfo(masterData()))
                .build(),
            datacampContext, AddProductInfoListener.NOOP
        );

    }

    @Test
    public void shouldSendMappingToRecheck() {
        storageKeyValueService.invalidateCache();
        storageKeyValueService.putValue("RecheckMapping.enabled", true);
        Offer offer = commonBlueOffer()
            .updateApprovedSkuMapping(new Offer.Mapping(MODEL_ID, DateTimeUtils.dateTimeNow()), CONTENT)
            .setProcessingStatusInternal(Offer.ProcessingStatus.PROCESSED);

        offerRepository.insertOffer(offer);
        var preProcessOffer = offerRepository.findOfferByBusinessSkuKeyWithContent(offer.getBusinessSkuKey());

        MboMappings.ProviderProductInfo.Builder productInfo = blueProductInfo()
            .setShopId(offer.getSupplierId())
            .setShopSkuId(offer.getShopSku());

        DatacampContext datacampContext = DatacampContext.builder()
            .processDataCampData(true)
            .existingOffers(Map.of(preProcessOffer.getBusinessSkuKey(), preProcessOffer))
            .offersAdditionalData(ImmutableMap.of(
                offer.getBusinessSkuKey(),
                AdditionalData.builder()
                    .dataCampContentVersion(1L)
                    .partnerMappingDecision(new Pair(offer.getApprovedSkuId(), DENY))
                    .build()
            ))
            .build();

        var productInfoResponse = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo())
                .addProviderProductInfo(productInfo.setMasterDataInfo(masterData()))
                .build(),
            datacampContext, AddProductInfoListener.NOOP
        );

        var resultOffer =
            offerRepository.findOfferByBusinessSkuKeyWithContent(offer.getBusinessSkuKey());

        assertEquals(ProviderProductInfoResponse.Status.OK, productInfoResponse.getStatus());

        assertEquals(Offer.ProcessingStatus.IN_RECHECK_MODERATION, resultOffer.getProcessingStatus());
        assertEquals(Offer.RecheckMappingStatus.ON_RECHECK, resultOffer.getRecheckMappingStatus());
        assertEquals(Offer.RecheckMappingSource.PARTNER, resultOffer.getRecheckMappingSource());
    }

    @Test
    public void shouldNotSendMappingToRecheckIfApprovedByPartner() {
        storageKeyValueService.invalidateCache();
        storageKeyValueService.putValue("RecheckMapping.enabled", true);
        Offer offer = commonBlueOffer()
            .updateApprovedSkuMapping(new Offer.Mapping(MODEL_ID, DateTimeUtils.dateTimeNow()), CONTENT)
            .setProcessingStatusInternal(Offer.ProcessingStatus.PROCESSED);

        offerRepository.insertOffer(offer);
        var preProcessOffer = offerRepository.findOfferByBusinessSkuKeyWithContent(offer.getBusinessSkuKey());

        MboMappings.ProviderProductInfo.Builder productInfo = blueProductInfo()
            .setShopId(offer.getSupplierId())
            .setShopSkuId(offer.getShopSku());

        DatacampContext datacampContext = DatacampContext.builder()
            .processDataCampData(true)
            .existingOffers(Map.of(preProcessOffer.getBusinessSkuKey(), preProcessOffer))
            .offersAdditionalData(ImmutableMap.of(
                offer.getBusinessSkuKey(),
                AdditionalData.builder()
                    .dataCampContentVersion(1L)
                    .partnerMappingDecision(new Pair(offer.getApprovedSkuId(), APPROVE))
                    .build()
            ))
            .build();

        var productInfoResponse = service.addProductInfo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(commonRequestInfo())
                .addProviderProductInfo(productInfo.setMasterDataInfo(masterData()))
                .build(),
            datacampContext, AddProductInfoListener.NOOP
        );

        var resultOffer =
            offerRepository.findOfferByBusinessSkuKeyWithContent(offer.getBusinessSkuKey());

        assertEquals(ProviderProductInfoResponse.Status.OK, productInfoResponse.getStatus());

        assertEquals(Offer.ProcessingStatus.PROCESSED, resultOffer.getProcessingStatus());
        assertNull(resultOffer.getRecheckMappingStatus());
        assertNull(resultOffer.getRecheckMappingSource());
    }

    private Offer getSingleCurrentOffer() {
        List<Offer> allOffers = offerRepository.findAll();
        assertThat(allOffers).hasSize(1);
        return allOffers.get(0);
    }

    private void deleteInsertSuppliers(Supplier... suppliers) {
        for (var supplier : suppliers) {
            supplierRepository.delete(List.of(supplier.getId()));
            supplierRepository.insert(supplier);
        }
    }

    private void mockSuggestedCategoryEnrichment(long categoryId) {
        doAnswer(i -> {
            List<Offer> offers = i.getArgument(0);
            offers.forEach(o -> o.setCategoryIdForTests(categoryId, Offer.BindingKind.SUGGESTED));
            return null;
        }).when(offersEnrichmentService).enrichOffers(anyList(), Mockito.anyBoolean(),
            Mockito.anyMap());
    }

    private void mockApprovedCategoryEnrichment(long categoryId) {
        doAnswer(i -> {
            List<Offer> offers = i.getArgument(0);
            offers.forEach(o -> o.setCategoryIdForTests(categoryId, Offer.BindingKind.APPROVED));
            return null;
        }).when(offersEnrichmentService).enrichOffers(anyList(), Mockito.anyBoolean(),
            Mockito.anyMap());
    }

    private void mockMatchingEnrichment(long categoryId, long modelId) {
        doAnswer(i -> {
            List<Offer> offers = i.getArgument(0);
            offers.forEach(o -> o.setCategoryIdForTests(categoryId, Offer.BindingKind.SUGGESTED));
            offers.forEach(o -> o.setModelId(modelId));
            return null;
        }).when(offersEnrichmentService).enrichOffers(anyList(), Mockito.anyBoolean(),
            Mockito.anyMap());
    }

    private void mockNonOperatorQualityModel(long modelId) {
        modelServiceMock.addModel(new Model()
            .setId(modelId)
            .setCategoryId(1)
            .setTitle("Non operator quality model")
            .setPublishedOnBlueMarket(true)
            .setSkuModel(true)
            .setModelType(Model.ModelType.GURU)
            .setModelQuality(Model.ModelQuality.OPERATOR)
            .setParameterValues(Collections.singletonList(ModelStorage.ParameterValue.newBuilder()
                .setParamId(ParamsUtils.MODEL_QUALITY_PARAM_ID)
                .setValueType(MboParameters.ValueType.ENUM)
                .setOptionId(ParamsUtils.OPERATOR_OPTION_ID + 1)
                .build())));
    }

    private void mockFastSku(long modelId, int supplierId) {
        modelServiceMock.addModel(new Model()
            .setId(modelId)
            .setCategoryId(1)
            .setTitle("Fast SKU")
            .setPublishedOnBlueMarket(true)
            .setModelType(Model.ModelType.FAST_SKU)
            .setModelQuality(Model.ModelQuality.OPERATOR)
            .setSupplierId((long) supplierId)
        );
    }
}
