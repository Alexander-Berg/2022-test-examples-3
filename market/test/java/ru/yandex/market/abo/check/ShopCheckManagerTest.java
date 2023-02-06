package ru.yandex.market.abo.check;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationContext;

import ru.yandex.market.abo.check.model.ShopCheck;
import ru.yandex.market.abo.check.model.ShopCheckSourceInfo;
import ru.yandex.market.abo.check.model.ShopCheckType;
import ru.yandex.market.abo.check.source.ShopCheckSource;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author antipov93@yndx-team.ru
 */
public class ShopCheckManagerTest {

    @InjectMocks
    private ShopCheckManager shopCheckManager;
    @Mock
    private ShopCheckService shopCheckService;
    @Mock
    private ShopCheckSourceService shopCheckSourceService;
    @Mock
    private ApplicationContext context;

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        // 1,1,"Перепроверка проблем",true,"0 0/10 * * * ?";
        String beanName = "problemSecondary";
        ShopCheckSourceInfo shopCheckSourceInfo =
                new ShopCheckSourceInfo(1, 1, beanName, "Перепроверка проблем", "0 0/10 * * * ?");
        when(shopCheckSourceService.getCheckSourcesToStart())
                .thenReturn(Collections.singletonList(shopCheckSourceInfo));

        ShopCheckSource source = mock(ShopCheckSource.class);
        ShopCheck shopCheck = new ShopCheck(ShopCheckType.SHOP_PROBLEM_RECHECK, 1, null, 1, 1, "");
        when(source.generate()).thenReturn(Collections.singletonList(shopCheck));
        when(context.getBean(beanName + "ShopCheckSource", ShopCheckSource.class)).thenReturn(source);
    }

    @Test
    public void testGenerateAll() {
        shopCheckManager.generateChecks();
        verify(shopCheckService, times(1)).storeChecks(any());
    }
}
