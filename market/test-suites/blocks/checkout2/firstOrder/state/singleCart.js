import {
    prepareSuite,
    makeSuite,
    mergeSuites,
} from 'ginny';

import * as kettle from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';
import {deliveryDeliveryMock} from '@self/root/src/spec/hermione/kadavr-mock/checkouter/delivery';
import {prepareMultiCartState} from '@self/root/src/spec/hermione/scenarios/cartResource';
import {prepareCheckoutPage} from '@self/root/src/spec/hermione/scenarios/checkout';
import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';

import singleCartProcess from './singleCartProcess';

export default makeSuite('сохранение данных получателя/адреса доставки/способ доставки курьером/способом оплаты', {
    feature: 'сохранение данных получателя/адреса доставки/способ доставки курьером/способом оплаты',
    environment: 'kadavr',
    story: mergeSuites(
        {
            async beforeEach() {
                const carts = [
                    buildCheckouterBucket({
                        items: [{
                            skuMock: kettle.skuMock,
                            offerMock: kettle.offerMock,
                            count: 1,
                        }],
                        deliveryOptions: [
                            deliveryDeliveryMock,
                            {
                                ...deliveryDeliveryMock,
                                id: 'mockedId1',
                                hash: 'mockedHash1',
                                dates: {
                                    fromDate: '05-03-2024',
                                    toDate: '06-03-2024',
                                },
                                deliveryOptionId: 1003938,
                            },
                        ],
                    }),
                ];

                const testState = await this.browser.yaScenario(
                    this,
                    prepareMultiCartState,
                    carts
                );

                await this.browser.setState('persAddress.lastState', {
                    paymentType: null,
                    paymentMethod: null,
                    contactId: null,
                    parcelsInfo: null,
                });

                await this.browser.yaScenario(
                    this,
                    prepareCheckoutPage,
                    {
                        items: testState.checkoutItems,
                        reportSkus: testState.reportSkus,
                        checkout2: true,
                    }
                );
            },
        },
        prepareSuite(singleCartProcess, {
            meta: {
                id: 'm-touch-3641',
                issue: 'MARKETFRONT-50238',
                feature: 'при возврате на страницу корзины под гостем',
            },
            suiteName: 'при возврате на страницу корзины под гостем',
        }),
        prepareSuite(singleCartProcess, {
            meta: {
                id: 'm-touch-3642',
                issue: 'MARKETFRONT-50238',
                feature: 'при возврате на страницу корзины авторизованным пользователем',
            },
            params: {
                isAuthWithPlugin: true,
            },
            suiteName: 'при возврате на страницу корзины авторизованным пользователем',
        })
    ),
});
