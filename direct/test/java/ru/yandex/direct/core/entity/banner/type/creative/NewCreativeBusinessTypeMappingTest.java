package ru.yandex.direct.core.entity.banner.type.creative;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.feed.model.BusinessType;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.banner.type.creative.BannerWithCreativeConstants.FEEDS_CREATIVES_COMPATIBILITY;

@RunWith(Parameterized.class)
public class NewCreativeBusinessTypeMappingTest {

    @Parameterized.Parameter
    public BusinessType feedBusinessType;

    @Parameterized.Parameters(name = "{0}")
    public static Iterable<BusinessType> data() {
        return Arrays.asList(BusinessType.values());
    }

    @Test
    public void testMapping() {
        assertThat(FEEDS_CREATIVES_COMPATIBILITY.get(feedBusinessType), notNullValue());
    }
}
