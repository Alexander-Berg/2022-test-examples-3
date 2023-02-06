package ru.yandex.market.abo.core.shop.alias;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.abo.core.CoreConfig;
import ru.yandex.market.abo.core.shop.alias.model.ShopAliasMarking;
import ru.yandex.market.abo.core.shop.alias.model.ShopAliasMarkingStatus;
import ru.yandex.market.abo.core.shop.alias.model.ShopAliasTaskSourceType;
import ru.yandex.market.util.db.ConfigurationService;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 30.09.2020
 */
class ShopAliasMarkingManagerTest {

    private static final long SHOP_ID = 123L;
    private static final String ALIAS = "holodilnik.ru";
    private static final long SOURCE_ID = 21421424L;

    @InjectMocks
    private ShopAliasMarkingManager shopAliasMarkingManager;

    @Mock
	private ShopAliasesService shopAliasesService;
    @Mock
	private ShopAliasMarkingService shopAliasMarkingService;
    @Mock
    private ConfigurationService coreConfigService;

    @Mock
    private ShopAliasMarking marking;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);

        when(coreConfigService.getValueAsInt(CoreConfig.DONT_CREATE_ALIAS_MARKING_TASKS.getId())).thenReturn(0);

        when(shopAliasesService.getAliasesForShop(SHOP_ID)).thenReturn(Set.of(ALIAS));

        when(marking.getShopId()).thenReturn(SHOP_ID);
        when(marking.getAlias()).thenReturn(ALIAS);
        when(marking.getStatus()).thenReturn(ShopAliasMarkingStatus.FINISHED);

        when(shopAliasMarkingService.findFinishedMarkings(SHOP_ID)).thenReturn(List.of(marking));
    }

    @Test
    void createAliasMarkingTask_aliasNotExist() {
        when(shopAliasesService.getAliasesForShop(SHOP_ID)).thenReturn(Collections.emptySet());

        shopAliasMarkingManager.createAliasMarkingTaskIfNecessary(SHOP_ID, SOURCE_ID, ShopAliasTaskSourceType.PREMOD_TICKET);

        verifyNoMoreInteractions(shopAliasMarkingService);
    }

    @Test
    void createAliasMarkingTask_aliasAlreadyChecked() {
        when(marking.getModificationTime()).thenReturn(LocalDateTime.now());

        shopAliasMarkingManager.createAliasMarkingTaskIfNecessary(SHOP_ID, SOURCE_ID, ShopAliasTaskSourceType.PREMOD_TICKET);

        verify(shopAliasMarkingService, never()).createAliasMarkingTaskIfNotExists(SHOP_ID, ALIAS, SOURCE_ID, ShopAliasTaskSourceType.PREMOD_TICKET);
    }

    @Test
    void createAliasMarkingTask_aliasMarkingExpired() {
        when(marking.getModificationTime()).thenReturn(LocalDateTime.now().minusDays(ShopAliasMarkingManager.ALIAS_MARKING_EXPIRATION_TIME_DAYS + 1));

        shopAliasMarkingManager.createAliasMarkingTaskIfNecessary(SHOP_ID, SOURCE_ID, ShopAliasTaskSourceType.PREMOD_TICKET);

        verify(shopAliasMarkingService).createAliasMarkingTaskIfNotExists(SHOP_ID, ALIAS, SOURCE_ID, ShopAliasTaskSourceType.PREMOD_TICKET);
    }

    @Test
    void createAliasMarkingTask_aliasNeverMarked() {
        when(shopAliasMarkingService.findFinishedMarkings(SHOP_ID)).thenReturn(Collections.emptyList());

        shopAliasMarkingManager.createAliasMarkingTaskIfNecessary(SHOP_ID, SOURCE_ID, ShopAliasTaskSourceType.PREMOD_TICKET);

        verify(shopAliasMarkingService).createAliasMarkingTaskIfNotExists(SHOP_ID, ALIAS, SOURCE_ID, ShopAliasTaskSourceType.PREMOD_TICKET);
    }
}
