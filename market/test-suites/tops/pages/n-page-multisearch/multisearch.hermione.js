import {prepareSuite, mergeSuites, makeSuite} from 'ginny';

import {routes} from '@self/platform/spec/hermione/configs/routes';
import WithCategorySuite from '@self/platform/spec/hermione/test-suites/blocks/FiltersInteraction/withCategory';
import SearchIntentsAside from '@self/root/src/widgets/content/search/Intents/components/IntentsTree/__pageObject';

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Страница мультивыдачи.', {
    story: mergeSuites(
        makeSuite('Игрушки и игровые наборы из франшизы.', {
            environment: 'testing',
            story: prepareSuite(WithCategorySuite, {
                pageObjects: {
                    searchIntent() {
                        return this.createPageObject(SearchIntentsAside);
                    },
                },
                hooks: {
                    beforeEach() {
                        return this.browser.yaOpenPage('market:multisearch', routes.search.multisearchFranchise);
                    },
                },
                params: {
                    queryParamName: 'glfilter',
                    queryParamValue: routes.search.multisearchFranchise.glfilter,
                },
            }),
        })
    ),
});
