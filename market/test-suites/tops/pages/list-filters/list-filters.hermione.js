import {prepareSuite, mergeSuites, makeSuite} from 'ginny';

import {createStories} from '@self/platform/spec/hermione/helpers/createStories';

import {routes} from '@self/platform/spec/hermione/configs/routes';

import FiltersSuite from '@self/platform/spec/hermione/test-suites/blocks/Filters';

import Filters from '@self/platform/components/Filters/__pageObject';
import FilterTumbler from '@self/platform/components/FilterTumbler/__pageObject';

import {state, filterId, filterName} from './fixtures';

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Страница фильтров.', {
    environment: 'kadavr',
    story: mergeSuites(
        createStories(
            {
                catalog: {
                    description: 'Каталог',
                    routeParams: routes.listFilters.catalog,
                },
                reviewsHubCategory: {
                    description: 'Хаб отзывов',
                    routeParams: routes.listFilters.reviewsHub,
                },
            },
            ({routeParams}) =>
                prepareSuite(FiltersSuite, {
                    hooks: {
                        async beforeEach() {
                            await this.browser.setState('report', state);
                            return this.browser.yaOpenPage('touch:list-filters', routeParams);
                        },
                    },
                    pageObjects: {
                        filterTumbler() {
                            return this.createPageObject(FilterTumbler, {
                                root: `[data-autotest-name="${filterName}"]`,
                            });
                        },
                        filters() {
                            return this.createPageObject(Filters);
                        },
                    },
                    params: {
                        filterId,
                        route: routeParams,
                    },
                })
        )
    ),
});
