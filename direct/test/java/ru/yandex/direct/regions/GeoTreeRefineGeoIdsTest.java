package ru.yandex.direct.regions;

import java.util.List;
import java.util.function.Function;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestContextManager;

import ru.yandex.direct.core.testing.configuration.CoreTest;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

@CoreTest
@RunWith(JUnitParamsRunner.class)
public class GeoTreeRefineGeoIdsTest {
    @Autowired
    public GeoTreeFactory geoTreeFactory;
    private GeoTree geoTree;

    @SuppressWarnings("unused")
    private Object[] commonValues() {
        return new Object[][]{
                {"Все, -Бельгия", asList(0L, -114L), asList(0L, -114L)},
                {"пустой список", emptyList(), emptyList()},
                {"Все", singletonList(0L), singletonList(0L)},
                {"Все, -Бельгия", asList(0L, -114L), asList(0L, -114L)},
                {"-Бельгия, Все", asList(-114L, 0L), asList(0L, -114L)},
                {"Бельгия, Все", asList(114L, 0L), asList(114L, 0L)},
                {"Все, Бельгия", asList(0L, 114L), asList(0L, 114L)},
                {"Россия", singletonList(225L), singletonList(225L)},
                {"Чехия, Бельгия", asList(125L, 114L), asList(125L, 114L)},
                {"Россия, -Кандалакша", asList(225L, -10895L), asList(225L, -10895L)},
                {"Россия, -Кандалакша, -Воронеж",
                        asList(225L, -10895L, -193L),
                        asList(225L, -10895L, -193L)},
                {"Бельгия, Россия, -Кандалакша, -Воронеж, Чехия",
                        asList(114L, 225L, -10895L, -193L, 125L),
                        asList(114L, 225L, -10895L, -193L, 125L)},
                {"Санкт-Петербург и Ленинградская область, -Выборг, Москва и область, -Мытищи, -Королёв",
                        asList(10174L, -969L, 1L, -98596L, -20728L),
                        asList(10174L, -969L, 1L, -98596L, -20728L)},
                {"Россия, Бельгия, -Кандалакша",
                        asList(225L, 114L, -10895L), // здесь минус-регион есть в первом регионе, но не во втором
                        asList(225L, -10895L, 114L)},
                {"-Москва, -Москва и область, Россия",
                        asList(-213L, -1L, 225L),
                        asList(225L, -213L, -1L)},
                {"Все, Москва и область, -Москва",
                        asList(0L, 1L, -213L),
                        asList(0L, 1L, -213L)},
                {"Россия, Украина, -Киев, -Москва",
                        asList(225L, 187L, -143L, -213L),
                        asList(225L, -213L, 187L, -143L)}
        };
    }

    @SuppressWarnings("unused")
    private Object[] translocalValues() {
        return new Object[][]{
                {"Россия, -Крым", asList(225L, -977L), asList(225L, -977L),
                        (Function<GeoTreeFactory, GeoTree>) GeoTreeFactory::getRussianGeoTree},
                {"Украина, -Крым", asList(187L, -977L), asList(187L, -977L),
                        (Function<GeoTreeFactory, GeoTree>) GeoTreeFactory::getGlobalGeoTree},
        };
    }

    @SuppressWarnings("unused")
    private Object[] negativeCommonValues() {
        return new Object[][]{
                {"Украина, -Республика Крым", asList(187L, -977L)},
                {"-Республика Крым, Украина", asList(-977L, 187L)},
                {"Россия, -Республика Крым", asList(225L, -977L)},
                {"-Республика Крым, Россия", asList(-977L, 225L)},
                {"СНГ (исключая Россию), Украина, -Республика Крым", asList(166L, 187L, -977L)},
                {"СНГ (исключая Россию), -Республика Крым, Украина", asList(166L, -977L, 187L)},
                {"Украина, СНГ (исключая Россию), -Республика Крым", asList(187L, 166L, -977L)},
                {"Украина, -Республика Крым, СНГ (исключая Россию)", asList(187L, -977L, 166L)},
                {"-Республика Крым, СНГ (исключая Россию), Украина", asList(-977L, 166L, 187L)},
                {"-Республика Крым, Украина, СНГ (исключая Россию)", asList(-977L, 187L, 166L)},
                {"Москва, -Москва и область", asList(213L, -1L)},
                {"-Москва и область, Москва", asList(-1L, 213L)},
                {"-Санкт-Петербург, Москва", asList(-2L, 213L)},
                {"Москва, -Санкт-Петербург", asList(213L, -2L)},
        };
    }

    @SuppressWarnings("unused")
    private Object[] negativeTranslocalValues() {
        return new Object[][]{
                {"Украина, -Республика Крым", asList(187L, -977L),
                        (Function<GeoTreeFactory, GeoTree>) GeoTreeFactory::getRussianGeoTree},
                {"Россия, -Республика Крым", asList(225L, -977L),
                        (Function<GeoTreeFactory, GeoTree>) GeoTreeFactory::getGlobalGeoTree},
        };
    }

    @Before
    public void setUp() throws Exception {
        TestContextManager testContextManager = new TestContextManager(getClass());
        testContextManager.prepareTestInstance(this);

        geoTree = geoTreeFactory.getApiGeoTree();
    }

    @Test
    @Parameters(method = "commonValues")
    @TestCaseName("{0}")
    public void testRefineGeoIds(@SuppressWarnings("unused") String testName, List<Long> sourceGeoIds,
                                 List<Long> resultGeoIds) {
        assertThat(geoTree.refineGeoIds(sourceGeoIds), equalTo(resultGeoIds));
    }

    @Test
    @Parameters(method = "translocalValues")
    @TestCaseName("{0}")
    public void testTranslocalRefineGeoIds(@SuppressWarnings("unused") String testName, List<Long> sourceGeoIds,
                                           List<Long> resultGeoIds, Function<GeoTreeFactory, GeoTree> geoTreeExtractor) {
        GeoTree geoTree = geoTreeExtractor.apply(geoTreeFactory);
        List<Long> actualResult = geoTree.refineGeoIds(sourceGeoIds);
        assertThat(actualResult, equalTo(resultGeoIds));
    }

    @Test(expected = IllegalArgumentException.class)
    @Parameters(method = "negativeCommonValues")
    @TestCaseName("{0}")
    public void testRefineGeoIds_throwsException(@SuppressWarnings("unused") String testName, List<Long> sourceGeoIds) {
        geoTree.refineGeoIds(sourceGeoIds);
    }

    @Test(expected = IllegalArgumentException.class)
    @Parameters(method = "negativeTranslocalValues")
    @TestCaseName("{0}")
    public void testTrancelocalRefineGeoIds_throwsException(@SuppressWarnings("unused") String testName,
                                                            List<Long> sourceGeoIds, Function<GeoTreeFactory, GeoTree> geoTreeExtractor) {
        GeoTree geoTree = geoTreeExtractor.apply(geoTreeFactory);
        geoTree.refineGeoIds(sourceGeoIds);
    }

}
