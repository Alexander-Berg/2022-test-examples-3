import {makeCase, makeSuite} from 'ginny';

import {createOfferForProduct, createProduct, mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';
import mergeReportState from '@yandex-market/kadavr/mocks/Report/helpers/mergeState';

import COOKIE_NAME from '@self/root/src/constants/cookie';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import {getRandomInt} from '@self/root/src/utils/random';
import BypassToCheckoutButton from '@self/root/src/components/BypassToCheckoutButton/__pageObject';
import Snippet from '@self/root/src/components/Snippet/__pageObject';
import priceDropFixture from '@self/root/src/spec/hermione/kadavr-mock/report/offer/pillow_pricedrop';
import dataFixture from '@self/platform/spec/hermione/test-suites/tops/pages/n-page-product-offers/fixtures/data';
import oneOfferCpa from
    '@self/platform/spec/hermione/test-suites/tops/pages/n-page-product-offers/fixtures/oneOfferCPA';
import productOptionsFixture from
    '@self/platform/spec/hermione/test-suites/tops/pages/n-page-product-offers/fixtures/productOptions';
import {spreadDiscountCountPromo} from '@self/platform/spec/hermione/fixtures/promo/promo.mock';

const CART_ITEM_ID = 12345456;
const PRICE_DROP_OFFERS_COUNT = 4;

const createOffersAndProductForPriceDrop = () => {
    const offers = [];
    const products = [];
    let i = 0;

    while (i <= PRICE_DROP_OFFERS_COUNT) {
        const productId = getRandomInt();
        const offerId = getRandomInt();
        products.push(createProduct(productOptionsFixture, productId));
        offers.push(createOfferForProduct(priceDropFixture, productId, offerId));

        i++;
    }

    return mergeReportState([
        ...offers,
        ...products,
        dataFixture,
    ]);
};

export default makeSuite('Покупка в один клик.', {
    environment: 'kadavr',
    issue: 'MARKETFRONT-83872',
    story: {
        async beforeEach() {
            this.setPageObjects({
                bypassToCheckoutButton: () => this.createPageObject(BypassToCheckoutButton),
                snippet: () => this.createPageObject(Snippet),
            });

            const cpaOffer = createOfferForProduct(
                {
                    ...oneOfferCpa.offerMock,
                    promos: [spreadDiscountCountPromo],
                    cpa: 'real',
                    benefit: {
                        type: 'recommended',
                        description: 'Хорошая цена от надёжного магазина',
                        isPrimary: true,
                    },
                },
                oneOfferCpa.params.productId,
                oneOfferCpa.offerId
            );
            const priceDropReportState = createOffersAndProductForPriceDrop();

            const state = mergeState([
                oneOfferCpa.reportState,
                cpaOffer,
                priceDropReportState,
            ]);

            await this.browser.setState('report', state);
        },
        'Пустая корзина.': {
            async beforeEach() {
                await this.browser.yaOpenPage('market:product', oneOfferCpa.params);

                await this.cartButton.click();

                return this.cartPopup.waitForAppearance();
            },
            'При нажатии на кнопку происходит переход в чекаут.': makeCase({
                id: 'marketfront-5941',
                async test() {
                    await this.browser.yaWaitForChangeUrl(
                        () => this.bypassToCheckoutButton.click()
                    );

                    await this.allure.runStep(
                        'Берем значение куки USER_UNCHECKED_CART_ITEM_IDS',
                        () => this.browser.getCookie(COOKIE_NAME.USER_UNCHECKED_CART_ITEM_IDS)
                            .then(cookie => {
                                const value = Boolean(cookie?.value);

                                return this.expect(value).to.be.equal(
                                    false,
                                    'Кука USER_UNCHECKED_CART_ITEM_IDS должна быть пустой'
                                );
                            })
                    );

                    await this.allure.runStep('Произошел переход в чекаут', async () => {
                        const currentUrl = await this.browser.getUrl();
                        const expectedUrl = await this.browser.yaBuildURL(PAGE_IDS_COMMON.CHECKOUT);

                        return this.expect(currentUrl).to.be.link(expectedUrl, {
                            skipProtocol: true,
                            skipHostname: true,
                        });
                    });
                },
            }),
            'При добавлении товара из рекомендательного блока.': {
                async beforeEach() {
                    await this.allure.runStep(
                        'Добавляем товар из прайсдроп блока',
                        () => this.snippet.clickOnCartButton()
                    );
                },
                'При нажатии на кнопку происходит переход в чекаут с товаром из блока.': makeCase({
                    id: 'marketfront-5943',
                    async test() {
                        await this.browser.yaWaitForChangeUrl(
                            () => this.bypassToCheckoutButton.click()
                        );

                        await this.allure.runStep(
                            'Берем значение куки USER_UNCHECKED_CART_ITEM_IDS',
                            () => this.browser.getCookie(COOKIE_NAME.USER_UNCHECKED_CART_ITEM_IDS)
                                .then(cookie => {
                                    const value = Boolean(cookie?.value);

                                    return this.expect(value).to.be.equal(
                                        false,
                                        'Кука USER_UNCHECKED_CART_ITEM_IDS должна быть пустой'
                                    );
                                })
                        );

                        await this.allure.runStep('Произошел переход в чекаут', async () => {
                            const currentUrl = await this.browser.getUrl();
                            const expectedUrl = await this.browser.yaBuildURL(PAGE_IDS_COMMON.CHECKOUT);

                            return this.expect(currentUrl).to.be.link(expectedUrl, {
                                skipProtocol: true,
                                skipHostname: true,
                            });
                        });
                    },
                }),
            },
        },
        'В корзине есть товары.': {
            async beforeEach() {
                await this.browser.setState('Carter.items', [{
                    offerId: '123412',
                    id: CART_ITEM_ID,
                    creationTime: Date.now(),
                    label: 'g06judyquh9',
                    objType: 'OFFER',
                    objId: '433432',
                    count: 1,
                }]);

                await this.browser.yaOpenPage('market:product', oneOfferCpa.params);

                await this.cartButton.click();

                return this.cartPopup.waitForAppearance();
            },
            'При нажатии на кнопку происходит переход в чекаут.': makeCase({
                id: 'marketfront-5942',
                async test() {
                    await this.browser.yaWaitForChangeUrl(
                        () => this.bypassToCheckoutButton.click()
                    );

                    await this.allure.runStep(
                        'Берем значение куки USER_UNCHECKED_CART_ITEM_IDS',
                        () => this.browser.getCookie(COOKIE_NAME.USER_UNCHECKED_CART_ITEM_IDS)
                            .then(cookie => {
                                const uncheckedCartItemIds = cookie ? JSON.parse(cookie.value) : [];

                                return this.expect(uncheckedCartItemIds.includes(CART_ITEM_ID)).to.be.equal(
                                    true,
                                    'Кука USER_UNCHECKED_CART_ITEM_IDS должна содержать не выбранный id'
                                );
                            })
                    );

                    await this.allure.runStep('Произошел переход в чекаут', async () => {
                        const currentUrl = await this.browser.getUrl();
                        const expectedUrl = await this.browser.yaBuildURL(PAGE_IDS_COMMON.CHECKOUT);

                        return this.expect(currentUrl).to.be.link(expectedUrl, {
                            skipProtocol: true,
                            skipHostname: true,
                        });
                    });
                },
            }),
            'При добавлении товара из рекомендательного блока.': {
                async beforeEach() {
                    await this.allure.runStep(
                        'Добавляем товар из прайсдроп блока',
                        () => this.snippet.clickOnCartButton()
                    );
                },
                'При нажатии на кнопку происходит переход в чекаут с товаром из блока.': makeCase({
                    id: 'marketfront-5947',
                    async test() {
                        await this.browser.yaWaitForChangeUrl(
                            () => this.bypassToCheckoutButton.click()
                        );

                        await this.allure.runStep(
                            'Берем значение куки USER_UNCHECKED_CART_ITEM_IDS',
                            () => this.browser.getCookie(COOKIE_NAME.USER_UNCHECKED_CART_ITEM_IDS)
                                .then(async cookie => {
                                    const uncheckedCartItemIds = cookie ? JSON.parse(cookie.value) : [];

                                    await this.expect(uncheckedCartItemIds.length).to.be.equal(
                                        1,
                                        'Кука USER_UNCHECKED_CART_ITEM_IDS должна содержать только один элемент'
                                    );

                                    await this.expect(uncheckedCartItemIds.includes(CART_ITEM_ID)).to.be.equal(
                                        true,
                                        'Кука USER_UNCHECKED_CART_ITEM_IDS должна содержать не выбранный id'
                                    );
                                })
                        );

                        await this.allure.runStep('Произошел переход в чекаут', async () => {
                            const currentUrl = await this.browser.getUrl();
                            const expectedUrl = await this.browser.yaBuildURL(PAGE_IDS_COMMON.CHECKOUT);

                            return this.expect(currentUrl).to.be.link(expectedUrl, {
                                skipProtocol: true,
                                skipHostname: true,
                            });
                        });
                    },
                }),
            },
        },
    },
});
