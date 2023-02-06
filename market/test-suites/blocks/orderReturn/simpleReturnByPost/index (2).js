import {prepareSuite} from 'ginny';
import assert from 'assert';

import checkoutItemIds from '@self/root/src/spec/hermione/configs/checkout/items';
import returnsFormData from '@self/root/src/spec/hermione/configs/returns/formData';

import simpleReturnByPostSuite from '@self/project/src/spec/hermione/test-suites/blocks/orderReturn/simpleReturnByPost';


const ORDER_ITEM = {
    skuId: checkoutItemIds.asus.skuId,
    offerId: checkoutItemIds.asus.offerId,
    count: 2,
    id: 11111,
    supplierType: 'FIRST_PARTY',
};

const FILL_FORM_SCENARIO_PARAMS = {
    itemsIndexes: [1],
    itemsCount: ORDER_ITEM.count,
    itemsReasons: [{reason: 'bad_quality', text: 'test'}],
    recipient: {
        formData: returnsFormData,
    },
};

export default prepareSuite(simpleReturnByPostSuite, {
    params: {
        items: [ORDER_ITEM],
        fillFormScenarioParams: FILL_FORM_SCENARIO_PARAMS,
    },
    hooks: {
        beforeEach() {
            assert(this.params.fillFormScenarioParams, 'Param fillFormScenarioParams must be defined');
        },
    },
});
