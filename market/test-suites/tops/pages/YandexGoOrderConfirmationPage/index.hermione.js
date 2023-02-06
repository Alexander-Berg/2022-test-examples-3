import {makeSuite, prepareSuite, mergeSuites} from '@yandex-market/ginny';
import {createUser} from '@yandex-market/kadavr/mocks/PersQa/helpers';

import {YANDEX_GO_EATS_KIT_TEST_COOKIE} from '@self/root/src/constants/eatsKit';
// configs
import {profiles} from '@self/platform/spec/hermione2/configs/profiles';
// suites
import OrderInfoUnpaid from '@self/platform/spec/hermione2/test-suites/blocks/YandexGoOrderConfirmation/orderInfoUnpaid';
import checkoutItemIds from '@self/root/src/spec/hermione/configs/checkout/items';
// page-objects
import YandexGoOrderInfoUnpaid from '@self/root/src/widgets/parts/OrderConfirmation/components/YandexGoOrderInfoUnpaid/__pageObject';

import {prepareUnpaidOrderConfirmationPage} from './utils';

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Страница подтверждения заказа Yandex Go.', {
    defaultParams: {
        items: [checkoutItemIds.asus],
        buyerTotal: checkoutItemIds.asus.buyerPrice,
        orderId: '12345',
    },
    environment: 'kadavr',
    issue: 'MARKETFRONT-62536',
    story: mergeSuites(
        {
            async beforeEach() {
                const testUser = profiles['pan-topinambur'];

                const currentUser = createUser({
                    id: testUser.uid,
                    uid: {
                        value: testUser.uid,
                    },
                    login: testUser.login,
                    display_name: {
                        name: 'Willy Wonka',
                        public_name: 'Willy W.',
                        avatar: {
                            default: '61207/462703116-1544492602',
                            empty: false,
                        },
                    },
                    dbfields: {
                        'userinfo.firstname.uid': 'Willy',
                        'userinfo.lastname.uid': 'Wonka',
                    },
                    public_id: testUser.publicId,
                });

                await this.browser.setState('schema', {
                    users: [currentUser],
                });

                await this.browser.yaLogin(
                    testUser.login,
                    testUser.password
                );

                await this.browser.yaSetCookie({name: YANDEX_GO_EATS_KIT_TEST_COOKIE, value: '1', path: '/'});
            },
        },
        mergeSuites(
            {
                async beforeEach() {
                    return prepareUnpaidOrderConfirmationPage.call(this, {
                        orderId: this.params.orderId,
                        items: this.params.items,
                        buyerTotal: this.params.buyerTotal,
                    });
                },
            },
            prepareSuite(OrderInfoUnpaid, {
                pageObjects: {
                    yandexGoOrderInfoUnpaid() {
                        return this.browser.createPageObject(YandexGoOrderInfoUnpaid);
                    },
                },
            })
        )
    ),
});
