import {
    prepareSuite,
    makeSuite,
    mergeSuites,
} from 'ginny';

import {region} from '@self/root/src/spec/hermione/configs/geo';
import {commonParams} from '@self/root/src/spec/hermione/configs/params';
import ConfirmationPage from '@self/root/src/widgets/pages.touch/OrdersConfirmationPage/__pageObject';

import pageWrapperSuite from '@self/platform/spec/hermione/test-suites/blocks/order-confirmation-page-wrapper';
import multiSuite from '@self/root/src/spec/hermione/test-suites/blocks/orderConfirmation/multi';
import dropshipSuite from '@self/root/src/spec/hermione/test-suites/blocks/orderConfirmation/dropship';
import multiOrder from '@self/platform/spec/hermione/test-suites/blocks/orderConfirmation/unauthorizedOrder/multiOrder';
import storagePeriod from '@self/root/src/spec/hermione/test-suites/blocks/orderConfirmation/storagePeriod';
import alco from '@self/platform/spec/hermione/test-suites/blocks/alco/order-confirmation';
import auth from '@self/project/src/spec/hermione/test-suites/blocks/orderConfirmation/auth';
import marketBrandedSuite from '@self/root/src/spec/hermione/test-suites/blocks/orderConfirmation/marketBranded';
// Кэшбэк я+
import cashback from '@self/project/src/spec/hermione/test-suites/blocks/orderConfirmation/cashback';
// accessibility доступность
import orderConfirmationA11y from '@self/root/src/spec/hermione/test-suites/blocks/orderConfirmation/a11y';
import EntryPointToReferralProgram from '@self/root/src/spec/hermione/test-suites/blocks/orderConfirmation/entryPointToReferralProgram';
import yandexHelpOrderConfirmation from '@self/root/src/spec/hermione/test-suites/blocks/orderConfirmation/yandexHelpOrderConfirmation';

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('"Спасибо за заказ"', {
    environment: 'testing',
    params: {
        ...commonParams.description,
        items: 'Товары',
    },
    defaultParams: {
        ...commonParams.value,
        needMuid: false,
    },
    story: mergeSuites(
        {
            beforeEach() {
                this.setPageObjects({
                    confirmationPage: () => this.createPageObject(ConfirmationPage),
                });
            },
        },

        prepareSuite(pageWrapperSuite, {
            suiteName: 'Неавторизованный пользователь. "Спасибо за заказ".',
            params: {
                needMuid: true,
            },
        }),

        prepareSuite(pageWrapperSuite, {
            suiteName: 'Авторизованный пользователь. "Спасибо за заказ".',
            params: {
                isAuthWithPlugin: true,
            },
        }),

        prepareSuite(dropshipSuite, {
            params: {
                needMuid: true,
            },
        }),

        prepareSuite(marketBrandedSuite('bluemarket-4118'), {}),

        prepareSuite(multiSuite, {
            params: {
                region: region['Ростов-на-Дону'],
                regionName: 'Ростов-на-Дону',
            },
        }),

        prepareSuite(multiOrder, {
            params: {
                region: region['Москва'],
                regionName: 'Москва',
                isAuthWithPlugin: false,
            },
        }),

        prepareSuite(alco, {}),
        prepareSuite(storagePeriod, {}),

        prepareSuite(auth, {
            suiteName: 'Авторизованный пользователь. Блок доавторизации',
            params: {
                isAuthWithPlugin: true,
            },
        }),

        prepareSuite(auth, {
            suiteName: 'Неавторизованный пользователь. Блок доавторизации',
            params: {
                isAuthWithPlugin: false,
            },
        }),

        // Кэшбэк я+
        prepareSuite(cashback),
        prepareSuite(EntryPointToReferralProgram),
        prepareSuite(yandexHelpOrderConfirmation),
        prepareSuite(orderConfirmationA11y)
    ),
});
