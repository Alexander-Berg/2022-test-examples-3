
import COOKIE_NAME from '@self/root/src/constants/cookie';
import {MARKET_CASHBACK_PERCENT} from '@self/root/src/entities/perkStatus/perks/yandexCashback';

import {
    checkTooltipNotShown,
    checkTooltipIsNotCreated,
} from '@self/root/src/widgets/content/PromoTooltip/common/__spec__/checkMethods';
import {
    checkContent,
    checkAutoOpenAndHide,
    checkCloseByClickToCloser,
    checkCloseOnEscKeyPress,
} from '../checkMethods/';

import {
    MOCKS_DATA,
} from '../mockData';

// Все тестовые сценарии

const PLUS_CONTENT_TEST_CASES = {
    describe: {
        name: 'Для плюсовика',
        suites: [{
            caseName: 'с балансом',
            mockData: MOCKS_DATA.plus_with_balance_with_delivery,
            expectedTitle: 'У вас 1 000 баллов',
            expectedDescription: 'Ими можно оплачивать покупки',
            expectedLink: 'Подробнее',
            method: checkContent,
        }, {
            caseName: 'с нулевым балансом в регионе с бесплатной доставкой',
            mockData: MOCKS_DATA.plus_with_empty_balance_with_delivery,
            expectedTitle: 'Покупки выгоднее с Плюсом',
            expectedDescription: 'Кешбэк баллами и бесплатная доставка',
            expectedLink: 'Подробнее',
            method: checkContent,
        }, {
            caseName: 'с нулевым балансом в регионе без бесплатной доставки',
            mockData: MOCKS_DATA.plus_with_empty_balance_without_delivery,
            expectedTitle: 'Покупки выгоднее с Плюсом',
            expectedDescription: `Кешбэк баллами до ${MARKET_CASHBACK_PERCENT}%`,
            expectedLink: 'Подробнее',
            method: checkContent,
        }],
    },
};

const NON_PLUS_CONTENT_TEST_CASES = {
    describe: {
        name: 'Для неплюсовика',
        suites: [{
            caseName: 'с баллами',
            mockData: MOCKS_DATA.non_plus_with_balance_with_delivery,
            expectedTitle: 'У вас 1 000 баллов',
            expectedDescription: 'Подключите Плюс, чтобы их тратить',
            expectedLink: 'Подробнее',
            method: checkContent,
        }, {
            caseName: 'с нулевым балансом в регионе с бесплатной доставкой',
            mockData: MOCKS_DATA.non_plus_with_empty_balance_with_delivery,
            expectedTitle: 'Покупки выгоднее с Плюсом',
            expectedDescription: 'Кешбэк баллами и бесплатная доставка',
            expectedLink: 'Подробнее',
            method: checkContent,
        }, {
            caseName: 'с нулевым балансом в регионе без бесплатной доставки',
            mockData: MOCKS_DATA.non_plus_with_empty_balance_without_delivery,
            expectedTitle: 'Покупки выгоднее с Плюсом',
            expectedDescription: `Кешбэк баллами до ${MARKET_CASHBACK_PERCENT}%`,
            expectedLink: 'Подробнее',
            method: checkContent,
        }],
    },
};

export const COMMON_TEST_CASES = {
    // Тултип не отображается
    NOT_SHOW_CASES: [{
        caseName: 'при выставленной CASHBACK_ONBOARDING куки',
        mockData: MOCKS_DATA.plus_with_cookie,
        ctxMock: {
            request: {
                cookie: {
                    [COOKIE_NAME.CASHBACK_ONBOARDING]: '1',
                },
            },
        },
        method: checkTooltipNotShown,
    }, {
        caseName: 'кешбэк не доступен для пользователя',
        mockData: MOCKS_DATA.plus_with_restricted_cashback,
        method: checkTooltipIsNotCreated,
    }, {
        caseName: 'пользователь уже видел тултип с таким контентом',
        mockData: MOCKS_DATA.plus_with_shown_tooltip,
        method: checkTooltipNotShown,
    }],
    // Тултип отображается
    SHOW_CASES: [{
        describe: {
            name: 'Контент отображается корректно',
            suites: [PLUS_CONTENT_TEST_CASES, NON_PLUS_CONTENT_TEST_CASES],
        },
    }, {
        caseName: 'корректно автоматически открывается и закрывается',
        mockData: MOCKS_DATA.plus_with_balance_with_delivery,
        method: checkAutoOpenAndHide,
    }, {
        caseName: 'корректно автоматически открывается и закрывается',
        mockData: MOCKS_DATA.plus_with_balance_with_delivery,
        method: checkCloseOnEscKeyPress,
    }, {
        caseName: 'корректно автоматически открывается и закрывается',
        mockData: MOCKS_DATA.plus_with_balance_with_delivery,
        method: checkCloseByClickToCloser,
    }],
};

export const TEST_CASES = [{
    describe: {
        name: 'Не отображается',
        suites: COMMON_TEST_CASES.NOT_SHOW_CASES,
    },
}, {
    describe: {
        name: 'Отображается',
        suites: COMMON_TEST_CASES.SHOW_CASES,
    },
}];
