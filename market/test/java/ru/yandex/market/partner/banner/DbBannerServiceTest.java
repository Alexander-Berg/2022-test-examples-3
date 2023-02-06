package ru.yandex.market.partner.banner;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.banner.BannerService;
import ru.yandex.market.core.banner.db.DbBannerService;
import ru.yandex.market.core.banner.model.Banner;
import ru.yandex.market.partner.test.context.FunctionalTest;

/**
 * @author Kirill Lakhtin (klaktin@yandex-team.ru)
 */
@DbUnitDataSet(before = "DbBannerServiceTest.before.csv")
public class DbBannerServiceTest extends FunctionalTest {

    @Autowired
    private DbBannerService bannerService;

    /**
     * Тест проверяет, что {@link BannerService#getShopBannerIds(long)}
     * возвращает значения с указанным SHOP_ID из БД.
     */
    @Test
    public void testGetShopBannersIdsByDatasource() {
        Collection<String> shopBannerIds = bannerService.getShopBannerIds(123L);

        MatcherAssert.assertThat(shopBannerIds, Matchers.containsInAnyOrder("banner_1", "banner_2"));
        MatcherAssert.assertThat(shopBannerIds, Matchers.not(Matchers.containsInAnyOrder("banner_3")));
    }

    /**
     * Тест проверяет, что {@link BannerService#getShopBannerIds()} ()}
     * возвращает все значения из БД и правильно агрегирует их в Map.
     */
    @Test
    public void testGetShopBannersIds() {
        Map<Long, Collection<String>> shopBannerIds = bannerService.getShopBannerIds();

        MatcherAssert.assertThat(shopBannerIds.get(123L), Matchers.containsInAnyOrder("banner_1", "banner_2"));
        MatcherAssert.assertThat(shopBannerIds.get(124L), Matchers.containsInAnyOrder("banner_3"));
    }

    /**
     * Тест проверяет, что {@link BannerService#getPartnerIdToBannersWithDisplayType()} ()}
     * возвращает все значения из БД и правильно агрегирует их в Map.
     */
    @Test
    public void testGetBusinessBannerIds() {
        Map<Long, Collection<Banner>> businessBanners = bannerService.getPartnerIdToBannersWithDisplayType();

        checkBannerIds(businessBanners, 999L, "busban_3", "busban_4");
        checkBannerIds(businessBanners, 998L, "busban_1", "busban_2");
    }

    private void checkBannerIds(Map<Long, Collection<Banner>> businessBanners,
                                long businessId,
                                String... expectedIds
    ) {
        List<String> bannerIds =
                businessBanners.get(businessId).stream().map(Banner::getBannerId).collect(Collectors.toList());
        MatcherAssert.assertThat(bannerIds, Matchers.containsInAnyOrder(expectedIds));
    }
}
