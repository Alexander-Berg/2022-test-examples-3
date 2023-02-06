package ru.yandex.market.abo.logbroker.checkouter;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import one.util.streamex.StreamEx;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.EmptyTestWithTransactionTemplate;
import ru.yandex.market.abo.cpa.cart_diff.CartDiffActualTypesResolver;
import ru.yandex.market.abo.cpa.cart_diff.CartDiffService;
import ru.yandex.market.abo.cpa.cart_diff.CartDiffType;
import ru.yandex.market.abo.cpa.cart_diff.diff.CartDiff;
import ru.yandex.market.abo.util.DeliveryAddressUtil;
import ru.yandex.market.common.report.model.Color;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.abo.cpa.cart_diff.CartDiffType.ITEM_COUNT;
import static ru.yandex.market.abo.logbroker.checkouter.CartDiffLogProcessor.DATE_PATTERN;

/**
 * @author artemmz
 * @date 08.09.17.
 */
public class CartDiffLogProcessorTest extends EmptyTestWithTransactionTemplate {
    private static final String TEST_LOG_PATH = "/cart_diff/cart_diff.log";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(DATE_PATTERN);

    @InjectMocks
    private CartDiffLogProcessor cartDiffLogProcessor;
    @Mock
    private CartDiffService cartDiffService;
    @Mock
    private DeliveryAddressUtil addressUtil;
    @Mock
    private CartDiffActualTypesResolver cartDiffActualTypesResolver;

    @Captor
    private ArgumentCaptor<List<CartDiff>> mergedCaptor;

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        when(cartDiffActualTypesResolver.getTypes(any(Color.class))).thenReturn(EnumSet.allOf(CartDiffType.class));
    }

    @Test
    public void processLogs() throws Exception {
        cartDiffLogProcessor.processDiffStream(
                Stream.of(IOUtils.toByteArray(CartDiffLogProcessorTest.class.getResourceAsStream(TEST_LOG_PATH)))
        );

        verify(cartDiffService).merge(mergedCaptor.capture());

        List<CartDiff> merged = mergedCaptor.getAllValues().stream()
                .flatMap(Collection::stream).collect(Collectors.toList());

        assertEquals(
                List.of(ITEM_COUNT),
                StreamEx.of(merged).map(CartDiff::getType).toList()
        );

        CartDiff cartDiff = merged.get(0);
        assertEquals(DATE_FORMAT.parse("[2117-09-11 06:36:53,363]"), cartDiff.getDiffDate());
        assertEquals(ITEM_COUNT, cartDiff.getType());
        assertEquals(363245, cartDiff.getShopId().longValue());
        assertEquals(464866, cartDiff.getFeedId().longValue());
        assertEquals("338193", cartDiff.getOfferId());
        assertEquals(Color.WHITE, cartDiff.getRgb());
        assertEquals(1, cartDiff.getLogUserCartInfo().getCount().intValue());
        assertEquals(3171.26, cartDiff.getLogShopCartInfo().getPrice().doubleValue());
        assertEquals(66, cartDiff.getRegionId());
    }

}
