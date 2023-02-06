package ru.yandex.market.mbo.mdm.common.datacamp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import Market.DataCamp.API.DatacampMessageOuterClass;
import Market.DataCamp.DataCampContentStatus;
import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferContent;
import Market.DataCamp.DataCampOfferIdentifiers;
import Market.DataCamp.DataCampOfferMapping;
import Market.DataCamp.DataCampOfferMeta;
import Market.DataCamp.DataCampOfferStatus;
import Market.DataCamp.DataCampUnitedOffer;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Timestamp;

import ru.yandex.kikimr.persqueue.compression.CompressionCodec;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageBatch;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageData;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageMeta;

import static Market.DataCamp.DataCampContentStatus.MappingConfidence.MAPPING_CONFIDENCE_CONTENT;

/**
 * @author dmserebr
 * @date 08/04/2021
 */
public class DatacampOffersTestUtil {
    private static final int BUSINESS_ID = 123;
    private static final String SHOP_SKU = "shapoklyak";
    private static final long MSKU_ID = 12392;
    private static final int CATEGORY_ID = 1;
    private static final long MASTER_DATA_VERSION = 5L;
    private static final DataCampContentStatus.MappingConfidence MAPPING_CONFIDENCE = MAPPING_CONFIDENCE_CONTENT;
    private static final DataCampOfferMeta.UpdateMeta DEFAULT_META = DataCampOfferMeta.UpdateMeta.newBuilder()
        .setTimestamp(Timestamp.newBuilder().setSeconds(9000).build()).build();

    public static MessageBatch createTestMessage(DataCampUnitedOffer.UnitedOffer... unitedOffers) {
        byte[] data = DatacampMessageOuterClass.DatacampMessage.newBuilder()
            .addUnitedOffers(DataCampUnitedOffer.UnitedOffersBatch.newBuilder()
                .addAllOffer(Stream.of(unitedOffers).collect(Collectors.toList())).build())
            .build()
            .toByteArray();

        MessageMeta messageMeta = new MessageMeta(
            "test-source".getBytes(),
            1L, 1L, 1L, "1.1.1.1", CompressionCodec.RAW, Map.of());

        List<MessageData> messageData = new ArrayList<>();
        messageData.add(new MessageData(data, 0, messageMeta));
        return new MessageBatch("", 0, messageData);
    }

    public static DataCampUnitedOffer.UnitedOffer createUnitedOfferWithTunedServiceVersions(
        DataCampOfferMeta.UpdateMeta meta,
        Long serviceMdVersion,
        DataCampOffer.Offer.Builder... serviceOffers) {
        return createUnitedOfferWithTunedVersions(meta, MASTER_DATA_VERSION, serviceMdVersion,
            serviceOffers);
    }

    public static DataCampUnitedOffer.UnitedOffer createUnitedOfferWithTunedVersions(
        DataCampOfferMeta.UpdateMeta meta,
        Long basicMdVersion,
        Long serviceMdVersion,
        DataCampOffer.Offer.Builder... serviceOffers) {

        // default mapping
        DataCampOfferMapping.ContentBinding contentBinding = defaultContentBinding(meta);

        return createUnitedOfferWithTunedVersions(meta, basicMdVersion, serviceMdVersion, contentBinding,
            serviceOffers);

    }

    private static DataCampOfferMapping.ContentBinding defaultContentBinding(DataCampOfferMeta.UpdateMeta meta) {
        DataCampOfferMapping.Mapping.Builder mappingBuilder = DataCampOfferMapping.Mapping.newBuilder()
            .setMarketSkuId(MSKU_ID)
            .setMarketCategoryId(CATEGORY_ID)
            .setMeta(meta);
        return DataCampOfferMapping.ContentBinding.newBuilder()
            .setApproved(mappingBuilder.build())
            .build();
    }

    public static DataCampUnitedOffer.UnitedOffer createUnitedOfferWithTunedVersions(
        DataCampOfferMeta.UpdateMeta meta,
        Long basicMdVersion,
        Long serviceMdVersion,
        DataCampOfferMapping.ContentBinding contentBinding,
        DataCampOffer.Offer.Builder... serviceOffers) {

        // some master data
        DataCampOfferContent.OriginalSpecification.Builder originalSpecBuilder =
            DataCampOfferContent.OriginalSpecification.newBuilder()
                .setCertificates(DataCampOfferMeta.StringListValue.newBuilder().addValue("345").build());
        DataCampOfferContent.PartnerContent.Builder partnerContent = DataCampOfferContent.PartnerContent.newBuilder()
            .setOriginal(originalSpecBuilder.build());

        // mapping confidence
        DataCampContentStatus.ContentStatus contentStatus = DataCampContentStatus.ContentStatus.newBuilder()
            .setContentSystemStatus(
                DataCampContentStatus.ContentSystemStatus.newBuilder()
                    .setMeta(meta)
                    .setSkuMappingConfidence(MAPPING_CONFIDENCE_CONTENT))
            .build();

        // basic offer status
        DataCampOfferStatus.OfferStatus.Builder basicOfferStatus = offerStatus(basicMdVersion);

        // final offer content
        DataCampOfferContent.OfferContent.Builder offerContentBuilder = DataCampOfferContent.OfferContent.newBuilder()
            .setPartner(partnerContent)
            .setBinding(contentBinding)
            .setStatus(contentStatus);

        DataCampOfferIdentifiers.OfferIdentifiers.Builder basicIdentifiers = basicIdentifiers(BUSINESS_ID, SHOP_SKU);

        DataCampOffer.Offer.Builder basicOffer = DataCampOffer.Offer.newBuilder()
            .setStatus(basicOfferStatus)
            .setContent(offerContentBuilder)
            .setIdentifiers(basicIdentifiers);

        // basic offer status
        DataCampOfferStatus.OfferStatus.Builder serviceOfferStatus = offerStatus(serviceMdVersion);

        var tunedServiceOffers = Arrays.stream(serviceOffers)
            .map(it -> it.setStatus(serviceOfferStatus))
            .toArray(DataCampOffer.Offer.Builder[]::new);

        return createUnitedOffer(basicOffer, tunedServiceOffers);
    }

    public static DataCampUnitedOffer.UnitedOffer createUnitedOffer(
        DataCampOffer.Offer.Builder basicOffer,
        DataCampOffer.Offer.Builder... serviceOffers) {

        var unitedOfferBuilder = DataCampUnitedOffer.UnitedOffer.newBuilder()
            .setBasic(basicOffer);

        for (DataCampOffer.Offer.Builder serviceOffer : serviceOffers) {
            unitedOfferBuilder.putService(serviceOffer.getIdentifiers().getShopId(),
                serviceOffer.build());
        }

        return unitedOfferBuilder.build();
    }

    public static DataCampOfferIdentifiers.OfferIdentifiers.Builder basicIdentifiers(Integer businessId,
                                                                                     String shopSku) {
        return DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
            .setBusinessId(businessId)
            .setOfferId(shopSku);
    }

    public static DataCampOfferStatus.OfferStatus.Builder offerStatus(Long mdVersion) {
        DataCampOfferStatus.VersionStatus.Builder versionStatus = DataCampOfferStatus.VersionStatus.newBuilder()
            .setMasterDataVersion(DataCampOfferMeta.VersionCounter.newBuilder()
                .setCounter(mdVersion));

        return DataCampOfferStatus.OfferStatus.newBuilder()
            .setVersion(versionStatus);
    }

    public static DataCampUnitedOffer.UnitedOffer createUnitedOffer(
        DataCampContentStatus.MappingConfidence confidence,
        DataCampOfferMeta.UpdateMeta meta,
        DataCampOffer.Offer.Builder... serviceOffers) {
        return createUnitedOfferWithTunedServiceVersions(meta, MASTER_DATA_VERSION, serviceOffers);
    }

    public static DataCampUnitedOffer.UnitedOffer createUnitedOfferWithEmptySilverData(
        DataCampContentStatus.MappingConfidence confidence,
        DataCampOfferMeta.UpdateMeta meta) {

        // some master data
        DataCampOfferContent.OriginalSpecification.Builder originalSpecBuilder =
            DataCampOfferContent.OriginalSpecification.newBuilder();
        DataCampOfferContent.PartnerContent.Builder partnerContent = DataCampOfferContent.PartnerContent.newBuilder()
            .setOriginal(originalSpecBuilder.build());

        // mapping
        DataCampOfferMapping.ContentBinding contentBinding = defaultContentBinding(meta);

        // mapping confidence
        DataCampContentStatus.ContentStatus contentStatus = DataCampContentStatus.ContentStatus.newBuilder()
            .setContentSystemStatus(
                DataCampContentStatus.ContentSystemStatus.newBuilder()
                    .setMeta(meta)
                    .setSkuMappingConfidence(confidence))
            .build();

        // final offer content
        DataCampOfferContent.OfferContent.Builder offerContentBuilder = DataCampOfferContent.OfferContent.newBuilder()
            .setPartner(partnerContent)
            .setBinding(contentBinding)
            .setStatus(contentStatus);

        DataCampOfferIdentifiers.OfferIdentifiers.Builder basicIdentifiers = basicIdentifiers(BUSINESS_ID, SHOP_SKU);

        DataCampOffer.Offer.Builder basicOffer = DataCampOffer.Offer.newBuilder()
            .setContent(offerContentBuilder.build())
            .setIdentifiers(basicIdentifiers);

        var unitedOfferBuilder = DataCampUnitedOffer.UnitedOffer.newBuilder()
            .setBasic(basicOffer);

        return unitedOfferBuilder.build();
    }


    public static DataCampOffer.Offer.Builder createBusinessOfferBuilder(
        int businessId,
        String shopSku
    ) {
        return createBusinessOfferBuilder(businessId, shopSku, DEFAULT_META);
    }

    public static DataCampOffer.Offer.Builder createBusinessOfferBuilder(
        int businessId,
        String shopSku,
        Market.DataCamp.DataCampOfferMeta.UpdateMeta meta
    ) {
        return createBusinessOfferBuilder(businessId, shopSku, meta, MASTER_DATA_VERSION);
    }

    public static DataCampOffer.Offer.Builder createBusinessOfferBuilder(
        int businessId,
        String shopSku,
        Market.DataCamp.DataCampOfferMeta.UpdateMeta meta,
        Long basicMdVersion
    ) {
        return createBusinessOfferBuilder(businessId, shopSku, meta, basicMdVersion, defaultContentBinding(meta));
    }

    public static DataCampOffer.Offer.Builder createBusinessOfferBuilder(
        int businessId,
        String shopSku,
        Market.DataCamp.DataCampOfferMeta.UpdateMeta meta,
        Long basicMdVersion,
        DataCampOfferMapping.ContentBinding contentBinding
    ) {
        // some master data
        DataCampOfferContent.OriginalSpecification.Builder originalSpecBuilder =
            DataCampOfferContent.OriginalSpecification.newBuilder()
                .setCertificates(DataCampOfferMeta.StringListValue.newBuilder().addValue("345").build());
        DataCampOfferContent.PartnerContent.Builder partnerContent = DataCampOfferContent.PartnerContent.newBuilder()
            .setOriginal(originalSpecBuilder.build());

        // mapping confidence
        DataCampContentStatus.ContentStatus contentStatus = DataCampContentStatus.ContentStatus.newBuilder()
            .setContentSystemStatus(
                DataCampContentStatus.ContentSystemStatus.newBuilder()
                    .setMeta(meta)
                    .setSkuMappingConfidence(MAPPING_CONFIDENCE_CONTENT))
            .build();

        // basic offer status
        DataCampOfferStatus.OfferStatus.Builder basicOfferStatus = offerStatus(basicMdVersion);

        // final offer content
        DataCampOfferContent.OfferContent.Builder offerContentBuilder = DataCampOfferContent.OfferContent.newBuilder()
            .setPartner(partnerContent)
            .setBinding(contentBinding)
            .setStatus(contentStatus);

        DataCampOfferIdentifiers.OfferIdentifiers.Builder basicIdentifiers = basicIdentifiers(businessId, shopSku);

        return DataCampOffer.Offer.newBuilder()
            .setStatus(basicOfferStatus)
            .setContent(offerContentBuilder)
            .setIdentifiers(basicIdentifiers);
    }

    public static DataCampOffer.Offer.Builder createServiceOfferBuilder(int businessId, int supplierId) {
        return createServiceOfferBuilder(businessId, supplierId, false);
    }

    public static DataCampOffer.Offer.Builder createServiceOfferBuilder(int businessId, int supplierId,
                                                                        boolean emptyServiceOffer) {
        DataCampOfferIdentifiers.OfferIdentifiers.Builder serviceIdentifiers =
            DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                .setBusinessId(businessId)
                .setOfferId(SHOP_SKU)
                .setShopId(supplierId);

        var originalTermsBuilder = DataCampOfferContent.OriginalTerms.newBuilder();
        if (!emptyServiceOffer) {
            originalTermsBuilder.setTransportUnitSize(DataCampOfferMeta.Ui32Value.newBuilder().setValue(12));
        }

        DataCampOffer.Offer.Builder serviceOffer = DataCampOffer.Offer.newBuilder()
            .setContent(DataCampOfferContent.OfferContent.newBuilder()
                .setPartner(DataCampOfferContent.PartnerContent.newBuilder()
                    .setOriginalTerms(originalTermsBuilder)))
            .setIdentifiers(serviceIdentifiers);

        return serviceOffer;
    }

    public static List<DatacampMessageOuterClass.DatacampMessage> getDataCampMessages(MessageBatch messageBatch) {
        return messageBatch.getMessageData().stream()
            .map(MessageData::getDecompressedData)
            .map(DatacampOffersTestUtil::parse)
            .collect(Collectors.toList());
    }

    public static DatacampMessageOuterClass.DatacampMessage parse(byte[] msgRawData) {
        try {
            return DatacampMessageOuterClass.DatacampMessage.parseFrom(msgRawData);
        } catch (InvalidProtocolBufferException ex) {
            return DatacampMessageOuterClass.DatacampMessage.newBuilder().build();
        }
    }
}
