package ru.yandex.market.tpl.core.query.usershift.mapper;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author valter
 */
class RussianTextUtilTest {

    @Test
    void warningTitleNotFinishedEnding() {
        assertThat(RussianTextUtil.warningTitleNotFinished(0))
                .isEqualTo("У вас не завершено 0 доставок");
        assertThat(RussianTextUtil.warningTitleNotFinished(1))
                .isEqualTo("У вас не завершена 1 доставка");
        assertThat(RussianTextUtil.warningTitleNotFinished(2))
                .isEqualTo("У вас не завершены 2 доставки");
        assertThat(RussianTextUtil.warningTitleNotFinished(3))
                .isEqualTo("У вас не завершены 3 доставки");
        assertThat(RussianTextUtil.warningTitleNotFinished(4))
                .isEqualTo("У вас не завершены 4 доставки");
        assertThat(RussianTextUtil.warningTitleNotFinished(5))
                .isEqualTo("У вас не завершено 5 доставок");
        assertThat(RussianTextUtil.warningTitleNotFinished(6))
                .isEqualTo("У вас не завершено 6 доставок");
        assertThat(RussianTextUtil.warningTitleNotFinished(7))
                .isEqualTo("У вас не завершено 7 доставок");
        assertThat(RussianTextUtil.warningTitleNotFinished(8))
                .isEqualTo("У вас не завершено 8 доставок");
        assertThat(RussianTextUtil.warningTitleNotFinished(9))
                .isEqualTo("У вас не завершено 9 доставок");
        assertThat(RussianTextUtil.warningTitleNotFinished(10))
                .isEqualTo("У вас не завершено 10 доставок");
        assertThat(RussianTextUtil.warningTitleNotFinished(11))
                .isEqualTo("У вас не завершено 11 доставок");
        assertThat(RussianTextUtil.warningTitleNotFinished(12))
                .isEqualTo("У вас не завершено 12 доставок");
        assertThat(RussianTextUtil.warningTitleNotFinished(19))
                .isEqualTo("У вас не завершено 19 доставок");
        assertThat(RussianTextUtil.warningTitleNotFinished(20))
                .isEqualTo("У вас не завершено 20 доставок");
        assertThat(RussianTextUtil.warningTitleNotFinished(21))
                .isEqualTo("У вас не завершена 21 доставка");
        assertThat(RussianTextUtil.warningTitleNotFinished(24))
                .isEqualTo("У вас не завершены 24 доставки");
        assertThat(RussianTextUtil.warningTitleNotFinished(25))
                .isEqualTo("У вас не завершено 25 доставок");
        assertThat(RussianTextUtil.warningTitleNotFinished(100))
                .isEqualTo("У вас не завершено 100 доставок");
        assertThat(RussianTextUtil.warningTitleNotFinished(112))
                .isEqualTo("У вас не завершено 112 доставок");
        assertThat(RussianTextUtil.warningTitleNotFinished(121))
                .isEqualTo("У вас не завершена 121 доставка");
        assertThat(RussianTextUtil.warningTitleNotFinished(122))
                .isEqualTo("У вас не завершены 122 доставки");
        assertThat(RussianTextUtil.warningTitleNotFinished(129))
                .isEqualTo("У вас не завершено 129 доставок");
    }

}
