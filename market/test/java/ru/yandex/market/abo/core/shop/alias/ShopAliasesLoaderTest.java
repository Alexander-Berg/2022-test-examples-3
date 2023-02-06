package ru.yandex.market.abo.core.shop.alias;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.abo.config.spring.yt.YtEnvironmentConfig;
import ru.yandex.market.abo.core.CoreCounter;
import ru.yandex.market.abo.core.shop.alias.model.ShopAliases;
import ru.yandex.market.abo.core.yt.YtService;
import ru.yandex.market.util.db.ConfigurationService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 30.09.2020
 */
class ShopAliasesLoaderTest {

    private static final String PROCESSED_TABLE = "20200930_160003";
    private static final String LAST_YT_TABLE = "20200930_180003";

    @InjectMocks
    private ShopAliasesLoader shopAliasesLoader;

    @Mock
    private YtService ytService;
    @Mock
    private ShopAliasesService shopAliasesService;
    @Mock
    private YtEnvironmentConfig ytEnvironmentConfig;
    @Mock
    private ConfigurationService coreCounterService;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);

        when(coreCounterService.getValue(CoreCounter.LAST_PROCESSED_SHOP_ALIASES_TABLE.name())).thenReturn(PROCESSED_TABLE);
        when(ytService.linkTargetPath(any())).thenReturn(LAST_YT_TABLE);

        var shopAliases = mock(ShopAliases.class);
        when(ytService.readTableJson(any(), eq(ShopAliases.class))).thenReturn(List.of(shopAliases));
    }

    @Test
    void loadShopsAliases_unprocessedTableNotExists() {
        when(ytService.linkTargetPath(any())).thenReturn(PROCESSED_TABLE);

        shopAliasesLoader.loadShopsAliases();

        verifyNoMoreInteractions(shopAliasesService);
        verify(ytService, never()).readTableJson(any(), eq(ShopAliases.class));
        verify(coreCounterService, never()).mergeValue(anyString(), anyString());
    }

    @Test
    void loadShopsAliases_newTableExists() {
        shopAliasesLoader.loadShopsAliases();

        verify(shopAliasesService).save(anyList());
        verify(coreCounterService).mergeValue(CoreCounter.LAST_PROCESSED_SHOP_ALIASES_TABLE.name(), LAST_YT_TABLE);
    }
}
