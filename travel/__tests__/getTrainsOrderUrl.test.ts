import {TLD} from 'constants/tld';

import {getTrainsOrderUrl} from '../getTrainsOrderUrl';

const DEFAULT_PRODUCTION_BASE_PATH = 'https://trains.yandex.ru';
const DEFAULT_TESTING_BASE_PATH = 'https://testing.train.common.yandex.ru';
const DEFAULT_UID = '0cc5e6189e0e4880fa00c7ed12a1ecff';

describe('getTrainsOrderUrl', () => {
    test('Должен вернуть url с нужным uid заказа для тестинга', () => {
        const originalNodeEnv = process.env.NODE_ENV;

        process.env.NODE_ENV = 'testing';

        expect(getTrainsOrderUrl(DEFAULT_UID, TLD.RU)).toBe(
            `${DEFAULT_TESTING_BASE_PATH}/orders/${DEFAULT_UID}/`,
        );

        process.env.NODE_ENV = originalNodeEnv;
    });

    test('Должен вернуть url с нужным uid заказа для продакшена', () => {
        const originalNodeEnv = process.env.NODE_ENV;

        process.env.NODE_ENV = 'production';

        expect(getTrainsOrderUrl(DEFAULT_UID, TLD.RU)).toBe(
            `${DEFAULT_PRODUCTION_BASE_PATH}/orders/${DEFAULT_UID}/`,
        );

        process.env.NODE_ENV = originalNodeEnv;
    });
});
