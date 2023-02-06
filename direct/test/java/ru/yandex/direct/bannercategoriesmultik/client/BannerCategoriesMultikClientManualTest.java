package ru.yandex.direct.bannercategoriesmultik.client;

import java.util.List;

import org.asynchttpclient.DefaultAsyncHttpClient;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.direct.asynchttp.FetcherSettings;
import ru.yandex.direct.asynchttp.ParallelFetcherFactory;
import ru.yandex.direct.bannercategoriesmultik.client.model.BannerCategories;
import ru.yandex.direct.bannercategoriesmultik.client.model.BannerInfo;
import ru.yandex.direct.bannercategoriesmultik.client.model.CategoriesRequest;
import ru.yandex.direct.bannercategoriesmultik.client.model.CategoriesResponse;

import static junit.framework.TestCase.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;

@Ignore
public class BannerCategoriesMultikClientManualTest {

    private BannerCategoriesMultikClient client;

    private static final String TESTING_URL = "https://production.yabs-runtime-models.yandex.net";

    @Before
    public void setUp() {
        client = new BannerCategoriesMultikClient(
                TESTING_URL,
                new ParallelFetcherFactory(new DefaultAsyncHttpClient(), new FetcherSettings()));
    }

    @Test
    public void test() {
        List<BannerInfo> bannerInfo = List.of(
                new BannerInfo("https://market.yandex.ru", "Refrigerator", "So cold!"),
                new BannerInfo("https://market.yandex.ru", "Toast", "Yummy"));
        int categoriesNumber = 1;
        CategoriesResponse response = client.getCategories(new CategoriesRequest(bannerInfo, categoriesNumber));
        assertThat(response).isNotNull();

        List<BannerCategories> categories = response.getBannerCategories();
        assertThat(categories).hasSize(bannerInfo.size());
        assertTrue(categories.stream().allMatch(bc -> bc.getCategoriesByModelIds().size() <= categoriesNumber));
    }
}
