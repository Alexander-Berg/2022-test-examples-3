package ru.yandex.market.partner.content.common.db.dao;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import org.junit.Test;

import ru.yandex.market.partner.content.common.BaseDbCommonTest;
import ru.yandex.market.partner.content.common.db.jooq.enums.GcSkuTicketStatus;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcSkuTicket;
import ru.yandex.market.partner.content.common.entity.ExtMonitoringColor;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.partner.content.common.entity.ExtMonitoringColor.Constants.ORANGE_AMOUNT_DAYS;
import static ru.yandex.market.partner.content.common.entity.ExtMonitoringColor.Constants.RED_AMOUNT_DAYS;
import static ru.yandex.market.partner.content.common.entity.ExtMonitoringColor.Constants.YELLOW_AMOUNT_HOURS;
import static ru.yandex.market.partner.content.common.entity.ExtMonitoringColor.RED;

public class GcSkuTicketDaoTest extends BaseDbCommonTest {

    @Test
    public void newOffersMonitoring() {
        createSource(1, 1);

        final GcSkuTicket redTicket = createTicket(Instant.now().minus(Duration.ofDays(RED_AMOUNT_DAYS + 3)), 1, 1);
        final GcSkuTicket redTicket2 = createTicket(Instant.now().minus(Duration.ofDays(RED_AMOUNT_DAYS + 1)), 1, 1);
        final GcSkuTicket orangeTicket =
                createTicket(Instant.now().minus(Duration.ofHours(ORANGE_AMOUNT_DAYS * 24 + 1)), 1, 1);
        final GcSkuTicket yellowTicket = createTicket(Instant.now().minus(Duration.ofHours(YELLOW_AMOUNT_HOURS + 1)),
                1, 1);
        final GcSkuTicket ignoredTicket = createTicket(Instant.now().minus(Duration.ofHours(YELLOW_AMOUNT_HOURS - 1)),
                1, 1);

        gcSkuTicketDao.insert(redTicket, redTicket2, orangeTicket, yellowTicket, ignoredTicket);

        Map<ExtMonitoringColor, Integer> ticketsByColor =
                gcSkuTicketDao.notProcessedTicketsByColor();
        assertThat(ticketsByColor.values()).allMatch(i -> i > 0);
        assertThat(ticketsByColor.get(RED)).isEqualTo(2);
        assertThat(ticketsByColor.get(ExtMonitoringColor.ORANGE)).isEqualTo(1);
        assertThat(ticketsByColor.get(ExtMonitoringColor.YELLOW)).isEqualTo(1);
    }

    @Test
    public void newOffersPartnerMonitoring() {
        createSource(1, 1);
        createSource(2, 2);

        final GcSkuTicket redTicket = createTicket(Instant.now().minus(Duration.ofDays(RED_AMOUNT_DAYS + 3)), 1, 1);
        final GcSkuTicket redTicket2 = createTicket(Instant.now().minus(Duration.ofDays(RED_AMOUNT_DAYS + 1)), 2, 2);
        final GcSkuTicket orangeTicket =
                createTicket(Instant.now().minus(Duration.ofHours(ORANGE_AMOUNT_DAYS * 24 + 1)), 1, 1);
        final GcSkuTicket orangeTicket2 =
                createTicket(Instant.now().minus(Duration.ofHours(ORANGE_AMOUNT_DAYS * 24 + 1)), 1, 1);
        final GcSkuTicket yellowTicket = createTicket(Instant.now().minus(Duration.ofHours(YELLOW_AMOUNT_HOURS + 1)),
                2, 2);
        final GcSkuTicket ignoredTicket = createTicket(Instant.now().minus(Duration.ofHours(YELLOW_AMOUNT_HOURS - 1)),
                1, 1);

        gcSkuTicketDao.insert(redTicket, redTicket2, orangeTicket, orangeTicket2, yellowTicket, ignoredTicket);

        Map<ExtMonitoringColor, Integer> partnersByColor = gcSkuTicketDao.notProcessedPartnersByColor();
        assertThat(partnersByColor.values()).allMatch(i -> i > 0);
        assertThat(partnersByColor.get(RED)).isEqualTo(2);
        assertThat(partnersByColor.get(ExtMonitoringColor.ORANGE)).isEqualTo(1);
        assertThat(partnersByColor.get(ExtMonitoringColor.YELLOW)).isEqualTo(1);
    }

    private GcSkuTicket createTicket(Instant createdTs, Integer sourceId, Integer partnerShopId) {
        Timestamp ts = Timestamp.from(createdTs);
        GcSkuTicket ticket = new GcSkuTicket();
        ticket.setCreateDate(ts);
        ticket.setUpdateDate(ts);
        ticket.setStatus(GcSkuTicketStatus.NEW);
        ticket.setSourceId(sourceId);
        ticket.setCategoryId(1L);
        ticket.setPartnerShopId(partnerShopId);
        return ticket;
    }
}
