package ru.yandex.market.abo.core.export.hidden;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.common.util.date.DateUtil;
import ru.yandex.market.abo.api.entity.complaint.ComplaintPlatform;
import ru.yandex.market.abo.api.entity.complaint.ComplaintType;
import ru.yandex.market.abo.api.entity.offer.hidden.HiddenOffer;
import ru.yandex.market.abo.api.entity.offer.hidden.details.HidingDetails;
import ru.yandex.market.abo.core.complain.model.Complaint;
import ru.yandex.market.abo.core.complain.service.ComplaintService;
import ru.yandex.market.abo.core.hiding.util.model.CheckStatus;
import ru.yandex.market.abo.core.indexer.Generation;
import ru.yandex.market.abo.core.indexer.GenerationService;
import ru.yandex.market.abo.cpa.cart_diff.CartDiffService;
import ru.yandex.market.abo.cpa.cart_diff.CartDiffStatus;
import ru.yandex.market.abo.cpa.cart_diff.diff.CartDiff;
import ru.yandex.market.abo.util.mapper.MapperUtil;
import ru.yandex.market.checkout.checkouter.order.Color;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static ru.yandex.market.abo.cpa.cart_diff.approve.CartDiffApproverTest.initCartDiff;

/**
 * @author artemmz
 * @date 30.06.2017
 */
 class HiddenOffersManagerTest extends EmptyTest {
    private static final long SHOP_ID = 23L;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private HiddenOffersManager hiddenOffersManager;

    @Autowired
    private CartDiffService cartDiffService;
    @Autowired
    private ComplaintService complaintService;
    @Autowired
    private GenerationService generationService;
    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
     void init() {
        Date threshold = DateUtil.addHour(new Date(), -1);
        Generation gen = new Generation();
        gen.setId(1L);
        gen.setReleaseDate(threshold);
        generationService.storeGeneration(gen);

        saveCartDiff();
        saveComplaint();

        entityManager.flush();
    }

    private void saveCartDiff() {
        CartDiff cartDiff = initCartDiff(ru.yandex.market.common.report.model.Color.WHITE);
        cartDiff.setShopId(SHOP_ID);
        cartDiff.setStatus(CartDiffStatus.APPROVED);
        cartDiffService.merge(Collections.singletonList(cartDiff));

        CartDiff diff = cartDiffService.selectAll().get(0);
        diff.setOfferRemovedDate(new Date());
        cartDiffService.updateTillNextGen(Collections.singletonList(diff));
    }

    private void saveComplaint() {
        Complaint complaint = complaintService.storeComplaint(SHOP_ID, 1L, "ware_md5", null, 2L, "offerId",
                ComplaintType.PRICE, ComplaintPlatform.DESKTOP, "text", true, 213L, null, Color.GREEN, null);
        complaint.setCheckStatus(CheckStatus.OFFER_REMOVED);
        complaint.setOfferRemovedDate(new Date());
        complaintService.updateTillNextGen(Collections.singletonList(complaint));
    }

    @Test
     void loadHidden() throws Exception {
        Collection<String> hiddenOffers = hiddenOffersManager.loadWithDetails().stream()
                .map(ho -> MapperUtil.writeUnsafe(ho, MAPPER)).collect(Collectors.toList());
        assertEquals(2, hiddenOffers.size());
        for (String json : hiddenOffers) {
            HiddenOffer hiddenOffer = MAPPER.readValue(json, HiddenOffer.class);
            assertNotNull(hiddenOffer);
            HidingDetails hidingDetails = hiddenOffer.getHidingDetails();
            if (hidingDetails == null) {
                continue;
            }
            switch (hiddenOffer.getHidingReason()) {
                case BAD_QUALITY:
                    assertNotNull(hidingDetails.getAssessorHidingDetails());
                    break;
                case CART_DIFF:
                case FEED_DIFF:
                    switch (hiddenOffer.getHidingSubReason()) {
                        case PRICE:
                            assertNotNull(hidingDetails.getPriceComparison());
                            break;
                        case STOCK:
                            assertNotNull(hidingDetails.getStockComparison());
                            break;
                        case DELIVERY_PRICE:
                        case DELIVERY_DATES:
                        case DELIVERY_REGIONS:
                            assertNotNull(hidingDetails.getDeliveryComparison());
                            break;
                    }
                    break;
                default:
                    throw new RuntimeException("unknown reason!");
            }
        }
    }
}
