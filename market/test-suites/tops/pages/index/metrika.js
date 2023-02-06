import schema from 'js-schema';
import {mergeSuites, prepareSuite, makeSuite} from 'ginny';

import nodeConfig from '@self/platform/configs/development/node';

import {createStories} from '@self/platform/spec/hermione/helpers/createStories';
import metricConfig from '@self/platform/spec/hermione/configs/metric/index-page';
import MetricaVisibleSuite from '@self/platform/spec/hermione/test-suites/blocks/Metrica/visible';
import MetricaClickSuite from '@self/platform/spec/hermione/test-suites/blocks/Metrica/click';
import Roll from '@self/platform/spec/page-objects/Roll';

export default makeSuite('Метрика.', {
    environment: 'testing',
    story: mergeSuites(
        makeSuite('Видимость виджета.', {
            story: createStories(
                metricConfig.widgetsVisible,
                ({meta, description, ...restParams}) => prepareSuite(MetricaVisibleSuite, {
                    hooks: {
                        beforeEach() {
                            return this.browser
                                .yaProfile('dzot61', 'touch:index');
                        },
                        afterEach() {
                            return this.browser.yaLogout();
                        },
                    },
                    meta,
                    params: restParams,
                })
            ),
        }),
        makeSuite('Клик по элементу.', {
            story: createStories(
                metricConfig.elementClick,
                ({meta, description, ...restParams}) => prepareSuite(MetricaClickSuite, {
                    hooks: {
                        beforeEach() {
                            return this.browser
                                .yaProfile('dzot61', 'touch:index');
                        },
                        afterEach() {
                            return this.browser.yaLogout();
                        },
                    },
                    meta,
                    params: restParams,
                })
            ),
        }),
        makeSuite('Лента.', {
            story: mergeSuites(
                makeSuite('Клик по сниппету.', {
                    story: createStories(
                        metricConfig.popupElementClick,
                        ({meta, popupSelector, ...restParams}) => prepareSuite(MetricaClickSuite, {
                            hooks: {
                                async beforeEach() {
                                    await this.browser.yaOpenPage('touch:index');
                                    await this.browser.yaSlowlyScroll(popupSelector);
                                    await this.browser.allure.runStep(
                                        'Открываем попап',
                                        () => this.browser.click(popupSelector)
                                    );
                                },
                            },
                            meta,
                            params: restParams,
                        })
                    ),
                }),
                makeSuite('Клик по кнопке "Показать еще"', {
                    story: prepareSuite(MetricaClickSuite, {
                        hooks: {
                            async beforeEach() {
                                await this.browser.yaOpenPage('touch:index');
                                await this.browser.waitUntil(async () => {
                                    await this.browser.yaSlowlyScroll(Roll.loadMoreButton);
                                    return this.browser.isVisible(Roll.loadMoreButton);
                                }, 5000);
                            },
                        },
                        meta: {
                            issue: 'MOBMARKET-9789',
                            id: 'm-touch-2392',
                        },
                        params: {
                            expectedGoalName: 'index-page_feed_roll-recommendations-load',
                            counterId: nodeConfig.yaMetrika.market.id,
                            payloadSchema: schema({
                                reqId: String,
                                pageId: 'touch:index',
                            }),
                            selector: `${Roll.root} ${Roll.loadMoreButton}`,
                        },
                    }),
                })
            ),
        })
    ),
});
