package ru.yandex.market.gutgin.tms.pipeline.good.taskaction.datacamp_pipeline;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.market.gutgin.tms.engine.task.ProcessTaskResult;
import ru.yandex.market.gutgin.tms.service.goodcontent.MappingActualizeService;
import ru.yandex.market.ir.autogeneration.common.helpers.MboMappingsServiceHelper;
import ru.yandex.market.ir.autogeneration_api.http.service.MboMappingsServiceMock;
import ru.yandex.market.mboc.http.SupplierOffer;
import ru.yandex.market.partner.content.common.DBDcpStateGenerator;
import ru.yandex.market.partner.content.common.PreparedOfferState;
import ru.yandex.market.partner.content.common.db.dao.dcp.FakeDatacampOfferDao;
import ru.yandex.market.partner.content.common.db.jooq.enums.GcSkuTicketStatus;
import ru.yandex.market.partner.content.common.db.jooq.enums.MappingConfidenceType;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcSkuTicket;
import ru.yandex.market.partner.content.common.engine.parameter.ProcessDataBucketData;
import ru.yandex.market.partner.content.common.entity.goodcontent.SkuTicket;

public class DcpMappingActualizeTaskActionTest extends DBDcpStateGenerator {

    private static final String[] OFFER_IDS = {"OFFER1", "OFFER2", "OFFER3"};
    private static final long[] MSKU_IDS = {100500, 100501, 100502};

    private DcpMappingActualizeTaskAction dcpMappingActualizeTaskAction;
    private MboMappingsServiceMock mboMappingsServiceMock;

    @Before
    public void setUp() {
        super.setUp();
        mboMappingsServiceMock = new MboMappingsServiceMock();

        FakeDatacampOfferDao fakeDatacampOfferDao = Mockito.mock(FakeDatacampOfferDao.class);
        Mockito.when(fakeDatacampOfferDao.getFake(Mockito.anyList())).thenReturn(Collections.emptyList());
        Mockito.when(fakeDatacampOfferDao.getNonFake(Mockito.anyList()))
               .thenAnswer(invocation -> invocation.getArgument(0));

        MboMappingsServiceHelper mboMappingsServiceHelper = new MboMappingsServiceHelper(mboMappingsServiceMock);
        MappingActualizeService mappingActualizeService = new MappingActualizeService(
            gcSkuTicketDao,
            mboMappingsServiceHelper,
            fakeDatacampOfferDao
        );
        dcpMappingActualizeTaskAction = new DcpMappingActualizeTaskAction(mappingActualizeService,
            gcSkuTicketDao,
            partnerShopService
        );
    }

    @Test
    public void runOnTickets() {
        List<GcSkuTicket> tickets = generateDBDcpInitialStateNew(3, states -> {
            for (int i = 0; i < states.size(); i++) {
                PreparedOfferState state = states.get(i);
                state.reInitWithNewIdentifiers(PARTNER_SHOP_ID, OFFER_IDS[i]);
            }
        });

        mboMappingsServiceMock.addMapping(CATEGORY_ID, PARTNER_SHOP_ID, OFFER_IDS[0], MSKU_IDS[0],
            SupplierOffer.ApprovedMappingConfidence.MAPPING_CONTENT
        );
        mboMappingsServiceMock.addMapping(CATEGORY_ID, PARTNER_SHOP_ID, OFFER_IDS[1], MSKU_IDS[1],
            SupplierOffer.ApprovedMappingConfidence.MAPPING_PARTNER
        );
        mboMappingsServiceMock.addMapping(CATEGORY_ID, PARTNER_SHOP_ID, OFFER_IDS[2], MSKU_IDS[2],
            SupplierOffer.ApprovedMappingConfidence.MAPPING_PARTNER_SELF
        );

        ProcessTaskResult<ProcessDataBucketData> result = dcpMappingActualizeTaskAction.runOnTickets(
            tickets,
            new ProcessDataBucketData(dataBucketId)
        );
        assertThat(result.getResult().getDataBucketId()).isEqualTo(dataBucketId);

        List<Long> ticketIds = tickets.stream()

            .map(GcSkuTicket::getId)
            .collect(Collectors.toList());

        Map<String, GcSkuTicket> ticketByOfferId = gcSkuTicketDao.fetchById(ticketIds).stream()
            .collect(Collectors.toMap(GcSkuTicket::getShopSku, Function.identity()));

        GcSkuTicket gcSkuTicket1 = ticketByOfferId.get(OFFER_IDS[0]);
        GcSkuTicket gcSkuTicket2 = ticketByOfferId.get(OFFER_IDS[1]);
        GcSkuTicket gcSkuTicket3 = ticketByOfferId.get(OFFER_IDS[2]);

        assertThat(gcSkuTicket1.getStatus()).isEqualTo(GcSkuTicketStatus.RESULT_UPLOAD_STARTED);
        assertThat(gcSkuTicket1.getMappedMboSkuId()).isEqualTo(MSKU_IDS[0]);
        assertThat(gcSkuTicket1.getExistingMboPskuId()).isNull();
        assertThat(gcSkuTicket1.getMappingConfidence()).isEqualTo(MappingConfidenceType.CONTENT);

        assertThat(gcSkuTicket2.getStatus()).isEqualTo(GcSkuTicketStatus.NEW);
        assertThat(gcSkuTicket2.getMappedMboSkuId()).isEqualTo(MSKU_IDS[1]);
        assertThat(gcSkuTicket2.getExistingMboPskuId()).isNull();
        assertThat(gcSkuTicket2.getMappingConfidence()).isEqualTo(MappingConfidenceType.PARTNER);

        assertThat(gcSkuTicket3.getStatus()).isEqualTo(GcSkuTicketStatus.NEW);
        assertThat(gcSkuTicket3.getMappedMboSkuId()).isEqualTo(MSKU_IDS[2]);
        assertThat(gcSkuTicket3.getExistingMboPskuId()).isEqualTo(MSKU_IDS[2]);
        assertThat(gcSkuTicket3.getExistingCategoryId()).isEqualTo(CATEGORY_ID);
        assertThat(gcSkuTicket3.getMappingConfidence()).isEqualTo(MappingConfidenceType.PARTNER_SELF);
    }

    @Test
    public void whenThereAreRemovedOffersThenExcludeThemFromProcessing() {
        List<GcSkuTicket> tickets = generateDBDcpInitialStateNew(3, states -> {
            for (int i = 0; i < states.size(); i++) {
                PreparedOfferState state = states.get(i);
                state.reInitWithNewIdentifiers(PARTNER_SHOP_ID, OFFER_IDS[i]);
            }
        });
        mboMappingsServiceMock.addMapping(CATEGORY_ID, PARTNER_SHOP_ID, OFFER_IDS[0], MSKU_IDS[0],
            SupplierOffer.ApprovedMappingConfidence.MAPPING_CONTENT
        );

        ProcessTaskResult<ProcessDataBucketData> result = dcpMappingActualizeTaskAction.runOnTickets(
            tickets,
            new ProcessDataBucketData(dataBucketId)
        );
        assertThat(result.getResult().getDataBucketId()).isEqualTo(dataBucketId);

        List<Long> ticketIds = tickets.stream()

            .map(GcSkuTicket::getId)
            .collect(Collectors.toList());

        Map<String, GcSkuTicket> ticketByOfferId = gcSkuTicketDao.fetchById(ticketIds).stream()
            .collect(Collectors.toMap(GcSkuTicket::getShopSku, Function.identity()));

        GcSkuTicket gcSkuTicket1 = ticketByOfferId.get(OFFER_IDS[0]);
        GcSkuTicket gcSkuTicket2 = ticketByOfferId.get(OFFER_IDS[1]);
        GcSkuTicket gcSkuTicket3 = ticketByOfferId.get(OFFER_IDS[2]);

        assertThat(gcSkuTicket1.getStatus()).isEqualTo(GcSkuTicketStatus.RESULT_UPLOAD_STARTED);
        assertThat(gcSkuTicket1.getMappedMboSkuId()).isEqualTo(MSKU_IDS[0]);
        assertThat(gcSkuTicket1.getExistingMboPskuId()).isNull();
        assertThat(gcSkuTicket1.getMappingConfidence()).isEqualTo(MappingConfidenceType.CONTENT);

        assertThat(gcSkuTicket2.getStatus()).isEqualTo(GcSkuTicketStatus.OFFER_IS_REMOVED);
        assertThat(gcSkuTicket3.getStatus()).isEqualTo(GcSkuTicketStatus.OFFER_IS_REMOVED);
    }
}
