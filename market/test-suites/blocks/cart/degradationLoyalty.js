import {
    makeSuite,
    makeCase,
    mergeSuites,
} from 'ginny';
import {createOffer, mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';

import {region} from '@self/root/src/spec/hermione/configs/geo';

import CartItemGroup from '@self/root/src/widgets/content/cart/CartList/components/CartItemGroup/__pageObject';
import CartBonuses from '@self/root/src/widgets/content/cart/CartAvailableBonuses/components/CartCoins/__pageObject';
import BonusWithTearOff from '@self/root/src/components/BonusWithTearOffControl/__pageObject';
import CartGroup from '@self/root/src/widgets/content/cart/CartLayout/components/View/__pageObject';
import OrderTotal from '@self/root/src/components/OrderTotalV2/__pageObject';
import RemoveCartItemContainer
    from '@self/root/src/widgets/content/cart/CartList/containers/RemoveCartItemContainer/__pageObject';
import CartAvailableBonusesView from
    '@self/root/src/widgets/content/cart/CartAvailableBonuses/components/View/__pageObject';

import {offerMock, skuMock} from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';
import Footer from '@self/platform/spec/page-objects/footer-market';
import LoyaltyError from '@self/root/src/components/LoyaltyError/__pageObject';
import Header from '@self/platform/spec/page-objects/header2';

import CartOffer
    from '@self/root/src/widgets/content/cart/CartList/components/CartOffer/__pageObject';
import CartTotalInformation
    from '@self/root/src/widgets/content/cart/CartTotalInformation/components/View/__pageObject';
import CartCheckoutButton
    from '@self/root/src/widgets/content/cart/CartCheckoutControl/components/CartCheckoutButton/__pageObject';
import CartEmptyView from '@self/root/src/widgets/content/cart/CartEmpty/components/View/__pageObject';
import COOKIE_NAME from '@self/root/src/constants/cookie';
import {BACKENDS_NAME} from '@self/root/src/constants/backendsIdentifier';
import bonuses from '@self/root/src/spec/hermione/kadavr-mock/loyalty/bonuses';
import {waitForCartActualization} from '@self/root/src/spec/hermione/scenarios/cart';
import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';
import {prepareMultiCartState} from '@self/root/src/spec/hermione/scenarios/cartResource';

const offer = createOffer(offerMock, offerMock.wareId);
const reportState = mergeState([offer]);
const sku = {
    ...skuMock,
    offers: {
        items: [offerMock],
    },
};

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Деградация Loyalty.', {
    feature: 'Деградация Loyalty.',
    issue: 'BLUEMARKET-10366',
    environment: 'kadavr',
    params: {
        region: 'Регион',
        isAuthWithPlugin: 'Авторизован ли пользователь',
    },
    defaultParams: {
        region: region['Москва'],
        percentBonus: bonuses.PERCENT,
        isAuthWithPlugin: true,
    },
    story: mergeSuites(
        {
            async beforeEach() {
                this.setPageObjects({
                    header: () => this.createPageObject(Header),
                    footer: () => this.createPageObject(Footer),
                    cartOffer: () => this.createPageObject(CartOffer, {parent: this.cartItem}),
                    orderInfo: () => this.createPageObject(CartTotalInformation, {parent: this.cartGroup}),
                    cartOrderCheckout: () => this.createPageObject(CartCheckoutButton, {
                        parent: this.orderInfo,
                    }),
                    cartEmpty: () => this.createPageObject(CartEmptyView),
                    cartBonuses: () => this.createPageObject(CartBonuses),
                    bonus: () => this.createPageObject(
                        BonusWithTearOff,
                        {
                            parent: this.cartBonuses,
                            root: `${BonusWithTearOff.root}:nth-child(1)`,
                        }
                    ),
                    cartGroup: () => this.createPageObject(CartGroup),
                    cartItems: () => this.createPageObject(CartItemGroup, {parent: this.cartGroup}),
                    cartItemRemoveButton: () => this.createPageObject(RemoveCartItemContainer, {parent: this.cartItem}),
                    orderTotal: () => this.createPageObject(OrderTotal),
                    bonusParent: () => this.createPageObject(CartAvailableBonusesView),
                    loyaltyError: () => this.createPageObject(
                        LoyaltyError
                    ),
                });

                const carts = [
                    buildCheckouterBucket({
                        items: [{
                            skuMock,
                            offerMock,
                            count: 1,
                        }],
                    }),
                ];

                await this.browser.yaScenario(
                    this,
                    prepareMultiCartState,
                    carts,
                    {existingReportState: reportState}
                );

                await this.browser.yaScenario(
                    this,
                    'cart.prepareCartPageWithBonus',
                    {
                        items: [{
                            skuId: skuMock.id,
                            offerId: offerMock.wareId,
                            count: 1,
                        }],
                        region: this.params.region,
                        bonuses: {
                            applicableCoins: [this.params.percentBonus],
                        },
                        reportSkus: [sku],
                    }
                );
            },
        },
        makeSuite('Отказ бэкенда loyalty', {
            defaultParams: {
                cookie: {
                    [COOKIE_NAME.AT_ENDPOINTS_SETTINGS]: {
                        name: COOKIE_NAME.AT_ENDPOINTS_SETTINGS,
                        value: JSON.stringify({
                            [BACKENDS_NAME.LOYALTY]: {
                                isCrashed: true,
                            },
                        }),
                    },
                },
            },
            story: getTests(),
        }),
        makeSuite('Отказ бэкенда loyalty внутри checkouter', {
            defaultParams: {
                cookie: {
                    [COOKIE_NAME.AT_ENDPOINTS_SETTINGS]: {
                        name: COOKIE_NAME.AT_ENDPOINTS_SETTINGS,
                        value: JSON.stringify({
                            [BACKENDS_NAME.CHECKOUTER]: {
                                loyaltyDegradation: true,
                            },
                        }),
                    },
                },
            },
            story: getTests(),
        })
    ),
});


function getTests() {
    return {
        async beforeEach() {
            await this.browser.yaScenario(this, waitForCartActualization);
            await this.loyaltyError.waitForComponentIsVisible().should.eventually.to.be.equal(true);
            await this.loyaltyError.confirmLoyaltyDegradation();
            return this.loyaltyError.waitForComponentIsHidden().should.eventually.to.be.equal(true);
        },
        'Страница должна содержать шапку с лого и футер': makeCase({
            async test() {
                await this.header.isVisible()
                    .should.eventually.to.be.equal(true, 'Шапка должна быть видна');

                await this.browser.yaSlowlyScroll(Footer.root);

                await this.footer.isVisible()
                    .should.eventually.to.be.equal(true, 'Футер должен быть виден');
            },
        }),

        'Страница должна содержать оффер': makeCase({
            async test() {
                return this.cartOffer.isVisible()
                    .should.eventually.be.equal(true, 'Корзинный оффер должен быть виден');
            },
        }),

        'Переход в чекаут c параметром skipDiscountCalculation': makeCase({
            async test() {
                await this.cartOrderCheckout.isVisible()
                    .should.eventually.to.be.equal(true, 'Кнопка перехода в чекаут должна быть видна');

                await this.browser.yaWaitForChangeUrl(
                    () => this.cartOrderCheckout.click(),
                    10000
                )
                    .should.eventually.to.be.link({
                        pathname: '/checkout',
                        query: {
                            skipDiscountCalculation: 'true',
                        },
                    }, {
                        mode: 'match',
                        skipProtocol: true,
                        skipHostname: true,
                    });
            },
        }),
    };
}
