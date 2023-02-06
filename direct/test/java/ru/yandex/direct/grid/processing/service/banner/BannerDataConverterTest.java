package ru.yandex.direct.grid.processing.service.banner;

import java.lang.reflect.Field;
import java.util.Set;

import one.util.streamex.StreamEx;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.reflections.ReflectionUtils;

import ru.yandex.direct.core.entity.banner.model.BannerPrice;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.grid.core.entity.banner.model.GdiBanner;
import ru.yandex.direct.grid.model.entity.adgroup.GdAdGroupType;
import ru.yandex.direct.grid.processing.model.banner.GdAdFilter;
import ru.yandex.direct.grid.processing.model.banner.GdAdPrice;
import ru.yandex.direct.grid.processing.model.banner.GdAdPriceCurrency;
import ru.yandex.direct.grid.processing.model.banner.GdAdPricePrefix;
import ru.yandex.direct.grid.processing.model.group.GdCpmBannerAdGroup;
import ru.yandex.direct.grid.processing.model.group.GdCpmGeoproductAdGroup;

import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.TEN;
import static java.math.RoundingMode.HALF_UP;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.banner.model.BannerPricesCurrency.RUB;
import static ru.yandex.direct.core.entity.banner.model.BannerPricesPrefix.FROM;
import static ru.yandex.direct.core.entity.creative.model.CreativeType.CPM_VIDEO_CREATIVE;
import static ru.yandex.direct.grid.processing.model.banner.GdAdType.CPM_GEOPRODUCT;
import static ru.yandex.direct.grid.processing.model.banner.GdAdType.CPM_VIDEO;
import static ru.yandex.direct.grid.processing.service.banner.BannerDataConverter.getGdAdType;
import static ru.yandex.direct.grid.processing.service.banner.BannerDataConverter.toGdBannerPrice;

public class BannerDataConverterTest {

    private static BannerPrice defaultBannerPrice() {
        return new BannerPrice()
                .withPrice(ONE)
                .withPriceOld(TEN)
                .withPrefix(FROM)
                .withCurrency(RUB);
    }

    private static GdAdPrice defaultGdAdPrice() {
        return new GdAdPrice()
                .withPrice(ONE.setScale(2, HALF_UP).toString())
                .withPriceOld(TEN.setScale(2, HALF_UP).toString())
                .withPrefix(GdAdPricePrefix.FROM)
                .withCurrency(GdAdPriceCurrency.RUB);
    }

    @Test
    public void toGdBannerPrice_null_returnsNull() {
        assertThat(toGdBannerPrice(null), nullValue());
    }

    @Test
    public void toGdBannerPrice_allFields_converted() {
        BannerPrice price = defaultBannerPrice();
        assertThat(toGdBannerPrice(price), beanDiffer(defaultGdAdPrice()));
    }

    @Test
    public void toGdBannerPrice_prefixNull_converted() {
        BannerPrice price = defaultBannerPrice();
        assertThat(toGdBannerPrice(price.withPrefix(null)), beanDiffer(defaultGdAdPrice().withPrefix(null)));
    }

    @Test
    public void toGdBannerPrice_priceOldNull_converted() {
        BannerPrice price = defaultBannerPrice();
        assertThat(toGdBannerPrice(price.withPriceOld(null)), beanDiffer(defaultGdAdPrice().withPriceOld(null)));
    }

    @Test
    public void getGdAdType_CpmGeoproductAdGroup_CpmGeoproductTypeReturned() {
        GdCpmGeoproductAdGroup adGroup = new GdCpmGeoproductAdGroup().withType(GdAdGroupType.CPM_GEOPRODUCT);
        assertThat(getGdAdType(new GdiBanner(), adGroup), is(CPM_GEOPRODUCT));
    }

    @Test
    public void getGdAdType_BannerWithCpmVideoCreative_CpmVideoTypeReturned() {
        GdiBanner banner = new GdiBanner().withTypedCreative(new Creative().withType(CPM_VIDEO_CREATIVE));
        assertThat(getGdAdType(banner, new GdCpmBannerAdGroup()), is(CPM_VIDEO));
    }

    /**
     * Проверяем что в модель GdAdFilter не было добавленно новых полей
     * <p>
     * Если поля были добавленны и по ним происходит фильтрация групп в коде, то нужно не забыть их учесть в методе
     * {@link ru.yandex.direct.grid.processing.service.banner#hasAnyCodeFilter}
     */
    @Test
    public void testForNewFilterFields() {
        String[] expectFieldsName = {"adIdIn", "adIdNotIn", "adIdContainsAny", "campaignIdIn", "isTouch",
                "adGroupIdIn", "typeIn", "internalAdTemplateIdIn", "archived", "titleContains", "titleOrBodyContains",
                "titleIn", "titleNotContains", "titleNotIn", "internalAdTitleContains", "internalAdTitleIn",
                "internalAdTitleNotContains", "internalAdTitleNotIn", "titleExtensionContains", "titleExtensionIn",
                "titleExtensionNotContains", "titleExtensionNotIn", "bodyContains", "bodyIn", "bodyNotContains",
                "bodyNotIn", "hrefContains", "hrefIn", "hrefNotContains", "hrefNotIn", "primaryStatusContains",
                "vcardExists", "imageExists", "sitelinksExists", "turbolandingsExist", "stats", "goalStats",
                "recommendations", "reasonsContainSome", "exportIdIn", "exportIdNotIn", "exportIdContainsAny"};

        @SuppressWarnings("unchecked")
        Set<Field> fields = ReflectionUtils.getAllFields(GdAdFilter.class);

        Set<String> actualFieldsName = StreamEx.of(fields)
                .map(Field::getName)
                .filter(fieldName -> !Character.isUpperCase(fieldName.charAt(0)))
                .toSet();

        Assertions.assertThat(actualFieldsName)
                .as("Нет новых полей в GdAdFilter")
                .containsExactlyInAnyOrder(expectFieldsName);
    }
}
