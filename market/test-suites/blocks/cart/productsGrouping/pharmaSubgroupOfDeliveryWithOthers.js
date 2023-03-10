import {makeCase, makeSuite} from 'ginny';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import {prepareMultiCartState} from '@self/root/src/spec/hermione/scenarios/cartResource';
import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';
import {waitForCartActualization} from '@self/root/src/spec/hermione/scenarios/cart';

import * as kettle from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';
import * as farma from '@self/root/src/spec/hermione/kadavr-mock/report/farma';
import x5outletMock from '@self/root/src/spec/hermione/kadavr-mock/report/outlets/x5outlet';
import * as dsbs from '@self/root/src/spec/hermione/kadavr-mock/report/dsbs';

import {
    deliveryDeliveryMock,
    deliveryPickupMock,
    paymentOptions,
} from '@self/root/src/spec/hermione/kadavr-mock/checkouter/delivery';

import ParcelTitle from '@self/root/src/widgets/content/cart/CartList/components/ParcelTitle/__pageObject';
import BusinessGroupsStrategiesSelector
    from '@self/root/src/widgets/content/cart/CartList/components/BusinessGroupsStrategiesSelector/__pageObject';

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

module.exports = makeSuite('?????????????????? ???????????? ?????????? 2.', {
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
                secondTitle: () => this.createPageObject(
                    ParcelTitle,
                    {
                        root: `${BusinessGroupsStrategiesSelector.bucket(1)} ${ParcelTitle.root}`,
                    }
                ),
            });

            await this.browser.yaScenario(
                this,
                prepareMultiCartState,
                [commonCart, farmaCart, dsbsCart]
            );

            await this.browser.yaOpenPage(PAGE_IDS_COMMON.CART, {lr: 213});
            return this.browser.yaScenario(this, waitForCartActualization);
        },

        '???????????? ???????????????????????? ?? ???????????????????? ??????????????': makeCase({
            async test() {
                return checkOrder.call(this);
            },
        }),
    },
});

async function checkOrder() {
    await this.firstTitle.getTitleText().should.be.eventually.equal('???????????????? ?????????????? ?? ??????????????????');
    await this.secondTitle.getTitleText().should.be.eventually.equal('?????????????????? ?? ???????????? ?????? ????????????????');
}
