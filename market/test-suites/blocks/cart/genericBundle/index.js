import {makeSuite, prepareSuite, mergeSuites} from 'ginny';
import {createCombineStrategy, mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';

import * as primary from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';
import * as gift from '@self/root/src/spec/hermione/kadavr-mock/report/televizor';

import {buildCheckouterBucketLabel, prepareBasicStrategy} from '@self/root/src/spec/utils/checkouter';
import {generateCheckoutCartItemLabelKadavr} from '@self/root/src/spec/utils/kadavr/checkouter';
import {prepareCartPageBySkuId} from '@self/root/src/spec/hermione/scenarios/cart';
import {prepareKadavrReportState} from '@self/root/src/spec/hermione/kadavr-mock/report/genericBundle';
import {setReportState} from '@self/root/src/spec/hermione/scenarios/kadavr';

import CartGroup from '@self/root/src/widgets/content/cart/CartLayout/components/View/__pageObject';
import CartItem from '@self/root/src/widgets/content/cart/CartList/components/CartItem/__pageObject';
import DiscountPrice
    from '@self/root/src/widgets/content/cart/CartList/components/CartOfferPrice/components/DiscountPrice/__pageObject';
import AmountSelect from '@self/root/src/components/AmountSelect/__pageObject';
import CartOffer from '@self/root/src/widgets/content/cart/CartList/components/CartOffer/__pageObject';
import Text from '@self/root/src/uikit/components/Text/__pageObject';

import OrderTotal from '@self/root/src/components/OrderTotalV2/__pageObject';
import PriceDistributionNotification
    from '@self/root/src/widgets/content/cart/CartNotifications/components/PriceDistributionNotification/__pageObject';
import BundleNewNotification
    from '@self/root/src/widgets/content/cart/CartNotifications/components/BundleNewNotification/__pageObject';

import RemoveCartItemContainer from
    '@self/root/src/widgets/content/cart/CartList/containers/RemoveCartItemContainer/__pageObject';
import {
    RemovedCartItemNotification,
} from '@self/root/src/widgets/content/cart/CartList/components/CartItem/Notification/__pageObject';

import genericBundleDisplay from './genericBundleDisplay';
import changeCount from './changeCount';
import removePrimary from './removePrimary';
import priceDistribution from './priceDistribution';

export default makeSuite('Товар + подарок', {
    environment: 'kadavr',
    issue: 'BLUEMARKET-9121',
    params: {
        bundleCount: 'Исходное количество комплектов "товар+подарок" в корзине',
    },
    defaultParams: {
        bundleCount: 1,
    },
    story: mergeSuites(
        {
            async beforeEach() {
                await this.setPageObjects({
                    cartGroup: () => this.createPageObject(CartGroup),

                    primaryCartItem: () => this.createPageObject(CartItem, {
                        parent: `[data-auto="cart-item"][data-id="${this.yaTestData.bundles.primary.label}"]`,
                    }),
                    primaryCartOffer: () => this.createPageObject(CartOffer, {
                        parent: this.primaryCartItem,
                    }),
                    primaryAmountSelect: () => this.createPageObject(AmountSelect, {
                        parent: this.primaryCartOffer,
                    }),
                    primaryRemovedNotification: () => this.createPageObject(RemovedCartItemNotification, {
                        parent: this.primaryCartItem,
                    }),
                    primaryCartItemRemoveButton: () => this.createPageObject(RemoveCartItemContainer, {
                        parent: this.primaryCartItem,
                    }),
                    primaryDiscountPrice: () => this.createPageObject(DiscountPrice, {
                        parent: this.primaryCartItem,
                    }),

                    giftCartItem: () => this.createPageObject(CartItem, {
                        root: `[data-auto="cart-item"][data-id="${this.yaTestData.bundles.gift.label}"]`,
                    }),
                    giftCartOffer: () => this.createPageObject(CartOffer, {
                        parent: this.giftCartItem,
                    }),
                    giftAmountSelect: () => this.createPageObject(AmountSelect, {
                        parent: this.giftCartOffer,
                    }),
                    giftAmountText: () => this.createPageObject(Text, {
                        parent: this.giftCartOffer,
                        root: '[data-auto="staticCount"]',
                    }),
                    giftRemovedNotification: () => this.createPageObject(RemovedCartItemNotification, {
                        parent: this.giftCartItem,
                    }),
                    giftCartItemRemoveButton: () => this.createPageObject(RemoveCartItemContainer, {
                        parent: this.giftCartItem,
                    }),
                    giftDiscountPrice: () => this.createPageObject(DiscountPrice, {
                        parent: this.giftCartItem,
                    }),

                    orderTotal: () => this.createPageObject(OrderTotal),
                    priceDistributionNotification: () => this.createPageObject(PriceDistributionNotification),
                    bundleNewNotification: () => this.createPageObject(BundleNewNotification),
                });

                const bundleCount = this.params.bundleCount;

                await this.browser.allure.runStep(
                    `Подготавливаем стейт корзины с ${bundleCount} комплект${
                        bundleCount === 1 ? 'ом' : 'ами'
                    } "товар+подарок"`,
                    async () => {
                        const bundlesData = prepareCartKadavrState(bundleCount);

                        await this.browser.yaScenario(this, setReportState, {
                            state: bundlesData.reportState,
                        });

                        await this.browser.setState('Checkouter.collections', bundlesData.checkouterState);

                        await this.browser.yaScenario(this, prepareCartPageBySkuId, {
                            region: this.params.region,
                            items: [
                                bundlesData.primary.cartItem,
                                bundlesData.gift.cartItem,
                            ],
                            reportSkus: [
                                bundlesData.primary.sku,
                                bundlesData.gift.sku,
                            ],
                        });

                        this.yaTestData = this.yaTestData || {};
                        this.yaTestData.bundles = bundlesData;
                    }
                );
            },
        },
        prepareSuite(genericBundleDisplay, {}),
        prepareSuite(priceDistribution, {}),
        prepareSuite(changeCount, {}),
        prepareSuite(removePrimary, {})
    ),
});

export function prepareCartKadavrState(bundleCount) {
    const reportState = prepareKadavrReportState(primary, gift, undefined);

    const bundleId = 'testBundle';
    const marketPromoId = 'DSVfwHKA6A3LWQv9CiPAqw==';

    const primaryPrice = parseFloat(primary.offerMock.prices.value);
    const giftPrice = parseFloat(gift.offerMock.prices.value);

    const newGiftPrice = 1;
    const newPrimaryPrice = primaryPrice - newGiftPrice;

    const buyerTotalBeforeDiscount = (giftPrice + primaryPrice) * bundleCount;
    const buyerTotalAfterDiscount = (newGiftPrice + newPrimaryPrice) * bundleCount;
    const buyerItemsDiscount = buyerTotalBeforeDiscount - buyerTotalAfterDiscount;

    const bundleProps = {
        bundleCount,
        bundleId,
        marketPromoId,
    };

    const cartLabel = buildCheckouterBucketLabel(
        [primary.offerMock.wareId, gift.offerMock.wareId],
        100
    );

    const primaryItem = createItemMocks({
        mock: reportState.primary,
        isPrimaryInBundle: true,
        newPrice: newPrimaryPrice,
        bundleProps,
        cartLabel,
    });
    const giftItem = createItemMocks({
        mock: reportState.gift,
        isPrimaryInBundle: false,
        newPrice: newGiftPrice,
        bundleProps,
        cartLabel,
    });

    const checkouterState = {
        cart: {
            0: {
                shopId: 431782,
                warehouseId: 100,
                label: cartLabel,
                items: [0, 1],
                promos: [
                    {
                        type: 'GENERIC_BUNDLE',
                        bundleId,
                        marketPromoId,
                        buyerItemsDiscount,
                    },
                ],
                buyerItemsTotalBeforeDiscount: buyerTotalBeforeDiscount,
                buyerTotalBeforeDiscount,
                buyerItemsTotalDiscount: buyerItemsDiscount,
                buyerTotalDiscount: buyerItemsDiscount,
            },
        },
        cartItem: {
            0: primaryItem.checkoutCartItem,
            1: giftItem.checkoutCartItem,
        },
    };

    const strategy = prepareBasicStrategy(Object.values(checkouterState.cart), Object.values(checkouterState.cartItem));
    const strategyState = createCombineStrategy(strategy, strategy.name);

    return {
        count: bundleCount,

        primary: primaryItem,
        gift: giftItem,

        buyerTotalBeforeDiscount,
        buyerTotalAfterDiscount,
        buyerItemsDiscount,

        checkouterState,
        reportState: mergeState([reportState.stateWithSkuOffers, strategyState]),
    };
}

function createItemMocks({
    mock,
    isPrimaryInBundle,
    newPrice,
    bundleProps: {
        bundleCount,
        bundleId,
        marketPromoId,
    },
    cartLabel,
}) {
    const {skuMock, offerMock} = mock;
    const {feed} = offerMock;
    const originalPrice = parseFloat(offerMock.prices.value);
    const label = generateCheckoutCartItemLabelKadavr(offerMock);
    const computedLabel = `${cartLabel}-${label}`;

    return {
        originalPrice,
        newPrice,
        label: computedLabel,
        sku: {
            ...skuMock,
            offers: {
                items: [offerMock],
            },
        },
        cartItem: {
            skuId: skuMock.id,
            offerId: offerMock.wareId,
            bundleId,
            isPrimaryInBundle,
            count: bundleCount,
        },
        checkoutCartItem: {
            offerId: offerMock.wareId,
            feedId: feed.id,
            feedOfferId: offerMock.wareId,
            buyerPrice: newPrice,
            buyerPriceNominal: newPrice,
            count: bundleCount,
            label,
            primaryInBundle: isPrimaryInBundle,
            promos: [
                {
                    type: 'GENERIC_BUNDLE',
                    marketPromoId,
                    bundleId,
                    buyerDiscount: originalPrice - newPrice,
                },
            ],
        },
    };
}
