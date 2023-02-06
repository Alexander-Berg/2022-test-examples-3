import {mergeSuites, makeSuite} from 'ginny';

import promoDealsPageSuite from './promoDealsPage';

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Промо-страница.', {
    environment: 'testing',
    story: mergeSuites(
        promoDealsPageSuite
    ),
});
