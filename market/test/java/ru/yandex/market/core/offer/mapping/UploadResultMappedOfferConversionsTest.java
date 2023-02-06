package ru.yandex.market.core.offer.mapping;

import java.util.Optional;

import javax.annotation.ParametersAreNonnullByDefault;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.offer.mapping.offerlist.Emergency;
import ru.yandex.market.core.offer.mapping.offerlist.LogMessage;
import ru.yandex.market.core.offer.mapping.offerlist.OfferListStatistics;
import ru.yandex.market.core.offer.mapping.offerlist.StatusStatistics;
import ru.yandex.market.core.offer.mapping.offerlist.UploadResult;
import ru.yandex.market.core.tanker.model.UserMessage;
import ru.yandex.market.mbi.util.MbiMatchers;
import ru.yandex.market.mboc.http.MboMappings;
import ru.yandex.market.mboc.http.MbocCommon;
import ru.yandex.market.mboc.http.SupplierOffer;
import ru.yandex.market.mboc.http.SupplierOffer.Offer.MappingProcessingStatus;
import ru.yandex.market.mboc.http.SupplierOffer.Offer.MappingProcessingStatus.ChangeStatus;

@ParametersAreNonnullByDefault
class UploadResultMappedOfferConversionsTest extends FunctionalTest {

    @Autowired
    private OfferConversionService offerConversionService;

    @Test
    void toUploadResultTest() {
        MboMappings.OfferExcelUpload.Result mbocUploadResult =
                MboMappings.OfferExcelUpload.Result.newBuilder()
                        .setStatus(MboMappings.OfferExcelUpload.Status.WARNING)
                        .setStatusMessage(MbocCommon.Message.newBuilder()
                                .setMessageCode("mboc.error.some-offers-skipped")
                                .setMustacheTemplate("Some offers skipped")
                                .build())
                        .setUploadStatistics(MboMappings.OfferExcelUpload.UploadStatistics.newBuilder()
                                .setErrorsSkipped(1)
                                .setExistingOffers(MboMappings.OfferStatusStatistics.newBuilder()
                                        .setApproved(2)
                                        .setInWork(3)
                                        .setRejected(4)
                                        .build())
                                .setNewOffers(5)
                                .setTotal(6)
                                .build())
                        .addItemMessages(MboMappings.OfferExcelUpload.ItemMessage.newBuilder()
                                .setEmergency(MboMappings.OfferExcelUpload.Status.ERROR)
                                .setExcelLineReference(1)
                                .setMessage(MbocCommon.Message.newBuilder()
                                        .setMustacheTemplate("Unpublished marketSku: {{marketSku}}")
                                        .setMessageCode("mboc.error.unpublished")
                                        .setJsonDataForMustacheTemplate(/*language=json*/ "{\"marketSku\": 124151536}")
                                        .build())
                                .build())
                        .addItemMessages(MboMappings.OfferExcelUpload.ItemMessage.newBuilder()
                                .setEmergency(MboMappings.OfferExcelUpload.Status.WARNING)
                                .setExcelLineReference(2)
                                .setMessage(MbocCommon.Message.newBuilder()
                                        .setMustacheTemplate("Invalid shopSku: {{shopSku}}")
                                        .setMessageCode("mboc.error.invalid-shop-sku")
                                        .setJsonDataForMustacheTemplate(/*language=json*/ "{\"shopSku\": \"sss\"}")
                                        .build())
                                .build())
                        .addItemMessages(MboMappings.OfferExcelUpload.ItemMessage.newBuilder()
                                .setEmergency(MboMappings.OfferExcelUpload.Status.ERROR)
                                .setExcelLineReference(2)
                                .setMessage(MbocCommon.Message.newBuilder()
                                        .setMustacheTemplate("Invalid vat")
                                        .setMessageCode("mboc.error.invalid-vat")
                                        .build())
                                .build())
                        .addParsedOffers(MboMappings.OfferExcelUpload.ParsedOffer.newBuilder()
                                .setExcelDataLineIndex(0)
                                .setOffer(
                                        SupplierOffer.Offer.newBuilder()
                                                .setTitle("Test H123")
                                                .setSupplierId(123)
                                                .setShopSkuId("H123")
                                                .setShopCategoryName("Shop/Category/Name")
                                                .setBarcode("sdkgjsdh12431254, sdjgh124314231, dskjghs124152")
                                                .setVendorCode("sgsd23523")
                                                .setShopVendor("Apple")
                                                .setDescription("Test H123 Description")
                                                .setVat("1")
                                                .setShopPrice("123.99")
                                                .setIsRealizing(false)
                                                .setApprovedMapping(
                                                        SupplierOffer.Mapping.newBuilder()
                                                                .setSkuId(1288)
                                                                .setSkuName("MarketSku1288")
                                                                .setCategoryName("Category123")
                                                                .setCategoryId(123)
                                                                .build())
                                                .setSupplierMapping(
                                                        SupplierOffer.Mapping.newBuilder()
                                                                .setSkuId(1214)
                                                                .setSkuName("MarketSku1214")
                                                                .setCategoryId(123)
                                                                .setCategoryName("Category123")
                                                                .build())
                                                .setSuggestMapping(
                                                        SupplierOffer.Mapping.newBuilder()
                                                                .setSkuId(1215)
                                                                .setSkuName("MarketSku1215")
                                                                .setCategoryId(123)
                                                                .setCategoryName("Category123")
                                                                .build())
                                                .setSupplierMappingStatus(
                                                        MappingProcessingStatus.newBuilder()
                                                                .setStatus(ChangeStatus.MODERATION)
                                                                .build())
                                                .build()
                                )
                                .build())
                        .addParsedOffers(MboMappings.OfferExcelUpload.ParsedOffer.newBuilder()
                                .setExcelDataLineIndex(1)
                                .setOffer(
                                        SupplierOffer.Offer.newBuilder()
                                                .setTitle("Test H124")
                                                .setSupplierId(123)
                                                .setShopSkuId("H124")
                                                .setShopCategoryName("Shop/Category/Name")
                                                .setBarcode("sdkgjsdh12431254, sdjgh124314231, dskjghs124152")
                                                .setVendorCode("sgsd23523")
                                                .setShopVendor("Apple")
                                                .setDescription("Test H124 Description")
                                                .setVat("sdgklhsl") // Should be skipped
                                                .setShopPrice("sdgklhdskl") // Should be skipped
                                                .setIsRealizing(true)
                                                .setApprovedMapping(
                                                        SupplierOffer.Mapping.newBuilder()
                                                                .setSkuId(1288)
                                                                .setSkuName("MarketSku1288")
                                                                .setCategoryId(123)
                                                                .setCategoryName("Category123")
                                                                .build())
                                                .setSupplierMapping(
                                                        SupplierOffer.Mapping.newBuilder()
                                                                // Illegal Msku has no info: should be skipped
                                                                .setSkuId(124151536)
                                                                .build())
                                                .setSupplierMappingStatus(
                                                        MappingProcessingStatus.newBuilder()
                                                                .setStatus(ChangeStatus.MODERATION)
                                                                .build())
                                                .build()
                                )
                                .build())
                        .addParsedOffers(MboMappings.OfferExcelUpload.ParsedOffer.newBuilder()
                                .setExcelDataLineIndex(2)
                                .setOffer(
                                        SupplierOffer.Offer.newBuilder()
                                                .setTitle("Test H125")
                                                .setSupplierId(123)
                                                .setShopSkuId("H125")
                                                .build()
                                )
                                .build())
                        .build();

        UploadResult uploadResult = offerConversionService.toUploadResult(mbocUploadResult);
        System.out.println(uploadResult.logMessages());

        MatcherAssert.assertThat(
                uploadResult,
                MbiMatchers.<UploadResult>newAllOfBuilder()
                        .add(UploadResult::summary, MbiMatchers.<UploadResult.Summary>newAllOfBuilder()
                                .add(UploadResult.Summary::emergency, Matchers.is(Emergency.WARNING))
                                .add(UploadResult.Summary::userMessage, MbiMatchers.<UserMessage>newAllOfBuilder()
                                        .add(UserMessage::defaultTranslation, "Some offers skipped")
                                        .add(UserMessage::messageCode, "mboc.error.some-offers-skipped")
                                        .add(UserMessage::mustacheArguments, "{}")
                                        .build())
                                .add(UploadResult.Summary::statistics, MbiMatchers.isPresent(
                                        MbiMatchers.<OfferListStatistics>newAllOfBuilder()
                                                .add(OfferListStatistics::newOffers, 5)
                                                .add(OfferListStatistics::skippedErroneousOffers, 1)
                                                .add(OfferListStatistics::totalOffers, 6)
                                                .add(OfferListStatistics::existingOffers,
                                                        MbiMatchers.<StatusStatistics>newAllOfBuilder()
                                                                .add(StatusStatistics::approved, 2)
                                                                .add(StatusStatistics::inProgress, 3)
                                                                .add(StatusStatistics::rejected, 4)
                                                                .add(StatusStatistics::needInfo, 0)
                                                                .build())
                                                .build()))
                                .build())
                        .add(UploadResult::logMessages, Matchers.allOf(
                                Matchers.hasEntry(Matchers.is(1), Matchers.contains(MbiMatchers.<LogMessage>newAllOfBuilder()
                                        .add(LogMessage::emergency, Emergency.ERROR)
                                        .add(LogMessage::userMessage, MbiMatchers.<UserMessage>newAllOfBuilder()
                                                .add(UserMessage::defaultTranslation, "Unpublished marketSku: {{marketSku}}")
                                                .add(UserMessage::messageCode, "mboc.error.unpublished")
                                                .add(UserMessage::mustacheArguments, /*language=json*/ "{\"marketSku\": 124151536}")
                                                .build())
                                        .build())),
                                Matchers.hasEntry(Matchers.is(2), Matchers.contains(
                                        MbiMatchers.<LogMessage>newAllOfBuilder()
                                                .add(LogMessage::emergency, Emergency.WARNING)
                                                .add(LogMessage::userMessage, MbiMatchers.<UserMessage>newAllOfBuilder()
                                                        .add(UserMessage::defaultTranslation, "Invalid shopSku: {{shopSku}}")
                                                        .add(UserMessage::messageCode, "mboc.error.invalid-shop-sku")
                                                        .add(UserMessage::mustacheArguments, /*language=json*/ "{\"shopSku\": \"sss\"}")
                                                        .build())
                                                .build(),
                                        MbiMatchers.<LogMessage>newAllOfBuilder()
                                                .add(LogMessage::emergency, Emergency.ERROR)
                                                .add(LogMessage::userMessage, MbiMatchers.<UserMessage>newAllOfBuilder()
                                                        .add(UserMessage::defaultTranslation, "Invalid vat")
                                                        .add(UserMessage::messageCode, "mboc.error.invalid-vat")
                                                        .add(UserMessage::mustacheArguments, /*language=json*/ "{}")
                                                        .build())
                                                .build()
                                ))
                        ))
                        .add(UploadResult::offers, Matchers.allOf(
                                Matchers.hasEntry(Matchers.is(0), MbiMatchers.<MappedOffer>newAllOfBuilder()
                                        .add(MappedOffer::shopOffer, MbiMatchers.<ShopOffer>newAllOfBuilder()
                                                .add(ShopOffer::title, "Test H123")
                                                .add(ShopOffer::supplierId, 123L)
                                                .add(ShopOffer::shopSku, "H123")
                                                .build())
                                        .add(MappedOffer::activeLink,
                                                MbiMatchers.isPresent(MappedOfferMatchers.isMarketSku(1288)))
                                        .add(MappedOffer::partnerLink, MbiMatchers.isPresent(
                                                MbiMatchers.<ModeratedLink<MarketEntityInfo>>newAllOfBuilder()
                                                        .add(ModeratedLink::status, ModerationStatus.MODERATION)
                                                        .add(ModeratedLink::target, MappedOfferMatchers.isMarketSku(1214L))
                                                        .build()

                                        ))
                                        .add(MappedOffer::suggestedLink,
                                                MbiMatchers.isPresent(MappedOfferMatchers.isMarketSku(1215L)))
                                        .build()),
                                Matchers.hasEntry(Matchers.is(1), MbiMatchers.<MappedOffer>newAllOfBuilder()
                                        .add(MappedOffer::shopOffer, MbiMatchers.<ShopOffer>newAllOfBuilder()
                                                .add(ShopOffer::title, "Test H124")
                                                .add(ShopOffer::supplierId, 123L)
                                                .add(ShopOffer::shopSku, "H124")
                                                .build())
                                        .add(MappedOffer::activeLink,
                                                MbiMatchers.isPresent(MappedOfferMatchers.isMarketSku(1288)))
                                        .add(MappedOffer::partnerLink, Optional.empty())
                                        .add(MappedOffer::suggestedLink, Optional.empty())
                                        .build()),
                                Matchers.hasEntry(Matchers.is(2), MbiMatchers.<MappedOffer>newAllOfBuilder()
                                        .add(MappedOffer::shopOffer, MbiMatchers.<ShopOffer>newAllOfBuilder()
                                                .add(ShopOffer::title, "Test H125")
                                                .add(ShopOffer::supplierId, 123L)
                                                .add(ShopOffer::shopSku, "H125")
                                                .build())
                                        .add(MappedOffer::activeLink, Optional.empty())
                                        .add(MappedOffer::partnerLink, Optional.empty())
                                        .add(MappedOffer::suggestedLink, Optional.empty())
                                        .build())
                        ))
                        .build()
        );
    }
}
