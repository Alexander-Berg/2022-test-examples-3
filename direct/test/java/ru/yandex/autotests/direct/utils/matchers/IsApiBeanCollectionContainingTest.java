package ru.yandex.autotests.direct.utils.matchers;

import org.junit.BeforeClass;
import org.junit.Test;
import ru.yandex.autotests.directapi.common.api45.BannerInfo;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by mariabye on 15.09.14.
 */
public class IsApiBeanCollectionContainingTest {
    static BannerInfo[] banners = new BannerInfo[2];
    static BannerInfo banner1 = new BannerInfo();
    static BannerInfo banner2 = new BannerInfo();
    static BannerInfo banner3 = new BannerInfo();

    @BeforeClass
    public static void initData(){
        banner1.setBannerID(1l);
        banner2.setBannerID(2l);
        banner3.setBannerID(3l);
    }

    @Test
    public void collectionContainsItem(){
        banners = new BannerInfo[]{banner1,banner2};

        assertThat(banners,
                IsApiBeanCollectionContaining.containItems(BeanEquals.beanEquals(banner1)));

    }

    @Test
    public void collectionContainsItems(){
        banners = new BannerInfo[]{banner1,banner2};

        assertThat(banners,
                IsApiBeanCollectionContaining.containItems(
                        BeanEquals.beanEquals(banner2),BeanEquals.beanEquals(banner1)));

    }

    @Test
    public void collectionHasItems(){
        banners = new BannerInfo[]{banner1,banner2};

        assertThat(banners,
                IsApiBeanCollectionContaining.containItems(
                        BeanEquals.beanEquals(banner2),BeanEquals.beanEquals(banner1)));

    }

    @Test
    public void collectionHasSortedItems(){
        banners = new BannerInfo[]{banner1,banner2};

        assertThat(banners,
                IsApiBeanCollectionContaining.hasSameOrderItems(
                        BeanEquals.beanEquals(banner1),BeanEquals.beanEquals(banner2)));

    }

    @Test
    public void arrayHasSortedItems(){
        banners = new BannerInfo[]{banner1,banner2};

        assertThat(banners,
                IsApiBeanCollectionContaining.hasSameOrderItems(
                        BeanEquals.beanEquals(banner1),BeanEquals.beanEquals(banner2)));

    }

}
