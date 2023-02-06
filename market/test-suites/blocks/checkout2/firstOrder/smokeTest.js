import {
    makeCase,
    makeSuite,
} from 'ginny';

import {prepareCheckoutPage} from '@self/root/src/spec/hermione/scenarios/checkout';
import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';
import * as kettle from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';
import {prepareMultiCartState} from '@self/root/src/spec/hermione/scenarios/cartResource';

export default makeSuite('Smoke test.', {
    feature: 'Чекаут',
    story: {
        async beforeEach() {
            const carts = [
                buildCheckouterBucket({
                    items: [{
                        skuMock: kettle.skuMock,
                        offerMock: kettle.offerMock,
                        count: 1,
                    }],
                }),
            ];


            const testState = await this.browser.yaScenario(
                this,
                prepareMultiCartState,
                carts
            );

            await this.browser.yaScenario(
                this,
                prepareCheckoutPage,
                {
                    items: testState.checkoutItems,
                    reportSkus: testState.reportSkus,
                    checkout2: true,
                }
            );
        },

        'Delivery Editor должен открываться': makeCase({
            issue: 'MARKETFRONT-34670',
            async test() {
                await this.deliveryEditor.waitForVisible();
            },
        }),
    },
});
