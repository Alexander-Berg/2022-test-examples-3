package ru.yandex.market.checkout.helpers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.context.WebApplicationContext;

import ru.yandex.market.checkout.common.WebTestHelper;
import ru.yandex.market.checkout.helpers.utils.MockMvcAware;
import ru.yandex.market.checkout.util.serialization.TestSerializationService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

/**
 * @author mmetlov
 */
@WebTestHelper
public class FreezeHelper extends MockMvcAware {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public FreezeHelper(WebApplicationContext webApplicationContext,
                        TestSerializationService testSerializationService) {
        super(webApplicationContext, testSerializationService);
    }

    public void assertFreezeCount(long orderId, Integer... expectedFreezeCount) {
        List<Integer> freezeCount = jdbcTemplate.queryForList("SELECT fit_freezed FROM order_item WHERE order_id = ?",
                Integer.class, orderId);
        assertThat(freezeCount, containsInAnyOrder(expectedFreezeCount));
    }
}
