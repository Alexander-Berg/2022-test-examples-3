package ru.yandex.market.partner.content.common;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferIdentifiers;
import Market.DataCamp.DataCampOfferPictures;
import org.apache.commons.lang3.mutable.MutableInt;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.partner.content.common.db.dao.dcp.DcpPartnerPictureDao;
import ru.yandex.market.partner.content.common.db.jooq.Tables;
import ru.yandex.market.partner.content.common.db.jooq.enums.DatacampOfferStatus;
import ru.yandex.market.partner.content.common.db.jooq.enums.GcSkuTicketStatus;
import ru.yandex.market.partner.content.common.db.jooq.enums.GcSkuTicketType;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.DataBucket;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.DatacampOffer;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.DcpPartnerPicture;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcSkuTicket;
import ru.yandex.market.partner.content.common.engine.parameter.ProcessDataBucketData;
import ru.yandex.market.partner.content.common.helpers.DatacampOfferHelper;

import static ru.yandex.market.partner.content.common.db.jooq.tables.GcSkuTicket.GC_SKU_TICKET;

public abstract class DBDcpStateGenerator
        extends BaseDBStateGenerator {

    @Autowired
    protected DcpPartnerPictureDao dcpPartnerPictureDao;

    public static final String PICTURE_URL = "https://pic.png";
    public static final List<String> PICTURES = Collections.singletonList(PICTURE_URL);


    @Before
    public void setUp() {
        super.setUp();
        dataBucketId = createDataBucketId(CATEGORY_ID, SOURCE_ID);
        processDataBucketData = new ProcessDataBucketData(dataBucketId);
    }

    @Deprecated
    // удобнее использовать generateDBDcpInitialStateNew
    public List<GcSkuTicket> generateDBDcpInitialState(int amount) {
        return generateDBDcpInitialState(amount, __ -> {
        });
    }

    public List<GcSkuTicket> generateDBDcpInitialState(int amount,
                                                       Consumer<List<DatacampOffer>> offerSettings) {
        return generateDBDcpInitialState(amount, GcSkuTicketType.DATA_CAMP, offerSettings);
    }

    public List<GcSkuTicket> generateDBDcpInitialState(int amount, GcSkuTicketType ticketType,
                                                       Consumer<List<DatacampOffer>> offerSettings) {
        MutableInt sequence = new MutableInt(0);
        List<String> uniqPSKUNames = generator.objects(String.class, amount)
            .map(randomShopSkuName -> randomShopSkuName + "_" + sequence.getAndIncrement())
            .collect(Collectors.toList());
        Iterator<String> pskuNamesIterator = uniqPSKUNames.iterator();
        int groupId = generator.nextInt();

        List<DatacampOffer> offers = generator.objects(DatacampOffer.class, amount, "id")
            .peek(t -> t.setSourceId(SOURCE_ID))
            .peek(t -> t.setStatus(DatacampOfferStatus.ACTIVATED))
            .peek(t -> t.setGroupId(groupId))
            .peek(t -> t.setBusinessId(PARTNER_SHOP_ID))
            .peek(t -> t.setCreateTime(Timestamp.from(Instant.now())))
            .peek(t -> t.setRequestTs(Timestamp.from(Instant.now())))
            .peek(t -> t.setOfferId(pskuNamesIterator.next()))
            .collect(Collectors.toList());

        offerSettings.accept(offers);

        actualizeFromDatacamp(offers);


        DataBucket dataBucket = dataBucketDao.findById(dataBucketId);
        List<Long> ticketIds = gcSkuTicketDao.saveDatacampTickets(dataBucket, offers, ticketType);
        dsl().update(Tables.GC_SKU_TICKET)
            .set(Tables.GC_SKU_TICKET.VALID, true)
            .execute();
        List<GcSkuTicket> gcSkuTickets = gcSkuTicketDao.fetchById(ticketIds);
        gcSkuTickets.sort(Comparator.comparing(GcSkuTicket::getId));
        return gcSkuTickets;
    }

    protected List<Long> generateDBInitialStateAndGetIds(int amount) {
        return generateDBInitialStateAndGetIds(amount, skuTickets -> {
        });
    }

    protected List<Long> generateDBInitialStateAndGetIds(int amount,
                                                         Consumer<List<PreparedOfferState>> offerStatesConsumer) {
        return generateDBDcpInitialStateNew(amount, offerStatesConsumer).stream()
            .map(GcSkuTicket::getId)
            .collect(Collectors.toList());
    }

    protected Map<Long, GcSkuTicket> generateDBInitialStateAndGetIdsMap(int amount,
                                                                        Consumer<List<PreparedOfferState>> offerStateConsumer) {
        return generateDBDcpInitialStateNew(amount, offerStateConsumer).stream()
            .collect(Collectors.toMap(GcSkuTicket::getId, Function.identity()));
    }

    public GcSkuTicket generateDBDcpInitialStateNew() {
        return generateDBDcpInitialStateNew(1).get(0);
    }

    public List<GcSkuTicket> generateDBDcpInitialStateNew(int amount) {
        return generateDBDcpInitialStateNew(amount, __ -> {
        });
    }

    public GcSkuTicket generateDBDcpInitialStateNew(Consumer<PreparedOfferState> offerStateConsumer) {
        return generateDBDcpInitialStateNew(1, states -> offerStateConsumer.accept(states.get(0))).get(0);
    }

    public List<GcSkuTicket> generateDBDcpInitialStateNew(int amount,
                                                          Consumer<List<PreparedOfferState>> offerStatesConsumer) {
        return generateDBDcpInitialStateNew(amount, dataBucketId, offerStatesConsumer);
    }

    public List<GcSkuTicket> generateDBDcpInitialStateNew(int amount,
                                                          long databucketId,
                                                          Consumer<List<PreparedOfferState>> offerStatesConsumer) {
        MutableInt sequence = new MutableInt(0);
        List<String> uniqPSKUNames = generator.objects(String.class, amount)
            .map(randomShopSkuName -> randomShopSkuName + "_" + sequence.getAndIncrement())
            .collect(Collectors.toList());
        Iterator<String> pskuNamesIterator = uniqPSKUNames.iterator();
        int groupId = generator.nextInt();

        List<PreparedOfferState> offerStates = generator.objects(DatacampOffer.class, amount, "id")
            .peek(t -> t.setSourceId(SOURCE_ID))
            .peek(t -> t.setStatus(DatacampOfferStatus.ACTIVATED))
            .peek(t -> t.setGroupId(groupId))
            .peek(t -> t.setBusinessId(PARTNER_SHOP_ID))
            .peek(t -> t.setCreateTime(Timestamp.from(Instant.now())))
            .peek(t -> t.setRequestTs(Timestamp.from(Instant.now())))
            .peek(t -> t.setOfferId(pskuNamesIterator.next()))
            .map(PreparedOfferState::new)
            .collect(Collectors.toList());

        offerStatesConsumer.accept(offerStates);
        List<DatacampOffer> offers = offerStates.stream()
            .map(PreparedOfferState::buildDatacampOffer).collect(Collectors.toList());

        DataBucket dataBucket = dataBucketDao.findById(databucketId);
        List<Long> ticketIds = gcSkuTicketDao.saveDatacampTickets(dataBucket, offers, GcSkuTicketType.DATA_CAMP);
        dsl().update(Tables.GC_SKU_TICKET)
            .set(Tables.GC_SKU_TICKET.VALID, true)
            .execute();
        List<GcSkuTicket> gcSkuTickets = gcSkuTicketDao.fetchById(ticketIds);
        gcSkuTickets.sort(Comparator.comparing(GcSkuTicket::getId));
        return gcSkuTickets;
    }

    private void actualizeFromDatacamp(List<DatacampOffer> offers) {
        for (DatacampOffer offer : offers) {
            DataCampOfferIdentifiers.OfferIdentifiers identifiers = offer.getData().getIdentifiers();
            if (identifiers.hasOfferId()) {
                offer.setOfferId(identifiers.getOfferId());
            }
            if (identifiers.hasShopId()) {
                offer.setBusinessId(identifiers.getBusinessId());
            }
            if (offer.getData().getContent().getPartner().getOriginal().hasGroupId()) {
                offer.setGroupId(offer.getData().getContent().getPartner().getOriginal().getGroupId().getValue());
            } else {
                offer.setGroupId(null);
            }
        }
    }

    protected List<GcSkuTicket> generateTickets(
        Integer ticketsCount,
        Consumer<DatacampOffer> offersSettings,
        GcSkuTicketStatus status
    ) {
        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialState(
            ticketsCount,
            offers -> offers.forEach(offersSettings)
        );
        gcSkuTickets.forEach(gcSkuTicket -> gcSkuTicket.setStatus(status));
        updateTicketsStatus(status);
        return gcSkuTickets;
    }

    protected void updateTicketsStatus(GcSkuTicketStatus status) {
        dsl().update(GC_SKU_TICKET)
            .set(GC_SKU_TICKET.STATUS, status)
            .execute();
    }

    /**
     * Method simulates successful upload run result of ImageUploader#saveResults(java.util.List)
     *
     * @param gcSkuTicketId - ticket id (actually the id of ticket in which specific url appeared for the first time)
     * @param url           - idx img url
     */
    protected void insertUploadedPictures(Long gcSkuTicketId, String url) {
        dcpPartnerPictureDao.saveData(Collections.singletonList(
            new DcpPartnerPicture(
                null,
                url,
                null,
                ModelStorage.OperationStatus.newBuilder()
                    .setStatus(ModelStorage.OperationStatusType.OK)
                    .setType(ModelStorage.OperationType.CREATE)
                    .build(),
                ModelStorage.Picture.newBuilder().setUrl(url).build(),
                gcSkuTicketId,
                Timestamp.from(Instant.now()),
                true,
                false,
                null
            )
        ));
    }

    protected void setPictureToOffer(DataCampOffer.Offer.Builder offer, String url) {
        DataCampOfferPictures.PartnerPictures.Builder partnerBuilder = offer.getPicturesBuilder().getPartnerBuilder();
        partnerBuilder.getOriginalBuilder().addSource(
                DataCampOfferPictures.SourcePicture.newBuilder()
                        .setUrl(url)
                        .build()
        );
        partnerBuilder.putActual(url, DataCampOfferPictures.MarketPicture.newBuilder()
                .setOriginal(
                        DataCampOfferPictures.MarketPicture.Picture.newBuilder()
                                .setUrl(url)
                                .build()
                )
                .build());
    }

    public void insertUploadedPictures(GcSkuTicket gcSkuTicket) {
        DatacampOfferHelper.collectPicturesFromTicket(gcSkuTicket, ((url, isMain) -> {
            // these url has prefix IDX_, since the test offer builder puts it for idx urls
            insertUploadedPictures(gcSkuTicket.getId(), url);
        }));
    }

    protected void setCategoryForTicketsAndDatabucket(List<GcSkuTicket> gcSkuTickets, long categoryId) {
        gcSkuTickets.forEach(gcSkuTicket -> gcSkuTicket.setCategoryId(categoryId));
        gcSkuTicketDao.update(gcSkuTickets);
        DataBucket dataBucket = dataBucketDao.fetchOneById(gcSkuTickets.get(0).getDataBucketId());
        dataBucket.setCategoryId(categoryId);
        dataBucketDao.update(dataBucket);
    }
}
