import {prepareSuite, mergeSuites, makeSuite} from 'ginny';

import {createStories} from '@self/platform/spec/hermione/helpers/createStories';
// configs
import metricConfig from '@self/platform/spec/hermione/configs/metric/search-page';
import {routes} from '@self/platform/spec/hermione/configs/routes';
import {profiles} from '@self/platform/spec/hermione/configs/profiles';
// suites
import MetricaVisibleSuite from '@self/platform/spec/hermione/test-suites/blocks/Metrica/visible';
import MetricaClickSuite from '@self/platform/spec/hermione/test-suites/blocks/Metrica/click';

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Страница поиска.', {
    environment: 'testing',
    story: makeSuite('Метрика.', {
        beforeEach() {
            const profile = profiles.dzot61;
            return this.browser.yaLogin(profile.login, profile.password);
        },
        story: mergeSuites(
            makeSuite('Видимость виджета.', {
                environment: 'testing',
                story: createStories(
                    metricConfig.widgetsVisible,
                    ({meta, description, ...restParams}) => prepareSuite(MetricaVisibleSuite, {
                        hooks: {
                            beforeEach() {
                                return this.browser.yaOpenPage('market:search', routes.search.cats);
                            },
                        },
                        meta,
                        params: restParams,
                    })
                ),
            }),
            makeSuite('Видимость сниппета.', {
                environment: 'testing',
                story: createStories(
                    metricConfig.snippetsVisible,
                    ({meta, ...restParams}) => prepareSuite(MetricaVisibleSuite, {
                        hooks: {
                            beforeEach() {
                                return this.browser.yaOpenPage('market:search', routes.search.cats);
                            },
                        },
                        meta,
                        params: restParams,
                    })
                ),
            }),
            makeSuite('Клик по элементу.', {
                environment: 'testing',
                story: createStories(
                    metricConfig.elementClick,
                    ({meta, ...restParams}) => prepareSuite(MetricaClickSuite, {
                        hooks: {
                            beforeEach() {
                                return this.browser.yaOpenPage('market:search', routes.search.cats);
                            },
                        },
                        meta,
                        params: restParams,
                    })
                ),
            })
        ),
    }),
});
