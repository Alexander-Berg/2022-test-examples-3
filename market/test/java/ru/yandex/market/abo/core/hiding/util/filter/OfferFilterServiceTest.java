package ru.yandex.market.abo.core.hiding.util.filter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.abo.core.hiding.util.HideOffersDBService;
import ru.yandex.market.abo.cpa.MbiApiService;
import ru.yandex.market.abo.cpa.cart_diff.CartDiffStatus;
import ru.yandex.market.abo.cpa.cart_diff.diff.CartDiff;
import ru.yandex.market.common.report.model.Color;
import ru.yandex.market.core.feature.model.ShopFeatureListItem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.abo.cpa.cart_diff.approve.CartDiffApproverTest.initCartDiff;

/**
 * @author artemmz
 * created on 06.04.17.
 */
public class OfferFilterServiceTest {
    @InjectMocks
    private CartDiffOfferFilterService offerFilterService;

    @Mock
    private HideOffersDBService<CartDiff> hideOffersDBService;
    @Mock
    private MbiApiService mbiApiService;
    @Mock
    private ShopFeatureListItem shopFeatureListItem;

    private final CartDiff cartDiff = initCartDiff(Color.BLUE);

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void avoidDuplicates() {
        List<CartDiff> duplicates = new ArrayList<>();
        List<CartDiff> diffs = new ArrayList<>();

        duplicates.add(initCartDiff(Color.BLUE));
        duplicates.add(initCartDiff(Color.BLUE));
        diffs.add(initCartDiff(Color.BLUE));
        diffs.addAll(duplicates);

        offerFilterService.removeDuplicates(diffs);

        assertEquals(1, diffs.size());
        verify(hideOffersDBService).updateTillNextGen(duplicates);
        duplicates.forEach(diff -> assertEquals(CartDiffStatus.CANCELLED, diff.getStatus()));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void removePiDiffs(boolean isPi) {
        when(mbiApiService.getShopWithFeature(anyLong(), any())).thenReturn(shopFeatureListItem);
        when(shopFeatureListItem.isCpaPartnerInterface()).thenReturn(isPi);
        Long initialDiffState = cartDiff.getCheckState();

        offerFilterService.removePiDiffs(new ArrayList<>(List.of(cartDiff)));
        if (isPi) {
            verify(hideOffersDBService).updateTillNextGen(Collections.singletonList(cartDiff));
            assertEquals(CartDiffStatus.CANCELLED, cartDiff.getStatus());
        } else {
            verify(hideOffersDBService, never()).updateTillNextGen(Collections.singletonList(cartDiff));
            assertEquals(initialDiffState, cartDiff.getCheckState());
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void processChecksByCondition() {
        List<CartDiff> diffs = new ArrayList<>(Collections.singletonList(cartDiff));
        Consumer<CartDiff> diffConsumer = mock(Consumer.class);

        offerFilterService.processChecksByCondition(cartDiff -> true, diffConsumer, diffs);
        verify(diffConsumer, times(1)).accept(cartDiff);
        verify(hideOffersDBService).updateTillNextGen(Collections.singletonList(cartDiff));
    }

}
