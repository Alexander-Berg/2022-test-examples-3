package ru.yandex.market.abo.core.hiding.util;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang.time.DateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.common.util.xml.XmlWriter;
import ru.yandex.market.abo.core.hiding.util.model.Mailable;
import ru.yandex.market.abo.core.indexer.Generation;
import ru.yandex.market.abo.core.indexer.GenerationService;
import ru.yandex.market.abo.cpa.MbiApiService;
import ru.yandex.market.abo.cpa.cart_diff.diff.CartDiff;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author artemmz
 *         created on 06.04.17.
 */
public class HiddenOffersNotifierTest {
    private static final long SHOP_WITHOUT_NEW_HIDDEN = 0;
    private static final long SHOP_WITH_NEW_HIDDEN = 1;
    private static final int DIFFS_SIZE = 10;
    private static final int ADDITIONAL_TIMEOUT = 10;

    @InjectMocks
    private HiddenOffersNotifier hiddenOffersNotifier;

    @Mock
    private MbiApiService mbiApiService;
    @Mock
    private HideOffersDBService hideOffersDBService;
    @Mock
    private GenerationService generationService;
    @Mock
    private Generation generation;

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        when(mbiApiService.sendMessageToShop(anyLong(), anyInt(), anyString())).thenReturn(true);
        when(generationService.loadPrevReleaseGeneration()).thenReturn(generation);
    }

    @Test
    public void sendMailsToShops() throws Exception {
        checkSendMails(false);
    }

    @Test
    public void mbiApiFail() throws Exception {
        when(mbiApiService.sendMessageToShop(anyLong(), anyInt(), anyString())).thenReturn(false);
        checkSendMails(true);
    }

    @Test
    public void mbiFailWithException() throws Exception {
        when(mbiApiService.sendMessageToShop(anyLong(), anyInt(), anyString())).thenThrow(new RuntimeException("foo"));
        checkSendMails(true);
    }

    @SuppressWarnings("unchecked")
    private void checkSendMails(boolean mbiFail) {
        Map<Long, List<Mailable>> problemsByShopId = new HashMap<>();
        List<Mailable> newHiddenOffers = Collections.singletonList(initMailable(true));
        List<Mailable> oldHiddenOffers = Collections.singletonList(initMailable(false));
        List<Mailable> newAndOldHiddenOffers = Stream.concat(newHiddenOffers.stream(), oldHiddenOffers.stream()).collect(toList());

        problemsByShopId.put(SHOP_WITH_NEW_HIDDEN, newAndOldHiddenOffers);
        problemsByShopId.put(SHOP_WITHOUT_NEW_HIDDEN, oldHiddenOffers);

        hiddenOffersNotifier.sendMailsToShops(problemsByShopId, 0, hideOffersDBService);
        verify(hideOffersDBService).updateMailDates(mbiFail ? new ArrayList<>() : newHiddenOffers);
    }

    @Test
    public void loadPrevGenDate_indexerTooEarly() throws Exception {
        when(generation.getReleaseDate()).thenReturn(new Timestamp(System.currentTimeMillis()));
        Date minSendTimeout = DateUtils.addHours(new Date(), -1);
        Date prevGenDate = hiddenOffersNotifier.loadPrevGenDate(minSendTimeout);
        assertEquals(minSendTimeout, prevGenDate);
    }

    @Test
    public void loadPrevGenDate_indexerDate() throws Exception {
        Date indexerDate = returnNormalIndexerDate();
        Date minSendTimeout = DateUtils.addHours(new Date(), -HiddenOffersNotifier.MIN_SEND_TIMEOUT + 1);
        Date prevGenDate = hiddenOffersNotifier.loadPrevGenDate(minSendTimeout);
        assertEquals(indexerDate, prevGenDate);
    }

    @Test
    public void additionalTimeout_send() throws Exception {
        additionalTimeout(true);
    }

    @Test
    public void additionalTimeout_wait() throws Exception {
        additionalTimeout(false);
    }

    private void additionalTimeout(boolean shouldSend) {
        Date indexerDate = returnNormalIndexerDate();
        List<CartDiff> diffsToMail = hiddenOffersNotifier.getProblemsToMail(
                initDiffs(DateUtils.addMinutes(indexerDate, shouldSend ? -ADDITIONAL_TIMEOUT - 1 : -ADDITIONAL_TIMEOUT + 1)),
                ADDITIONAL_TIMEOUT);
        assertTrue(shouldSend ? diffsToMail.size() == DIFFS_SIZE : diffsToMail.isEmpty());
    }

    private Date returnNormalIndexerDate() {
        Date indexerDate = DateUtils.addHours(new Date(), -HiddenOffersNotifier.MIN_SEND_TIMEOUT - 1);
        when(generation.getReleaseDate()).thenReturn(new Timestamp(indexerDate.getTime()));
        return indexerDate;
    }

    private static List<CartDiff> initDiffs(Date offerRemovedDate) {
        return Stream.iterate(0, i -> i + 1).limit(DIFFS_SIZE)
                .map(i -> initDiff(offerRemovedDate))
                .collect(Collectors.toList());
    }

    private static CartDiff initDiff(Date offerRemovedDate) {
        CartDiff cartDiff = new CartDiff();
        cartDiff.setOfferRemovedDate(offerRemovedDate);
        cartDiff.hideOffer();
        return cartDiff;
    }

    private Mailable initMailable(boolean isNew) {
        return new Mailable() {
            @Override
            public boolean isNew() {
                return isNew;
            }

            @Override
            public long getId() {
                return 0;
            }

            @Override
            public void toXml(XmlWriter xmlWriter) throws IOException {
                //do nothing
            }
        };
    }
}
