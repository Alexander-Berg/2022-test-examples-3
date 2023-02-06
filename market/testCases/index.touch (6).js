import {
    checkContent,
    checkWidgetNotExist,
} from '../checkMethods/';
import {MOCKS_DATA, CASHBACK_AMOUNT, PLUS_DELIVERY_COST} from '../mocks';

const TIER_1_PLUS_TEST_CASES = {
    describe: {
        name: 'ТИР-1.',
        suites: [{
            caseName: 'Заказ с начислением.',
            mockData: MOCKS_DATA.plus_user_with_cashback_emit_offer_tier_1,
            method: checkContent,
            expectedTitle: 'Баллы придут с заказом',
            expectedDescription: 'Потратьте их на следующую покупку',
            expectedPrice: 200,
        }, {
            caseName: 'Заказ со списанием.',
            mockData: MOCKS_DATA.plus_user_with_cashback_spend_offer_tier_1,
            method: checkContent,
            expectedTitle: 'Выгоднее с Плюсом',
            expectedDescription: 'Отлично! Вы сэкономили на покупке благодаря Плюсу',
            expectedPrice: `${CASHBACK_AMOUNT} ₽`,
        }, {
            caseName: 'Заказ без списания и начисления',
            mockData: MOCKS_DATA.plus_user_without_spend_and_emit_offer_tier_1,
            method: checkWidgetNotExist,
        }],
    },
};

const TIER_3_PLUS_TEST_CASES = {
    describe: {
        name: 'ТИР-3.',
        suites: [{
            caseName: 'Заказ с начислением.',
            mockData: MOCKS_DATA.plus_user_with_cashback_emit_offer_tier_3,
            method: checkContent,
            expectedTitle: 'Баллы придут с заказом',
            expectedDescription: 'Потратьте их на следующую покупку',
            expectedPrice: 200,
        }, {
            caseName: 'Заказ со списанием.',
            mockData: MOCKS_DATA.plus_user_with_cashback_spend_offer_tier_3,
            method: checkContent,
            expectedTitle: 'Выгоднее с Плюсом',
            expectedDescription: 'Отлично! Вы сэкономили на покупке благодаря Плюсу',
            expectedPrice: '200 ₽',
        }, {
            caseName: 'Заказ без списания и начисления',
            mockData: MOCKS_DATA.plus_user_without_spend_and_emit_offer_tier_3,
            method: checkWidgetNotExist,
        }],
    },
};

const TIER_1_MINUS_TEST_CASES = {
    describe: {
        name: 'ТИР-1.',
        suites: [{
            caseName: 'Заказ с начислением.',
            mockData: MOCKS_DATA.non_plus_user_with_cashback_emit_offer_tier_1,
            method: checkContent,
            expectedTitle: 'Баллы придут с заказом',
            expectedDescription: 'С подпиской Яндекс Плюс их можно тратить в сервисах Яндекса и пользоваться бесплатной доставкой',
            expectedPrice: 200,
        }, {
            caseName: 'Заказ без списания и начисления',
            mockData: MOCKS_DATA.non_plus_user_without_spend_and_emit_offer_tier_1,
            method: checkContent,
            expectedDescription: `Подключите Плюс — это кешбэк баллами, бесплатная доставка заказов от ${PLUS_DELIVERY_COST} ₽ (зависит от города) и многое другое`,
            expectedLink: 'Читать про Плюс',
        }],
    },
};

const TIER_3_MINUS_TEST_CASES = {
    describe: {
        name: 'ТИР-3.',
        suites: [{
            caseName: 'Заказ с начислением.',
            mockData: MOCKS_DATA.non_plus_user_with_cashback_emit_offer_tier_3,
            method: checkContent,
            expectedTitle: 'Баллы придут с заказом',
            expectedDescription: 'С подпиской Яндекс Плюс их можно тратить в сервисах Яндекса',
            expectedPrice: 200,
            expectedLink: 'Подключить Яндекс Плюс',
        }, {
            caseName: 'Заказ без списания и начисления',
            mockData: MOCKS_DATA.non_plus_user_without_spend_and_emit_offer_tier_3,
            method: checkContent,
            expectedDescription: 'Подключите Плюс — это кешбэк баллами на Маркете и в других сервисах Яндекса, а ещё подписка на кино и музыку',
            expectedLink: 'Читать про Плюс',
        }],
    },
};

export const TEST_CASES = [{
    describe: {
        name: 'пользователь с подпиской Яндекс.Плюс',
        suites: [TIER_1_PLUS_TEST_CASES, TIER_3_PLUS_TEST_CASES],
    },
}, {
    describe: {
        name: 'пользователь без подписки Яндекс.Плюс',
        suites: [TIER_1_MINUS_TEST_CASES, TIER_3_MINUS_TEST_CASES],
    },
}];
