import {
    makeSuite,
    prepareSuite,
    mergeSuites,
} from 'ginny';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';

// scenarios
import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';
import {prepareMultiCartState} from '@self/root/src/spec/hermione/scenarios/cartResource';
import {waitForCartActualization} from '@self/root/src/spec/hermione/scenarios/cart';

// pageObjects
import CartOrderInfo
    from '@self/root/src/widgets/content/cart/CartTotalInformation/components/View/__pageObject';
import CartParcel from '@self/root/src/widgets/content/cart/CartList/components/CartParcel/__pageObject';
import CartItem from '@self/root/src/widgets/content/cart/CartList/components/CartItem/__pageObject';
import BusinessGroupsStrategiesSelector
    from '@self/root/src/widgets/content/cart/CartList/components/BusinessGroupsStrategiesSelector/__pageObject';

// mocks
import * as dsbs from '@self/root/src/spec/hermione/kadavr-mock/report/dsbs';
import {deliveryDeliveryMock} from '@self/root/src/spec/hermione/kadavr-mock/checkouter/delivery';

// suites
import mainInfoSuite from '@self/root/src/spec/hermione/test-suites/blocks/cart/page/mainInfo';
import cartItemInfoSuite from '@self/root/src/spec/hermione/test-suites/blocks/cart/page/cartItemInfo';
import totalInfoSuite from '@self/root/src/spec/hermione/test-suites/blocks/cart/page/totalInfo';
import cartParcelSuite from '@self/root/src/spec/hermione/test-suites/blocks/cart/page/cartParcelInfo';

import params from './params';

const dsbsCarts = [
    buildCheckouterBucket({
        items: [{
            skuMock: dsbs.skuPhoneMock,
            offerMock: dsbs.offerPhoneMock,
            count: 1,
        }],
        deliveryOptions: [{
            ...deliveryDeliveryMock,
            deliveryPartnerType: 'SHOP',
        }],
    }),
];

module.exports = makeSuite('Для dsbs офера', {
    environment: 'kadavr',
    id: 'marketfront-4347',
    issue: 'MARKETFRONT-35402',

    story: mergeSuites(
        {
            async beforeEach() {
                this.setPageObjects({
                    cartTotalInformation: () => this.createPageObject(CartOrderInfo),
                    cart: () => this.createPageObject(CartParcel),
                    cartItem: () => this.createPageObject(CartItem, {
                        parent: this.cart,
                    }),
                    removedCartItem: () => this.createPageObject(
                        CartItem,
                        {
                            parent: this.cartGroup,
                            root: `${BusinessGroupsStrategiesSelector.bucket(0)} ${CartItem.root}`,
                        }
                    ),
                });

                await this.browser.yaScenario(
                    this,
                    prepareMultiCartState,
                    dsbsCarts
                );

                await this.browser.yaOpenPage(PAGE_IDS_COMMON.CART, {lr: 213});

                await this.browser.yaScenario(this, waitForCartActualization);
            },
        },

        prepareSuite(mainInfoSuite, {
            params: {
                itemCount: 1,
            },
        }),

        prepareSuite(totalInfoSuite, {
            params: {
                totalPrice: Number(dsbs.offerPhoneMock.prices.value),
            },
        }),

        prepareSuite(cartParcelSuite, {
            params: {
                date: 'c 23 февраля',
                supplier: 'продавца',
                count: 1,
            },
        }),

        prepareSuite(cartItemInfoSuite, {
            params: {
                hasRemoveBtn: true,
                hasWishlistBtn: params.hasWishlistBtn,
            },
        })
    ),
});
