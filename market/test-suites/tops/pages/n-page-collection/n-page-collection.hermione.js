import {prepareSuite, makeSuite, mergeSuites} from 'ginny';

import {routes} from '@self/platform/spec/hermione/configs/routes';
import CollectionListSuite from '@self/platform/spec/hermione/test-suites/blocks/n-collection-list';
import CollectionList from '@self/platform/spec/page-objects/n-collection-list';

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Страница коллекции.', {
    environment: 'testing',
    issue: 'MARKETVERSTKA-26709',
    story: mergeSuites(
        prepareSuite(CollectionListSuite, {
            pageObjects: {
                collectionList() {
                    return this.createPageObject(CollectionList);
                },
            },

            hooks: {
                beforeEach() {
                    const params = routes.collection.comparison;
                    return this.browser.yaOpenPage('market:collections', params);
                },
            },

            params: {
                collectionItemsCount: 7,
            },
        })
    ),
});
