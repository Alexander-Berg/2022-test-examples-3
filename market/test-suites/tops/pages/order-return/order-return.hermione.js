import {
    makeSuite,
    mergeSuites,
    prepareSuite,
} from 'ginny';

import {commonParams} from '@self/root/src/spec/hermione/configs/params';

import orderReturnsFormSuite from '@self/platform/spec/hermione/test-suites/blocks/orderReturn/form';
import orderReturnsCreateSuite from '@self/platform/spec/hermione/test-suites/blocks/orderReturn/create';
import dropship from '@self/platform/spec/hermione/test-suites/blocks/orderReturn/dropship';
import unfitReasonRestriction from '@self/platform/spec/hermione/test-suites/blocks/orderReturn/unfitReasonRestriction';
import orderReturnsMapSuite from '@self/platform/spec/hermione/test-suites/blocks/orderReturn/map';
import regionDiffersFromOrder from '@self/platform/spec/hermione/test-suites/blocks/orderReturn/regionDiffersFromOrder';
import simpleReturnByPostSuite from '@self/platform/spec/hermione/test-suites/blocks/orderReturn/simpleReturnByPost';
import returnDeliveryCompensation from '@self/platform/spec/hermione/test-suites/blocks/orderReturn/returnDeliveryCompensation';
import fbsOrderReturnInBrandedPickupPoint
    from '@self/root/src/spec/hermione/test-suites/blocks/orderReturn/fbsOrderReturnInBrandedPickupPoint';

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Возвраты.', {
    environment: 'kadavr',
    params: {
        ...commonParams.description,
    },
    defaultParams: {
        ...commonParams.value,
        isAuthWithPlugin: true,
    },
    story: mergeSuites(
        prepareSuite(orderReturnsFormSuite, {}),
        prepareSuite(orderReturnsCreateSuite, {}),
        prepareSuite(dropship, {}),
        prepareSuite(unfitReasonRestriction, {}),
        prepareSuite(orderReturnsMapSuite),
        prepareSuite(regionDiffersFromOrder),
        prepareSuite(returnDeliveryCompensation),
        prepareSuite(simpleReturnByPostSuite),
        prepareSuite(fbsOrderReturnInBrandedPickupPoint)
    ),
});
