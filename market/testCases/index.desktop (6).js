import {
    checkContent,
} from '../checkMethods/';

import {MOCKS_DATA, widgetParams} from '../mocks';
import {VIEW_TYPE} from '../../constants';
const WITH_PHONE_TEST_CASES = {
    describe: {
        name: 'c телефоном',
        suites: [{
            caseName: 'Заказ обратного звонка c КМ.',
            mockData: MOCKS_DATA.authed_user_with_phone,
            method: checkContent,
            widgetParams: widgetParams[VIEW_TYPE.PRODUCT_CARD],
        }, {
            caseName: 'Заказ обратного звонка со страницы Вопросов.',
            mockData: MOCKS_DATA.authed_user_with_phone,
            method: checkContent,
            widgetParams: widgetParams[VIEW_TYPE.QUESTIONS],
        }],
    },
};

const WITHOUT_PHONE_TEST_CASES = {
    describe: {
        name: 'без телефона',
        suites: [{
            caseName: 'Заказ обратного звонка с КМ.',
            mockData: MOCKS_DATA.authed_user_without_phone,
            method: checkContent,
            widgetParams: widgetParams[VIEW_TYPE.PRODUCT_CARD],
        }, {
            caseName: 'Заказ обратного звонка со страницы Вопросов.',
            mockData: MOCKS_DATA.authed_user_without_phone,
            method: checkContent,
            widgetParams: widgetParams[VIEW_TYPE.QUESTIONS],
        }],
    },
};

const UNAUTHED_USER_TEST_CASES = {
    describe: {
        name: 'без телефона',
        suites: [{
            caseName: 'Заказ обратного звонка с КМ.',
            mockData: MOCKS_DATA.non_user_without_phone,
            method: checkContent,
            widgetParams: widgetParams[VIEW_TYPE.PRODUCT_CARD],
        }, {
            caseName: 'Заказ обратного звонка со страницы Вопросов.',
            mockData: MOCKS_DATA.non_user_without_phone,
            method: checkContent,
            widgetParams: widgetParams[VIEW_TYPE.QUESTIONS],
        }],
    },
};

export const TEST_CASES = [{
    describe: {
        name: 'Авторизованный пользователь',
        suites: [WITH_PHONE_TEST_CASES, WITHOUT_PHONE_TEST_CASES],
    },
}, {
    describe: {
        name: 'пользователь не авторизован',
        suites: [UNAUTHED_USER_TEST_CASES],
    },
}];
