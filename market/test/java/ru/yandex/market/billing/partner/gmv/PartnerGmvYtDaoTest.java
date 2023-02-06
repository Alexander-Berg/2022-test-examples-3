package ru.yandex.market.billing.partner.gmv;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Value;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.common.GUID;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.tables.YTableEntryType;
import ru.yandex.inside.yt.kosher.tables.YtTables;
import ru.yandex.inside.yt.kosher.transactions.Transaction;
import ru.yandex.inside.yt.kosher.transactions.YtTransactions;
import ru.yandex.market.FunctionalTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static ru.yandex.market.yt.matchers.JsonNodeMatchers.hasIntValue;
import static ru.yandex.market.yt.matchers.JsonNodeMatchers.hasStrValue;

@ExtendWith(MockitoExtension.class)
public class PartnerGmvYtDaoTest extends FunctionalTest {
    private static final LocalDate TEST_DATE = LocalDate.of(2022, 2, 17);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final GUID TR_GUID = new GUID(33, 55);

    @Mock
    private Yt yt;

    @Mock
    private Cypress cypress;

    @Value("${market.billing.partner.gmv.yt.path}")
    private String ytPath;

    @Mock
    private YtTables ytTables;

    @Mock
    private YtTransactions ytTransactions;

    @Mock
    private Transaction transaction;

    @Captor
    private ArgumentCaptor<Iterator<JsonNode>> nodesCaptor;

    private PartnerGmvYtDao partnerGmvYtDao;

    @BeforeEach
    void beforeEach() {
        when(yt.cypress())
                .thenReturn(cypress);

        when(yt.tables())
                .thenReturn(ytTables);

        when(yt.transactions())
                .thenReturn(ytTransactions);

        when(ytTransactions.startAndGet(any()))
                .thenReturn(transaction);

        when(ytTransactions.startAndGet(any(), anyBoolean(), any()))
                .thenReturn(transaction);

        when(transaction.getId())
                .thenReturn(TR_GUID);

        partnerGmvYtDao = new PartnerGmvYtDao(yt, ytPath);
    }

    @DisplayName("Экспорт GMV по партнёрам в YT")
    @Test
    void testExport() {
        //готовим данные
        YPath yPath = YPath.simple(ytPath).child(TEST_DATE.format(DATE_TIME_FORMATTER));

        when(cypress.exists(yPath))
                .thenReturn(true);

        // вызов
        partnerGmvYtDao.storeGmvsToYt(TEST_DATE, getGmvs());

        // проверяем
        Mockito.verify(cypress)
                .exists(eq(yPath));
        Mockito.verify(cypress)
                .remove(eq(Optional.of(TR_GUID)), eq(true), eq(yPath));

        Mockito.verify(ytTables)
                .write(
                        eq(Optional.of(TR_GUID)),
                        anyBoolean(),
                        eq(yPath),
                        any(YTableEntryType.class),
                        nodesCaptor.capture()
                );

        List<JsonNode> partnerGmvs = ImmutableList.copyOf(nodesCaptor.getValue());

        assertEquals(2, partnerGmvs.size());

        assertThat(partnerGmvs,
                Matchers.containsInAnyOrder(
                        allOf(
                                hasIntValue("partner_id", 774),
                                hasStrValue("processed_date", "2022-02-17"),
                                hasIntValue("shop_gmv_key_regions_cehac", 100),
                                hasIntValue("shop_gmv_key_regions_other_cats", 200),
                                hasIntValue("shop_gmv_other_regions_cehac", 300),
                                hasIntValue("shop_gmv_other_regions_other_cats", 0),
                                hasIntValue("bids_delivered_gmv", 100)
                        ),
                        allOf(
                                hasIntValue("partner_id", 775),
                                hasStrValue("processed_date", "2022-02-17"),
                                hasIntValue("shop_gmv_key_regions_cehac", 0),
                                hasIntValue("shop_gmv_key_regions_other_cats", 0),
                                hasIntValue("shop_gmv_other_regions_cehac", 0),
                                hasIntValue("shop_gmv_other_regions_other_cats", 400),
                                hasIntValue("bids_delivered_gmv", 400)
                        )
                )
        );
    }


    private List<PartnerGmv> getGmvs() {
        return List.of(
                PartnerGmv.builder()
                        .setPartnerId(774L)
                        .setDate(TEST_DATE)
                        .setShopGmvKeyRegionsCEHAC(100L)
                        .setShopGmvKeyRegionsOtherCats(200L)
                        .setShopGmvOtherRegionsCEHAC(300L)
                        .setShopGmvOtherRegionsOtherCats(0L)
                        .setBidsDeliveredGmv(100L)
                        .build(),
                PartnerGmv.builder()
                        .setPartnerId(775L)
                        .setDate(TEST_DATE)
                        .setShopGmvKeyRegionsCEHAC(0L)
                        .setShopGmvKeyRegionsOtherCats(0L)
                        .setShopGmvOtherRegionsCEHAC(0L)
                        .setShopGmvOtherRegionsOtherCats(400L)
                        .setBidsDeliveredGmv(400L)
                        .build()
        );

    }
}
