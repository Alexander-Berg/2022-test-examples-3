package ru.yandex.market.logistics.iris.service.datacamp.synchronization;

import java.util.Collections;
import java.util.Map;

import javax.annotation.Nonnull;

import Market.DataCamp.API.DatacampMessageOuterClass.DatacampMessage;
import Market.DataCamp.DataCampOffer.Offer;
import Market.DataCamp.DataCampOfferContent.MarketMasterData;
import Market.DataCamp.DataCampOfferContent.OfferContent;
import Market.DataCamp.DataCampOfferContent.OriginalSpecification;
import Market.DataCamp.DataCampOfferContent.PartnerContent;
import Market.DataCamp.DataCampOfferContent.ProcessedSpecification;
import Market.DataCamp.DataCampOfferIdentifiers.OfferIdentifiers;
import Market.DataCamp.DataCampOfferMeta.StringListValue;
import Market.DataCamp.DataCampOfferMeta.Ui32Value;
import Market.DataCamp.DataCampOfferMeta.UpdateMeta;
import Market.DataCamp.DataCampUnitedOffer;
import Market.DataCamp.DataCampUnitedOffer.UnitedOffersBatch;
import Market.DataCamp.MappingToOffersOuterClass.MappingToOffers;
import Market.DataCamp.MarketSkuMboContentOuterClass.MarketSkuMboContent;
import Market.DataCamp.MarketSkuOuterClass.MarketSku;
import Market.DataCamp.MarketSkuOuterClass.MarketSkuBatch;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.google.protobuf.Timestamp;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageBatch;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageData;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageMeta;
import ru.yandex.market.logistics.iris.configuration.AbstractContextualTest;
import ru.yandex.market.logistics.iris.core.index.ImmutableReferenceIndex;
import ru.yandex.market.logistics.iris.entity.EmbeddableItemNaturalKey;
import ru.yandex.market.logistics.iris.service.index.ReferenceIndexMergeService;
import ru.yandex.market.logistics.iris.service.mdm.synchronization.item.DataCampItemSyncService;
import ru.yandex.market.mbo.export.ExportReportModels.ExportReportModel;
import ru.yandex.market.mbo.export.ExportReportModels.LocalizedString;
import ru.yandex.market.mbo.export.ExportReportModels.ParameterValue;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.times;

public class DataCampItemSyncServiceTest extends AbstractContextualTest {

    @Autowired
    private DataCampItemSyncService dataCampItemSyncService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @SpyBean
    private ReferenceIndexMergeService referenceIndexMergeService;

    @Captor
    private ArgumentCaptor<Map<EmbeddableItemNaturalKey, ImmutableReferenceIndex>> captor;

    /**
     * Сценарий #1:
     * <p>
     * Подается батч c null телом.
     * <p>
     * Ожидается пустая очередь QueueTasks в БД.
     */
    @Test
    @ExpectedDatabase(value = "classpath:fixtures/expected/datacamp_item_sync_service/1.xml", assertionMode =
            NON_STRICT_UNORDERED)
    public void shouldNotCreateAnyReferenceIndexMergerQueueTask() {
        transactionTemplate.execute(tx -> {
            dataCampItemSyncService.process(createEmptyMessageBatch());
            return null;
        });
    }

    /**
     * Сценарий #2:
     * <p>
     * Подается батч  c ItemBatch в котором находятся Offer и MarketSku.
     * <p>
     * Ожидается создание задачи на сохранение четырёх Item-ов в БД,
     * 1 для Offer, 3 для MarketSku (по количеству идентификаторов).
     */
    @Test
    public void shouldCreateReferenceIndexMergerQueueTaskWithTwoItems() {
        transactionTemplate.execute(tx -> {
            dataCampItemSyncService.process(createMessageBatchWithItemBatch());
            return null;
        });

        Mockito.verify(referenceIndexMergeService, times(2)).mergeAsync(captor.capture());

        var allValues = captor.getAllValues();
        assertions().assertThat(allValues).hasSize(2);

        assertSoftly(assertions -> assertions.assertThat(allValues.get(0).size()).isEqualTo(3));
        assertSoftly(assertions -> assertions.assertThat(allValues.get(1).size()).isEqualTo(2));
    }

    private MessageBatch createMessageBatchWithItemBatch() {
        return new MessageBatch(
                "topic",
                1,
                Collections.singletonList(new MessageData(createRawItemBatch(), 0, createMeta()))
        );
    }

    private UnitedOffersBatch.Builder createOffer() {
        return UnitedOffersBatch.newBuilder()
                .addOffer(DataCampUnitedOffer.UnitedOffer.newBuilder()
                        .setBasic(Offer.newBuilder()
                                .setContent(OfferContent.newBuilder()
                                        .setPartner(PartnerContent.newBuilder()
                                                .setOriginal(OriginalSpecification.newBuilder()
                                                        .setBarcode(StringListValue.newBuilder()
                                                                .addValue("barcode1")
                                                                .setMeta(UpdateMeta.newBuilder()
                                                                        .setTimestamp(Timestamp.newBuilder()
                                                                                .setSeconds(1623772841)))))
                                                .setActual(ProcessedSpecification.newBuilder()
                                                        .setBarcode(StringListValue.newBuilder()
                                                                .addValue("barcode2")))
                                        )
                                        .setMasterData(MarketMasterData.newBuilder()
                                                .setBoxCount(Ui32Value.newBuilder()
                                                        .setValue(2))
                                                .setGtins(StringListValue.newBuilder()
                                                        .addValue("barcode3"))
                                        )
                                )
                                .build())
                        .putService(1, Offer.newBuilder()
                                .setIdentifiers(OfferIdentifiers.newBuilder()
                                        .setShopId(1)
                                        .setOfferId("shopSku_1"))
                                .build())
                        .putService(2, Offer.newBuilder()
                                .setIdentifiers(OfferIdentifiers.newBuilder()
                                        .setShopId(2)
                                        .setOfferId("shopSku_2"))
                                .build())
                );
    }

    private MarketSkuBatch.Builder createMarketSku() {
        return MarketSkuBatch.newBuilder()
                .addMsku(MarketSku.newBuilder()
                        .setMboContent(MarketSkuMboContent.newBuilder()
                                .setMsku(ExportReportModel.newBuilder()
                                        .addTitles(LocalizedString.newBuilder()
                                                .setValue("NAME")
                                                .setIsoCode("ru"))
                                        .setModifiedTs(1623772841000L)
                                        .addParameterValues(getStrParam(18598830, "SN MASK"))
                                        .addParameterValues(getStrParam(18598650, "IMEI MASK"))
                                        .addParameterValues(getBoolParam(18598790, true, "mdm_serial_number_control"))
                                        .addParameterValues(getBoolParam(18598310, false, "mdm_imei_control"))
                                        .addParameterValues(getStrParam(14202862, "Barcode1"))
                                        .addParameterValues(getStrParam(14202862, "Barcode2"))
                                        .addParameterValues(getStrParam(7351757, "VendorCode1"))
                                        .addParameterValues(getStrParam(7351757, "VendorCode2"))
                                        .addParameterValues(getStrParam(7351757, "VendorCode3"))
                                        .addParameterValues(getBoolParam(16618894, true, "cargoType80"))
                                        .addParameterValues(getBoolParam(16618956, true, "cargoType200"))
                                        .addParameterValues(getBoolParam(16402543, true, "cargoType400"))
                                        .addParameterValues(getBoolParam(16619313, false, "cargoType520"))
                                        .addParameterValues(getNumParam(15343057, "5", "packageNumInSpike"))
                                ))
                        .setMappingToOffers(MappingToOffers.newBuilder()
                                .addOffers(OfferIdentifiers.newBuilder().setOfferId("msku_1").setShopId(1))
                                .addOffers(OfferIdentifiers.newBuilder().setOfferId("msku_2").setShopId(2))
                                .addOffers(OfferIdentifiers.newBuilder().setOfferId("msku_3").setShopId(3))
                        )
                );
    }

    @Nonnull
    private ParameterValue.Builder getStrParam(int paramId, String value) {
        return ParameterValue.newBuilder()
                .addStrValue(LocalizedString.newBuilder()
                        .setValue(value)
                        .setIsoCode("ru"))
                .setModificationDate(1623833300000L)
                .setParamId(paramId);
    }

    @Nonnull
    private ParameterValue.Builder getNumParam(int paramId, String value, String xslName) {
        return ParameterValue.newBuilder()
                .setNumericValue(value)
                .setModificationDate(1623833300000L)
                .setParamId(paramId)
                .setXslName(xslName);
    }

    @Nonnull
    private ParameterValue.Builder getBoolParam(int paramId, boolean value, String xslName) {
        return ParameterValue.newBuilder()
                .setBoolValue(value)
                .setModificationDate(1623833300000L)
                .setParamId(paramId)
                .setXslName(xslName);
    }

    private MessageMeta createMeta() {
        return new MessageMeta(null, 1, 0, 0, null, null, null);
    }


    private byte[] createRawItemBatch() {
        return DatacampMessage.newBuilder()
                .addUnitedOffers(createOffer())
                .setMarketSkus(createMarketSku())
                .build()
                .toByteArray();
    }


    private MessageBatch createEmptyMessageBatch() {
        return new MessageBatch("topic", 1, null);
    }
}
