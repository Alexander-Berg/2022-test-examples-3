package ru.yandex.market.abo.core.premod;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.EmptyTestWithTransactionTemplate;
import ru.yandex.market.abo.core.premod.model.PremodItem;
import ru.yandex.market.abo.core.premod.model.PremodItemStatus;
import ru.yandex.market.abo.core.premod.model.PremodItemType;
import ru.yandex.market.abo.core.premod.model.PremodTicket;
import ru.yandex.market.abo.core.quality_monitoring.startrek.newcategories.CategoryToMonitorValue;
import ru.yandex.market.abo.core.quality_monitoring.startrek.newcategories.CategoryToMonitorValueManager;
import ru.yandex.market.abo.core.quality_monitoring.startrek.newcategories.ShopCategoryOffersCount;
import ru.yandex.market.abo.core.quality_monitoring.startrek.newcategories.YtCategoriesManager;
import ru.yandex.market.abo.core.storage.json.premod.assortment.JsonPremodCategoryChangeResultService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 25.03.2020
 */
class PremodCategoryChangeManagerTest extends EmptyTestWithTransactionTemplate {

    private static final long TICKET_ID = 12345678L;
    private static final long SHOP_ID = 123L;

    @InjectMocks
    private PremodCategoryChangeManager premodCategoryChangeManager;

    @Mock
    private PremodManager premodManager;
    @Mock
    private PremodTicketService premodTicketService;
    @Mock
    private PremodItemService premodItemService;
    @Mock
    private YtCategoriesManager ytCategoriesManager;
    @Mock
    private JsonPremodCategoryChangeResultService jsonPremodCategoryChangeResultService;
    @Mock
    private CategoryToMonitorValueManager categoryToMonitorValueManager;

    @Mock
    private PremodItem categoryChangeItem;
    @Mock
    private PremodTicket ticket;
    @Mock
    private CategoryToMonitorValue categoryToMonitorValue;
    @Mock
    private ShopCategoryOffersCount shopCategoryOffersCount;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
        when(premodItemService.loadPremodItemsByStatusAndType(PremodItemStatus.NEWBORN, PremodItemType.CATEGORY_CHANGE))
                .thenReturn(List.of(categoryChangeItem));
        when(premodItemService.loadPremodItemByTicketIdAndType(TICKET_ID, PremodItemType.CATEGORY_CHANGE))
                .thenReturn(categoryChangeItem);

        when(categoryChangeItem.getTicketId()).thenReturn(TICKET_ID);

        when(premodTicketService.loadTicketsByIds(any())).thenReturn(List.of(ticket));

        when(ticket.getShopId()).thenReturn(SHOP_ID);
        when(ticket.getId()).thenReturn(TICKET_ID);

        when(ytCategoriesManager.loadAllShopCategoriesWithOffersCountInPlaneshift())
                .thenReturn(List.of(shopCategoryOffersCount));

        when(shopCategoryOffersCount.getShopId()).thenReturn(SHOP_ID);
    }

    @Test
    void processPremodCategoryChangeWhenDoNotExistNewItems() {
        when(premodTicketService.loadTicketsByIds(any())).thenReturn(Collections.emptyList());
        premodCategoryChangeManager.processPremodCategoryChange();
        verify(premodManager, never()).updatePremodItem(any(PremodItem.class));
    }

    @Test
    void processPremodCategoryChangeWithProblemShop() {
        when(categoryToMonitorValueManager.searchNewCategories(Set.of(SHOP_ID), List.of(shopCategoryOffersCount)))
                .thenReturn(Map.of(SHOP_ID, List.of(categoryToMonitorValue)));
        premodCategoryChangeManager.processPremodCategoryChange();
        verify(categoryChangeItem).setStatus(PremodItemStatus.NEW);
        verify(premodManager).updatePremodItem(categoryChangeItem);
    }

    @Test
    void processPremodCategoryChangeWithNonProblemShop() {
        when(categoryToMonitorValueManager.searchNewCategories(Set.of(SHOP_ID), List.of(shopCategoryOffersCount)))
                .thenReturn(Map.of(SHOP_ID, Collections.emptyList()));
        premodCategoryChangeManager.processPremodCategoryChange();
        verify(categoryChangeItem).setStatus(PremodItemStatus.PASS);
        verify(premodManager).updatePremodItem(categoryChangeItem);
    }
}
