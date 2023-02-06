package ru.yandex.market.gutgin.tms.pipeline.good.taskaction.csku;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.gutgin.tms.config.pipeline.SkuDuplicateConfig;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.SkuDuplicates;

import java.sql.Timestamp;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(
        classes = {
                SkuDuplicateConfig.class,
        })
@TestPropertySource(properties = {
        "gg.yt.arnold.url=",
        "gg.yt.hahn.url=hahn.yt.yandex.net",
        "gg.yt.token=token",
        "mbo.yt.http.proxy=",
        "yt.sku.duplicates.path=//home/market/testing/ir/gutgin/sku_duplicates"
})
public class SkuDuplicatesRunnerTest {
    @Autowired
    private SkuDuplicateService skuDuplicateService;

    @Ignore
    @Test
    public void SkuDuplicatesService() {
        List<SkuDuplicates> skuDuplicates = List.of(
                new SkuDuplicates(0L, 0L, 1L, 1L, new Timestamp(System.currentTimeMillis())),
                new SkuDuplicates(1L, 1L, 2L, 1L, new Timestamp(System.currentTimeMillis())),
                new SkuDuplicates(2L, 2L, 0L, 1L, new Timestamp(System.currentTimeMillis())));
        assertThat(skuDuplicateService.writeToYt(skuDuplicates)).isTrue();
    }
}
