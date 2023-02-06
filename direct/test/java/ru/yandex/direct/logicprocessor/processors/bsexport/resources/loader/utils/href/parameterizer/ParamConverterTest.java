package ru.yandex.direct.logicprocessor.processors.bsexport.resources.loader.utils.href.parameterizer;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.model.StrategyData;
import ru.yandex.direct.core.entity.campaign.model.StrategyName;
import ru.yandex.direct.currency.Currencies;
import ru.yandex.direct.currency.CurrencyCode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class ParamConverterTest {
    private final ParamConverter paramConverter = new ParamConverter();

    @ParameterizedTest
    @EnumSource(value = CampaignType.class, mode = EnumSource.Mode.EXCLUDE, names = {"DYNAMIC", "PERFORMANCE",
            "WALLET", "BILLING_AGGREGATE", "MCB"})
    void bidSubstitutionTest(CampaignType campaignType) {
        var replacingParams = getReplacingParams(campaignType, CurrencyCode.RUB);
        var bidsTokens = List.of("ad_id", "adid", "banner_id", "bannerid");

        var expectedConversionResult = ConversionResult.substitution("7065904107");
        SoftAssertions assertions = new SoftAssertions();
        bidsTokens.stream()
                .map(token -> paramConverter.convert(token, replacingParams))
                .forEach(conversionResult -> assertions.assertThat(conversionResult).isEqualToComparingFieldByField(expectedConversionResult));
        assertions.assertAll();
    }

    @ParameterizedTest
    @EnumSource(value = CampaignType.class, mode = EnumSource.Mode.INCLUDE, names = {"DYNAMIC", "PERFORMANCE"})
    void bidSubstitution_GeneratedBannerTest(CampaignType campaignType) {
        var replacingParams = getReplacingParams(campaignType, CurrencyCode.RUB);
        var bidsTokens = List.of("ad_id", "adid", "banner_id", "bannerid");

        var expectedConversionResult = ConversionResult.macro("PEID");
        SoftAssertions assertions = new SoftAssertions();
        bidsTokens.stream()
                .map(token -> paramConverter.convert(token, replacingParams))
                .forEach(conversionResult -> assertions.assertThat(conversionResult).isEqualToComparingFieldByField(expectedConversionResult));
        assertions.assertAll();
    }

    @ParameterizedTest
    @MethodSource("tokensToSubstitutionsTest")
    void campaignSubstitutionsTest(List<String> tokens, String expectedSubstitution) {
        var replacingParams = getReplacingParams(CampaignType.TEXT, CurrencyCode.RUB);

        var expectedConversionResult = ConversionResult.substitution(expectedSubstitution);
        SoftAssertions assertions = new SoftAssertions();
        tokens.stream()
                .map(token -> paramConverter.convert(token, replacingParams))
                .forEach(conversionResult -> assertions.assertThat(conversionResult).isEqualToComparingFieldByField(expectedConversionResult));
        assertions.assertAll();
    }

    @Test
    void unknownTokenTest() {
        var replacingParams = getReplacingParams(CampaignType.TEXT, CurrencyCode.RUB);
        var token = "unknownToken";

        var expectedConversionResult = ConversionResult.notFound();
        var conversionResult = paramConverter.convert(token, replacingParams);
        assertThat(conversionResult).isEqualToComparingFieldByField(expectedConversionResult);
    }

    @ParameterizedTest
    @MethodSource("tokensToBsMacros")
    void bsMacroConversionTest(String token, String bsMacro) {
        var replacingParams = getReplacingParams(CampaignType.TEXT, CurrencyCode.RUB);
        var expectedConversionResult = ConversionResult.macro(bsMacro);
        var conversionResult = paramConverter.convert(token, replacingParams);
        assertThat(conversionResult).isEqualToComparingFieldByField(expectedConversionResult);
    }

    @ParameterizedTest
    @EnumSource(value = CurrencyCode.class)
    void currencyMacroSubstitutionTest(CurrencyCode currency) {
        var replacingParams = getReplacingParams(CampaignType.TEXT, currency);
        var currencyTokens = List.of("campaign_currency", "campaigncurrency");
        var currencyCodeTokens = List.of("campaign_currency_code", "campaigncurrencycode");

        var expectedCurrencyConversionResult = currency == CurrencyCode.YND_FIXED
                ? ConversionResult.notFound() : ConversionResult.substitution(currency.toString());
        var expectedCurrencyCodeConversionResult = currency == CurrencyCode.YND_FIXED
                ? ConversionResult.notFound() : ConversionResult.substitution(Currencies.getCurrency(currency).getIsoNumCode().toString());
        SoftAssertions assertions = new SoftAssertions();
        currencyTokens.stream()
                .map(token -> paramConverter.convert(token, replacingParams))
                .forEach(conversionResult -> assertions.assertThat(conversionResult).isEqualToComparingFieldByField(expectedCurrencyConversionResult));
        currencyCodeTokens.stream()
                .map(token -> paramConverter.convert(token, replacingParams))
                .forEach(conversionResult -> assertions.assertThat(conversionResult).isEqualToComparingFieldByField(expectedCurrencyCodeConversionResult));
        assertions.assertAll();
    }

    @ParameterizedTest
    @MethodSource("costSubstitutionsTest")
    void costMacroSubstitutionsTest(StrategyName strategyName, Long goalId, String expectedType, String expectedCost) {
        var replacingParams = ReplacingParams.builder()
                .withPid(3702883644L)
                .withCid(41007238L)
                .withCampaignType(CampaignType.TEXT)
                .withCampaignName("Съедобные_букеты_23_февраля_Поиск_Москва")
                .withCampaignNameLat("Sedobnye_bukety_23_fevralya_Poisk_Moskva")
                .withCampaignCurrency(CurrencyCode.RUB)
                .withCampaignStrategy((DbStrategy) new DbStrategy()
                        .withStrategyName(strategyName)
                        .withStrategyData(new StrategyData()
                                .withAvgBid(new BigDecimal("12.34"))
                                .withAvgCpa(new BigDecimal("56.7"))
                                .withAvgCpi(new BigDecimal("89"))
                                .withAvgCpm(new BigDecimal("111.10"))
                                .withGoalId(goalId)))
                .build();
        var costTypeTokens = List.of("campaign_cost_type", "campaigncosttype");
        var costTokens = List.of("campaign_cost", "campaigncost");

        var expectedCostTypeResult = expectedType == null
                ? ConversionResult.notFound() : ConversionResult.substitution(expectedType);
        var expectedCostResult = expectedCost == null
                ? ConversionResult.notFound() : ConversionResult.substitution(expectedCost);
        SoftAssertions assertions = new SoftAssertions();
        costTypeTokens.stream()
                .map(token -> paramConverter.convert(token, replacingParams))
                .forEach(conversionResult -> assertions.assertThat(conversionResult).isEqualToComparingFieldByField(expectedCostTypeResult));
        costTokens.stream()
                .map(token -> paramConverter.convert(token, replacingParams))
                .forEach(conversionResult -> assertions.assertThat(conversionResult).isEqualToComparingFieldByField(expectedCostResult));
        assertions.assertAll();
    }

    private ReplacingParams getReplacingParams(CampaignType campaignType, CurrencyCode currency) {
        return ReplacingParams.builder()
                .withBid(7065904107L)
                .withPid(3702883644L)
                .withCid(41007238L)
                .withCampaignType(campaignType)
                .withCampaignName("Съедобные_букеты_23_февраля_Поиск_Москва")
                .withCampaignNameLat("Sedobnye_bukety_23_fevralya_Poisk_Moskva")
                .withCampaignCurrency(currency)
                .build();
    }

    static Stream<Arguments> tokensToSubstitutionsTest() {
        return Stream.of(
                arguments(List.of("campaign_id", "campaignid"), "41007238"),
                arguments(List.of("adgroup_id", "adgroupid"), "3702883644"),
                arguments(List.of("campaign_name", "campaignname"), "Съедобные_букеты_23_февраля_Поиск_Москва"),
                arguments(List.of("campaign_name_lat", "campaignnamelat"), "Sedobnye_bukety_23_fevralya_Poisk_Moskva"),
                arguments(List.of("campaign_type", "campaigntype"), "type1"),
                arguments(List.of("campaign_currency", "campaigncurrency"), "RUB"),
                arguments(List.of("campaign_currency_code", "campaigncurrencycode"), "643")
        );
    }

    /**
     * Источник perl - https://a.yandex-team.ru/arc/trunk/arcadia/direct/perl/protected/BS/Export.pm?rev=7482993#L222
     */
    static Stream<Arguments> tokensToBsMacros() {
        return Stream.of(
                arguments("position", "POS"),
                arguments("position_type", "PTYPE"),
                arguments("source", "SRC"),
                arguments("source_type", "STYPE"),
                arguments("addphrases", "BM"),
                arguments("param1", "PARAM1"),
                arguments("param2", "PARAM2"),
                arguments("phraseid", "PHRASE_EXPORT_ID"),
                arguments("phrase_id", "PHRASE_EXPORT_ID"),
                arguments("adtarget_id", "PHRASE_EXPORT_ID"),
                arguments("retargeting_id", "PARAM126"),
                arguments("adtarget_name", "PARAM126"),
                arguments("interest_id", "PARAM125"),
                arguments("keyword", "PHRASE"),
                arguments("gbid", "GBID"),
                arguments("device_type", "DEVICE_TYPE"),
                arguments("logid", "LOGID"),
                arguments("trackid", "TRACKID"),
                arguments("android_id", "ANDROID_ID_LC"),
                arguments("androidid", "ANDROID_ID_LC"),
                arguments("android_id_lc_sh1", "ANDROID_ID_LC_SH1_HEX"),
                arguments("google_aid", "GOOGLE_AID_LC"),
                arguments("googleaid", "GOOGLE_AID_LC"),
                arguments("google_aid_lc_sh1", "GOOGLE_AID_LC_SH1_HEX"),
                arguments("ios_ifa", "IDFA_UC"),
                arguments("iosifa", "IDFA_UC"),
                arguments("idfa_lc_sh1", "IDFA_UC_SH1_HEX"),
                arguments("idfa_lc_sh1_hex", "IDFA_UC_SH1_HEX"),
                arguments("idfa_lc", "IDFA_UC"),
                arguments("idfa_lc_md5", "IDFA_UC_MD5_HEX"),
                arguments("idfa_lc_md5_hex", "IDFA_UC_MD5_HEX"),
                arguments("region_id", "REG_BS"),
                arguments("region_name", "REGN_BS"),
                arguments("addphrasestext", "PHRASE_BM"),
                arguments("smartbanner_id", "CREATIVE_ID"),
                arguments("offer_id", "OFFER_ID"),
                arguments("coef_goal_context_id", "COEF_GOAL_CONTEXT_ID"),
                arguments("creative_id", "CREATIVE_ID"),
                arguments("match_type", "MATCH_TYPE"),
                arguments("matched_keyword", "PHRASE_RKW"),
                arguments("oaid", "OAID"),
                arguments("oaid_lc", "OAID_LC"),
                arguments("oaid_lc_sh1", "OAID_LC_SH1_HEX"),
                arguments("oaid_lc_sh1_hex", "OAID_LC_SH1_HEX"),
                arguments("oaid_lc_md5", "OAID_LC_MD5_HEX"),
                arguments("oaid_lc_md5_hex", "OAID_LC_MD5_HEX"),
                arguments("client_ip", "CLIENTIP"),
                arguments("user_agent", "USER_AGENT"),
                arguments("device_lang", "DEVICE_LANG"),
                arguments("yclid", "YCLID")
        );
    }

    static Stream<Arguments> costSubstitutionsTest() {
        return Stream.of(
                arguments(StrategyName.AUTOBUDGET_AVG_CPA, null, null, null),
                arguments(StrategyName.AUTOBUDGET_AVG_CPA_PER_CAMP, null, null, null),
                arguments(StrategyName.AUTOBUDGET_AVG_CPA_PER_FILTER, null, null, null),
                arguments(StrategyName.AUTOBUDGET_AVG_CLICK, null, "CPC", "12.34"),
                arguments(StrategyName.AUTOBUDGET_AVG_CPC_PER_FILTER, null, "CPC", "12.34"),
                arguments(StrategyName.AUTOBUDGET_AVG_CPC_PER_CAMP, null, "CPC", "12.34"),
                arguments(StrategyName.AUTOBUDGET_AVG_CPI, null, "CPI", "89"),
                arguments(StrategyName.AUTOBUDGET_AVG_CPI, 38403095L, null, null),
                arguments(StrategyName.CPM_DEFAULT, null, "CPM", "111.10"),
                arguments(StrategyName.AUTOBUDGET, null, null, null)
        );
    }

}
