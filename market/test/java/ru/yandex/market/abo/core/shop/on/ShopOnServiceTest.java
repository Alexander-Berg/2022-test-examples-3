package ru.yandex.market.abo.core.shop.on;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.common.util.date.DateUtil;
import ru.yandex.market.abo.core.shop.on.model.OnParam;
import ru.yandex.market.abo.core.shop.on.model.OnShopKey;
import ru.yandex.market.abo.core.shop.on.model.SwitchedOnShop;

import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author artemmz
 * @date 17.07.17.
 */
public class ShopOnServiceTest extends EmptyTest {
    @Autowired
    private ShopOnService shopOnService;

    @Test
    public void testDAO() {
        OnShopKey onShopKey = new OnShopKey(-1, OnParam.CPC);
        SwitchedOnShop onShop = shopOnService.save(onShopKey, new Date());
        assertEquals(onShopKey, onShop.getShopKey());
    }

    @Test
    void testLoadLastSwitchOnDates() {
        OnShopKey onShopKey = new OnShopKey(1, OnParam.RED);
        Date yesterday = DateUtil.asDate(LocalDateTime.now().minusDays(1));
        Date today = new Date();
        shopOnService.save(onShopKey, yesterday);
        shopOnService.save(onShopKey, today);
        shopOnService.save(new OnShopKey(2, OnParam.RED), yesterday);

        Map<Long, Date> onDates = shopOnService.loadLastSwitchOnDates(Stream.of(1L, 2L).collect(toSet()), OnParam.RED);
        Map<Long, Date> expected = new HashMap<>();
        expected.put(1L, today);
        expected.put(2L, yesterday);
        assertEquals(expected, onDates);
    }
}
