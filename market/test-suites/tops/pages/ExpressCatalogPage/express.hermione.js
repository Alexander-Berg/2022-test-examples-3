import {makeSuite, mergeSuites, prepareSuite} from '@yandex-market/ginny';

import ExpressCatalogPage from './index';

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Express', {
    environment: 'kadavr',
    feature: 'special',
    story: mergeSuites(
        prepareSuite(ExpressCatalogPage)
    ),
});
