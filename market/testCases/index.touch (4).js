import COOKIE_NAME from '@self/root/src/constants/cookie';
import {
    checkTooltipIsNotCreated,
} from '@self/root/src/widgets/content/PromoTooltip/common/__spec__/checkMethods';

import {MOCKS_DATA} from '../mockData';
import {
    checkContent,
    checkOpenByScrollAndAutoHide,
} from '../checkMethod';

const PLUS_CONTENT_TEST_CASES = {
    describe: {
        name: 'Для плюсовика',
        suites: [{
            caseName: 'по умолчанию',
            mockData: MOCKS_DATA.plus_with_threshold_balance,
            expectedTitle: 'Спишите баллы при покупке',
            expectedDescription: 'У вас 1 000 баллов',
            method: checkContent,
        }, {
            caseName: 'когда баланс больше цены товара',
            mockData: MOCKS_DATA.plus_with_balance_more_offer_price,
            expectedTitle: 'Купите этот товар за баллы',
            expectedDescription: 'У вас 100 000 баллов',
            method: checkContent,
        }],
    },
};

const NON_PLUS_CONTENT_TEST_CASES = {
    describe: {
        name: 'Для неплюсовика',
        suites: [{
            caseName: 'по умолчанию',
            mockData: MOCKS_DATA.non_plus_with_threshold_balance,
            expectedTitle: 'Спишите баллы при покупке',
            expectedDescription: 'У вас 1 000 баллов',
            method: checkContent,
        }, {
            caseName: 'когда баланс больше цены товара',
            mockData: MOCKS_DATA.non_plus_with_balance_more_offer_price,
            expectedTitle: 'Купите этот товар за баллы',
            expectedDescription: 'У вас 100 000 баллов',
            method: checkContent,
        }],
    },
};

const OFFER_TEST_CASES = {
    NOT_SHOW_CASES: [{
        caseName: 'при выставленной YA_PLUS_TOAST_ON_SKU_SHOW куки',
        mockData: MOCKS_DATA.non_plus_with_balance_more_offer_price,
        ctxMock: {
            request: {
                cookie: {
                    [COOKIE_NAME.YA_PLUS_TOAST_ON_SKU_SHOW]: '1',
                },
            },
        },
        method: checkTooltipIsNotCreated,
    }, {
        caseName: 'когда для пользователя не доступен кэшбэк',
        mockData: MOCKS_DATA.restricted_cashbac_balance,
        method: checkTooltipIsNotCreated,
    }, {
        caseName: 'для плюсовика при выключенном тогле',
        mockData: MOCKS_DATA.off_show_tooltip_toggle,
        method: checkTooltipIsNotCreated,
    }, {
        caseName: 'когда баланс плюса меньше заданного порога',
        mockData: MOCKS_DATA.user_balance_less_than_threshold,
        method: checkTooltipIsNotCreated,
    }, {
        caseName: 'для не cpa офера',
        mockData: MOCKS_DATA.non_cpa_offer,
        method: checkTooltipIsNotCreated,
    }, {
        caseName: 'для офера не доступно списание баллов',
        mockData: MOCKS_DATA.offer_with_restrected_spend,
        method: checkTooltipIsNotCreated,
    }],
    SHOW_CASES: [{
        describe: {
            name: 'Контент отображается корректно',
            suites: [NON_PLUS_CONTENT_TEST_CASES, PLUS_CONTENT_TEST_CASES],
        },
    }, {
        caseName: 'корректно автоматически открывается и закрывается',
        mockData: MOCKS_DATA.non_plus_with_threshold_balance,
        method: checkOpenByScrollAndAutoHide,
    },
    // TODO MARKETFRONTECH-4441
    //     {
    //     caseName: 'корректно автоматически открывается и закрывается',
    //     mockData: MOCKS_DATA.non_plus_with_threshold_balance,
    //     method: checkCloseOnEscKeyPress,
    // }
    ],
};

export const TEST_CASES = [{
    describe: {
        name: 'Не отображается',
        suites: OFFER_TEST_CASES.NOT_SHOW_CASES,
    },
}, {
    describe: {
        name: 'Отображается',
        suites: OFFER_TEST_CASES.SHOW_CASES,
    },
}];
