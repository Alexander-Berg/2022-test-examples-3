package ru.yandex.market.ff4shops.logbroker;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import Market.DataCamp.API.DatacampMessageOuterClass;
import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferContent;
import Market.DataCamp.DataCampOfferIdentifiers;
import Market.DataCamp.DataCampOfferMapping;
import Market.DataCamp.DataCampOfferStatus;
import Market.DataCamp.DataCampUnitedOffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.kikimr.persqueue.compression.CompressionCodec;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageBatch;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageData;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageMeta;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.ff4shops.config.FunctionalTest;
import ru.yandex.market.ff4shops.partner.PartnerOffersLbImporterService;

import static Market.DataCamp.API.DatacampMessageOuterClass.DatacampMessage;

class DatacampOffersDataProcessorTest  extends FunctionalTest {

    private static final String TOPIC_NAME = "datacamp_offers_topic";
    private static final int PARTITION = 100500;

    @Autowired
    PartnerOffersLbImporterService partnerOffersImporterService;

    DatacampOffersDataProcessor dataProcessor;

    @BeforeEach
    void init() {
        dataProcessor = new DatacampOffersDataProcessor(partnerOffersImporterService);
    }

    @Test
    @DbUnitDataSet(before = "datacampOffersDataProcessorTest.before.csv",
    after = "datacampOffersDataProcessorTest.twoOffers.oneMessage.after.csv")
    void processTestTwoOffersOneMessage(){
        var messages = List.of(
                createMessage(100,
                        Map.of("1", false, "2", false),
                        1,
                        DataCampOfferStatus.SummaryPublicationStatus.AVAILABLE));
       processOffers(messages);
    }

    @Test
    @DbUnitDataSet(before = "datacampOffersDataProcessorTest.before.csv",
            after = "datacampOffersDataProcessorTest.twoOffers.twoMessage.after.csv")
    void processTestTwoOffersTwoMessages(){
        var messages = List.of(
                createMessage(100,
                        Map.of("3", false),
                        1,
                        DataCampOfferStatus.SummaryPublicationStatus.AVAILABLE),
                createMessage(100,
                        Map.of("4", false),
                        1,
                        DataCampOfferStatus.SummaryPublicationStatus.AVAILABLE));
        processOffers(messages);
    }

    @Test
    @DbUnitDataSet(before = "datacampOffersDataProcessorTest.before.csv",
            after = "datacampOffersDataProcessorTest.twoOffers.twoMessage.oldArch.New.after.csv")
    void processTestTwoOffersTwoMessagesOldArchNew(){
        var messages = List.of(
                createMessage(777,
                        Map.of("sku6", false),
                        1,
                        DataCampOfferStatus.SummaryPublicationStatus.AVAILABLE),
                createMessage(777,
                        Map.of("sku5", true),
                        1,
                        DataCampOfferStatus.SummaryPublicationStatus.AVAILABLE));
        processOffers(messages);
    }

    @Test
    @DbUnitDataSet(before = "datacampOffersDataProcessorTest.before.csv",
            after = "datacampOffersDataProcessorTest.notCorrectOffer.after.csv")
    void processTestNotCorrectOffer(){
        var messages = List.of(
                createMessage(777,
                        Map.of("sku7", false),
                        1,
                        DataCampOfferStatus.SummaryPublicationStatus.HIDDEN));
        processOffers(messages);
    }

    @Test
    @DbUnitDataSet(before = "datacampOffersDataProcessorTest.before.csv",
            after = "datacampOffersDataProcessorTest.notCorrectOffer.after.csv")
    void processTestNewArchOffer(){
        var messages = List.of(
                createMessage(777,
                        Map.of("sku7", true),
                        1,
                        DataCampOfferStatus.SummaryPublicationStatus.AVAILABLE));
        processOffers(messages);
    }

     @Test
     @DbUnitDataSet(before = "datacampOffersDataProcessorTest.before.csv",
             after = "datacampOffersDataProcessorTest.twoOffers.twoMessage.oldArch.New.after.csv")
     void processExistingOfferWithoutArchiveFiled()
     {
         var map = new HashMap<String, Boolean>();
         map.put("sku6", null);
         var messages = List.of(
                 createMessage(777,
                         map,
                         1,
                         DataCampOfferStatus.SummaryPublicationStatus.AVAILABLE),
                 createMessage(777,
                         Map.of("sku5", true),
                         1,
                         DataCampOfferStatus.SummaryPublicationStatus.AVAILABLE));
         processOffers(messages);
     }


    private void processOffers(List<DatacampMessageOuterClass.DatacampMessage> messages) {
        var messageData = generateMessageData(messages);
        var messageBatch = generateMessageBatch(messageData);
        dataProcessor.process(messageBatch);
    }

    private MessageBatch generateMessageBatch(List<MessageData> messageData) {
        return new MessageBatch(TOPIC_NAME, PARTITION, messageData);
    }

    private List<MessageData> generateMessageData(List<DatacampMessage> messages) {
        MessageMeta meta = new MessageMeta("test".getBytes(), 0, 0, 0, "::1", CompressionCodec.RAW,
                Collections.emptyMap());
        AtomicInteger offeset = new AtomicInteger();
        return messages.stream()
                .map( m -> new MessageData(m.toByteArray(), offeset.getAndIncrement(), meta))
                .collect(Collectors.toList());
    }

    private static DatacampMessage createMessage(int partnerId,
                                                 Map<String, Boolean> shopSku,
                                                 int marketsku,
                                                 DataCampOfferStatus.SummaryPublicationStatus status) {

        var offers = shopSku.entrySet().stream()
                .map(sku -> createOffer(partnerId, sku.getKey(), marketsku, sku.getValue(), status))
                .collect(Collectors.toList());
        var offersBatch =  DataCampUnitedOffer.UnitedOffersBatch.newBuilder().addAllOffer(offers).build();

        return DatacampMessage.newBuilder()
                .addUnitedOffers(offersBatch)
                .build();
    }

    private static DataCampUnitedOffer.UnitedOffer createOffer(int partnerId,
                                                   String shopSku,
                                                   int marketSku,
                                                   Boolean isArchive,
                                                   DataCampOfferStatus.SummaryPublicationStatus offerStatus) {
        var serviceOffer = DataCampOffer.Offer.newBuilder()
                .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                        .setOfferId(shopSku)
                        .setShopId(partnerId)
                        .build())
                .setStatus(DataCampOfferStatus.OfferStatus.newBuilder()
                        .setPublishByPartner(offerStatus));
        if (isArchive != null) {
            var content = DataCampOfferContent.OfferContent
                    .newBuilder()
                    .setPartner(DataCampOfferContent.PartnerContent.newBuilder()
                            .setOriginalTerms(DataCampOfferContent.OriginalTerms.newBuilder()
                                    .setSupplyPlan(DataCampOfferContent.SupplyPlan.newBuilder()
                                            .setValue(isArchive ?
                                                    DataCampOfferContent.SupplyPlan.Variation.ARCHIVE :
                                                    DataCampOfferContent.SupplyPlan.Variation.WILL_SUPPLY)
                                            .build())
                                    .build())
                            .build())
                    .build();
            serviceOffer.setContent(content);
        }
        return DataCampUnitedOffer.UnitedOffer.newBuilder()
                .setBasic(DataCampOffer.Offer.newBuilder()
                                .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                                        .setOfferId(shopSku)
                                        .setShopId(partnerId)
                                        .build())
                                .setContent(DataCampOfferContent.OfferContent.newBuilder()
                                    .setBinding(DataCampOfferMapping.ContentBinding.newBuilder()
                                        .setApproved(DataCampOfferMapping.Mapping.newBuilder()
                                        .setMarketSkuId(marketSku)
                                        .build())
                                    .build())
                                .build()))
                .putService(partnerId,  serviceOffer.build())
                .build();
    }
}
