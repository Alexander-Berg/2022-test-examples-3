import {
    makeSuite,
    mergeSuites,
    prepareSuite,
} from 'ginny';

import {commonParams} from '@self/root/src/spec/hermione/configs/params';

import outlet from '@self/root/market/src/spec/hermione/test-suites/outlet';
import outletRouteInformation from '@self/root/market/src/spec/hermione/test-suites/outlet/outletRouteInformation';

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Страница ПВЗ', {
    environment: 'kadavr',
    params: {
        ...commonParams.description,
    },
    defaultParams: {
        ...commonParams.value,
    },
    story: mergeSuites(
        prepareSuite(outlet),
        prepareSuite(outletRouteInformation)
    ),
});
