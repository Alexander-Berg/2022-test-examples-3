package ru.yandex.direct.intapi.entity.bidmodifiers;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategy;
import ru.yandex.direct.core.entity.bidmodifier.AgeType;
import ru.yandex.direct.core.entity.bidmodifier.BidModifier;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDemographicsAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierMobile;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierMobileAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierTrafaretPosition;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierTrafaretPositionAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierType;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierWeather;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierWeatherAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierWeatherLiteral;
import ru.yandex.direct.core.entity.bidmodifier.GenderType;
import ru.yandex.direct.core.entity.bidmodifier.OperationType;
import ru.yandex.direct.core.entity.bidmodifier.TrafaretPosition;
import ru.yandex.direct.core.entity.bidmodifier.WeatherType;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.testing.info.AdGroupBidModifierInfo;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignBidModifierInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.entity.bidmodifiers.controller.BidModifiersController;
import ru.yandex.direct.intapi.entity.bidmodifiers.model.BidModifierResponse;
import ru.yandex.direct.liveresource.LiveResourceFactory;
import ru.yandex.direct.utils.JsonUtils;
import ru.yandex.direct.web.core.entity.bidmodifiers.ComplexBidModifierConverter;
import ru.yandex.direct.web.core.entity.bidmodifiers.model.BidModifiersListWebResponse;
import ru.yandex.direct.web.core.entity.bidmodifiers.model.ComplexBidModifierWeb;
import ru.yandex.direct.web.core.model.WebResponse;
import ru.yandex.qatools.allure.annotations.Description;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultBidModifierDemographics;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultDemographicsAdjustment;

@IntApiTest
@RunWith(SpringJUnit4ClassRunner.class)
@Description("Сценарии работы контроллера по корректировкам цен")
public class BidModifiersControllerTest {

    private static final String DEMOGRAPHY_MULTIPLIER_FILE = "classpath:///bidmodifiers/demography_multiplier.json";
    private static final String MOBILE_MULTIPLIER_FILE = "classpath:///bidmodifiers/mobile_multiplier.json";
    private static final String WEATHER_MULTIPLIER_FILE = "classpath:///bidmodifiers/weather_multiplier.json";
    private static final String TRAFARET_POSITION_MULTIPLIER_FILE = "classpath:///bidmodifiers" +
            "/trafaret_position_multiplier.json";

    @Autowired
    private BidModifiersController bidModifiersController;

    @Autowired
    private Steps steps;

    private CampaignInfo campaignInfo;
    private AdGroupInfo adGroupInfo;
    private String demographyJson;
    private String mobileJson;
    private String weatherJson;
    private String trafaretPositionJson;

    private static final DefaultCompareStrategy COMPARE_STRATEGY = getCompareStrategy();

    @Before
    public void before() {
        campaignInfo = steps.campaignSteps().createActiveTextCampaign();
        adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo);
        demographyJson =
                "\"demography_multiplier\": " + LiveResourceFactory.get(DEMOGRAPHY_MULTIPLIER_FILE).getContent();
        mobileJson = "\"mobile_multiplier\": " + LiveResourceFactory.get(MOBILE_MULTIPLIER_FILE).getContent();
        weatherJson =
                "\"weather_multiplier\": " + LiveResourceFactory.get(WEATHER_MULTIPLIER_FILE).getContent();
        trafaretPositionJson =
                "\"trafaret_position_multiplier\": "
                        + LiveResourceFactory.get(TRAFARET_POSITION_MULTIPLIER_FILE).getContent();

    }

    @Test
    public void updateBidModifiers_AddOneBidModifier() {
        String resultJson = "{" + demographyJson + "}";
        ComplexBidModifierWeb demographyComplexBidModifierWeb =
                JsonUtils.fromJson(resultJson, ComplexBidModifierWeb.class);

        WebResponse updateResponse = bidModifiersController.updateBidModifiers(
                demographyComplexBidModifierWeb,
                campaignInfo.getCampaignId(),
                null,
                CampaignType.TEXT);

        CampaignBidModifierInfo campaignBidModifierInfo = steps.bidModifierSteps()
                .getCampaignBidModifiers(campaignInfo.getShard(), campaignInfo.getCampaignId(),
                        singleton(BidModifierType.DEMOGRAPHY_MULTIPLIER));

        assertThat("Ответ instanceof BidModifierResponse",
                updateResponse,
                instanceOf(BidModifierResponse.class));
        assertThat("Ожидается 1 измененная запись",
                ((BidModifierResponse) updateResponse).getAffectedResult(),
                equalTo(1L));
        assertThat("Ожидаются взятые из БД корректировки такие же как добавлялись",
                campaignBidModifierInfo.getBidModifiers().get(0),
                beanDiffer(expectedDemographyBidModifier()).useCompareStrategy(COMPARE_STRATEGY));
    }

    @Test
    public void updateBidModifiers_AddOneWeatherBidModifier() {
        String resultJson = "{" + weatherJson + "}";
        ComplexBidModifierWeb weatherComplexBidModifierWeb =
                JsonUtils.fromJson(resultJson, ComplexBidModifierWeb.class);

        WebResponse updateResponse = bidModifiersController.updateBidModifiers(
                weatherComplexBidModifierWeb,
                campaignInfo.getCampaignId(),
                adGroupInfo.getAdGroupId(),
                CampaignType.CPM_BANNER);

        AdGroupBidModifierInfo campaignBidModifierInfo = steps.bidModifierSteps()
                .getAdGroupBidModifiers(campaignInfo.getShard(), campaignInfo.getCampaignId(),
                        adGroupInfo.getAdGroupId(),
                        singleton(BidModifierType.WEATHER_MULTIPLIER));

        assertThat("Ответ instanceof BidModifierResponse",
                updateResponse,
                instanceOf(BidModifierResponse.class));
        assertThat("Ожидается 1 измененная запись",
                ((BidModifierResponse) updateResponse).getAffectedResult(),
                equalTo(1L));
        assertThat("Ожидаются взятые из БД корректировки такие же как добавлялись",
                campaignBidModifierInfo.getBidModifiers().get(0),
                beanDiffer((BidModifier) expectedWeatherBidModifier()).useCompareStrategy(COMPARE_STRATEGY));
    }

    @Test
    public void updateBidModifiers_AddMobileBidModifier() {
        String resultJson = "{" + mobileJson + "}";
        ComplexBidModifierWeb mobileComplexBidModifierWeb =
                JsonUtils.fromJson(resultJson, ComplexBidModifierWeb.class);

        WebResponse updateResponse = bidModifiersController.updateBidModifiers(
                mobileComplexBidModifierWeb,
                campaignInfo.getCampaignId(),
                null,
                CampaignType.TEXT);

        CampaignBidModifierInfo campaignBidModifierInfo = steps.bidModifierSteps()
                .getCampaignBidModifiers(campaignInfo.getShard(), campaignInfo.getCampaignId(),
                        singleton(BidModifierType.MOBILE_MULTIPLIER));

        assertThat("Ответ instanceof BidModifierResponse",
                updateResponse,
                instanceOf(BidModifierResponse.class));
        assertThat("Ожидается 1 измененная запись по ответу",
                ((BidModifierResponse) updateResponse).getAffectedResult(),
                equalTo(1L));
        assertThat("Ожидается 1 измененная запись в БД",
                campaignBidModifierInfo.getBidModifiers().size(),
                equalTo(1));
        assertThat("Ожидаются взятые из БД корректировки такие же как добавлялись",
                campaignBidModifierInfo.getBidModifiers().get(0),
                beanDiffer(expectedMobileBidModifier()).useCompareStrategy(COMPARE_STRATEGY));
    }

    @Test
    public void updateBidModifiers_AddOneTrafaretPositionBidModifier() {
        String resultJson = "{" + trafaretPositionJson + "}";
        ComplexBidModifierWeb trafaretPositionComplexBidModifierWeb =
                JsonUtils.fromJson(resultJson, ComplexBidModifierWeb.class);

        WebResponse updateResponse = bidModifiersController.updateBidModifiers(
                trafaretPositionComplexBidModifierWeb,
                campaignInfo.getCampaignId(),
                null,
                CampaignType.TEXT);

        CampaignBidModifierInfo campaignBidModifierInfo = steps.bidModifierSteps()
                .getCampaignBidModifiers(campaignInfo.getShard(), campaignInfo.getCampaignId(),
                        singleton(BidModifierType.TRAFARET_POSITION_MULTIPLIER));

        assertThat("Ответ instanceof BidModifierResponse",
                updateResponse,
                instanceOf(BidModifierResponse.class));
        assertThat("Ожидается 1 измененная запись",
                ((BidModifierResponse) updateResponse).getAffectedResult(),
                equalTo(1L));
        assertThat("Ожидаются взятые из БД корректировки такие же как добавлялись",
                campaignBidModifierInfo.getBidModifiers().get(0),
                beanDiffer((BidModifier) expectedTrafaretPositionBidModifier()).useCompareStrategy(COMPARE_STRATEGY));
    }

    @Test
    public void updateBidModifiers_AddTwoBidModifiers() {
        String resultJson = "{" + demographyJson + "," + mobileJson + "}";
        ComplexBidModifierWeb bothComplexBidModifierWeb = JsonUtils.fromJson(resultJson, ComplexBidModifierWeb.class);

        WebResponse updateResponse = bidModifiersController.updateBidModifiers(
                bothComplexBidModifierWeb,
                campaignInfo.getCampaignId(),
                null,
                CampaignType.TEXT);

        CampaignBidModifierInfo campaignBidModifierInfo = steps.bidModifierSteps()
                .getCampaignBidModifiers(campaignInfo.getShard(), campaignInfo.getCampaignId(),
                        ImmutableSet.of(BidModifierType.DEMOGRAPHY_MULTIPLIER, BidModifierType.MOBILE_MULTIPLIER));

        assertThat("Ответ instanceof BidModifierResponse",
                updateResponse,
                instanceOf(BidModifierResponse.class));
        assertThat("Ожидается 1 измененная кампания",
                ((BidModifierResponse) updateResponse).getAffectedResult(),
                equalTo(1L));
        assertThat("Ожидается 2 измененных записи в БД",
                campaignBidModifierInfo.getBidModifiers().size(),
                equalTo(2));

        List<BidModifier> bidModifiers = campaignBidModifierInfo.getBidModifiers();
        // из-за того, что корректировки разных типов - при несовпадении порядка - падает beanDiffer
        bidModifiers.sort(Comparator.comparing(BidModifier::getType));
        assertThat("Ожидаются взятые из БД корректировки такие же как добавлялись",
                bidModifiers,
                contains(
                        beanDiffer(expectedMobileBidModifier()).useCompareStrategy(COMPARE_STRATEGY),
                        beanDiffer(expectedDemographyBidModifier()).useCompareStrategy(COMPARE_STRATEGY)
                )
        );
    }

    @Test
    public void updateBidModifiers_DeleteLastBidModifier() {
        String resultJson = "{" + demographyJson + "}";
        // непустая корректировка
        ComplexBidModifierWeb demographyComplexBidModifierWeb =
                JsonUtils.fromJson(resultJson, ComplexBidModifierWeb.class);
        // пустая корректировка
        ComplexBidModifierWeb emptyComplexBidModifierWeb =
                JsonUtils.fromJson("{}", ComplexBidModifierWeb.class);

        // добавляем непустую корректировку
        bidModifiersController.updateBidModifiers(
                demographyComplexBidModifierWeb,
                campaignInfo.getCampaignId(),
                null,
                CampaignType.TEXT);

        // перезаписываем на новую пустую корректировку, что равносильно удалению
        WebResponse updateResponse = bidModifiersController.updateBidModifiers(
                emptyComplexBidModifierWeb,
                campaignInfo.getCampaignId(),
                null,
                CampaignType.TEXT);


        CampaignBidModifierInfo campaignBidModifierInfo = steps.bidModifierSteps()
                .getCampaignBidModifiers(campaignInfo.getShard(), campaignInfo.getCampaignId(),
                        singleton(BidModifierType.DEMOGRAPHY_MULTIPLIER));

        assertThat("Ответ instanceof BidModifierResponse",
                updateResponse,
                instanceOf(BidModifierResponse.class));
        assertThat("Ожидается 1 измененная кампания",
                ((BidModifierResponse) updateResponse).getAffectedResult(),
                equalTo(1L));
        assertThat("Ожидается, что корректировки сотрутся и больше их в БД не будет",
                campaignBidModifierInfo.getBidModifiers().size(),
                equalTo(0));
    }

    @Test
    public void updateBidModifiers_DontChangeModifiers_ZeroAffected() {
        String resultJson = "{" + demographyJson + "," + mobileJson + "}";
        ComplexBidModifierWeb bothComplexBidModifierWeb = JsonUtils.fromJson(resultJson, ComplexBidModifierWeb.class);

        // устанавливаем какие-то корректировки
        bidModifiersController.updateBidModifiers(
                bothComplexBidModifierWeb,
                campaignInfo.getCampaignId(),
                null,
                CampaignType.TEXT
        );
        // устанавливаем ещё раз те же самые корректировки
        var response = bidModifiersController.updateBidModifiers(
                bothComplexBidModifierWeb,
                campaignInfo.getCampaignId(),
                null,
                CampaignType.TEXT
        );
        assertThat("Ответ instanceof BidModifierResponse",
                response,
                instanceOf(BidModifierResponse.class));
        assertThat("Ожидается 0 измененных кампаний",
                ((BidModifierResponse) response).getAffectedResult(),
                equalTo(0L));
    }

    @Test
    public void getBidModifiers_DemographyBidModifier() {
        // Создаем корректировку через степы
        steps.bidModifierSteps().createCampaignBidModifier(expectedDemographyBidModifier(), campaignInfo);

        // Достаем корректировку через тестируемый контроллер
        BidModifiersListWebResponse gotResponse =
                bidModifiersController.getBidModifiers(campaignInfo.getCampaignId(), null);

        // Приводим ожидание к тому же типу, что получили из контроллера
        BidModifiersListWebResponse expectedResponse =
                ComplexBidModifierConverter.convertToExternal(singletonList(expectedDemographyBidModifier()));

        assertThat("Ожидаются взятые из БД корректировки такие же как добавлялись",
                gotResponse,
                beanDiffer(expectedResponse).useCompareStrategy(getCompareStrategyResponse()));
    }

    @Test
    public void getBidModifiers_WeatherBidModifier() {
        // Создаем корректировку через степы
        BidModifierWeather bidModifierToAdd = expectedWeatherBidModifier();
        bidModifierToAdd.withLastChange(LocalDateTime.now());
        bidModifierToAdd.getWeatherAdjustments().forEach(adj -> adj.withLastChange(LocalDateTime.now()));
        steps.bidModifierSteps().createAdGroupBidModifier(bidModifierToAdd, adGroupInfo);

        // Достаем корректировку через тестируемый контроллер
        BidModifiersListWebResponse gotResponse =
                bidModifiersController.getBidModifiers(campaignInfo.getCampaignId(), adGroupInfo.getAdGroupId());

        // Приводим ожидание к тому же типу, что получили из контроллера
        BidModifiersListWebResponse expectedResponse =
                ComplexBidModifierConverter.convertToExternal(singletonList(
                        // Без id не сконвертится
                        expectedWeatherBidModifier().withId(0L)));

        assertThat("Ожидаются взятые из БД корректировки такие же как добавлялись",
                gotResponse,
                beanDiffer(expectedResponse).useCompareStrategy(getCompareStrategyResponse()));
    }

    @Test
    public void getBidModifiers_TrafaretPositionBidModifier() {
        // Создаем корректировку через степы
        BidModifierTrafaretPosition bidModifierToAdd = expectedTrafaretPositionBidModifier();
        bidModifierToAdd.withLastChange(LocalDateTime.now());
        bidModifierToAdd.getTrafaretPositionAdjustments().forEach(adj -> adj.withLastChange(LocalDateTime.now()));
        steps.bidModifierSteps().createCampaignBidModifier(bidModifierToAdd, campaignInfo);

        // Достаем корректировку через тестируемый контроллер
        BidModifiersListWebResponse gotResponse =
                bidModifiersController.getBidModifiers(campaignInfo.getCampaignId(), null);

        // Приводим ожидание к тому же типу, что получили из контроллера
        BidModifiersListWebResponse expectedResponse =
                ComplexBidModifierConverter.convertToExternal(singletonList(
                        // Без id и lastChange не сконвертится
                        expectedTrafaretPositionBidModifier()
                                .withId(0L)
                                .withLastChange(LocalDateTime.now())));

        assertThat("Ожидаются взятые из БД корректировки такие же как добавлялись",
                gotResponse,
                beanDiffer(expectedResponse).useCompareStrategy(getCompareStrategyResponse()));
    }

    private BidModifier expectedDemographyBidModifier() {
        List<BidModifierDemographicsAdjustment> expectedBidModifierDemographicsAdjustments = Arrays.asList(
                createDefaultDemographicsAdjustment().withId(0L).withPercent(100).withAge(AgeType._0_17)
                        .withGender(GenderType.FEMALE),
                createDefaultDemographicsAdjustment().withId(0L).withPercent(200).withAge(AgeType._18_24)
                        .withGender(GenderType.FEMALE),
                createDefaultDemographicsAdjustment().withId(0L).withPercent(300).withAge(AgeType._25_34)
                        .withGender(GenderType.MALE));


        return createDefaultBidModifierDemographics(campaignInfo.getCampaignId())
                .withId(0L)
                .withType(BidModifierType.DEMOGRAPHY_MULTIPLIER)
                .withEnabled(true)
                .withDemographicsAdjustments(expectedBidModifierDemographicsAdjustments);

    }

    private BidModifier expectedMobileBidModifier() {
        BidModifierMobileAdjustment expectedBidModifierMobileAdjustment =
                new BidModifierMobileAdjustment().withId(0L).withPercent(110);


        return new BidModifierMobile()
                .withId(0L)
                .withCampaignId(campaignInfo.getCampaignId())
                .withType(BidModifierType.MOBILE_MULTIPLIER)
                .withEnabled(true)
                .withMobileAdjustment(expectedBidModifierMobileAdjustment);

    }

    private BidModifierWeather expectedWeatherBidModifier() {
        List<BidModifierWeatherAdjustment> expectedBidModifierWeatherAdjustments = Collections.singletonList(
                new BidModifierWeatherAdjustment().withId(0L).withPercent(110).withExpression(
                        singletonList(singletonList(new BidModifierWeatherLiteral()
                                .withValue(50)
                                .withOperation(OperationType.EQ)
                                .withParameter(WeatherType.CLOUDNESS))))
        );


        return new BidModifierWeather()
                .withCampaignId(campaignInfo.getCampaignId())
                .withAdGroupId(adGroupInfo.getAdGroupId())
                //.withId(0L)
                .withType(BidModifierType.WEATHER_MULTIPLIER)
                .withEnabled(true)
                .withWeatherAdjustments(expectedBidModifierWeatherAdjustments);

    }

    private BidModifierTrafaretPosition expectedTrafaretPositionBidModifier() {
        List<BidModifierTrafaretPositionAdjustment> expectedBidModifierTrafaretPositionAdjustments =
                Collections.singletonList(
                        new BidModifierTrafaretPositionAdjustment()
                                .withId(0L)
                                .withLastChange(LocalDateTime.now())
                                .withPercent(155)
                                .withTrafaretPosition(TrafaretPosition.ALONE)
                );


        return new BidModifierTrafaretPosition()
                .withCampaignId(campaignInfo.getCampaignId())
                .withType(BidModifierType.TRAFARET_POSITION_MULTIPLIER)
                .withEnabled(true)
                .withTrafaretPositionAdjustments(expectedBidModifierTrafaretPositionAdjustments);

    }

    /**
     * Стратегия сравнения для объектов BidModifier
     */
    private static DefaultCompareStrategy getCompareStrategy() {
        return DefaultCompareStrategies.allFieldsExcept(
                BeanFieldPath.newPath("id"),
                BeanFieldPath.newPath("lastChange"),
                BeanFieldPath.newPath("demographicsAdjustments", "\\d+", "id"),
                BeanFieldPath.newPath("demographicsAdjustments", "\\d+", "lastChange"),
                BeanFieldPath.newPath("weatherAdjustments", "\\d+", "id"),
                BeanFieldPath.newPath("weatherAdjustments", "\\d+", "lastChange"),
                BeanFieldPath.newPath("mobileAdjustment/id"),
                BeanFieldPath.newPath("mobileAdjustment/lastChange"),
                BeanFieldPath.newPath("trafaretPositionAdjustments", "\\d+", "id"),
                BeanFieldPath.newPath("trafaretPositionAdjustments", "\\d+", "lastChange"));
    }

    /**
     * Стратегия сравнения для объектов BidModifiersListWebResponse
     */
    private static DefaultCompareStrategy getCompareStrategyResponse() {
        return DefaultCompareStrategies.allFieldsExcept(
                BeanFieldPath.newPath("bidModifierWeather/hierarchicalMultiplierId"),
                BeanFieldPath.newPath("bidModifierWeather/conditions", "\\d+", "id"),
                BeanFieldPath.newPath("bidModifierDemography/hierarchicalMultiplierId"),
                BeanFieldPath.newPath("bidModifierDemography/conditions", "\\d+", "id"),
                BeanFieldPath.newPath("bidModifierMobile/hierarchicalMultiplierId"),
                BeanFieldPath.newPath("bidModifierTrafaretPosition/hierarchicalMultiplierId"),
                BeanFieldPath.newPath("bidModifierTrafaretPosition/lastChange"),
                BeanFieldPath.newPath("bidModifierTrafaretPosition/conditions", "\\d+", "id"),
                BeanFieldPath.newPath("bidModifierTrafaretPosition/conditions", "\\d+", "lastChange"));
    }

}
