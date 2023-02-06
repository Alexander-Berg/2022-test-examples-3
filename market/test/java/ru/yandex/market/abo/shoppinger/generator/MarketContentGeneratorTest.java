package ru.yandex.market.abo.shoppinger.generator;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.core.offer.report.Offer;
import ru.yandex.market.abo.core.offer.report.OfferService;
import ru.yandex.market.abo.core.pinger.model.MpGeneratorType;
import ru.yandex.market.abo.core.pinger.model.TaskState;
import ru.yandex.market.abo.shoppinger.generator.util.PopularOfferTaskGeneratorStat;
import ru.yandex.market.abo.shoppinger.generator.util.ShopForCheck;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

/**
 * Created by antipov93@yndx-team.ru
 */
public class MarketContentGeneratorTest extends EmptyTest {

    @Autowired
    private JdbcTemplate pgJdbcTemplate;

    @Autowired
    @InjectMocks
    private RegularMarketContentGenerator regularMarketContentGenerator;
    @Autowired
    @InjectMocks
    private PopularMarketContentGenerator popularMarketContentGenerator;

    @Mock
    private OfferService offerService;
    @Mock
    private ShopForCheck shopForCheck1;
    @Mock
    private ShopForCheck shopForCheck2;

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        doReturn(List.of(offer(), offer())).when(offerService).findWithParams(any());

        popularMarketContentGenerator.stat = new PopularOfferTaskGeneratorStat();

        when(shopForCheck1.getShopId()).thenReturn(1L);
        when(shopForCheck1.isRussian()).thenReturn(true);
        when(shopForCheck2.getShopId()).thenReturn(2L);
        when(shopForCheck2.isRussian()).thenReturn(true);
    }

    @Test
    public void testRegularLoad() {
        insertShop(1);
        List<ShopForCheck> shops = regularMarketContentGenerator.loadShops();
        assertEquals(1, shops.size());
    }

    @Test
    public void testPopularLoad() {
        insertShop(1);
        insertShop(2);
        Map<Long, ShopForCheck> shops = popularMarketContentGenerator.loadCpcShops();
        assertEquals(2, shops.size());
    }

    @Test
    public void testFilterLatest() {
        // TODO {@link PingerContentTaskRepo}
        pgJdbcTemplate.update(
                "INSERT INTO pinger_content_task (id, creation_time, gen_id, url, shop_id, ware_md5, state) " +
                        "VALUES (nextval('s_pinger_content_task'), now(), ?, 'url', ?, 'ware_md5', ?)",
                MpGeneratorType.POPULAR_PRICE.getId(), shopForCheck1.getShopId(), TaskState.RUNNING.name()
        );
        List<PopularMarketContentGenerator.Click> clicks = Lists.newArrayList(
                new PopularMarketContentGenerator.Click(shopForCheck1, "ware_md5", 1),
                new PopularMarketContentGenerator.Click(shopForCheck2, "ware_md5_new", 1)
        );
        clicks = popularMarketContentGenerator.filterLatestChecks(clicks);
        assertEquals(1, clicks.size());
    }

    @Test
    public void testRegularCreateTasks() {
        regularMarketContentGenerator.createTasks(List.of(shopForCheck1), 1);
    }

    private void insertShop(long shopId) {
        pgJdbcTemplate.update("INSERT INTO shop(id, is_enabled, ping_enabled, in_prd_base, is_offline) " +
                "VALUES (?, TRUE, TRUE, TRUE, FALSE)", shopId);

    }

    private static Offer offer() {
        Offer res = new Offer();
        res.setDirectUrl("direct-url");
        res.setPrice(BigDecimal.valueOf(1001));
        res.setShopPrice(BigDecimal.valueOf(1001));
        res.setClassifierMagicId("cmId");
        res.setWareMd5("wareMd5");
        return res;
    }
}
