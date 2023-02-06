package ru.yandex.direct.bsexport.testing.data;

import ru.yandex.adv.direct.expression.TargetingExpression;
import ru.yandex.adv.direct.expression.TargetingExpressionAtom;
import ru.yandex.adv.direct.expression.keywords.KeywordEnum;
import ru.yandex.adv.direct.expression.operations.OperationEnum;

import static ru.yandex.direct.bsexport.util.QueryComposer.disjunctionBuilder;

public class TestTargetingExpression {

    public static final TargetingExpressionAtom NETWORK_ID_EQUAL_2 = TargetingExpressionAtom.newBuilder()
            .setKeyword(KeywordEnum.NetworkId)
            .setOperation(OperationEnum.Equal)
            .setValue("2")
            .build();

    public static final TargetingExpressionAtom UNIQ_ID_LIKE_1 = TargetingExpressionAtom.newBuilder()
            .setKeyword(KeywordEnum.UniqId)
            .setOperation(OperationEnum.Like)
            .setValue("1644552271560853763")
            .build();

    public static final TargetingExpressionAtom UNIQ_ID_LIKE_2 = TargetingExpressionAtom.newBuilder()
            .setKeyword(KeywordEnum.UniqId)
            .setOperation(OperationEnum.Like)
            .setValue("286651621560892988")
            .build();

    public static final TargetingExpressionAtom UNIQ_ID_LIKE_3 = TargetingExpressionAtom.newBuilder()
            .setKeyword(KeywordEnum.UniqId)
            .setOperation(OperationEnum.Like)
            .setValue("4381022591554821458")
            .build();

    public static final TargetingExpressionAtom UNIQ_ID_LIKE_4 = TargetingExpressionAtom.newBuilder()
            .setKeyword(KeywordEnum.UniqId)
            .setOperation(OperationEnum.Like)
            .setValue("%3")
            .build();

    public static final TargetingExpressionAtom UNIQ_ID_NOT_LIKE_1 = TargetingExpressionAtom.newBuilder()
            .setKeyword(KeywordEnum.UniqId)
            .setOperation(OperationEnum.NotLike)
            .setValue("1407985211487344435")
            .build();

    public static final TargetingExpressionAtom BRAND_SAFETY_CATEGORIES_NOT_EQUAL_1 = TargetingExpressionAtom
            .newBuilder()
            .setKeyword(KeywordEnum.BrandSafetyCategories)
            .setOperation(OperationEnum.NotEqual)
            .setValue("1")
            .build();

    public static final TargetingExpressionAtom BRAND_SAFETY_CATEGORIES_NOT_EQUAL_4 = TargetingExpressionAtom
            .newBuilder()
            .setKeyword(KeywordEnum.BrandSafetyCategories)
            .setOperation(OperationEnum.NotEqual)
            .setValue("4")
            .build();

    public static final TargetingExpressionAtom BRAND_SAFETY_CATEGORIES_NOT_EQUAL_16 = TargetingExpressionAtom
            .newBuilder()
            .setKeyword(KeywordEnum.BrandSafetyCategories)
            .setOperation(OperationEnum.NotEqual)
            .setValue("16")
            .build();

    public static final TargetingExpressionAtom BRAND_SAFETY_CATEGORIES_NOT_EQUAL_128 = TargetingExpressionAtom
            .newBuilder()
            .setKeyword(KeywordEnum.BrandSafetyCategories)
            .setOperation(OperationEnum.NotEqual)
            .setValue("128")
            .build();

    public static final TargetingExpressionAtom BRAND_SAFETY_CATEGORIES_NOT_EQUAL_256 = TargetingExpressionAtom
            .newBuilder()
            .setKeyword(KeywordEnum.BrandSafetyCategories)
            .setOperation(OperationEnum.NotEqual)
            .setValue("256")
            .build();

    public static final TargetingExpressionAtom BRAND_SAFETY_CATEGORIES_NOT_EQUAL_1024 = TargetingExpressionAtom
            .newBuilder()
            .setKeyword(KeywordEnum.BrandSafetyCategories)
            .setOperation(OperationEnum.NotEqual)
            .setValue("1024")
            .build();

    public static final TargetingExpressionAtom BRAND_SAFETY_CATEGORIES_NOT_EQUAL_131072 = TargetingExpressionAtom
            .newBuilder()
            .setKeyword(KeywordEnum.BrandSafetyCategories)
            .setOperation(OperationEnum.NotEqual)
            .setValue("131072")
            .build();

    public static final TargetingExpressionAtom SERACH_ANTITARGETING_0 = TargetingExpressionAtom.newBuilder()
            .setKeyword(KeywordEnum.SearchAntitargeting)
            .setOperation(OperationEnum.Equal)
            .setValue("0")
            .build();

    public static final TargetingExpressionAtom DEVICE_IS_MOBILE_0 = TargetingExpressionAtom.newBuilder()
            .setKeyword(KeywordEnum.DeviceIsMobile)
            .setOperation(OperationEnum.Equal)
            .setValue("0")
            .build();

    public static final TargetingExpressionAtom DEVICE_IS_TABLET_0 = TargetingExpressionAtom.newBuilder()
            .setKeyword(KeywordEnum.DeviceIsTablet)
            .setOperation(OperationEnum.Equal)
            .setValue("0")
            .build();

    public static final TargetingExpressionAtom DEVICE_IS_TOUCH_0 = TargetingExpressionAtom.newBuilder()
            .setKeyword(KeywordEnum.DeviceIsTouch)
            .setOperation(OperationEnum.Equal)
            .setValue("0")
            .build();

    public static final TargetingExpressionAtom INSTALLED_YA_SOFT_2 = TargetingExpressionAtom.newBuilder()
            .setKeyword(KeywordEnum.InstalledYasoft)
            .setOperation(OperationEnum.Equal)
            .setValue("2")
            .build();

    public static final TargetingExpressionAtom INSTALLED_YA_SOFT_3 = TargetingExpressionAtom.newBuilder()
            .setKeyword(KeywordEnum.InstalledYasoft)
            .setOperation(OperationEnum.Equal)
            .setValue("3")
            .build();

    public static final TargetingExpressionAtom INSTALLED_YA_SOFT_4 = TargetingExpressionAtom.newBuilder()
            .setKeyword(KeywordEnum.InstalledYasoft)
            .setOperation(OperationEnum.Equal)
            .setValue("4")
            .build();

    public static final TargetingExpressionAtom NOT_INSTALLED_YA_SOFT_1 = TargetingExpressionAtom.newBuilder()
            .setKeyword(KeywordEnum.InstalledYasoft)
            .setOperation(OperationEnum.NotEqual)
            .setValue("1")
            .build();

    public static final TargetingExpressionAtom LANG_RU = TargetingExpressionAtom.newBuilder()
            .setKeyword(KeywordEnum.Lang)
            .setOperation(OperationEnum.IcaseMatch)
            .setValue("ru")
            .build();

    public static final TargetingExpressionAtom OPTIONS_NOT_CHROME_NEW_TAB = TargetingExpressionAtom.newBuilder()
            .setKeyword(KeywordEnum.Options)
            .setOperation(OperationEnum.IcaseNotMatch)
            .setValue("chromenewtab")
            .build();

    public static final TargetingExpressionAtom REFERER_NOT_2233627 = TargetingExpressionAtom.newBuilder()
            .setKeyword(KeywordEnum.Referer)
            .setOperation(OperationEnum.NotLike)
            .setValue("%clid=2233627%")
            .build();

    public static final TargetingExpressionAtom REFERER_NOT_2233628 = TargetingExpressionAtom.newBuilder()
            .setKeyword(KeywordEnum.Referer)
            .setOperation(OperationEnum.NotLike)
            .setValue("%clid=2233628%")
            .build();

    public static final TargetingExpressionAtom REFERER_NOT_2235263 = TargetingExpressionAtom.newBuilder()
            .setKeyword(KeywordEnum.Referer)
            .setOperation(OperationEnum.NotLike)
            .setValue("%clid=2235263%")
            .build();

    public static final TargetingExpressionAtom REFERER_NOT_2235264 = TargetingExpressionAtom.newBuilder()
            .setKeyword(KeywordEnum.Referer)
            .setOperation(OperationEnum.NotLike)
            .setValue("%clid=2235264%")
            .build();

    public static final TargetingExpressionAtom REFERER_NOT_2236846 = TargetingExpressionAtom.newBuilder()
            .setKeyword(KeywordEnum.Referer)
            .setOperation(OperationEnum.NotLike)
            .setValue("%clid=2236846%")
            .build();

    public static final TargetingExpressionAtom STAT_ID_NOT_2233627 = TargetingExpressionAtom.newBuilder()
            .setKeyword(KeywordEnum.StatId)
            .setOperation(OperationEnum.NotEqual)
            .setValue("2233627")
            .build();

    public static final TargetingExpressionAtom STAT_ID_NOT_2233628 = TargetingExpressionAtom.newBuilder()
            .setKeyword(KeywordEnum.StatId)
            .setOperation(OperationEnum.NotEqual)
            .setValue("2233628")
            .build();

    public static final TargetingExpressionAtom STAT_ID_NOT_2235263 = TargetingExpressionAtom.newBuilder()
            .setKeyword(KeywordEnum.StatId)
            .setOperation(OperationEnum.NotEqual)
            .setValue("2235263")
            .build();

    public static final TargetingExpressionAtom STAT_ID_NOT_2235264 = TargetingExpressionAtom.newBuilder()
            .setKeyword(KeywordEnum.StatId)
            .setOperation(OperationEnum.NotEqual)
            .setValue("2235264")
            .build();

    public static final TargetingExpressionAtom STAT_ID_NOT_2236846 = TargetingExpressionAtom.newBuilder()
            .setKeyword(KeywordEnum.StatId)
            .setOperation(OperationEnum.NotEqual)
            .setValue("2236846")
            .build();

    public static final TargetingExpressionAtom TEST_IDS_233280 = TargetingExpressionAtom.newBuilder()
            .setKeyword(KeywordEnum.TestIds)
            .setOperation(OperationEnum.Equal)
            .setValue("233280")
            .build();

    public static final TargetingExpressionAtom TEST_IDS_NOT_234888 = TargetingExpressionAtom.newBuilder()
            .setKeyword(KeywordEnum.TestIds)
            .setOperation(OperationEnum.NotEqual)
            .setValue("234888")
            .build();

    public static final TargetingExpressionAtom TEST_IDS_NOT_234889 = TargetingExpressionAtom.newBuilder()
            .setKeyword(KeywordEnum.TestIds)
            .setOperation(OperationEnum.NotEqual)
            .setValue("234889")
            .build();

    public static final TargetingExpressionAtom YANDEXUID_AGE_GREATER = TargetingExpressionAtom.newBuilder()
            .setKeyword(KeywordEnum.YandexuidAge)
            .setOperation(OperationEnum.GreaterOrEqual)
            .setValue("3600")
            .build();

    public static final TargetingExpressionAtom YANDEXUID_AGE_LESS = TargetingExpressionAtom.newBuilder()
            .setKeyword(KeywordEnum.YandexuidAge)
            .setOperation(OperationEnum.LessOrEqual)
            .setValue("7200")
            .build();

    public static final TargetingExpressionAtom CLOUDNESS_NOT_EQUAL_25 = TargetingExpressionAtom.newBuilder()
            .setKeyword(KeywordEnum.Cloudness)
            .setOperation(OperationEnum.NotEqual)
            .setValue("25")
            .build();

    public static final TargetingExpressionAtom TEMPERATURE_EQUAL_10 = TargetingExpressionAtom.newBuilder()
            .setKeyword(KeywordEnum.Temperature)
            .setOperation(OperationEnum.Equal)
            .setValue("10")
            .build();

    public static final TargetingExpression CONTEXT_TARGETING_EXPRESSION_1 = TargetingExpression.newBuilder()
            .addAND(disjunctionBuilder(NETWORK_ID_EQUAL_2))
            .addAND(disjunctionBuilder(UNIQ_ID_LIKE_1)
                    .addOR(UNIQ_ID_LIKE_2)
                    .addOR(UNIQ_ID_LIKE_3)
                    .addOR(UNIQ_ID_LIKE_4))
            .build();

    public static final TargetingExpression CONTEXT_TARGETING_EXPRESSION_2 = TargetingExpression.newBuilder()
            .addAND(disjunctionBuilder(NETWORK_ID_EQUAL_2))
            .addAND(disjunctionBuilder(SERACH_ANTITARGETING_0))
            .addAND(disjunctionBuilder(UNIQ_ID_NOT_LIKE_1))
            .build();

    public static final TargetingExpression CONTEXT_TARGETING_EXPRESSION_3 = TargetingExpression.newBuilder()
            .addAND(disjunctionBuilder(DEVICE_IS_MOBILE_0))
            .addAND(disjunctionBuilder(DEVICE_IS_TABLET_0))
            .addAND(disjunctionBuilder(DEVICE_IS_TOUCH_0))
            .addAND(disjunctionBuilder(INSTALLED_YA_SOFT_2)
                    .addOR(INSTALLED_YA_SOFT_3)
                    .addOR(INSTALLED_YA_SOFT_4))
            .addAND(disjunctionBuilder(NOT_INSTALLED_YA_SOFT_1))
            .addAND(disjunctionBuilder(LANG_RU))
            .addAND(disjunctionBuilder(OPTIONS_NOT_CHROME_NEW_TAB))
            .addAND(disjunctionBuilder(REFERER_NOT_2233627))
            .addAND(disjunctionBuilder(REFERER_NOT_2233628))
            .addAND(disjunctionBuilder(REFERER_NOT_2235263))
            .addAND(disjunctionBuilder(REFERER_NOT_2235264))
            .addAND(disjunctionBuilder(REFERER_NOT_2236846))
            .addAND(disjunctionBuilder(STAT_ID_NOT_2233627))
            .addAND(disjunctionBuilder(STAT_ID_NOT_2233628))
            .addAND(disjunctionBuilder(STAT_ID_NOT_2235263))
            .addAND(disjunctionBuilder(STAT_ID_NOT_2235264))
            .addAND(disjunctionBuilder(STAT_ID_NOT_2236846))
            .addAND(disjunctionBuilder(TEST_IDS_233280))
            .addAND(disjunctionBuilder(TEST_IDS_NOT_234888))
            .addAND(disjunctionBuilder(TEST_IDS_NOT_234889))
            .addAND(disjunctionBuilder(YANDEXUID_AGE_GREATER))
            .addAND(disjunctionBuilder(YANDEXUID_AGE_LESS))
            .build();

    public static final TargetingExpression ORDER_TARGETING_EXPRESSION_1 = TargetingExpression.newBuilder()
            .addAND(disjunctionBuilder(BRAND_SAFETY_CATEGORIES_NOT_EQUAL_1))
            .addAND(disjunctionBuilder(BRAND_SAFETY_CATEGORIES_NOT_EQUAL_4))
            .addAND(disjunctionBuilder(BRAND_SAFETY_CATEGORIES_NOT_EQUAL_16))
            .addAND(disjunctionBuilder(BRAND_SAFETY_CATEGORIES_NOT_EQUAL_128))
            .addAND(disjunctionBuilder(BRAND_SAFETY_CATEGORIES_NOT_EQUAL_256))
            .addAND(disjunctionBuilder(BRAND_SAFETY_CATEGORIES_NOT_EQUAL_1024))
            .addAND(disjunctionBuilder(BRAND_SAFETY_CATEGORIES_NOT_EQUAL_131072))
            .build();

    public static final TargetingExpression WEATHER_TARGETING_EXPRESSION_1 = TargetingExpression.newBuilder()
            .addAND(disjunctionBuilder(CLOUDNESS_NOT_EQUAL_25))
            .addAND(disjunctionBuilder(TEMPERATURE_EQUAL_10))
            .build();

    private TestTargetingExpression() {
    }

}
