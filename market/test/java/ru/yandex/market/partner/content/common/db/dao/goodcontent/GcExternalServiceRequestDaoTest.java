package ru.yandex.market.partner.content.common.db.dao.goodcontent;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.google.protobuf.Message;

import ru.yandex.market.mbo.http.ModelCardApi;
import ru.yandex.market.partner.content.common.DBDcpStateGenerator;
import ru.yandex.market.partner.content.common.db.jooq.enums.GcExternalRequestStatus;
import ru.yandex.market.partner.content.common.db.jooq.enums.GcExternalRequestType;

import static org.assertj.core.api.Assertions.assertThat;

public class GcExternalServiceRequestDaoTest extends DBDcpStateGenerator {
    public static final long CATEGORY_ID = 1234567L;
    public static final String SHOP_SKU_1 = "1";

    @Autowired
    GcExternalServiceRequestDao requestDao;
    @Autowired
    GcSkuTicketDao gcSkuTicketDao;


    @Before
    public void setUp() {
        super.setUp();
    }

    @Test
    public void testWhenThereAreOldRequestsThenCleanThemUp() {
        Long ticketId = generateDBDcpInitialStateNew(state -> {
            state.reInitWithNewIdentifiers(PARTNER_SHOP_ID, SHOP_SKU_1);
        }).getId();

        Message finishedRequest = ModelCardApi.SaveModelsGroupRequest.newBuilder()
                .build();
        Message newRequest = ModelCardApi.SaveModelsGroupRequest.newBuilder()
                .build();
        Long finishedRequestId = requestDao.saveRequest(finishedRequest, GcExternalRequestType.MBO_SAVE_PSKUS,
                GcExternalRequestStatus.FINISHED);
        Long newRequestId = requestDao.saveRequest(newRequest, GcExternalRequestType.MBO_SAVE_PSKUS);
        gcSkuTicketDao.saveRequestToTicket(Collections.singleton(ticketId), newRequestId);
        gcSkuTicketDao.saveRequestToTicket(Collections.singleton(ticketId), finishedRequestId);

        assertThat(requestDao.fetchById(finishedRequestId)).hasSize(1);

        assertThat(gcSkuTicketDao.getTicketRequestIds(ticketId, GcExternalRequestType.MBO_SAVE_PSKUS,
                GcExternalRequestStatus.CREATED)).containsOnly(newRequestId);
        assertThat(gcSkuTicketDao.getTicketRequestIds(ticketId, GcExternalRequestType.MBO_SAVE_PSKUS,
                GcExternalRequestStatus.FINISHED)).containsOnly(finishedRequestId);

        requestDao.removeOutdatedRequests(Collections.singletonList(ticketId), newRequestId, GcExternalRequestType.MBO_SAVE_PSKUS);

        assertThat(gcSkuTicketDao.getTicketRequestIds(ticketId, GcExternalRequestType.MBO_SAVE_PSKUS))
                .containsOnly(newRequestId);
    }
}
