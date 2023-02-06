package ru.yandex.direct.api.v5.entity.ads.converter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.yandex.direct.api.v5.ads.MobileAppAdFeatureGetItem;
import com.yandex.direct.api.v5.ads.MobileAppFeatureEnum;
import com.yandex.direct.api.v5.general.YesNoEnum;
import com.yandex.direct.api.v5.general.YesNoUnknownEnum;
import one.util.streamex.StreamEx;
import org.assertj.core.groups.Tuple;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.banner.model.NewReflectedAttribute;
import ru.yandex.direct.core.entity.mobilecontent.model.MobileContent;
import ru.yandex.direct.core.entity.mobilecontent.model.MobileContentExternalWorldMoney;
import ru.yandex.direct.core.entity.mobilecontent.model.StoreActionForPrices;
import ru.yandex.direct.core.entity.mobilecontent.model.StoreCountry;
import ru.yandex.direct.currency.CurrencyCode;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.junit.runners.Parameterized.Parameter;
import static org.junit.runners.Parameterized.Parameters;
import static ru.yandex.direct.api.v5.entity.ads.converter.MobileAppFeatureConverter.convertMobileAppFeatures;
import static ru.yandex.direct.core.entity.mobilecontent.util.MobileContentUtil.getExternalWorldMoney;

@RunWith(Parameterized.class)
public class MobileAppFeaturesConverterTest {

    @Parameter
    public String desc;

    @Parameter(1)
    public Map<NewReflectedAttribute, Boolean> reflectedAttributes;

    @Parameter(2)
    public MobileContent mobileContent;

    @Parameter(3)
    public List<Tuple> expectedFeatures;

    @Parameters(name = "{0}")
    public static Object[][] getParameters() {
        return new Object[][]{
                {"empty reflected attributes, no mobile content", emptyMap(), null,
                        asList(tuple(MobileAppFeatureEnum.CUSTOMER_RATING, YesNoEnum.NO, YesNoUnknownEnum.UNKNOWN),
                                tuple(MobileAppFeatureEnum.ICON, YesNoEnum.NO, YesNoUnknownEnum.UNKNOWN),
                                tuple(MobileAppFeatureEnum.PRICE, YesNoEnum.NO, YesNoUnknownEnum.UNKNOWN),
                                tuple(MobileAppFeatureEnum.RATINGS, YesNoEnum.NO, YesNoUnknownEnum.UNKNOWN))},

                {"not empty reflected attributes, no mobile content", buildReflectedAttributes(), null,
                        asList(tuple(MobileAppFeatureEnum.CUSTOMER_RATING, YesNoEnum.YES, YesNoUnknownEnum.UNKNOWN),
                                tuple(MobileAppFeatureEnum.ICON, YesNoEnum.YES, YesNoUnknownEnum.UNKNOWN),
                                tuple(MobileAppFeatureEnum.PRICE, YesNoEnum.YES, YesNoUnknownEnum.UNKNOWN),
                                tuple(MobileAppFeatureEnum.RATINGS, YesNoEnum.YES, YesNoUnknownEnum.UNKNOWN))},

                {"not empty reflected attributes (not all true), no mobile content",
                        buildReflectedAttributesWithRatingVotesDisabled(),
                        null,
                        asList(tuple(MobileAppFeatureEnum.CUSTOMER_RATING, YesNoEnum.YES, YesNoUnknownEnum.UNKNOWN),
                                tuple(MobileAppFeatureEnum.ICON, YesNoEnum.YES, YesNoUnknownEnum.UNKNOWN),
                                tuple(MobileAppFeatureEnum.PRICE, YesNoEnum.YES, YesNoUnknownEnum.UNKNOWN),
                                tuple(MobileAppFeatureEnum.RATINGS, YesNoEnum.NO, YesNoUnknownEnum.UNKNOWN))},

                {"not empty reflected attributes, mobile content modify time is null", buildReflectedAttributes(),
                        new MobileContent().withModifyTime(null),
                        asList(tuple(MobileAppFeatureEnum.CUSTOMER_RATING, YesNoEnum.YES, YesNoUnknownEnum.UNKNOWN),
                                tuple(MobileAppFeatureEnum.ICON, YesNoEnum.YES, YesNoUnknownEnum.UNKNOWN),
                                tuple(MobileAppFeatureEnum.PRICE, YesNoEnum.YES, YesNoUnknownEnum.UNKNOWN),
                                tuple(MobileAppFeatureEnum.RATINGS, YesNoEnum.YES, YesNoUnknownEnum.UNKNOWN))},

                {"not empty reflected attributes, mobile content not filled", buildReflectedAttributes(),
                        new MobileContent().withModifyTime(LocalDateTime.now()),
                        asList(tuple(MobileAppFeatureEnum.CUSTOMER_RATING, YesNoEnum.YES, YesNoUnknownEnum.NO),
                                tuple(MobileAppFeatureEnum.ICON, YesNoEnum.YES, YesNoUnknownEnum.NO),
                                tuple(MobileAppFeatureEnum.PRICE, YesNoEnum.YES, YesNoUnknownEnum.NO),
                                tuple(MobileAppFeatureEnum.RATINGS, YesNoEnum.YES, YesNoUnknownEnum.NO))},

                {"not empty reflected attributes, mobile content fetched and filled", buildReflectedAttributes(),
                        new MobileContent()
                                .withModifyTime(LocalDateTime.now())
                                .withIconHash("iconHash")
                                .withRating(BigDecimal.TEN)
                                .withPrices(
                                        ImmutableMap.<String, Map<StoreActionForPrices, MobileContentExternalWorldMoney>>builder()
                                                .put(StoreCountry.RU.toString(),
                                                        ImmutableMap.<StoreActionForPrices, MobileContentExternalWorldMoney>builder()
                                                                .put(StoreActionForPrices.update,
                                                                        getExternalWorldMoney("1.23", CurrencyCode.RUB))
                                                                .put(StoreActionForPrices.open,
                                                                        getExternalWorldMoney("1.23", CurrencyCode.RUB))
                                                                .put(StoreActionForPrices.buy,
                                                                        getExternalWorldMoney("1.23", CurrencyCode.RUB))
                                                                .put(StoreActionForPrices.more,
                                                                        getExternalWorldMoney("1.23", CurrencyCode.RUB))
                                                                .put(StoreActionForPrices.download,
                                                                        getExternalWorldMoney("1.23", CurrencyCode.RUB))
                                                                .put(StoreActionForPrices.install,
                                                                        getExternalWorldMoney("1.23", CurrencyCode.RUB))
                                                                .put(StoreActionForPrices.play,
                                                                        getExternalWorldMoney("1.23", CurrencyCode.RUB))
                                                                .put(StoreActionForPrices.get,
                                                                        getExternalWorldMoney("1.23", CurrencyCode.RUB))
                                                                .build())
                                                .build())
                                .withRatingVotes(5L),
                        asList(tuple(MobileAppFeatureEnum.CUSTOMER_RATING, YesNoEnum.YES, YesNoUnknownEnum.YES),
                                tuple(MobileAppFeatureEnum.ICON, YesNoEnum.YES, YesNoUnknownEnum.YES),
                                tuple(MobileAppFeatureEnum.PRICE, YesNoEnum.YES, YesNoUnknownEnum.YES),
                                tuple(MobileAppFeatureEnum.RATINGS, YesNoEnum.YES, YesNoUnknownEnum.YES))},
        };
    }

    private static Map<NewReflectedAttribute, Boolean> buildReflectedAttributesWithRatingVotesDisabled() {
        Map<NewReflectedAttribute, Boolean> reflectedAttributes = buildReflectedAttributes();
        reflectedAttributes.put(NewReflectedAttribute.RATING_VOTES, false);
        return reflectedAttributes;
    }

    private static Map<NewReflectedAttribute, Boolean> buildReflectedAttributes() {
        return StreamEx.of(NewReflectedAttribute.values()).toMap(v -> true);
    }

    @Test
    public void test() {
        Collection<MobileAppAdFeatureGetItem> actual = convertMobileAppFeatures(reflectedAttributes, mobileContent);
        assertThat(actual).extracting("feature", "enabled", "isAvailable").containsExactlyElementsOf(expectedFeatures);
    }

}
