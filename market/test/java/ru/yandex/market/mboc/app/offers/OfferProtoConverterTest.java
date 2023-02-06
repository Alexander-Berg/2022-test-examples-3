package ru.yandex.market.mboc.app.offers;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.deepmind.openapi.client.model.SskuStatus;
import ru.yandex.market.mbo.http.SkuBDApi;
import ru.yandex.market.mboc.app.offers.OfferProtoConverter.ConvertTarget;
import ru.yandex.market.mboc.app.proto.ProtoUtils;
import ru.yandex.market.mboc.common.availability.msku.MskuRepository;
import ru.yandex.market.mboc.common.dict.MbocSupplierType;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepositoryMock;
import ru.yandex.market.mboc.common.honestmark.EmptyCategoryRestriction;
import ru.yandex.market.mboc.common.honestmark.GroupCategoryRestriction;
import ru.yandex.market.mboc.common.honestmark.OfferCategoryRestrictionCalculator;
import ru.yandex.market.mboc.common.honestmark.SingleCategoryRestriction;
import ru.yandex.market.mboc.common.msku.TestUtils;
import ru.yandex.market.mboc.common.offers.model.ContentComment;
import ru.yandex.market.mboc.common.offers.model.ContentCommentType;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.model.OfferContent;
import ru.yandex.market.mboc.common.offers.model.OfferLite;
import ru.yandex.market.mboc.common.services.category.CategoryCachingServiceMock;
import ru.yandex.market.mboc.common.utils.DateTimeUtils;
import ru.yandex.market.mboc.common.utils.MustacheRenderer;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;
import ru.yandex.market.mboc.http.MboMappings;
import ru.yandex.market.mboc.http.MbocCommon;
import ru.yandex.market.mboc.http.SupplierOffer;
import ru.yandex.market.mboc.http.SupplierOffer.Offer.CategoryRestriction.AllowedType;
import ru.yandex.market.mboc.http.SupplierOffer.OfferProcessingStatus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.mboc.common.msku.TestUtils.TEST_MAPPING_NAME_BUILDER;
import static ru.yandex.market.mboc.common.utils.OfferTestUtils.DEFAULT_SHOP_CATEGORY_NAME;
import static ru.yandex.market.mboc.common.utils.OfferTestUtils.DEFAULT_SHOP_SKU;
import static ru.yandex.market.mboc.common.utils.OfferTestUtils.DEFAULT_TITLE;
import static ru.yandex.market.mboc.common.utils.OfferTestUtils.FMCG_SUPPLIER_ID;
import static ru.yandex.market.mboc.common.utils.OfferTestUtils.mapping;
import static ru.yandex.market.mboc.common.utils.OfferTestUtils.simpleOffer;
import static ru.yandex.market.mboc.common.utils.OfferTestUtils.simpleSupplier;
import static ru.yandex.market.mboc.http.SupplierOffer.ApprovedMappingConfidence.MAPPING_CONTENT;
import static ru.yandex.market.mboc.http.SupplierOffer.ApprovedMappingConfidence.MAPPING_PARTNER;
import static ru.yandex.market.mboc.http.SupplierOffer.ApprovedMappingConfidence.MAPPING_PARTNER_SELF;
import static ru.yandex.market.mboc.http.SupplierOffer.SkuType.TYPE_MARKET;
import static ru.yandex.market.mboc.http.SupplierOffer.SkuType.TYPE_PARTNER;

/**
 * @author yuramalinov
 * @created 28.09.18
 */
public class OfferProtoConverterTest {

    private static final Supplier TEST_SUPPLIER = new Supplier(OfferTestUtils.TEST_SUPPLIER_ID, "TEST");
    private static final Supplier FMCG_TEST_SUPPLIER = new Supplier(OfferTestUtils.FMCG_SUPPLIER_ID, "FMCG TEST")
        .setType(MbocSupplierType.FMCG);
    private static final int BERU_ID = 465852;
    private static final long MARKET_SKU_ID = 100500L;

    private OfferProtoConverter protoConverter;
    private CategoryCachingServiceMock categoryCachingService;
    private SupplierRepositoryMock supplierRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MustacheRenderer mustacheRenderer = new MustacheRenderer();
    private OfferCategoryRestrictionCalculator offerCategoryRestrictionCalculator;
    private MskuRepository mskuRepository;

    @Before
    public void setUp() {
        categoryCachingService = new CategoryCachingServiceMock();
        supplierRepository = new SupplierRepositoryMock();
        offerCategoryRestrictionCalculator = Mockito.mock(OfferCategoryRestrictionCalculator.class);
        mskuRepository = Mockito.mock(MskuRepository.class);
        TestUtils.mockMskuRepositoryFindTitles(mskuRepository);
        protoConverter = new OfferProtoConverter(categoryCachingService,
            offerCategoryRestrictionCalculator, mskuRepository, BERU_ID);
    }

    @Test
    public void testEmptyOfferDoesntThrowException() {
        // In case of MBI UploadExcel requests we can output offers with almost no data (they are marked as error)
        // conversion shouldn't fall on these
        protoConverter.convertBaseOfferToProto(new Offer().setBusinessId(1), new Supplier(1, "Test")).build();

        // No fail is OK
    }

    @Test
    public void testCategoryParamsAreProperlySet() {
        categoryCachingService.addCategory(1, "Категория номер один");

        SupplierOffer.Offer.Builder offer = protoConverter.convertBaseOfferToProto(
            new Offer().setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED), null);

        Assertions.assertThat(offer.getMarketCategoryId()).isEqualTo(1);
        Assertions.assertThat(offer.getMarketCategoryName()).isEqualTo("Категория номер один");

        offer = protoConverter.convertBaseOfferToProto(new Offer().setCategoryIdForTests(0L, Offer.BindingKind.SUGGESTED), null);
        Assertions.assertThat(offer.getMarketCategoryId()).isEqualTo(0);
        Assertions.assertThat(offer.hasMarketCategoryName()).isFalse();

        offer = protoConverter.convertBaseOfferToProto(new Offer(), null);
        Assertions.assertThat(offer.hasMarketCategoryId()).isFalse();
        Assertions.assertThat(offer.hasMarketCategoryName()).isFalse();
    }

    @Test
    public void testMappingNameAreProperlySet() {
        Offer.Mapping approved = new Offer.Mapping(1L, LocalDateTime.now(), Offer.SkuType.MARKET);
        Offer.Mapping deleted = new Offer.Mapping(2L, LocalDateTime.now(), Offer.SkuType.MARKET);
        Offer.Mapping suggested = new Offer.Mapping(3L, LocalDateTime.now(), Offer.SkuType.MARKET);
        Offer.Mapping supplier = new Offer.Mapping(4L, LocalDateTime.now(), Offer.SkuType.MARKET);
        Offer offerWithMapping = new Offer()
            .setDeletedApprovedSkuMapping(deleted)
            .setApprovedSkuMappingInternal(approved)
            .setSuggestSkuMapping(suggested)
            .setSupplierSkuMapping(supplier);
        SupplierOffer.Offer.Builder offer = protoConverter.convertBaseOfferToProto(offerWithMapping, null);
        Assertions.assertThat(offer.getDeletedMapping().getSkuName())
            .isEqualTo(TEST_MAPPING_NAME_BUILDER.apply(deleted.getMappingId()));
        Assertions.assertThat(offer.getApprovedMapping().getSkuName())
            .isEqualTo(TEST_MAPPING_NAME_BUILDER.apply(approved.getMappingId()));
        Assertions.assertThat(offer.getSupplierMapping().getSkuName())
            .isEqualTo(TEST_MAPPING_NAME_BUILDER.apply(supplier.getMappingId()));
        Assertions.assertThat(offer.getSuggestMapping().getSkuName())
            .isEqualTo(TEST_MAPPING_NAME_BUILDER.apply(suggested.getMappingId()));
    }

    @Test
    public void testCategoryRestrictionsSet() {
        SupplierOffer.Offer.CategoryRestriction categoryRestriction;

        Mockito.when(offerCategoryRestrictionCalculator.calculateRestriction(Mockito.any()))
            .thenReturn(Optional.of(new SingleCategoryRestriction(1L)));
        categoryRestriction = protoConverter.convertBaseOfferToProto(new Offer(), null).getCategoryRestriction();
        Assertions.assertThat(categoryRestriction.getType()).isEqualTo(AllowedType.SINGLE);
        Assertions.assertThat(categoryRestriction.getAllowedCategoryId()).isEqualTo(1L);
        Assertions.assertThat(categoryRestriction.getAllowedGroupId()).isEqualTo(0L);

        Mockito.when(offerCategoryRestrictionCalculator.calculateRestriction(Mockito.any()))
            .thenReturn(Optional.of(new GroupCategoryRestriction(1L, Set.of())));
        categoryRestriction = protoConverter.convertBaseOfferToProto(new Offer(), null).getCategoryRestriction();
        Assertions.assertThat(categoryRestriction.getType()).isEqualTo(AllowedType.GROUP);
        Assertions.assertThat(categoryRestriction.getAllowedCategoryId()).isEqualTo(0L);
        Assertions.assertThat(categoryRestriction.getAllowedGroupId()).isEqualTo(1L);

        Mockito.when(offerCategoryRestrictionCalculator.calculateRestriction(Mockito.any()))
            .thenReturn(Optional.empty());
        categoryRestriction = protoConverter.convertBaseOfferToProto(new Offer(), null).getCategoryRestriction();
        Assertions.assertThat(categoryRestriction.getType()).isEqualTo(AllowedType.INDETERMINABLE);
        Assertions.assertThat(categoryRestriction.getAllowedCategoryId()).isEqualTo(0L);
        Assertions.assertThat(categoryRestriction.getAllowedGroupId()).isEqualTo(0L);

        Mockito.when(offerCategoryRestrictionCalculator.calculateRestriction(Mockito.any()))
            .thenReturn(Optional.of(new EmptyCategoryRestriction()));
        categoryRestriction = protoConverter.convertBaseOfferToProto(new Offer(), null).getCategoryRestriction();
        Assertions.assertThat(categoryRestriction.getType()).isEqualTo(AllowedType.ANY);
        Assertions.assertThat(categoryRestriction.getAllowedCategoryId()).isEqualTo(0L);
        Assertions.assertThat(categoryRestriction.getAllowedGroupId()).isEqualTo(0L);
    }

    @Test
    public void testModelParamsAreProperlySet() {
        SupplierOffer.Offer.Builder offer = protoConverter.convertBaseOfferToProto(
            new Offer().setModelId(1L).setMarketModelName("Модель один"), null);

        Assertions.assertThat(offer.getMarketModelId()).isEqualTo(1);
        Assertions.assertThat(offer.getMarketModelName()).isEqualTo("Модель один");

        offer = protoConverter.convertBaseOfferToProto(
            new Offer().setModelId(0L).setMarketModelName("Модель один"), null);
        Assertions.assertThat(offer.getMarketModelId()).isEqualTo(0);
        Assertions.assertThat(offer.hasMarketModelName()).isFalse();

        offer = protoConverter.convertBaseOfferToProto(new Offer().setModelId(0L), null);
        Assertions.assertThat(offer.getMarketModelId()).isEqualTo(0);
        Assertions.assertThat(offer.hasMarketModelName()).isFalse();

        offer = protoConverter.convertBaseOfferToProto(new Offer(), null);
        Assertions.assertThat(offer.hasMarketModelId()).isFalse();
        Assertions.assertThat(offer.hasMarketModelName()).isFalse();
    }

    @Test
    public void testConvertAllOfMappingDestinations() {
        for (Offer.MappingDestination destination : Offer.MappingDestination.values()) {
            SupplierOffer.Offer.Builder offer = protoConverter.convertBaseOfferToProto(
                new Offer().setMappingDestination(destination), null);

            Assertions.assertThat(offer.getMappingDestination())
                .isNotNull();
            // little hack to check if mapping destination converts properly
            Assertions.assertThat(offer.getMappingDestination().toString())
                .contains(destination.toString());
        }

        SupplierOffer.Offer.Builder defaultOffer = protoConverter.convertBaseOfferToProto(
            new Offer(), null);

        Assertions.assertThat(defaultOffer.getMappingDestination())
            .isNotNull();
    }

    @Test
    public void testConvertSupplierType() {
        for (MbocSupplierType supplierType : MbocSupplierType.values()) {
            SupplierOffer.Offer.Builder offer = protoConverter.convertBaseOfferToProto(
                new Offer().setBusinessId(1), new Supplier(1, "Test").setType(supplierType));

            Assertions.assertThat(offer.getSupplierType())
                .isNotNull();
            // little hack to check if supplier type converts properly
            Assertions.assertThat(offer.getSupplierType().toString())
                .contains(supplierType.toString());
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSimpleMessage() throws IOException {
        SupplierOffer.ContentComment comment = protoConverter.convertContentComment(
            new ContentComment(ContentCommentType.NO_KNOWLEDGE));

        Assertions.assertThat(comment.getType()).isEqualTo("NO_KNOWLEDGE");
        Assertions.assertThat(comment.getItemsList()).isEmpty();

        MbocCommon.Message message = comment.getMessage();
        Assertions.assertThat(message.getMessageCode()).isEqualTo("mboc.content-comment.NO_KNOWLEDGE");
        Assertions.assertThat(
            mustacheRenderer.render(message.getMustacheTemplate(),
                objectMapper.readValue(message.getJsonDataForMustacheTemplate(), Map.class)))
            .isEqualTo("Нет знаний в категории");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testMessageWithItems() throws IOException {
        SupplierOffer.ContentComment comment = protoConverter.convertContentComment(
            new ContentComment(ContentCommentType.CONFLICTING_INFORMATION, "Shop_title", "Description", "URL"));
        Assertions.assertThat(comment.getType()).isEqualTo("CONFLICTING_INFORMATION");
        Assertions.assertThat(comment.getItemsList()).containsExactly("Shop_title", "Description", "URL");

        MbocCommon.Message message = comment.getMessage();
        Assertions.assertThat(message.getMessageCode()).isEqualTo("mboc.content-comment.CONFLICTING_INFORMATION");
        Assertions.assertThat(
            mustacheRenderer.render(message.getMustacheTemplate(),
                objectMapper.readValue(message.getJsonDataForMustacheTemplate(), Map.class)))
            .isEqualTo("Расхождение информации в полях: Shop_title, Description, URL");
    }

    @Test
    public void testProcessingStatus() {
        Assertions.assertThat(status(simpleOffer().updateAcceptanceStatusForTests(Offer.AcceptanceStatus.NEW)))
            .isEqualTo(OfferProcessingStatus.REVIEW);
        Assertions.assertThat(status(simpleOffer()
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.NEW)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.PROCESSED))).isEqualTo(OfferProcessingStatus.REVIEW);
        Assertions.assertThat(status(simpleOffer()
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.TRASH)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.PROCESSED))).isEqualTo(OfferProcessingStatus.REJECTED);
        Assertions.assertThat(status(simpleOffer()
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.LEGAL_PROBLEM))).isEqualTo(OfferProcessingStatus.REJECTED);
        Assertions.assertThat(status(simpleOffer()
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            // No mapping = IN_WORK
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.PROCESSED))).isEqualTo(OfferProcessingStatus.IN_WORK);
        Assertions.assertThat(status(simpleOffer()
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            // No matter status: approved mapping = ready
            .updateApprovedSkuMapping(mapping(1), Offer.MappingConfidence.CONTENT))).isEqualTo(
                OfferProcessingStatus.READY);

        Assertions.assertThat(status(simpleOffer()
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.NEED_INFO))).isEqualTo(OfferProcessingStatus.NEED_INFO);
        Assertions.assertThat(status(simpleOffer()
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.NO_KNOWLEDGE))).isEqualTo(OfferProcessingStatus.SUSPENDED);
        Assertions.assertThat(status(simpleOffer()
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.NO_CATEGORY))).isEqualTo(OfferProcessingStatus.SUSPENDED);
    }

    @Test
    public void testDeletedMappingStatus() {
        Offer offer = simpleOffer().updateApprovedSkuMapping(mapping(0), null);
        Assertions.assertThat(protoConverter.convertBaseOfferToProto(offer, simpleSupplier()).getProcessingStatus())
            .isEqualTo(OfferProcessingStatus.REVIEW);
    }

    @Test
    public void testPartnerConverterShowsComments() {
        Offer offer = simpleOffer()
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.NEED_INFO)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setContentComments(new ContentComment(ContentCommentType.NEED_PICTURES));

        SupplierOffer.Offer proto;
        proto = protoConverter.convertBaseOfferToProto(offer, TEST_SUPPLIER, Collections.emptyMap(), ConvertTarget.PARTNER).build();
        Assertions.assertThat(proto.getProcessingStatus()).isEqualTo(OfferProcessingStatus.NEED_INFO);
        Assertions.assertThat(proto.getContentCommentList()).isNotEmpty();

        proto = protoConverter.convertBaseOfferToProto(offer, TEST_SUPPLIER, Collections.emptyMap(), ConvertTarget.INTERNAL).build();
        Assertions.assertThat(proto.getProcessingStatus()).isEqualTo(OfferProcessingStatus.NEED_INFO);
        Assertions.assertThat(proto.getContentCommentList()).isNotEmpty();

        Offer ready = offer.copy().updateApprovedSkuMapping(mapping(1L), Offer.MappingConfidence.CONTENT);
        proto = protoConverter.convertBaseOfferToProto(ready, TEST_SUPPLIER, Collections.emptyMap(), ConvertTarget.PARTNER).build();
        Assertions.assertThat(proto.getProcessingStatus()).isEqualTo(OfferProcessingStatus.READY);
        Assertions.assertThat(proto.getContentCommentList()).isEmpty(); // No comments for partner for READY

        proto = protoConverter.convertBaseOfferToProto(ready, TEST_SUPPLIER, Collections.emptyMap(), ConvertTarget.INTERNAL).build();
        Assertions.assertThat(proto.getProcessingStatus()).isEqualTo(OfferProcessingStatus.READY);
        Assertions.assertThat(proto.getContentCommentList()).isNotEmpty(); // But show it for INTERNAL
    }

    @Test
    public void sizeMeasureAndToSizeMeasureShouldReturnAsNeedInfo() {
        List<Offer> offers = Arrays.asList(
            simpleOffer()
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.NEED_INFO)
                .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
                .setContentComments(new ContentComment(ContentCommentType.NO_SIZE_MEASURE)),
            simpleOffer()
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.NEED_SIZE_MEASURE)
                .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
                .setContentComments(new ContentComment(ContentCommentType.NO_SIZE_MEASURE)));

        List<SupplierOffer.Offer> protos = offers.stream()
            .map(offer -> protoConverter.convertBaseOfferToProto(offer, TEST_SUPPLIER, Collections.emptyMap(), ConvertTarget.PARTNER).build())
            .collect(Collectors.toList());

        Assertions.assertThat(protos)
            .extracting(SupplierOffer.Offer::getProcessingStatus)
            .allMatch(processingStatus -> processingStatus.equals(SupplierOffer.OfferProcessingStatus.NEED_INFO));
    }

    @Test
    public void testFmcgSuggestBarcodeMappings() {
        Offer fmcgOfferBarcodeSkutch = new Offer()
            .setBusinessId(FMCG_SUPPLIER_ID)
            .setShopSku(DEFAULT_SHOP_SKU)
            .setTitle(DEFAULT_TITLE)
            .setShopCategoryName(DEFAULT_SHOP_CATEGORY_NAME)
            .setIsOfferContentPresent(true)
            .storeOfferContent(OfferContent.initEmptyContent())
            .setSuggestSkuMapping(new Offer.Mapping(MARKET_SKU_ID, DateTimeUtils.dateTimeNow()))
            .setSuggestSkuMappingType(SkuBDApi.SkutchType.BARCODE_SKUTCH);

        SupplierOffer.Offer proto;
        proto = protoConverter.convertBaseOfferToProto(
            fmcgOfferBarcodeSkutch, FMCG_TEST_SUPPLIER, Collections.emptyMap(), ConvertTarget.PARTNER, true
        ).build();
        Assert.assertTrue(proto.hasApprovedMapping());
        assertFalse(proto.hasSuggestMapping());
        Assert.assertEquals(
            proto.getApprovedMapping().getSkuId(),
            Long.valueOf(fmcgOfferBarcodeSkutch.getSuggestedSkuIdStr()).longValue()
        );

        proto = protoConverter.convertBaseOfferToProto(
            fmcgOfferBarcodeSkutch, FMCG_TEST_SUPPLIER, Collections.emptyMap(), ConvertTarget.PARTNER, false
        ).build();
        assertFalse(proto.hasApprovedMapping());
        Assert.assertTrue(proto.hasSuggestMapping());
        Assert.assertEquals(
            proto.getSuggestMapping().getSkuId(),
            Long.valueOf(fmcgOfferBarcodeSkutch.getSuggestedSkuIdStr()).longValue()
        );

        Offer fmcgOfferNotBarcodeSkutch = new Offer()
            .setBusinessId(FMCG_SUPPLIER_ID)
            .setShopSku(DEFAULT_SHOP_SKU)
            .setTitle(DEFAULT_TITLE)
            .setShopCategoryName(DEFAULT_SHOP_CATEGORY_NAME)
            .setIsOfferContentPresent(true)
            .storeOfferContent(OfferContent.initEmptyContent())
            .setSuggestSkuMapping(new Offer.Mapping(MARKET_SKU_ID, DateTimeUtils.dateTimeNow()))
            .setSuggestSkuMappingType(SkuBDApi.SkutchType.SKUTCH_BY_PARAMETERS);

        proto = protoConverter.convertBaseOfferToProto(
            fmcgOfferNotBarcodeSkutch, FMCG_TEST_SUPPLIER, Collections.emptyMap(), ConvertTarget.PARTNER, true
        ).build();
        assertFalse(proto.hasApprovedMapping());
        Assert.assertTrue(proto.hasSuggestMapping());
        Assert.assertEquals(
            proto.getSuggestMapping().getSkuId(),
            Long.valueOf(fmcgOfferNotBarcodeSkutch.getSuggestedSkuIdStr()).longValue()
        );

        proto = protoConverter.convertBaseOfferToProto(
            fmcgOfferNotBarcodeSkutch, FMCG_TEST_SUPPLIER, Collections.emptyMap(), ConvertTarget.PARTNER, false
        ).build();
        assertFalse(proto.hasApprovedMapping());
        Assert.assertTrue(proto.hasSuggestMapping());
        Assert.assertEquals(
            proto.getSuggestMapping().getSkuId(),
            Long.valueOf(fmcgOfferNotBarcodeSkutch.getSuggestedSkuIdStr()).longValue()
        );
    }

    @Test
    public void shouldConvertTicketAndDeadlineIfPossible() {
        LocalDate now = LocalDate.now();

        Offer offer = simpleOffer()
            .setTrackerTicket("TST-123")
            .setTicketDeadline(now);
        SupplierOffer.Offer proto = protoConverter
            .convertBaseOfferToProto(offer, simpleSupplier(), Collections.emptyMap(), ConvertTarget.INTERNAL).build();
        Assertions.assertThat(proto.getProcessingTicket()).isEqualTo(offer.getTrackerTicket());
        Assertions.assertThat(proto.getTicketDeadlineDate()).isEqualTo(offer.getTicketDeadline().toEpochDay());


        offer = simpleOffer()
            .setTrackerTicket((String) null)
            .setTicketDeadline(null);
        proto = protoConverter
            .convertBaseOfferToProto(offer, simpleSupplier(), Collections.emptyMap(), ConvertTarget.INTERNAL).build();
        Assertions.assertThat(proto.hasProcessingTicket()).isFalse();
        Assertions.assertThat(proto.hasTicketDeadlineDate()).isFalse();
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void testContentMappingTaskId() {
        Offer offer = simpleOffer().setContentProcessingTaskId(11L);
        Assertions.assertThat(protoConverter.convertBaseOfferToProto(offer, simpleSupplier()).getContentProcessingTaskId())
            .isEqualTo(11L);
    }

    @Test
    public void testEffectiveSupplierIdShouldBeTheSame() {
        Offer offer = simpleOffer();
        SupplierOffer.Offer.Builder converted = protoConverter.convertBaseOfferToProto(offer, simpleSupplier());
        Assertions.assertThat(converted.getEffectiveShopSkuId()).isEqualTo(offer.getShopSku());
        Assertions.assertThat(converted.getEffectiveSupplierId()).isEqualTo(offer.getBusinessId());
    }

    @Test
    public void testEffectiveSupplierIdShouldBeBeruForRealSuppliers() {
        Offer offer = simpleOffer();
        Supplier supplier = simpleSupplier();
        String realSupplierId = "00713";
        supplier.setRealSupplierId(realSupplierId);
        supplier.setType(MbocSupplierType.REAL_SUPPLIER);

        SupplierOffer.Offer.Builder converted = protoConverter.convertBaseOfferToProto(offer, supplier);
        Assertions.assertThat(converted.getEffectiveShopSkuId()).isEqualTo(realSupplierId + "." + offer.getShopSku());
        Assertions.assertThat(converted.getEffectiveSupplierId()).isEqualTo(BERU_ID);
    }

    @Test
    public void testSetInternalProcessingStatus() {
        Offer offer = simpleOffer();

        for (Offer.ProcessingStatus status : Offer.ProcessingStatus.values()) {
            offer.setProcessingStatusInternal(status);
            SupplierOffer.Offer.Builder converted = protoConverter.convertBaseOfferToProto(offer, simpleSupplier());
            Assertions.assertThat(converted.getInternalProcessingStatus().name())
                .isEqualTo(offer.getProcessingStatus().name());
        }
    }

    @Test
    public void testVendorData() {
        Offer offer = simpleOffer()
            .setVendorId(123)
            .setMarketVendorName("Vendor 123");

        SupplierOffer.Offer.Builder converted = protoConverter.convertBaseOfferToProto(offer, simpleSupplier());
        Assertions.assertThat(converted.getMarketVendorId()).isEqualTo(123);
        Assertions.assertThat(converted.getMarketVendorName()).isEqualTo("Vendor 123");
    }

    @Test
    public void testSetWaitContentTicket() {
        Offer offer = simpleOffer();

        AtomicInteger j = new AtomicInteger();
        OfferProtoConverter.ADDITIONAL_TICKET_TYPES_TO_IMPORT.forEach(type -> {
            offer.addAdditionalTicket(type, "ticket" + j.incrementAndGet());
        });

        SupplierOffer.Offer converted = protoConverter.convertBaseOfferToProto(offer, simpleSupplier()).build();
        for (int i = 0; i < SupplierOffer.AdditionalTicket.Type.values().length; ++i) {
            Assertions.assertThat(converted.getAdditionalTicket(i).getType())
                .isEqualTo(SupplierOffer.AdditionalTicket.Type.values()[i]);
            Assertions.assertThat(converted.getAdditionalTicket(i).getKey())
                .isEqualTo(new ArrayList<>(offer.getAdditionalTickets().values()).get(i));
        }
    }

    @Test
    public void testConvertAllOfMappingSkuType() {
        Offer.Mapping mskuMapping =
            new Offer.Mapping(1L, DateTimeUtils.dateTimeNow(), Offer.SkuType.MARKET);
        Offer.Mapping pskuLegacyMapping =
            new Offer.Mapping(2L, DateTimeUtils.dateTimeNow(), Offer.SkuType.PARTNER);
        Offer.Mapping psku10Mapping =
            new Offer.Mapping(2L, DateTimeUtils.dateTimeNow(), Offer.SkuType.PARTNER10);
        Offer.Mapping psku20Mapping =
            new Offer.Mapping(2L, DateTimeUtils.dateTimeNow(), Offer.SkuType.PARTNER20);
        Offer offer = new Offer()
            .setSuggestSkuMapping(mskuMapping)
            .setSupplierSkuMapping(mskuMapping)
            .setContentSkuMapping(mskuMapping)
            .setDeletedApprovedSkuMapping(pskuLegacyMapping)
            .setDeletedApprovedSkuMappingConfidence(Offer.MappingConfidence.PARTNER_SELF)
            .updateApprovedSkuMapping(mskuMapping, Offer.MappingConfidence.CONTENT);
        SupplierOffer.Offer.Builder b = protoConverter.convertBaseOfferToProto(offer, null);
        Assertions.assertThat(b.getApprovedMapping().getSkuType()).isNotNull();
        Assertions.assertThat(b.getApprovedMapping().getSkuType()).isEqualTo(TYPE_MARKET);
        Assertions.assertThat(b.getApprovedMappingConfidence()).isNotNull();
        Assertions.assertThat(b.getApprovedMappingConfidence()).isEqualTo(MAPPING_CONTENT);
        Assertions.assertThat(b.getDeletedMapping().getSkuType()).isNotNull();
        Assertions.assertThat(b.getDeletedMapping().getSkuType()).isEqualTo(TYPE_PARTNER);
        Assertions.assertThat(b.getDeletedApprovedMappingConfidence()).isNotNull();
        Assertions.assertThat(b.getDeletedApprovedMappingConfidence()).isEqualTo(MAPPING_PARTNER_SELF);

        Assertions.assertThat(b.getApprovedMapping().getSkuName()).isEqualTo(TEST_MAPPING_NAME_BUILDER.apply(1L));
        Assertions.assertThat(b.getDeletedMapping().getSkuName()).isEqualTo(TEST_MAPPING_NAME_BUILDER.apply(2L));
        Assertions.assertThat(b.getSuggestMapping().getSkuName()).isEqualTo(TEST_MAPPING_NAME_BUILDER.apply(1L));
        Assertions.assertThat(b.getSupplierMapping().getSkuName()).isEqualTo(TEST_MAPPING_NAME_BUILDER.apply(1L));

        Offer offer2 = new Offer()
            .setSuggestSkuMapping(pskuLegacyMapping)
            .setSupplierSkuMapping(psku10Mapping)
            .setContentSkuMapping(psku20Mapping)
            .setDeletedApprovedSkuMapping(mskuMapping)
            .setDeletedApprovedSkuMappingConfidence(Offer.MappingConfidence.PARTNER)
            .updateApprovedSkuMapping(psku20Mapping, Offer.MappingConfidence.PARTNER_SELF);
        SupplierOffer.Offer.Builder b2 = protoConverter.convertBaseOfferToProto(offer2, null);
        Assertions.assertThat(b2.getApprovedMapping().getSkuType()).isNotNull();
        Assertions.assertThat(b2.getApprovedMapping().getSkuType()).isEqualTo(TYPE_PARTNER);
        Assertions.assertThat(b2.getApprovedMappingConfidence()).isNotNull();
        Assertions.assertThat(b2.getApprovedMappingConfidence()).isEqualTo(MAPPING_PARTNER_SELF);
        Assertions.assertThat(b2.getDeletedMapping().getSkuType()).isNotNull();
        Assertions.assertThat(b2.getDeletedMapping().getSkuType()).isEqualTo(TYPE_MARKET);
        Assertions.assertThat(b2.getDeletedApprovedMappingConfidence()).isNotNull();
        Assertions.assertThat(b2.getDeletedApprovedMappingConfidence()).isEqualTo(MAPPING_PARTNER);

        Assertions.assertThat(b2.getApprovedMapping().getSkuName()).isEqualTo(TEST_MAPPING_NAME_BUILDER.apply(2L));
        Assertions.assertThat(b2.getDeletedMapping().getSkuName()).isEqualTo(TEST_MAPPING_NAME_BUILDER.apply(1L));
        Assertions.assertThat(b2.getSuggestMapping().getSkuName()).isEqualTo(TEST_MAPPING_NAME_BUILDER.apply(2L));
        Assertions.assertThat(b2.getSupplierMapping().getSkuName()).isEqualTo(TEST_MAPPING_NAME_BUILDER.apply(2L));
    }

    @Test
    public void convertToProviderProductInfoLites() {
        LocalDateTime now = LocalDateTime.now();
        OfferLite offer = new OfferLite()
            .setShopSku("test")
            .setBusinessId(3)
            .setUploadToYtStamp(15L)
            .setMappedModelId(30L)
            .setMappedCategoryId(40L)
            .setApprovedSkuMapping(new Offer.Mapping(20L, now))
            .setServiceOffers(Arrays.asList(
                new Offer.ServiceOffer(1)
                    .setSupplierType(MbocSupplierType.THIRD_PARTY)
                    .setSupplierId(1),
                new Offer.ServiceOffer(2)
                    .setSupplierType(MbocSupplierType.REAL_SUPPLIER)
                    .setSupplierId(2)))
            .setIsDataCampOffer(true);

        Map<Integer, Supplier> suppliers = new HashMap<>();
        suppliers.put(1, new Supplier());
        suppliers.put(2, new Supplier()
            .setRealSupplierId("supplier2")
            .setType(MbocSupplierType.REAL_SUPPLIER));
        suppliers.put(3, new Supplier().setType(MbocSupplierType.BUSINESS));

        List<MboMappings.ProviderProductInfoLite> providerProductInfoLites =
            protoConverter.convertToProviderProductInfoLites(offer, suppliers);

        long mappingTimestamp = now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        assertEquals(providerProductInfoLites.size(), 3);
        assertProductInfoLite(providerProductInfoLites.get(0), 1, "test", mappingTimestamp);
        assertProductInfoLite(providerProductInfoLites.get(1), 2, "test", mappingTimestamp);
        assertProductInfoLite(providerProductInfoLites.get(2), 465852, "supplier2.test", mappingTimestamp);
    }

    @Test
    public void convertAll() {
        for (var status : SskuStatus.values()) {
            // check no exception
            ProtoUtils.convertAvailability(status);
        }
    }

    private void assertProductInfoLite(MboMappings.ProviderProductInfoLite productInfoLite,
                                       int shopId,
                                       String shopSkuId,
                                       long mappingTimestamp) {
        assertEquals(shopId, productInfoLite.getShopId());
        assertEquals(shopSkuId, productInfoLite.getShopSkuId());
        assertEquals(15, productInfoLite.getUploadToYtStamp());
        assertEquals(20, productInfoLite.getMarketSkuId());
        assertEquals(30, productInfoLite.getMarketModelId());
        assertEquals(40, productInfoLite.getMappedCategoryId());
        assertEquals(mappingTimestamp, productInfoLite.getMappingTimeStamp());
        assertTrue(productInfoLite.hasIsDatacampOffer());
        assertTrue(productInfoLite.getIsDatacampOffer());
    }

    private OfferProcessingStatus status(Offer offer) {
        return protoConverter.convertBaseOfferToProto(offer, TEST_SUPPLIER).getProcessingStatus();
    }
}
