package ru.yandex.market.loyalty.core.service.bundle;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;

import ru.yandex.common.util.IOUtils;
import ru.yandex.market.loyalty.core.service.bundle.strategy.condition.GiftWithPurchaseCondition;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

public class GiftWithPurchaseConditionSerializationTest extends AbstractBundleStrategyTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Value("classpath:sample/gift-with-purchase-condition-old.json")
    private Resource jsonDataResource;

    @Test
    public void shouldDeserializeOldFormatCondition() throws IOException {
        GiftWithPurchaseCondition condition =
                MAPPER.readValue(
                        IOUtils.readInputStream(jsonDataResource.getInputStream()), GiftWithPurchaseCondition.class);

        assertThat(condition.getFeedId(), notNullValue());
        assertThat(condition.getMappings(), notNullValue());
        assertThat(condition.getMappings(), not(empty()));
    }
}
