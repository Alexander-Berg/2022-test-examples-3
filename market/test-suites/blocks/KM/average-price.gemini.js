import AveragePrice from '@self/platform/spec/page-objects/w-average-price';
// Скип от https://st.yandex-team.ru/MARKETFRONT-76159
import {HARDCODE_HIDE_AVERAGE_PRICE} from '@self/root/src/constants/productPage';

const Suite = {
    suiteName: 'AveragePrice',
    selector: AveragePrice.content,
    capture() {},
};

/**
 * Скип от https://st.yandex-team.ru/MARKETFRONT-76159
 * Удалить If целиком при выпиливании
 */
if (HARDCODE_HIDE_AVERAGE_PRICE) {
    Suite.capture = undefined;
}

export default Suite;
