package ru.yandex.market.mboc.common.datacamp.service.converter;

import java.util.Arrays;
import java.util.Collections;

import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferContent;
import Market.DataCamp.DataCampOfferIdentifiers;
import Market.DataCamp.DataCampOfferMapping;
import Market.DataCamp.DataCampOfferMeta;
import Market.DataCamp.PartnerCategoryOuterClass;
import org.junit.Test;

import ru.yandex.market.mboc.common.datacamp.DataCampOfferUtil;
import ru.yandex.market.mboc.http.MboMappings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class DataCampOfferToProviderProductInfoConverterTest {
    @Test
    public void convertDatacampOfferWhiteComplete() {
        var offer = DataCampOffer.Offer.newBuilder()
            .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                .setOfferId("offer1234")
                .setBusinessId(123)
                .build()
            )
            .setMeta(DataCampOfferMeta.OfferMeta.newBuilder()
                .setRgb(DataCampOfferMeta.MarketColor.WHITE)
                .build()
            )
            .setContent(DataCampOfferContent.OfferContent.newBuilder()
                .setPartner(DataCampOfferContent.PartnerContent.newBuilder()
                    .setActual(DataCampOfferContent.ProcessedSpecification.newBuilder()
                        .setTitle(DataCampOfferMeta.StringValue.newBuilder().setValue("TITLE").build())
                        .setVendor(DataCampOfferMeta.StringValue.newBuilder().setValue("VENDOR").build())
                        .setVendorCode(DataCampOfferMeta.StringValue.newBuilder().setValue("VENDOR_CODE").build())
                        .setBarcode(DataCampOfferMeta.StringListValue.newBuilder()
                            .addValue("BAR_CODE1")
                            .addValue("BAR_CODE2")
                            .build()
                        )
                        .setDescription(DataCampOfferMeta.StringValue.newBuilder().setValue("DESCRIPTION").build())
                        .setUrl(DataCampOfferMeta.StringValue.newBuilder().setValue("http://the.url").build())
                        .setCategory(PartnerCategoryOuterClass.PartnerCategory.newBuilder()
                            .setName("CATEGORY")
                            .build())
                        .build()
                    )
                    .setOriginal(DataCampOfferContent.OriginalSpecification.newBuilder()
                        .setGroupId(DataCampOfferMeta.Ui32Value.newBuilder()
                            .setValue(1)
                            .build())
                        .build())
                    .build()
                )
                .setBinding(DataCampOfferMapping.ContentBinding.newBuilder()
                    .setPartner(DataCampOfferMapping.Mapping.newBuilder()
                        .setMarketCategoryId(12)
                        .setMarketModelId(23)
                        .setMarketSkuId(34)
                        .build()
                    )
                    .build()
                )
                .build()
            )
            .build();

        var serviceOffer = DataCampOffer.Offer.newBuilder()
            .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                .setOfferId("offer1234")
                .setBusinessId(123)
                .setShopId(12)
                .build()
            )
            .setContent(DataCampOfferContent.OfferContent.newBuilder()
                .setPartner(DataCampOfferContent.PartnerContent.newBuilder()
                    .setActual(DataCampOfferContent.ProcessedSpecification.newBuilder()
                        .setUrl(DataCampOfferMeta.StringValue.newBuilder().setValue("http://the.service.url").build())
                        .build()
                    )
                    .build()
                )
                .build()
            )
            .build();

        var pi = DataCampOfferToProviderProductInfoConverter
            .convert(DataCampOfferUtil.extractExternalBusinessSkuKey(offer), offer, Collections.singletonMap(12, serviceOffer));
        assertEquals("offer1234", pi.getShopSkuId());
        assertEquals(123, pi.getShopId());
        assertEquals(MboMappings.MappingType.PRICE_COMPARISION, pi.getMappingType());
        assertEquals("TITLE", pi.getTitle());
        assertEquals("VENDOR", pi.getVendor());
        assertEquals("VENDOR_CODE", pi.getVendorCode());
        assertEquals(Arrays.asList("BAR_CODE1", "BAR_CODE2"), pi.getBarcodeList());
        assertEquals("DESCRIPTION", pi.getDescription());
        assertEquals(Arrays.asList("http://the.url", "http://the.service.url"), pi.getUrlList());
        assertEquals("CATEGORY", pi.getShopCategoryName());
        assertEquals(12, pi.getMarketCategoryId());
        assertEquals(23, pi.getMarketModelId());
        assertEquals(34, pi.getMarketSkuId());
    }

    @Test
    public void convertDatacampOfferBluePartial() {
        var offer = DataCampOffer.Offer.newBuilder()
            .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                .setOfferId("offer1234")
                .setBusinessId(123)
                .build()
            )
            .setContent(DataCampOfferContent.OfferContent.newBuilder().build())
            .build();

        var serviceOffer = DataCampOffer.Offer.newBuilder()
            .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                .setOfferId("offer1234")
                .setBusinessId(123)
                .setShopId(12)
                .build()
            )
            .setContent(DataCampOfferContent.OfferContent.newBuilder().build())
            .build();

        var pi = DataCampOfferToProviderProductInfoConverter
            .convert(DataCampOfferUtil.extractExternalBusinessSkuKey(offer), offer, Collections.singletonMap(12, serviceOffer));
        assertEquals("offer1234", pi.getShopSkuId());
        assertEquals(123, pi.getShopId());
        assertFalse(pi.hasTitle());
        assertFalse(pi.hasVendor());
        assertFalse(pi.hasVendorCode());
        assertEquals(Collections.emptyList(), pi.getBarcodeList());
        assertFalse(pi.hasDescription());
        assertEquals(Collections.emptyList(), pi.getUrlList());
        assertFalse(pi.hasShopCategoryName());
        assertFalse(pi.hasMarketCategoryId());
        assertFalse(pi.hasMarketModelId());
        assertFalse(pi.hasMarketSkuId());
    }
}
