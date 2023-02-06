package ru.yandex.market.gutgin.tms.service.goodcontent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.gutgin.tms.config.TestServiceConfig;
import ru.yandex.market.ir.autogeneration.common.helpers.MboMappingsServiceHelper;
import ru.yandex.market.mboc.http.SupplierOffer;
import ru.yandex.market.partner.content.common.DBDcpStateGenerator;
import ru.yandex.market.partner.content.common.db.dao.dcp.FakeDatacampOfferDao;
import ru.yandex.market.partner.content.common.db.dao.goodcontent.GcSkuTicketDao;
import ru.yandex.market.partner.content.common.db.jooq.enums.GcSkuTicketType;
import ru.yandex.market.partner.content.common.db.jooq.enums.MappingConfidenceType;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.DatacampOffer;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcSkuTicket;
import ru.yandex.market.partner.content.common.utils.DcpOfferBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestServiceConfig.class)
public class MappingActualizeServiceTest extends DBDcpStateGenerator {

    private static final Long SUPPLIER_ID = 123L;
    private static final Long SKU_ID = 1234567L;

    @Autowired
    private GcSkuTicketDao gcSkuTicketDao;

    private MboMappingsServiceHelper mboMappingsServiceHelper = mock(MboMappingsServiceHelper.class);

    @Autowired
    private FakeDatacampOfferDao fakeDatacampOfferDao;

    private MappingActualizeService service;

    private GcSkuTicket ticket;

    @Before
    public void setUp() {
        super.setUp();
        service = new MappingActualizeService(gcSkuTicketDao,
                mboMappingsServiceHelper, fakeDatacampOfferDao);
        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialState(1, datacampOffers -> {

            DatacampOffer datacampOffer = datacampOffers.get(0);
            DcpOfferBuilder dcpOfferBuilder = new DcpOfferBuilder(
                    datacampOffer.getBusinessId(),
                    datacampOffer.getOfferId()
            );
            dcpOfferBuilder.build();
        });
        ticket = gcSkuTickets.get(0);
    }

    @Test
    public void testWhenCskuContentModificationThenSetExistingMapping() {
        ticket.setType(GcSkuTicketType.CSKU);
        gcSkuTicketDao.update(ticket);
        List<SupplierOffer.Offer> offers = new ArrayList<>();
        offers.add(SupplierOffer.Offer.newBuilder()
                .setSupplierId(SUPPLIER_ID)
                .setShopSkuId(ticket.getShopSku())
                .setApprovedMapping(SupplierOffer.Mapping.newBuilder().setSkuId(SKU_ID).build())
                .setApprovedMappingConfidence(SupplierOffer.ApprovedMappingConfidence.MAPPING_CONTENT)
                .build());
        when(mboMappingsServiceHelper.searchMappingsByBusinessKeys(any(), eq(SUPPLIER_ID.intValue())))
                .thenReturn(offers);
        service.actualizeForDcp(Collections.singletonList(ticket), SUPPLIER_ID.intValue());
        assertThat(ticket.getExistingMboPskuId()).isEqualTo(SKU_ID);
    }

    @Test
    public void testWhenCskuPartnerModificationThenSetExistingMapping() {
        ticket.setType(GcSkuTicketType.CSKU);
        gcSkuTicketDao.update(ticket);
        List<SupplierOffer.Offer> offers = new ArrayList<>();
        offers.add(SupplierOffer.Offer.newBuilder()
                .setSupplierId(SUPPLIER_ID)
                .setShopSkuId(ticket.getShopSku())
                .setApprovedMapping(SupplierOffer.Mapping.newBuilder().setSkuId(SKU_ID).build())
                .setApprovedMappingConfidence(SupplierOffer.ApprovedMappingConfidence.MAPPING_PARTNER)
                .build());
        when(mboMappingsServiceHelper.searchMappingsByBusinessKeys(any(), eq(SUPPLIER_ID.intValue())))
                .thenReturn(offers);
        service.actualizeForDcp(Collections.singletonList(ticket), SUPPLIER_ID.intValue());
        assertThat(ticket.getExistingMboPskuId()).isEqualTo(SKU_ID);
    }

    @Test
    public void testWhenNewCskuThenSetNoMapping() {
        ticket.setType(GcSkuTicketType.CSKU);
        gcSkuTicketDao.update(ticket);
        List<SupplierOffer.Offer> offers = new ArrayList<>();
        offers.add(SupplierOffer.Offer.newBuilder()
                .setSupplierId(SUPPLIER_ID)
                .setShopSkuId(ticket.getShopSku())
                .build());
        when(mboMappingsServiceHelper.searchMappingsByBusinessKeys(any(), eq(SUPPLIER_ID.intValue())))
                .thenReturn(offers);
        service.actualizeForDcp(Collections.singletonList(ticket), SUPPLIER_ID.intValue());
        assertThat(ticket.getExistingMboPskuId()).isNull();
    }

    @Test
    public void testWhenDatacampSelfModificationThenSetExistingMapping() {
        ticket.setType(GcSkuTicketType.DATA_CAMP);
        gcSkuTicketDao.update(ticket);

        List<SupplierOffer.Offer> offers = new ArrayList<>();
        offers.add(SupplierOffer.Offer.newBuilder()
                .setSupplierId(SUPPLIER_ID)
                .setShopSkuId(ticket.getShopSku())
                .setApprovedMapping(SupplierOffer.Mapping.newBuilder()
                        .setSkuId(SKU_ID)
                        .build())
                .setApprovedMappingConfidence(SupplierOffer.ApprovedMappingConfidence.MAPPING_PARTNER_SELF)
                .build());
        when(mboMappingsServiceHelper.searchMappingsByBusinessKeys(any(), eq(SUPPLIER_ID.intValue())))
                .thenReturn(offers);
        service.actualizeForDcp(Collections.singletonList(ticket), SUPPLIER_ID.intValue());
        assertThat(ticket.getExistingMboPskuId()).isEqualTo(SKU_ID);
    }

    @Test
    public void testWhenFCModificationThenSetFCMappingConfidence() {
        ticket.setType(GcSkuTicketType.CSKU);
        gcSkuTicketDao.update(ticket);

        List<SupplierOffer.Offer> offers = new ArrayList<>();
        offers.add(SupplierOffer.Offer.newBuilder()
                .setSupplierId(SUPPLIER_ID)
                .setShopSkuId(ticket.getShopSku())
                .setApprovedMapping(SupplierOffer.Mapping.newBuilder()
                        .setSkuId(SKU_ID)
                        .build())
                .setApprovedMappingConfidence(SupplierOffer.ApprovedMappingConfidence.MAPPING_PARTNER_FAST)
                .build());
        when(mboMappingsServiceHelper.searchMappingsByBusinessKeys(any(), eq(SUPPLIER_ID.intValue())))
                .thenReturn(offers);
        service.actualizeForDcp(Collections.singletonList(ticket), SUPPLIER_ID.intValue());
        assertThat(ticket.getExistingMboPskuId()).isEqualTo(SKU_ID);
        assertThat(ticket.getMappingConfidence()).isEqualTo(MappingConfidenceType.PARTNER_FAST);
    }

}
