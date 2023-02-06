package ru.yandex.market.wms.common.spring.dao;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.dao.entity.SkuId;
import ru.yandex.market.wms.common.spring.dao.implementation.SkuUrlDAO;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;

public class SkuUrlDAOTest extends IntegrationTest {

    @Autowired
    private SkuUrlDAO skuUrlDao;

    @Test
    @DatabaseSetup("/db/dao/sku-url/before.xml")
    @ExpectedDatabase(value = "/db/dao/sku-url/before.xml", assertionMode = NON_STRICT)
    public void getUrlsForSkus() {
        SkuId firstId = new SkuId("465852", "ROV0000000000000001456");
        SkuId secondId = new SkuId("465852", "ROV0000000000000001457");
        Set<SkuId> skuIds = new HashSet<>();
        skuIds.add(firstId);
        skuIds.add(secondId);
        Map<SkuId, Set<String>> urlsForSkus = skuUrlDao.getUrlsForSkus(skuIds);
        assertions.assertThat(urlsForSkus).hasSize(2);
        Set<String> firstUrls = urlsForSkus.get(firstId);
        Set<String> secondUrls = urlsForSkus.get(secondId);
        assertions.assertThat(firstUrls).isNotNull();
        assertions.assertThat(secondUrls).isNotNull();
        assertions.assertThat(firstUrls)
                .containsExactlyInAnyOrder("https://beru.ru/product/1723947007",
                        "https://market.yandex.ru/product/1723947007");
        assertions.assertThat(secondUrls)
                .containsExactlyInAnyOrder("https://market.yandex.ru/product/1723947008");
    }
}
