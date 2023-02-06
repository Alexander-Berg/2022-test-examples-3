import {mergeSuites, makeSuite} from 'ginny';

import promoDealsPage from './promoDealsPage';

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Промо-страница.', {
    environment: 'testing',
    story: mergeSuites(
        promoDealsPage
    ),
});
