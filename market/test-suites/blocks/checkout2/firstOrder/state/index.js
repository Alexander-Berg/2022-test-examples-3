import {
    prepareSuite,
    makeSuite,
    mergeSuites,
} from 'ginny';

import singleCart from './singleCart';

export default makeSuite('ХСЧ', {
    issue: 'MARKETFRONT-45669',
    feature: 'ХСЧ',
    environment: 'kadavr',
    story: mergeSuites(
        prepareSuite(singleCart)
    ),
});
