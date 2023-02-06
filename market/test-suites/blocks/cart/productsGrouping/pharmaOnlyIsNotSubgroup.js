import {makeCase, makeSuite} from 'ginny';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import {prepareMultiCartState} from '@self/root/src/spec/hermione/scenarios/cartResource';
import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';
import {waitForCartActualization} from '@self/root/src/spec/hermione/scenarios/cart';

import * as farma from '@self/root/src/spec/hermione/kadavr-mock/report/farma';
import x5outletMock from '@self/root/src/spec/hermione/kadavr-mock/report/outlets/x5outlet';

import {
    deliveryPickupMock,
    paymentOptions,
} from '@self/root/src/spec/hermione/kadavr-mock/checkouter/delivery';

import ParcelTitle from '@self/root/src/widgets/content/cart/CartList/components/ParcelTitle/__pageObject';
import BusinessGroupsStrategiesSelector
    from '@self/root/src/widgets/content/cart/CartList/components/BusinessGroupsStrategiesSelector/__pageObject';

const farmaCart = buildCheckouterBucket({
    cartIndex: 0,
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

module.exports = makeSuite('Заголовок группы фармы 1.', {
    environment: 'kadavr',
    story: {
        async beforeEach() {
            this.setPageObjects({
                firstTitle: () => this.createPageObject(
                    ParcelTitle,
                    {
                        root: `${BusinessGroupsStrategiesSelector.bucket(0)} ${ParcelTitle.root}`,
                    }
                ),
            });

            await this.browser.yaScenario(
                this,
                prepareMultiCartState,
                [farmaCart]
            );

            await this.browser.yaOpenPage(PAGE_IDS_COMMON.CART, {lr: 213});
            return this.browser.yaScenario(this, waitForCartActualization);
        },

        'В корзине только фарма.': makeCase({
            async test() {
                return this.firstTitle.getTitleText().should.be.eventually.equal('Доставка Яндекса и продавцов');
            },
        }),
    },
});

