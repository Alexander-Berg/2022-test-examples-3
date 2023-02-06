import {makeSuite, mergeSuites, prepareSuite} from '@yandex-market/ginny';

// suites
import CatalogSuites from './catalog/index.screens';

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Страница выдачи', {
    environment: 'kadavr',
    story: mergeSuites(
        prepareSuite(CatalogSuites)
    ),
});
