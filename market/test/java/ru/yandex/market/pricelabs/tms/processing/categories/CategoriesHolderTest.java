package ru.yandex.market.pricelabs.tms.processing.categories;

import java.io.IOException;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.pricelabs.misc.Utils;
import ru.yandex.market.pricelabs.model.NewShopCategory;
import ru.yandex.market.pricelabs.tms.processing.TaskInfo;
import ru.yandex.market.pricelabs.tms.processing.TmsTestUtils;
import ru.yandex.market.pricelabs.tms.processing.offers.ProcessingContext;
import ru.yandex.market.pricelabs.tms.processing.offers.ShopLoopShopState;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.pricelabs.misc.TimingUtils.getInstant;
import static ru.yandex.market.pricelabs.tms.processing.TmsTestUtils.shop;

class CategoriesHolderTest {

    private CategoriesHolder holder;

    @BeforeEach
    void init() {
        var ctx = new ProcessingContext(
                TmsTestUtils.defaultOffersArg(),
                shop(1),
                getInstant(),
                601923,
                new ShopLoopShopState(),
                TaskInfo.UNKNOWN
        );
        holder = new CategoriesHolder(ctx);
    }

    @Test
    void testStackOverflowCategories() throws IOException {

        ObjectMapper objectMapper = new ObjectMapper();
        try (GZIPInputStream stream = new GZIPInputStream(
                Utils.getResourceStream("tms/processing/categories/sample.json.gz"))) {
            Stream.of(objectMapper.readValue(stream, NewShopCategory[].class))
                    .forEach(holder::addNewCategory);
            holder.matchTrees(map -> assertEquals(5805, map.size())).flush();
        }
    }

}
