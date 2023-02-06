package ru.yandex.direct.bannercategoriesmultik.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.direct.bannercategoriesmultik.client.model.BannerCategories;
import ru.yandex.direct.bannercategoriesmultik.client.model.BannerInfo;
import ru.yandex.direct.bannercategoriesmultik.client.model.CategoriesRequest;
import ru.yandex.direct.bannercategoriesmultik.client.model.CategoriesResponse;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;


public class BannerCategoriesMultikClientTest {

    private BannerCategoriesMultikClient client;

    @Rule
    public final MockedMultik mockedMultik = new MockedMultik();

    @Before
    public void setUp() {
        client = mockedMultik.createClient();
    }

    @Test
    public void test() {
        List<BannerInfo> bannerInfo = List.of(
                new BannerInfo("https://market.yandex.ru", "Refrigerator", "So cold!"),
                new BannerInfo("https://market.yandex.ru", "Toast", "Yummy"));
        int categoriesNumber = 2;
        CategoriesResponse response = client.getCategories(new CategoriesRequest(bannerInfo, categoriesNumber));

        Map<Integer, List<Long>> map1 = new HashMap<>();
        Map<Integer, List<Long>> map2 = new HashMap<>();
        map1.put(0, List.of(200001097L, 200014809L));
        map2.put(0, List.of(200001097L, 200014809L));
        List<BannerCategories> expected = List.of(new BannerCategories(map1), new BannerCategories(map2));
        List<BannerCategories> got = response.getBannerCategories();
        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i).getCategoriesByModelIds(), got.get(i).getCategoriesByModelIds());
        }
        assertTrue(got.stream().allMatch(bc -> bc.getCategoriesByModelIds().size() <= categoriesNumber));
    }

}
