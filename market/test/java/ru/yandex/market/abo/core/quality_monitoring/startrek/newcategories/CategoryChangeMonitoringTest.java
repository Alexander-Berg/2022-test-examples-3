package ru.yandex.market.abo.core.quality_monitoring.startrek.newcategories;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.abo.core.premod.category.PremodShopCategoriesService;
import ru.yandex.market.abo.core.shop.ShopInfo;
import ru.yandex.market.abo.core.shop.ShopInfoService;
import ru.yandex.market.abo.core.startrek.StartrekTicketManager;
import ru.yandex.market.abo.core.startrek.model.StartrekTicketReason;
import ru.yandex.startrek.client.model.IssueCreate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.abo.core.quality_monitoring.startrek.StartrekTicketMonitoring.MAX_TICKETS_PER_SESSION;

/**
 * @author artemmz
 *         created on 22.03.17.
 */
class CategoryChangeMonitoringTest {
    private static final long CATEGORY = 1L;
    private static final String CATEGORY_NAME = "foo";
    private static final long THRESHOLD = 10;
    private static final long SHOP = 774;

    private static final CategoryToMonitorValue CATEGORY_TO_MONITOR_VALUE = new CategoryToMonitorValue(
            new CategoryToMonitor(CATEGORY, CATEGORY_NAME, THRESHOLD, MonitoringScope.CATEGORY_CHANGE), THRESHOLD
    );

    @InjectMocks
    private CategoryChangeMonitoring categoryChangeMonitoring;

    @Mock
    private YtCategoriesManager ytCategoriesManager;
    @Mock
    private PremodShopCategoriesService premodShopCategoriesService;
    @Mock
    private StartrekTicketManager startrekTicketManager;
    @Mock
    private ShopInfoService shopInfoService;
    @Mock
    private CategoryToMonitorValueManager categoryToMonitorValueManager;

    @Mock
    private Map<Long, List<Long>> premodShopCategories = new HashMap<>();
    private Map<Long, List<CategoryToMonitorValue>> shopsNewCategories = new HashMap<>();

    private List<Long> enabledShopsWithSavedCategories = new ArrayList<>();
    private List<ShopCategoryOffersCount> shopsCategoriesWithOffersCounts = new ArrayList<>();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        categoryChangeMonitoring.setStartrekTicketReason(StartrekTicketReason.SHOP_CHANGED_CATEGORIES);

        Arrays.asList(enabledShopsWithSavedCategories, shopsCategoriesWithOffersCounts).forEach(Collection::clear);

        when(premodShopCategoriesService.getEnabledShopsWithSavedCategories()).thenReturn(enabledShopsWithSavedCategories);
        when(premodShopCategoriesService.getShopsPremodCategories(anyList())).thenReturn(premodShopCategories);

        when(ytCategoriesManager.loadShopCategoriesWithOffersCountOverThreshold())
                .thenReturn(shopsCategoriesWithOffersCounts);

        when(categoryToMonitorValueManager.searchNewCategories(anyList(), eq(shopsCategoriesWithOffersCounts)))
                .thenReturn(shopsNewCategories);

        when(startrekTicketManager.hasNoNewTickets(anyLong(), any(), anyInt())).thenReturn(true);

        when(shopInfoService.getShopInfo(anyLong())).thenReturn(initShopInfo());
    }

    @Test
    void hasNewCategories() {
        init(true);
        Map<Long, IssueCreate> ticketsToCreate = categoryChangeMonitoring.ticketsToCreate();
        assertEquals(1, ticketsToCreate.size());
        assertTrue(ticketsToCreate.containsKey(SHOP));
    }

    @Test
    void hasNoNewCategories() {
        init(false);
        Map<Long, IssueCreate> ticketsToCreate = categoryChangeMonitoring.ticketsToCreate();
        assertTrue(ticketsToCreate.isEmpty());
    }

    @Test
    void hasNewTickets() {
        when(startrekTicketManager.hasNoNewTickets(anyLong(), any(), anyInt())).thenReturn(false);
        init(true);
        categoryChangeMonitoring.monitor();
        verify(startrekTicketManager, never()).createTicket(any());
    }

    @Test
    void limit() {
        init(true);
        Stream.iterate(0L, i -> i + 1).limit(2 * MAX_TICKETS_PER_SESSION).forEach(shop -> {
            enabledShopsWithSavedCategories.add(shop);
            shopsCategoriesWithOffersCounts.add(new ShopCategoryOffersCount(shop, CATEGORY, THRESHOLD));
            shopsNewCategories.put(shop, List.of(CATEGORY_TO_MONITOR_VALUE));
        });

        categoryChangeMonitoring.monitor();
        verify(startrekTicketManager, times(MAX_TICKETS_PER_SESSION)).createTicket(any());
    }

    private void init(boolean hasNewCategories) {
        enabledShopsWithSavedCategories.add(SHOP);
        shopsCategoriesWithOffersCounts.add(new ShopCategoryOffersCount(SHOP, CATEGORY, THRESHOLD));
        shopsNewCategories.put(SHOP, hasNewCategories ? List.of(CATEGORY_TO_MONITOR_VALUE) : Collections.emptyList());
        when(premodShopCategories.get(anyLong())).thenReturn(List.of(hasNewCategories ? CATEGORY + 1 : CATEGORY));
    }

    private ShopInfo initShopInfo() {
        ShopInfo shopInfo = new ShopInfo();
        shopInfo.setName("surprise me");
        return shopInfo;
    }
}
