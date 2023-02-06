import {makeCase, makeSuite} from 'ginny';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import {prepareMultiCartState} from '@self/root/src/spec/hermione/scenarios/cartResource';
import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';
import {waitForCartActualization} from '@self/root/src/spec/hermione/scenarios/cart';

import * as kettle from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';
import * as dsbs from '@self/root/src/spec/hermione/kadavr-mock/report/dsbs';
import * as digital from '@self/root/src/spec/hermione/kadavr-mock/report/digital';
import * as express from '@self/root/src/spec/hermione/kadavr-mock/report/express';

import {deliveryDeliveryMock} from '@self/root/src/spec/hermione/kadavr-mock/checkouter/delivery';

import ParcelTitle from '@self/root/src/widgets/content/cart/CartList/components/ParcelTitle/__pageObject';
import CartParcel from '@self/root/src/widgets/content/cart/CartList/components/CartParcel/__pageObject';
import CartItemGroup from '@self/root/src/widgets/content/cart/CartList/components/CartItemGroup/__pageObject';
import BusinessGroupsStrategiesSelector
    from '@self/root/src/widgets/content/cart/CartList/components/BusinessGroupsStrategiesSelector/__pageObject';
import CartItem from '@self/root/src/widgets/content/cart/CartList/components/CartItem/__pageObject';
import RemoveButton from '@self/root/src/widgets/content/cart/CartList/containers/RemoveCartItemContainer/__pageObject';

const commonCart = buildCheckouterBucket({
    cartIndex: 0,
    items: [{
        skuMock: kettle.skuMock,
        offerMock: kettle.offerMock,
        count: 1,
    }],
});

const dsbsCart = buildCheckouterBucket({
    cartIndex: 1,
    items: [{
        skuMock: dsbs.skuPhoneMock,
        offerMock: dsbs.offerPhoneMock,
        count: 1,
    }],
    deliveryOptions: [{
        ...deliveryDeliveryMock,
        deliveryPartnerType: 'SHOP',
    }],
});

const digitalCart = buildCheckouterBucket({
    cartIndex: 2,
    items: [{
        skuMock: digital.skuMock,
        offerMock: digital.offerMock,
        count: 1,
    }],
    isDigital: true,
});

const expressCart = buildCheckouterBucket({
    cartIndex: 3,
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
});

module.exports = makeSuite('Группировка товаров на странице.', {
    environment: 'kadavr',
    id: 'm-touch-3612',
    issue: 'MARKETFRONT-68755',
    story: {
        async beforeEach() {
            this.setPageObjects({
                bucket: () => this.createPageObject(CartParcel,
                    {
                        root: `${BusinessGroupsStrategiesSelector.bucket(1)} ${CartParcel.root}`,
                    }
                ),
                firstTitle: () => this.createPageObject(
                    ParcelTitle,
                    {
                        root: `${BusinessGroupsStrategiesSelector.bucket(0)} ${ParcelTitle.root}`,
                    }
                ),
                offer1: () => this.createPageObject(CartItem, {
                    parent: this.bucket,
                    root: `${CartItemGroup.group(0)} ${CartItem.root}`,
                }),
                offer2: () => this.createPageObject(CartItem, {
                    parent: this.bucket,
                    root: `${CartItemGroup.group(1)} ${CartItem.root}`,
                }),
                removeButton1: () => this.createPageObject(RemoveButton, {
                    parent: this.offer1,
                }),
                removeButton2: () => this.createPageObject(RemoveButton, {
                    parent: this.offer2,
                }),
                secondTitle: () => this.createPageObject(
                    ParcelTitle,
                    {
                        root: `${BusinessGroupsStrategiesSelector.bucket(1)} ${ParcelTitle.root}`,
                    }
                ),
                thirdTitle: () => this.createPageObject(
                    ParcelTitle,
                    {
                        root: `${BusinessGroupsStrategiesSelector.bucket(2)} ${ParcelTitle.root}`,
                    }
                ),
            });

            await this.browser.yaScenario(
                this,
                prepareMultiCartState,
                [commonCart, dsbsCart, digitalCart, expressCart]
            );

            await this.browser.yaOpenPage(PAGE_IDS_COMMON.CART, {lr: 213});
            return this.browser.yaScenario(this, waitForCartActualization);
        },

        'По дефолту': {
            'Группы отображаются в правильном порядке': makeCase({
                async test() {
                    return checkOrder.call(this);
                },
            }),
        },

        'После удаления товаров': {
            async beforeEach() {
                await this.removeButton2.click();
                await this.removeButton1.click();
                await this.browser.yaScenario(this, waitForCartActualization);
            },
            'Группы отображаются в правильном порядке': makeCase({
                async test() {
                    return checkOrderAfter.call(this);
                },
            }),
        },
    },
});

async function checkOrder() {
    await this.firstTitle.getTitleText().should.be.eventually.equal('Экспресс-доставка Яндекса');
    await this.secondTitle.getTitleText().should.be.eventually.equal('Доставка Яндекса и продавцов');
    await this.thirdTitle.getTitleText().should.be.eventually.equal('Получение по электронной почте');
}

async function checkOrderAfter() {
    await this.firstTitle.getTitleText().should.be.eventually.equal('Экспресс-доставка Яндекса');
    await this.secondTitle.getTitleText().should.be.eventually.equal('Получение по электронной почте');
}
