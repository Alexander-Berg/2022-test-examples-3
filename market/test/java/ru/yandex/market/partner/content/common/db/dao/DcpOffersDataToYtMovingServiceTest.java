package ru.yandex.market.partner.content.common.db.dao;

import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferContent;
import Market.DataCamp.DataCampOfferMarketContent;
import com.google.protobuf.InvalidProtocolBufferException;
import org.apache.commons.io.FileUtils;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.bolts.collection.IterableF;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.YtUtils;
import ru.yandex.inside.yt.kosher.operations.YtOperations;
import ru.yandex.inside.yt.kosher.tables.YTableEntryType;
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;
import ru.yandex.inside.yt.kosher.tables.YtTables;
import ru.yandex.market.partner.content.common.BaseDbCommonTest;
import ru.yandex.market.partner.content.common.db.jooq.enums.GcSkuTicketStatus;
import ru.yandex.market.partner.content.common.db.jooq.enums.GcSkuTicketType;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static ru.yandex.market.partner.content.common.db.dao.DcpOffersDataToYtMovingService.OLDNESS_THRESHOLD;
import static ru.yandex.market.partner.content.common.db.jooq.Tables.DATA_BUCKET;
import static ru.yandex.market.partner.content.common.db.jooq.Tables.GC_SKU_TICKET;
import static ru.yandex.market.partner.content.common.db.jooq.Tables.PARTNER_SKU;
import static ru.yandex.market.partner.content.common.db.jooq.Tables.SOURCE;

@Ignore
public class DcpOffersDataToYtMovingServiceTest extends BaseDbCommonTest {

    private static final int SOURCE_ID = 1255;
    private static final long PARTNER_SKU_ID = 55L;
    private static final String SKU = "super-343";
    private static final String OLD_DCP_OFFERS_YT_PATH =
        "//home/market/development/ir/gutgin/gc_sku_ticket_dcp_offers_data/tests";

    private static final long GC_SKU_TICKET_ID_1 = 101L;
    private static final long GC_SKU_TICKET_ID_2 = 102L;
    private static final long GC_SKU_TICKET_ID_3 = 103L;
    private static final long GC_SKU_TICKET_ID_4 = 104L;
    private static final Timestamp OLDNESS_THRESHOLD_TS = Timestamp.valueOf(LocalDateTime.now().minusWeeks(OLDNESS_THRESHOLD));


    private Yt ytMock;
    private YtTables ytTablesMock;
    private DcpOffersDataToYtMovingService serviceWithYtMock;

    @Before
    public void setUp() {
        ytTablesMock = Mockito.mock(YtTables.class);
        ytMock = Mockito.mock(Yt.class);
        YtOperations ytOperationMock = Mockito.mock(YtOperations.class);

        Mockito.when(ytMock.cypress()).thenReturn(Mockito.mock(Cypress.class));
        Mockito.when(ytMock.tables()).thenReturn(ytTablesMock);
        Mockito.when(ytMock.operations()).thenReturn(ytOperationMock);

        serviceWithYtMock = new DcpOffersDataToYtMovingService(
            configuration,
            slaveConfiguration,
            ytMock,
            OLD_DCP_OFFERS_YT_PATH
        );
    }

    @Test
    public void testMovesOnlyOldOffers() {
        long dataBucketId = prepareData();
        Timestamp toClean =
                Timestamp.valueOf(OLDNESS_THRESHOLD_TS.toLocalDateTime().minusDays(1));
        Timestamp toLeave =
                Timestamp.valueOf(OLDNESS_THRESHOLD_TS.toLocalDateTime().plusDays(1));
        insertSku(GC_SKU_TICKET_ID_1, dataBucketId, toClean);
        insertSku(GC_SKU_TICKET_ID_2, dataBucketId, toLeave);
        serviceWithYtMock.moveOldDcpData();
        Assertions.assertThat(getOffer(GC_SKU_TICKET_ID_1)).isNull();
        Assertions.assertThat(getOffer(GC_SKU_TICKET_ID_2)).isNotNull();
    }

    @Test
    public void testLeavesOldOfferWhenYtThrows() {
        long dataBucketId = prepareData();
        Timestamp toClean =
                Timestamp.valueOf(OLDNESS_THRESHOLD_TS.toLocalDateTime().minusDays(1));
        Timestamp toLeave =
                Timestamp.valueOf(OLDNESS_THRESHOLD_TS.toLocalDateTime().plusDays(1));
        insertSku(GC_SKU_TICKET_ID_1, dataBucketId, toClean);
        insertSku(GC_SKU_TICKET_ID_2, dataBucketId, toLeave);
        Mockito.doThrow(new RuntimeException("can't write yt data"))
            .when(ytTablesMock)
            .write(Mockito.any(YPath.class), Mockito.any(YTableEntryType.class), Mockito.any(IterableF.class));
        Assertions.assertThatThrownBy(() -> serviceWithYtMock.moveOldDcpData()).isInstanceOf(RuntimeException.class);
        Assertions.assertThat(getOffer(GC_SKU_TICKET_ID_1)).isNotNull();
        Assertions.assertThat(getOffer(GC_SKU_TICKET_ID_2)).isNotNull();
    }

    private Yt createDevYtClient() {
        String userHome = System.getProperty("user.home");
        File ytTokenFile = new File(userHome + "/.yt/token");
        if (!ytTokenFile.exists()) {
            throw new RuntimeException("Yt token not found, check file '" + ytTokenFile + "' is present");
        }
        try {
            String token = FileUtils.readFileToString(ytTokenFile, StandardCharsets.UTF_8.toString()).trim();
            return YtUtils.http("hahn.yt.yandex.net", token);
        } catch (IOException ioe) {
            throw new RuntimeException("Can't read yt token from file " + ytTokenFile, ioe);
        }
    }

    @Test
    @Ignore("For manual run only: the test is too long and needs yt token")
    public void ytDevTest() {
        Yt yt = createDevYtClient();
        DcpOffersDataToYtMovingService service = new DcpOffersDataToYtMovingService(
            configuration,
            slaveConfiguration,
            createDevYtClient(),
            OLD_DCP_OFFERS_YT_PATH
        );
        Timestamp toLeave =
                Timestamp.valueOf(OLDNESS_THRESHOLD_TS.toLocalDateTime().plusDays(1));
        Timestamp toCleanDay1 =
                Timestamp.valueOf(OLDNESS_THRESHOLD_TS.toLocalDateTime().minusDays(1));
        Timestamp toCleanDay2 =
                Timestamp.valueOf(OLDNESS_THRESHOLD_TS.toLocalDateTime().minusDays(5));
        long dataBucketId = prepareData();
        insertSku(GC_SKU_TICKET_ID_1, dataBucketId, toLeave);
        insertSku(GC_SKU_TICKET_ID_2, dataBucketId, toCleanDay1);
        insertSku(GC_SKU_TICKET_ID_3, dataBucketId, toCleanDay2);
        service.moveOldDcpData();

        String day1TableName =
            toCleanDay1.toLocalDateTime().format(DcpOffersDataToYtMovingService.TABLE_NAME_DATE_FORMAT);
        String day2TableName =
            toCleanDay2.toLocalDateTime().format(DcpOffersDataToYtMovingService.TABLE_NAME_DATE_FORMAT);
        YPath day1TablePath = YPath.simple(OLD_DCP_OFFERS_YT_PATH + "/" + day1TableName);
        YPath day2TablePath = YPath.simple(OLD_DCP_OFFERS_YT_PATH + "/" + day2TableName);
        checkTableExists(yt, day1TablePath);
        checkTableExists(yt, day2TablePath);
        Map<Long, DataCampOffer.Offer> data1 = loadTicketData(yt, day1TablePath);
        Map<Long, DataCampOffer.Offer> data2 = loadTicketData(yt, day2TablePath);
        Assertions.assertThat(data1.keySet()).containsExactlyInAnyOrder(GC_SKU_TICKET_ID_2);
        Assertions.assertThat(data2.keySet()).containsExactlyInAnyOrder(GC_SKU_TICKET_ID_3);

        insertSku(GC_SKU_TICKET_ID_4, dataBucketId, toCleanDay1);
        service.moveOldDcpData();
        data1 = loadTicketData(yt, day1TablePath);
        data2 = loadTicketData(yt, day2TablePath);
        Assertions.assertThat(data1.keySet()).containsExactlyInAnyOrder(GC_SKU_TICKET_ID_2, GC_SKU_TICKET_ID_4);
        Assertions.assertThat(data2.keySet()).containsExactlyInAnyOrder(GC_SKU_TICKET_ID_3);
    }

    private Map<Long, DataCampOffer.Offer> loadTicketData(Yt yt, YPath yPath) {
        Map<Long, DataCampOffer.Offer> result = new HashMap<>();
        yt.tables().read(yPath, YTableEntryTypes.YSON, (row) -> {
            long ticketId = row.getLong("gc_sku_ticket_id");
            DataCampOffer.Offer offer = null;
            try {
                offer = DataCampOffer.Offer.parseFrom(row.getBytes("datacamp_offer"));
            } catch (InvalidProtocolBufferException e) {
                throw new RuntimeException(e);
            }
            result.put(ticketId, offer);
        });
        return result;
    }

    private void checkTableExists(Yt yt, YPath path) {
        Assertions.assertThat(yt.cypress().exists(path))
            .as("Check target table exists " + path)
            .isTrue();
    }

    private DataCampOffer.Offer getOffer(long gcSkuId) {
        return dsl().select(GC_SKU_TICKET.DATACAMP_OFFER)
            .from(GC_SKU_TICKET)
            .where(GC_SKU_TICKET.ID.eq(gcSkuId))
            .fetchOne()
            .into(DataCampOffer.Offer.class);
    }

    private long prepareData() {
        dsl().insertInto(SOURCE)
            .set(SOURCE.SOURCE_ID, SOURCE_ID)
            .set(SOURCE.SOURCE_NAME, "testSource")
            .execute();

        Long dataBucketId = dsl().insertInto(DATA_BUCKET)
            .set(DATA_BUCKET.CATEGORY_ID, 1L)
            .set(DATA_BUCKET.SOURCE_ID, SOURCE_ID)
            .set(DATA_BUCKET.PROCESS_CREATE_TIME, Timestamp.from(Instant.now()))
            .returning(DATA_BUCKET.ID)
            .fetchOne()
            .get(DATA_BUCKET.ID);

        dsl().insertInto(PARTNER_SKU)
            .set(PARTNER_SKU.ID, PARTNER_SKU_ID)
            .set(PARTNER_SKU.SOURCE_ID, SOURCE_ID)
            .set(PARTNER_SKU.SKU, "")
            .execute();
        return dataBucketId;
    }

    private void insertSku(long gcSkuTicketId, long dataBucketId, java.sql.Timestamp updateTime) {
        configuration.dsl()
            .insertInto(GC_SKU_TICKET)
            .set(GC_SKU_TICKET.ID, gcSkuTicketId)
            .set(GC_SKU_TICKET.SOURCE_ID, SOURCE_ID)
            .set(GC_SKU_TICKET.PARTNER_SHOP_ID, 1232)
            .set(GC_SKU_TICKET.PARTNER_SKU_ID, PARTNER_SKU_ID)
            .set(GC_SKU_TICKET.CATEGORY_ID, 1L)
            .set(GC_SKU_TICKET.SHOP_SKU, SKU)
            .set(GC_SKU_TICKET.NAME, "Test")
            .set(GC_SKU_TICKET.DATA_BUCKET_ID, dataBucketId)
            .set(GC_SKU_TICKET.CREATE_DATE, updateTime)
            .set(GC_SKU_TICKET.UPDATE_DATE, updateTime)
            .set(GC_SKU_TICKET.STATUS, GcSkuTicketStatus.SUCCESS)
            .set(GC_SKU_TICKET.TYPE, GcSkuTicketType.DATA_CAMP)
            .set(GC_SKU_TICKET.DATACAMP_OFFER, DataCampOffer.Offer.newBuilder()
                .setContent(DataCampOfferContent.OfferContent.newBuilder()
                    .setPartner(DataCampOfferContent.PartnerContent.newBuilder()
                        .setMarketSpecificContent(DataCampOfferMarketContent.MarketSpecificContent.newBuilder()
                            .setPictures(DataCampOfferMarketContent.MarketSpecificPictures.newBuilder()
                                .addPictures(DataCampOfferMarketContent.MarketSpecificPicture.newBuilder()
                                    .setUrl("test.ri")
                                    .build())
                                .build())
                            .build())
                        .build())
                    .build())
                .build())
            .execute();
    }

}
