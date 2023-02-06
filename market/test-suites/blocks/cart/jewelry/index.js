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
import CartOrderInfo from '@self/root/src/widgets/content/cart/CartTotalInformation/components/View/__pageObject';
import CartParcel from '@self/root/src/widgets/content/cart/CartList/components/CartParcel/__pageObject';
import CartItem from '@self/root/src/widgets/content/cart/CartList/components/CartItem/__pageObject';
import BusinessGroupsStrategiesSelector
    from '@self/root/src/widgets/content/cart/CartList/components/BusinessGroupsStrategiesSelector/__pageObject';

// mocks
import * as dsbs from '@self/root/src/spec/hermione/kadavr-mock/report/dsbs';
import {deliveryDeliveryMock} from '@self/root/src/spec/hermione/kadavr-mock/checkouter/delivery';

// suites
import totalInfoSuite from '@self/root/src/spec/hermione/test-suites/blocks/cart/page/totalInfo';
import cartItemInfoSuite from '@self/root/src/spec/hermione/test-suites/blocks/cart/page/cartItemInfo';
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
        shopId: dsbs.offerPhoneMock?.shop?.id,
        validationErrors: [{
            code: 'JEWELRY_COST_LIMIT_200K',
            severity: 'ERROR',
            type: 'basic',
        }],
        validationWarnings: [{
            code: 'JEWELRY_PASSPORT_REQUIRED',
            severity: 'WARNING',
            type: 'basic',
        }],
    }),
];

module.exports = makeSuite('Ограничения ювелирки', {
    environment: 'kadavr',
    issue: 'MARKETFRONT-54745',

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
                    // Чаще всего это именно DBS
                    dsbsCarts
                );

                await this.browser.yaOpenPage(PAGE_IDS_COMMON.CART, {lr: 213});

                await this.browser.yaScenario(this, waitForCartActualization);
            },
        },

        prepareSuite(totalInfoSuite, {
            meta: {
                issue: 'MARKETFRONT-54745',
                id: 'marketfront-5073',
            },
            params: {
                totalPrice: Number(dsbs.offerPhoneMock.prices.value),
                checkoutBtnDisabled: true,
                checkoutBtnErrorText: params.hasCheckoutBtnText
                    ? 'Нельзя заказать товары от продавца сайт.рф более чем на 200 000 ₽'
                    : undefined,
                errorOpenerText: params.hasCheckoutBtnText
                    ? 'У продавца сайт.рф нельзя оформить заказ с ювелирными ' +
                    'изделиями дороже 200 000 ₽ — это ограничение закона'
                    : undefined,
            },
        }),

        prepareSuite(cartItemInfoSuite, {
            meta: {
                issue: 'MARKETFRONT-54745',
                id: 'marketfront-5075',
            },
            params: {
                hasRemoveBtn: true,
                hasWishlistBtn: params.hasWishlistBtn,
                cantAddAmount: true,
            },
        }),

        prepareSuite(cartParcelSuite, {
            meta: {
                issue: 'MARKETFRONT-54745',
                id: 'marketfront-5077',
            },
            params: {
                date: 'c 23 февраля',
                supplier: 'продавца',
                count: 1,
                errorText: params.hasParcelError
                    ? 'Нельзя заказать более чем на 200 000 ₽ у продавца сайт.рф'
                    : undefined,
                errorPopupTitle: params.hasParcelError
                    ? 'Уменьшите заказ до 200 000 ₽'
                    : undefined,
                errorPopupText: params.hasParcelError
                    ? 'У продавца сайт.рф нельзя оформить заказ с ювелирными ' +
                    'изделиями дороже 200 000 ₽ — это ограничение закона'
                    : undefined,
            },
        })
    ),
});
