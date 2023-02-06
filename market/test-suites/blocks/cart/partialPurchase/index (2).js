import {
    makeSuite,
    prepareSuite,
    mergeSuites,
} from 'ginny';

import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';
import {prepareCartPageBySkuId} from '@self/platform/spec/hermione/scenarios/cart';
import {prepareMultiCartState} from '@self/root/src/spec/hermione/scenarios/cartResource';
import {region} from '@self/root/src/spec/hermione/configs/geo';
import OrderTotal from '@self/root/src/components/OrderTotalV2/__pageObject';
import CartCheckoutButton from '@self/root/src/widgets/content/cart/CartCheckoutControl/components/CartCheckoutButton/__pageObject';
import CartOffer from '@self/root/src/widgets/content/cart/CartList/components/CartOffer/__pageObject';
import {SummaryPlaceholder} from '@self/root/src/components/OrderTotalV2/components/SummaryPlaceholder/__pageObject';
import RemoveCartItemContainer
    from '@self/root/src/widgets/content/cart/CartList/containers/RemoveCartItemContainer/__pageObject';
import CartItem from '@self/root/src/widgets/content/cart/CartList/components/CartItem/__pageObject';
import CartTotalInformation
    from '@self/root/src/widgets/content/cart/CartTotalInformation/components/View/__pageObject';
import Checkbox from '@self/root/src/uikit/components/Checkbox/__pageObject';
import BusinessGroupsStrategiesSelector
    from '@self/root/src/widgets/content/cart/CartList/components/BusinessGroupsStrategiesSelector/__pageObject';
import * as kettle from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';
import * as express from '@self/root/src/spec/hermione/kadavr-mock/report/express';
import {deliveryDeliveryMock, deliveryPickupMock} from '@self/root/src/spec/hermione/kadavr-mock/checkouter/delivery';
import NoCheckedItemsInfo from '@self/root/src/widgets/content/cart/CartTotalInformation/components/NoCheckedItemsInfo/__pageObject';

import emptyState from './emptyState';
import summaryDataChange from './summaryDataChange';
import removeChecked from './removeChecked';
import noUncheckedActualization from './noUncheckedActualization';
import checkboxStateKeeping from './checkboxStateKeeping';
import itemsToSelectAll from './itemsToSelectAll';
import selectAllToItems from './selectAllToItems';
import removeAll from './removeAll';
import removeSelected from './removeSelected';
import expired from './expired';
import noDelivery from './noDelivery';
import checkout from './checkout';

const singleCart = [
    buildCheckouterBucket({
        items: [{
            skuMock: kettle.skuMock,
            offerMock: kettle.offerMock,
            count: 1,
        }],
    }),
];

const expiredSingleCart = [
    buildCheckouterBucket({
        items: [{
            skuMock: kettle.skuMock,
            offerMock: kettle.offerMock,
            count: 0,
            expired: true,
        }],
        deliveryOptions: [deliveryPickupMock],
    }),
];

const noDeliverySingleCart = [
    buildCheckouterBucket({
        items: [{
            skuMock: kettle.skuMock,
            offerMock: kettle.offerMock,
            count: 0,
        }],
        withoutDelivery: true,
        deliveryOptions: [],
    }),
];

const multiCarts = [
    buildCheckouterBucket({
        cartIndex: 0,
        items: [{
            skuMock: express.skuExpressMock,
            offerMock: express.offerExpressMock,
            count: 1,
        }],
        deliveryOptions: [{
            ...deliveryDeliveryMock,
            isExpress: true,
            deliveryPartnerType: 'YANDEX_MARKET',
        }],
        additionalCartInfo: [{
            weight: Number(express.offerExpressMock.weight) * 1000,
            width: 50,
            height: 50,
            depth: 50,
        }],
    }),
    buildCheckouterBucket({
        cartIndex: 1,
        items: [{
            skuMock: kettle.skuMock,
            offerMock: kettle.offerMock,
            count: 1,
        }],
        additionalCartInfo: [{
            weight: Number(kettle.offerMock.weight) * 1000,
            width: 50,
            height: 50,
            depth: 50,
        }],
    }),
];

export default makeSuite('Частичная покупка.', {
    environment: 'kadavr',
    params: {
        carts: 'Корзины',
    },
    story: mergeSuites(
        {
            async beforeEach() {
                this.setPageObjects({
                    firstCartItem: () => this.createPageObject(
                        CartItem,
                        {
                            root: `${BusinessGroupsStrategiesSelector.bucket(0)} ${CartItem.root}`,
                        }
                    ),
                    firstCheckbox: () => this.createPageObject(Checkbox, {
                        root: `${BusinessGroupsStrategiesSelector.bucket(0)} ${CartOffer.root} ${Checkbox.root}`,
                    }),

                    secondCartItem: () => this.createPageObject(
                        CartItem,
                        {
                            root: `${BusinessGroupsStrategiesSelector.bucket(1)} ${CartItem.root}`,
                        }
                    ),
                    secondCheckbox: () => this.createPageObject(Checkbox, {
                        root: `${BusinessGroupsStrategiesSelector.bucket(1)} ${CartOffer.root} ${Checkbox.root}`,
                    }),
                    secondCartItemRemoveButton: () => this.createPageObject(RemoveCartItemContainer, {parent: this.secondCartItem}),


                    orderInfo: () => this.createPageObject(CartTotalInformation, {parent: this.cartGroup}),
                    orderInfoPreloader: () => this.createPageObject(SummaryPlaceholder, {parent: this.orderInfo}),
                    orderTotal: () => this.createPageObject(OrderTotal),
                    cartCheckoutButton: () => this.createPageObject(CartCheckoutButton),
                    noCheckedItemsInfo: () => this.createPageObject(NoCheckedItemsInfo),
                });

                const testState = await this.browser.yaScenario(
                    this,
                    prepareMultiCartState,
                    this.params.carts
                );

                await this.browser.yaScenario(
                    this,
                    prepareCartPageBySkuId,
                    {
                        items: testState.checkoutItems,
                        reportSkus: testState.reportSkus,
                        region: region['Москва'],
                    }
                );
            },
        },
        prepareSuite(emptyState, {
            meta: {
                id: 'marketfront-5249',
                issue: 'MARKETFRONT-62586',
            },
            params: {
                carts: singleCart,
            },
        }),
        prepareSuite(summaryDataChange, {
            meta: {
                id: 'marketfront-5250',
                issue: 'MARKETFRONT-62586',
            },
            params: {
                carts: multiCarts,
            },
        }),
        prepareSuite(removeChecked, {
            meta: {
                id: 'marketfront-5251',
                issue: 'MARKETFRONT-62586',
            },
            params: {
                carts: multiCarts,
            },
        }),
        prepareSuite(noUncheckedActualization, {
            meta: {
                id: 'marketfront-5252',
                issue: 'MARKETFRONT-62586',
            },
            params: {
                carts: multiCarts,
            },
        }),
        prepareSuite(checkboxStateKeeping, {
            meta: {
                id: 'marketfront-5253',
                issue: 'MARKETFRONT-62586',
            },
            params: {
                carts: multiCarts,
            },
        }),
        prepareSuite(itemsToSelectAll, {
            meta: {
                id: 'marketfront-5254',
                issue: 'MARKETFRONT-62586',
            },
            params: {
                carts: multiCarts,
            },
        }),
        prepareSuite(selectAllToItems, {
            meta: {
                id: 'marketfront-5255',
                issue: 'MARKETFRONT-62586',
            },
            params: {
                carts: multiCarts,
            },
        }),
        prepareSuite(removeAll, {
            meta: {
                id: 'marketfront-5256',
                issue: 'MARKETFRONT-62586',
            },
            params: {
                carts: multiCarts,
            },
        }),
        prepareSuite(removeSelected, {
            meta: {
                id: 'marketfront-5257',
                issue: 'MARKETFRONT-62586',
            },
            params: {
                carts: multiCarts,
            },
        }),
        prepareSuite(expired, {
            meta: {
                id: 'marketfront-5260',
                issue: 'MARKETFRONT-62586',
            },
            params: {
                carts: expiredSingleCart,
            },
        }),
        prepareSuite(noDelivery, {
            meta: {
                id: 'marketfront-5260',
                issue: 'MARKETFRONT-62586',
            },
            params: {
                carts: noDeliverySingleCart,
            },
        }),
        prepareSuite(checkout, {
            meta: {
                id: 'marketfront-5261',
                issue: 'MARKETFRONT-62586',
            },
            params: {
                carts: multiCarts,
                isAuthWithPlugin: true,
            },
        })
    ),
});
