import {prepareSuite} from 'ginny';

import checkoutItemIds from '@self/root/src/spec/hermione/configs/checkout/items';

import simpleReturnByPostSuite from '@self/project/src/spec/hermione/test-suites/blocks/orderReturn/simpleReturnByPost';

export default prepareSuite(simpleReturnByPostSuite, {
    params: {
        items: [{
            skuId: checkoutItemIds.asus.skuId,
            offerId: checkoutItemIds.asus.offerId,
            count: 1,
            id: 11111,
            supplierType: 'FIRST_PARTY',
        }],
    },
});
