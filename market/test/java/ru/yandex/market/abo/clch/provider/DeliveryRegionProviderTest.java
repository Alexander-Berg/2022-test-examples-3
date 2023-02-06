package ru.yandex.market.abo.clch.provider;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.abo.clch.model.SimpleDeliveryRegion;
import ru.yandex.market.abo.core.region.ShopDeliveryRegionService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.when;

/**
 * @author artemmz
 * @date 14.11.17.
 */
public class DeliveryRegionProviderTest {
    private static final SimpleDeliveryRegion SOME_REGION = new SimpleDeliveryRegion(0, "name", true);

    @InjectMocks
    private DeliveryRegionProvider deliveryRegionProvider;
    @Mock
    private ShopDeliveryRegionService shopDeliveryRegionService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        when(shopDeliveryRegionService.getDeliveryRegionsFromDb(anySet())).then(inv -> {
            Map<Long, List<SimpleDeliveryRegion>> result = new HashMap<>();
            Set<Long> shopIds = (Set<Long>) inv.getArguments()[0];
            shopIds.forEach(shopId -> result.put(shopId, Collections.singletonList(SOME_REGION)));
            return result;
        });
    }

    @Test
    public void findRegions() {
        Map<Long, List<SimpleDeliveryRegion>> result = new HashMap<>();
        Set<Long> shops = new HashSet<>(Arrays.asList(1L, 2L, 3L));
        deliveryRegionProvider.fillShopValueMap(shops, result);
        assertEquals(shops, result.keySet());
        assertEquals(SOME_REGION, result.values().iterator().next().get(0));
    }
}
