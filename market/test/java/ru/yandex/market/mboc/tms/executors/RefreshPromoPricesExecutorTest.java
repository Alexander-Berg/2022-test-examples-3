package ru.yandex.market.mboc.tms.executors;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.FieldDefinition;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.BuyPromoPrice;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.msku.BuyPromoPriceRepository;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;

/**
 * @author moskovkin@yandex-team.ru
 * @created 09.07.2019
 */
@SuppressWarnings("checkstyle:magicnumber")
public class RefreshPromoPricesExecutorTest extends BaseDbTestClass {
    private static final int SEED = 33;
    private static final EnhancedRandom RANDOM = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
        .randomize(new FieldDefinition<>("id", Long.class, BuyPromoPrice.class), (Supplier<Long>) () -> null)
        .seed(SEED).build();

    private static final int TEST_DATA_COUNT = 100;

    private RefreshPromoPricesExecutor refreshPromoPricesExecutor;

    @Autowired
    private TransactionHelper transactionHelper;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private BuyPromoPriceRepository buyPromoPriceRepository;

    @Before
    public void setup() {
        refreshPromoPricesExecutor = new RefreshPromoPricesExecutor(
            Mockito.mock(JdbcTemplate.class),
            transactionHelper,
            jdbcTemplate,
            "test/path1",
            "test/path2"
        );
    }

    @Test
    public void testTableSwap() {
        List<BuyPromoPrice> newData =
            RANDOM.objects(BuyPromoPrice.class, TEST_DATA_COUNT)
                .collect(Collectors.toList());

        refreshPromoPricesExecutor.replacePrices(newData);
        refreshPromoPricesExecutor.replacePrices(newData);
        refreshPromoPricesExecutor.replacePrices(newData);
    }

    @Test
    public void testReplacePrices() {
        List<BuyPromoPrice> oldData =
            RANDOM.objects(BuyPromoPrice.class, TEST_DATA_COUNT)
                .collect(Collectors.toList());
        buyPromoPriceRepository.save(oldData);

        List<BuyPromoPrice> newData =
            RANDOM.objects(BuyPromoPrice.class, TEST_DATA_COUNT)
                .collect(Collectors.toList());

        refreshPromoPricesExecutor.replacePrices(newData);

        List<BuyPromoPrice> repositoryData = buyPromoPriceRepository.find(BuyPromoPriceRepository.Filter.all());

        Assertions.assertThat(repositoryData).usingElementComparatorIgnoringFields("id")
            .containsOnlyElementsOf(newData);
    }
}
