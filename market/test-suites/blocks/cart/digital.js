import {makeSuite, makeCase} from 'ginny';

import * as digital from '@self/root/src/spec/hermione/kadavr-mock/report/digital';

import CartHeader from '@self/root/src/widgets/content/cart/CartHeader/components/View/__pageObject';
import CartParcel from '@self/root/src/widgets/content/cart/CartList/components/CartParcel/__pageObject';
import CartGroupHeader from '@self/root/src/widgets/content/cart/CartList/components/ParcelTitle/__pageObject';
import CartTotalInformation
    from '@self/root/src/widgets/content/cart/CartTotalInformation/components/View/__pageObject';
import Promocode from '@self/root/src/components/Promocode/__pageObject';

import {deliveryPickupMock} from '@self/root/src/spec/hermione/kadavr-mock/checkouter/delivery';
import {prepareCartPageBySkuId} from '@self/platform/spec/hermione/scenarios/cart';
import {prepareMultiCartState} from '@self/root/src/spec/hermione/scenarios/cartResource';
import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';
import {CART_TITLE} from '@self/root/src/entities/checkout/cart/constants';

/**
 * Тесты на цифру в корзине.
 * @param {PageObject.CartOrderInfo} orderInfo
 * @param {PageObject.CartGroup} cartGroup
 */
export default makeSuite('Цифровой товар.', {
    feature: 'Цифровой товар',
    environment: 'kadavr',
    defaultParams: {
        items: [{
            skuMock: digital.skuMock,
            offerMock: digital.offerMock,
        }],
    },
    story: {
        async beforeEach() {
            this.setPageObjects({
                cartHeader: () => this.createPageObject(CartHeader, {parent: this.cartGroup}),
                bucket: () => this.createPageObject(CartParcel, {parent: this.strategiesSelector}),
                cartGroupHeader: () => this.createPageObject(CartGroupHeader, {parent: this.bucket}),
                summary: () => this.createPageObject(CartTotalInformation, {parent: this.cartGroup}),
                promocode: () => this.createPageObject(Promocode, {parent: this.summary}),
            });

            const testState = await this.browser.yaScenario(
                this,
                prepareMultiCartState,
                [buildCheckouterBucket({
                    items: this.params.items,
                    deliveryOptions: [deliveryPickupMock],
                    isDigital: true,
                })]
            );

            return this.browser.yaScenario(
                this,
                prepareCartPageBySkuId,
                {
                    items: testState.checkoutItems,
                    reportSkus: testState.reportSkus,
                    region: this.params.region,
                }
            );
        },

        'Товар отображается нужным образом': makeCase({
            id: 'bluemarket-3737',
            issue: 'MARKETFRONT-24016',
            async test() {
                const header = this.cartHeader.getTitleText();
                await header.should.eventually.be.include(
                    CART_TITLE,
                    `Шапка страницы должна содержать текст «${CART_TITLE}»`
                );

                const expectedTitle = 'Получение по электронной почте';
                const title = this.cartGroupHeader.getTitleText();
                await title.should.eventually.be.equal(
                    expectedTitle,
                    `Заголовок посылки должен быть «${expectedTitle}»`
                );

                const expectedCount = 1;
                const count = this.bucket.getItemsCount();
                await count.should.eventually.be.equal(
                    expectedCount,
                    `Должно быть количество товаров в посылке равным ${expectedCount}`
                );

                await this.summary.isVisible().should.eventually.be.equal(
                    true,
                    'Саммари должно быть видно'
                );
                await this.promocode.isVisible().should.eventually.be.equal(
                    true,
                    'Поле ввода промокода должно быть видно'
                );
            },
        }),
    },
});
