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
import CartParcel from '@self/root/src/widgets/content/cart/CartList/components/CartParcel/__pageObject';
import CartItem from '@self/root/src/widgets/content/cart/CartList/components/CartItem/__pageObject';
import BusinessGroupsStrategiesSelector
    from '@self/root/src/widgets/content/cart/CartList/components/BusinessGroupsStrategiesSelector/__pageObject';

// mocks
import * as dsbs from '@self/root/src/spec/hermione/kadavr-mock/report/dsbs';
import {deliveryDeliveryMock} from '@self/root/src/spec/hermione/kadavr-mock/checkouter/delivery';

// suites
import cartItemInfoSuite from '@self/root/src/spec/hermione/test-suites/blocks/cart/page/cartItemInfo';


const dsbsCarts = [
    buildCheckouterBucket({
        items: [{
            skuMock: dsbs.skuPhoneMock,
            offerMock: {
                ...dsbs.offerPhoneMock,
                delivery: {
                    ...dsbs.offerPhoneMock.delivery,
                    options: [{
                        ...dsbs.offerPhoneMock.delivery.options[0],
                        isEstimated: true,
                        dayFrom: 30,
                    }],
                },
                isUniqueOffer: true,
            },
            count: 1,
        }],
        deliveryOptions: [{
            ...deliveryDeliveryMock,
            deliveryPartnerType: 'SHOP',
        }],
        shopId: dsbs.offerPhoneMock?.shop?.id,
    }),
];

module.exports = makeSuite('Офер по предзаказу', {
    environment: 'kadavr',

    story: mergeSuites(
        {
            async beforeEach() {
                this.setPageObjects({
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

        prepareSuite(cartItemInfoSuite, {
            meta: {
                issue: 'MARKETFRONT-81055',
                id: 'marketfront-5820',
            },
            params: {
                hasRemoveBtn: true,
                hasWishlistBtn: true,
                cantAddAmount: true,
                estimatedText: 'Увеличенные сроки доставки',
            },
        })
    ),
});
