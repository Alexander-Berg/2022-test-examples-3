import {makeCase, makeSuite, mergeSuites} from 'ginny';

import * as kettle from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';
import * as dsbs from '@self/root/src/spec/hermione/kadavr-mock/report/dsbs';
import CartLayout from '@self/root/src/widgets/content/cart/CartLayout/components/View/__pageObject';
import CartParcel from '@self/root/src/widgets/content/cart/CartList/components/CartParcel/__pageObject';
import CartItem from '@self/root/src/widgets/content/cart/CartList/components/CartItem/__pageObject';
import DiscountPrice
    from '@self/root/src/widgets/content/cart/CartList/components/CartOfferPrice/components/DiscountPrice/__pageObject';
import AmountSelect from '@self/root/src/components/AmountSelect/__pageObject';
import OrderTotal from '@self/root/src/components/OrderTotalV2/__pageObject';

import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';
import {prepareMultiCartState} from '@self/root/src/spec/hermione/scenarios/cartResource';

import pharmaOfferMock from '@self/root/src/spec/hermione/kadavr-mock/report/offer/farma';
import pharmaSkuMock from '@self/root/src/spec/hermione/kadavr-mock/report/sku/farma';
import {
    prepareCartPageBySkuId,
    waitForCartActualization,
} from '@self/root/src/spec/hermione/scenarios/cart';

// TODO написать моки для фармы

const kettleOfferMock = {
    ...kettle.offerMock,
    specs: {
        internal: [
            {
                type: 'spec',
                value: 'medicine',
                usedParams: [],
            },
        ],
    },
};

const dsbsOfferMock = {
    ...dsbs.offerPhoneMock,
    specs: {
        internal: [
            {
                type: 'spec',
                value: 'medicine',
                usedParams: [],
            },
        ],
    },
};

export default makeSuite('Покупка списком. Отображение в корзине Фармы.', {
    environment: 'kadavr',
    id: 'marketfront-5208',
    issue: 'MARKETFRONT-81668',
    story: mergeSuites({
        async beforeEach() {
            this.setPageObjects({
                cartLayout: () => this.createPageObject(CartLayout),
                cart: () => this.createPageObject(CartParcel),
                cartItem: () => this.createPageObject(CartItem, {
                    parent: this.cart,
                }),
                discountPrice: () => this.createPageObject(DiscountPrice, {
                    parent: this.cartItem,
                }),
                amountSelect: () => this.createPageObject(AmountSelect, {
                    parent: this.cartItem,
                }),
                orderTotal: () => this.createPageObject(OrderTotal),
            });

            this.params = {
                plusClickCount: 5,
            };

            const carts = [
                buildCheckouterBucket({
                    items: [{
                        skuMock: kettle.skuMock,
                        offerMock: kettleOfferMock,
                        count: 1,
                    }, {
                        skuMock: dsbs.skuPhoneMock,
                        offerMock: dsbsOfferMock,
                        count: 1,
                    }],
                    isMedicalParcel: true,
                }),
            ];

            const {
                reportSkus,
                checkoutItems,
            } = await this.browser.yaScenario(
                this,
                prepareMultiCartState,
                carts
            );

            this.queryParams = {
                purchaseList: 1,
            };

            return this.browser.yaScenario(this, prepareCartPageBySkuId, {
                region: this.params.region,
                items: checkoutItems,
                reportSkus,
                queryParams: this.queryParams,
            });
        },
        'При переходе в корзину': {
            'товары объединены по посылкам': makeCase({
                async test() {
                    await this.cartLayout.getParcelsCount()
                        .should.eventually.be.equal(1, 'Количество посылок должно быть 1');
                },
            }),
            'у товаров проставлена цена в формате "от"': makeCase({
                async test() {
                    await this.discountPrice.getCurrentPrice()
                        .should.eventually.to.match(
                            /^от/,
                            'цена в формате "от"'
                        );
                },
            }),
            'два товара': makeCase({
                async test() {
                    await this.cart.getItemsCount()
                        .should.eventually.be.equal(2, 'Количество офферов должно быть 2');
                },
            }),
            'общая сумма корзины проставлена в формате "от"': makeCase({
                async test() {
                    await this.orderTotal.getPrice()
                        .should.eventually.to.match(
                            /^от/,
                            'Итого в формате "от"'
                        );
                },
            }),
        },
        'При многократном клике по кнопке плюс': {
            async beforeEach() {
                for (let i = 0; i < this.params.plusClickCount; i++) {
                    // eslint-disable-next-line no-await-in-loop
                    await this.amountSelect.plusFromButton();
                }
                await this.browser.yaScenario(this, waitForCartActualization);
            },
            'количество увеличивается': makeCase({
                async test() {
                    await this.browser.allure.runStep(
                        'Проверяем количество товара',
                        () => this.amountSelect.getCurrentCountText()
                            .should.eventually.be.equal('1', 'Количество товаров должно быть 1')
                    );
                },
            }),
        },
        'Добавить в корзину рецептурный товар': {
            async beforeEach() {
                const carts = [
                    buildCheckouterBucket({
                        items: [{
                            skuMock: pharmaSkuMock,
                            offerMock: pharmaOfferMock,
                            count: 1,
                        }, {
                            skuMock: kettle.skuMock,
                            offerMock: kettleOfferMock,
                            count: 1,
                        }, {
                            skuMock: dsbs.skuPhoneMock,
                            offerMock: dsbsOfferMock,
                            count: 1,
                        }],
                        isMedicalParcel: true,
                    }),
                ];

                const {
                    reportSkus,
                    checkoutItems,
                } = await this.browser.yaScenario(
                    this,
                    prepareMultiCartState,
                    carts
                );

                await this.browser.yaScenario(this, prepareCartPageBySkuId, {
                    region: this.params.region,
                    items: checkoutItems,
                    reportSkus,
                    queryParams: this.queryParams,
                });
            },
            'товар добавлен в посылку': makeCase({
                async test() {
                    await this.cartLayout.getParcelsCount()
                        .should.eventually.be.equal(1, 'Количество посылок должно быть 1');
                },
            }),
        },
    }),
});
