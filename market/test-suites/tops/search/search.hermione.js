import {makeSuite, mergeSuites} from '@yandex-market/ginny';

import VisualSearch from '@self/platform/spec/hermione2/test-suites/blocks/SearchSnippet/VisualSearch';
import filters from './filters';

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Страница поисковой выдачи.', {
    environment: 'testing',
    story: mergeSuites(
        filters,
        VisualSearch
    ),
});
