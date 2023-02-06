import {makeCase, makeSuite} from 'ginny';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import {prepareMultiCartState} from '@self/root/src/spec/hermione/scenarios/cartResource';
import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';
import {waitForCartActualization} from '@self/root/src/spec/hermione/scenarios/cart';

import * as kettle from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';
import * as farma from '@self/root/src/spec/hermione/kadavr-mock/report/farma';
import x5outletMock from '@self/root/src/spec/hermione/kadavr-mock/report/outlets/x5outlet';
import * as dsbs from '@self/root/src/spec/hermione/kadavr-mock/report/dsbs';
import * as digital from '@self/root/src/spec/hermione/kadavr-mock/report/digital';
import * as express from '@self/root/src/spec/hermione/kadavr-mock/report/express';

import {
    deliveryDeliveryMock,
    deliveryPickupMock,
    paymentOptions,
} from '@self/root/src/spec/hermione/kadavr-mock/checkouter/delivery';

import ParcelTitle from '@self/root/src/widgets/content/cart/CartList/components/ParcelTitle/__pageObject';
import BusinessGroupsStrategiesSelector
    from '@self/root/src/widgets/content/cart/CartList/components/BusinessGroupsStrategiesSelector/__pageObject';
import AmountSelect from '@self/root/src/components/AmountSelect/__pageObject';

const commonCart = buildCheckouterBucket({
    cartIndex: 0,
    items: [{
        skuMock: kettle.skuMock,
        offerMock: kettle.offerMock,
        count: 1,
    }],
});

const farmaCart = buildCheckouterBucket({
    cartIndex: 1,
    items: [{
        skuMock: farma.skuMock,
        offerMock: farma.offerMock,
        count: 1,
    }],
    deliveryOptions: [{
        ...deliveryPickupMock,
        paymentOptions: [
            paymentOptions.cashOnDelivery,
        ],
        outlets: [
            {id: x5outletMock.id, regionId: 0},
            {id: farma.outletMock.id, regionId: 0},
        ],
    }],
    outlets: [
        x5outletMock,
        farma.outletMock,
    ],
});

const dsbsCart = buildCheckouterBucket({
    cartIndex: 2,
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
    cartIndex: 3,
    items: [{
        skuMock: digital.skuMock,
        offerMock: digital.offerMock,
        count: 1,
    }],
    isDigital: true,
});

const expressCart = buildCheckouterBucket({
    cartIndex: 4,
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

const increasedExpressCart = buildCheckouterBucket({
    cartIndex: 4,
    items: [{
        skuMock: express.skuExpressMock,
        offerMock: express.offerExpressMock,
        count: 2,
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
                firstTitle: () => this.createPageObject(
                    ParcelTitle,
                    {
                        root: `${BusinessGroupsStrategiesSelector.bucket(0)} ${ParcelTitle.root}`,
                    }
                ),
                amountSelect: () => this.createPageObject(
                    AmountSelect,
                    {
                        root: `${BusinessGroupsStrategiesSelector.bucket(0)} ${AmountSelect.root}`,
                    }
                ),
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
                fourthTitle: () => this.createPageObject(
                    ParcelTitle,
                    {
                        root: `${BusinessGroupsStrategiesSelector.bucket(3)} ${ParcelTitle.root}`,
                    }
                ),
            });

            await this.browser.yaScenario(
                this,
                prepareMultiCartState,
                [commonCart, farmaCart, dsbsCart, digitalCart, expressCart]
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

        'После изменения количества': {
            async beforeEach() {
                await this.browser.yaScenario(
                    this,
                    prepareMultiCartState,
                    [commonCart, farmaCart, dsbsCart, digitalCart, increasedExpressCart]
                );
                await this.amountSelect.plusFromButton();
            },
            'Группы отображаются в правильном порядке': makeCase({
                async test() {
                    await this.browser.yaScenario(this, waitForCartActualization);
                    await this.browser.allure.runStep(
                        'Проверяем количество товара',
                        () => this.amountSelect.getCurrentCountText()
                            .should.eventually.be.equal('2', 'Количество экспресс товаров должно быть 2')
                    );
                    return checkOrder.call(this);
                },
            }),
        },
    },
});

async function checkOrder() {
    await this.firstTitle.getTitleText().should.be.eventually.equal('Экспресс-доставка Яндекса');
    await this.secondTitle.getTitleText().should.be.eventually.equal('Доставка Яндекса и продавцов');
    await this.thirdTitle.getTitleText().should.be.eventually.equal('Лекарства и товары для здоровья');
    await this.fourthTitle.getTitleText().should.be.eventually.equal('Получение по электронной почте');
}
