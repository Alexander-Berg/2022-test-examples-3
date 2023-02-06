package ru.yandex.market.antifraud.filter.generators;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTime;
import ru.yandex.market.antifraud.filter.ClickGenerator;
import ru.yandex.market.antifraud.filter.FilterGenerator;
import ru.yandex.market.antifraud.filter.TestClick;
import ru.yandex.market.antifraud.filter.fields.Cookie;
import ru.yandex.market.antifraud.filter.fields.FilterConstants;
import static java.util.stream.Collectors.toList;

@Slf4j
public class For41Filter implements FilterGenerator {

    private static final int WHITE_MARKET_DESKTOP_PP_EXAMPLE = 153;
    private static final int WHITE_MARKET_TOUCH_PP_EXAMPLE = 273;
    private static final int BLUE_MARKET_DESKTOP_PP_EXAMPLE = 1902;
    private static final int NO_MARKET_PP_EXAMPLE = 908;
    private static final DateTime TEST_DT = new DateTime().withMillisOfSecond(0);

    public List<TestClick> generate() {
        return Stream.of(
                // 2 разных показных куки, отличаются от кликовой куки, нужный пп
                generateClicks(1, 11, FilterConstants.FILTER_41, WHITE_MARKET_DESKTOP_PP_EXAMPLE, false, 2),
                // 2 разных показных куки, отличаются от кликовой куки, нужный пп, но не больше 10 кликов
                generateClicks(12, 10, FilterConstants.FILTER_0, WHITE_MARKET_DESKTOP_PP_EXAMPLE, false, 2),
                // 18 нормальных и 2 разных показных куки, отличаются от кликовой куки, нужный пп но меньше <=15%
                generateClicks(13, 20, FilterConstants.FILTER_0, WHITE_MARKET_DESKTOP_PP_EXAMPLE, true, 3),


                // 2 разных показных куки, отличаются от кликовой куки, другие пп
                generateClicks(2, 11, FilterConstants.FILTER_0, WHITE_MARKET_TOUCH_PP_EXAMPLE, false, 2),
                generateClicks(3, 11, FilterConstants.FILTER_0, BLUE_MARKET_DESKTOP_PP_EXAMPLE, false, 2),
                generateClicks(4, 11, FilterConstants.FILTER_0, NO_MARKET_PP_EXAMPLE, false, 2),

                // 1 показная кука, отличается от кликовой куки, все клики с этой кукой, нужный пп
                generateClicks(5, 11, FilterConstants.FILTER_41, WHITE_MARKET_DESKTOP_PP_EXAMPLE, false, 1),
                // 1 показная кука, не отличается от кликовой куки, нужный пп
                generateClicks(6, 11, FilterConstants.FILTER_0, WHITE_MARKET_DESKTOP_PP_EXAMPLE, true, 1),
                // 2 показных кука, одна не отличается, вторая отличается от кликовой куки, нужный пп
                generateClicks(7, 11, FilterConstants.FILTER_0, WHITE_MARKET_DESKTOP_PP_EXAMPLE, true, 2),
                // 2 разных непустых показных куки, пустая кликовая куки, нужный пп
                generateClicksWithEmptyCookie(8, 11, FilterConstants.FILTER_41, WHITE_MARKET_DESKTOP_PP_EXAMPLE),
                // 2 разных показных куки, один пуст, второй нет. отличается от кликовой куки, нужный пп
                generateClicksWithEmptyShowCookie(9, 11, FilterConstants.FILTER_0, WHITE_MARKET_DESKTOP_PP_EXAMPLE),
                // 2 разных показных куки, один null, второй нет. отличается от кликовой куки, нужный пп
                generateClicksWithNullShowCookie(10, 11, FilterConstants.FILTER_0, WHITE_MARKET_DESKTOP_PP_EXAMPLE),
                // клики с разными непустыми show_cookie (откатываем) и с пустыми show_cookie(не откатываем) и совпадающими (не откатываем)
                generateClicksForFilterWithEmptyShowCookies(11)
                )
                .flatMap(Collection::stream)
                .collect(toList());
    }

    private static List<TestClick> generateClicks(int caseN, int amount, FilterConstants filter, int pp,
                                                  boolean cookieMatch, int showCookieCount) {
        String clickCookie = Cookie.generateYandexCookie(TEST_DT);
        String showCookie = cookieMatch ? clickCookie : Cookie.generateYandexCookie(TEST_DT.minusMinutes(5));
        int anotherCookieCount = (showCookieCount - 1);
        List<String> showCookies = generateShowCookiesWithDefault(amount, anotherCookieCount, showCookie);

        return getTestClicks(caseN, amount, filter, pp, clickCookie, showCookies);
    }

    private static List<TestClick> generateClicksWithEmptyCookie(int caseN, int amount, FilterConstants filter, int pp) {
        String clickCookie = "";
        String showCookie = Cookie.generateYandexCookie(TEST_DT.minusMinutes(5));
        List<String> showCookies = generateShowCookiesWithDefault(amount, 1, showCookie);
        return getTestClicks(caseN, amount, filter, pp, clickCookie, showCookies);
    }

    private static List<TestClick> generateClicksWithEmptyShowCookie(int caseN, int amount, FilterConstants filter, int pp) {
        String clickCookie = Cookie.generateYandexCookie(TEST_DT);
        List<String> showCookies = generateShowCookiesWithDefault(amount, 1, "");
        return getTestClicks(caseN, amount, filter, pp, clickCookie, showCookies);
    }

    private static List<TestClick> generateClicksWithNullShowCookie(int caseN, int amount, FilterConstants filter, int pp
    ) {
        String clickCookie = Cookie.generateYandexCookie(TEST_DT);
        List<String> showCookies = generateShowCookiesWithDefault(amount, 1, null);

        return getTestClicks(caseN, amount, filter, pp, clickCookie, showCookies);
    }

    private static List<TestClick> generateClicksForFilterWithEmptyShowCookies(int caseN) {
        String clickCookie = Cookie.generateYandexCookie(TEST_DT);
        // 3 разные непустые куки
        List<String> showCookies = Arrays.asList(
                Cookie.generateYandexCookie(TEST_DT.minusMinutes(1)),
                Cookie.generateYandexCookie(TEST_DT.minusMinutes(2)),
                Cookie.generateYandexCookie(TEST_DT.minusMinutes(3))
        );

        List<String> emptyShowCookies = Arrays.asList("", "", null);
        List<String> normalShowCookies = Arrays.asList(clickCookie, clickCookie, clickCookie, clickCookie, clickCookie);

        List<TestClick> clicksToBeRolledBack = getTestClicks(caseN, 3, FilterConstants.FILTER_41,
                WHITE_MARKET_DESKTOP_PP_EXAMPLE, clickCookie, showCookies);

        List<TestClick> clicksNotToBeRolledBack = getTestClicks(caseN, 3, FilterConstants.FILTER_0,
                WHITE_MARKET_DESKTOP_PP_EXAMPLE, clickCookie, emptyShowCookies);

        List<TestClick> clicksNotToBeRolledBack2 = getTestClicks(caseN, 5, FilterConstants.FILTER_0,
                WHITE_MARKET_DESKTOP_PP_EXAMPLE, clickCookie, normalShowCookies);

        List<TestClick> all = new ArrayList<>();
        all.addAll(clicksToBeRolledBack);
        all.addAll(clicksNotToBeRolledBack);
        all.addAll(clicksNotToBeRolledBack2);
        return all;
    }


    @NotNull
    private static List<String> generateShowCookiesWithDefault(int amount, int anotherCookieCount, String defaultShowCookie) {
        return IntStream.range(0, amount)
                .mapToObj(i -> {
                            if (i < anotherCookieCount) {
                                return Cookie.generateYandexCookie(TEST_DT.minusMinutes(i));
                            }
                            return defaultShowCookie;
                        }
                ).collect(toList());
    }

    @NotNull
    private static List<TestClick> getTestClicks(int caseN, int amount, FilterConstants filter, int pp, String clickCookie, List<String> showCookies) {
        return IntStream.range(0, amount)
                .mapToObj(i -> {
                            DateTime clickTime = TEST_DT.plusSeconds((i % 2) * 1000);
                            return ClickGenerator.generateUniqueClick("case_" + caseN + "_", clickTime)
                                    .set("pp", pp)
                                    .set("cookie", clickCookie)
                                    .set("show_cookie", showCookies.get(i))
                                    .set("shop_id", 777 + caseN)
                                    .setFilter(filter);
                        }
                )
                .collect(toList());
    }
}
