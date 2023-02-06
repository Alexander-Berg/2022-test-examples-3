import {
    prepareSuite,
    makeSuite,
    mergeSuites,
} from 'ginny';

import {commonParams} from '@self/root/src/spec/hermione/configs/params';

import {OrderTrack} from '@self/root/src/widgets/content/orders/OrderTrack/components/OrderTrack/__pageObject';

import DropshipSuite from '@self/platform/spec/hermione/test-suites/blocks/order-track/dropship';

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Трекинг', {
    environment: 'testing',
    params: {
        ...commonParams.description,
        items: 'Товары',
    },
    defaultParams: {
        ...commonParams.value,
    },
    story: mergeSuites(
        {
            beforeEach() {
                this.setPageObjects({
                    orderTrack: () => this.createPageObject(OrderTrack),
                });
            },
        },

        prepareSuite(DropshipSuite, {
            params: {
                needMuid: true,
            },
        })
    ),
});
