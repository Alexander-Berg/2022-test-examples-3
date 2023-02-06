package ru.yandex.market.api.partner.controllers.model.getmodels;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.api.partner.controllers.util.PagerHelper;
import ru.yandex.market.api.partner.request.InvalidRequestException;
import ru.yandex.market.api.partner.request.PartnerServletRequest;
import ru.yandex.market.checkout.common.rest.Pager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Тест для пейджера в ручке {@link ru.yandex.market.api.partner.controllers.model.ModelControllerV1#getModels(
 *long, String, String, int, int, PartnerServletRequest)}
 *
 * @author belmatter
 */
class PagerHelperTest {

    private static final int PAGE_SIZE = 10;

    static Stream<Arguments> buildPagerForGetModelsData() {
        return Stream.of(
                //Пустая выдача
                Arguments.of(0, PAGE_SIZE, 0, 0, new Pager(0, 0, 0, 0, 0, 0)),
                //Проверка пейджера на первой странице
                Arguments.of(1, PAGE_SIZE, 13, 10, new Pager(13, 1, 10, 10, 2, 1)),
                //Проверка пейджера на второй странице
                Arguments.of(2, PAGE_SIZE, 13, 3, new Pager(13, 11, 13, 3, 2, 2)),
                //Проверка пейджера при выдаче лишь с одной страницей
                Arguments.of(1, 20, 13, 13, new Pager(13, 1, 13, 13, 1, 1)),
                //Проверка пейджера на странице большей, чем всего в выдаче страниц
                Arguments.of(3, PAGE_SIZE, 14, 4, new Pager(14, 11, 14, 4, 2, 2))
        );
    }

    @ParameterizedTest
    @MethodSource("buildPagerForGetModelsData")
    void buildPagerForGetModels(int pageNum, int pageSize, int total, int onPage, Pager expectedPager) {
        Pager actualPager = PagerHelper.buildPagerForGetModels(pageNum, pageSize, total, onPage);
        assertThat(actualPager).isEqualTo(expectedPager);
    }

    @Test
    void initPager() {
        assertThat(PagerHelper.initPager(1, PAGE_SIZE, 50, Integer.MAX_VALUE))
                .isEqualTo(new Pager(null, 1, 10, PAGE_SIZE, null, 1));
        assertThat(PagerHelper.initPager(3, PAGE_SIZE, 50, Integer.MAX_VALUE))
                .isEqualTo(new Pager(null, 21, 30, PAGE_SIZE, null, 3));
        assertThatExceptionOfType(InvalidRequestException.class)
                .isThrownBy(() -> PagerHelper.initPager(0, PAGE_SIZE, 50, Integer.MAX_VALUE));
        assertThatExceptionOfType(InvalidRequestException.class)
                .isThrownBy(() -> PagerHelper.initPager(1, 1000, 50, Integer.MAX_VALUE));
    }

    @Test
    void addPostProcessData() {
        var pagerMiddle = PagerHelper.initPager(3, PAGE_SIZE, 50, Integer.MAX_VALUE);
        PagerHelper.addPostProcessData(pagerMiddle, PAGE_SIZE);
        assertThat(pagerMiddle)
                .isEqualTo(new Pager(null, 21, 30, PAGE_SIZE, null, 3));

        var pagerEnd = PagerHelper.initPager(3, PAGE_SIZE, 50, Integer.MAX_VALUE);
        PagerHelper.addPostProcessData(pagerEnd, 7);
        assertThat(pagerEnd)
                .as("total и pagesCount обновляются, если нашли меньше, чем запрашивали")
                .isEqualTo(new Pager(null, 21, 27, 7, null, 3));

        var pagerEmpty = PagerHelper.initPager(3, PAGE_SIZE, 50, Integer.MAX_VALUE);
        PagerHelper.addPostProcessData(pagerEmpty, 0);
        assertThat(pagerEmpty)
                .isEqualTo(new Pager(null, 21, 20, 0, null, 3));
    }
}
