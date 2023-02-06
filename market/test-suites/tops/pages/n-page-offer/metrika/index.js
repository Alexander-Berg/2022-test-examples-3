import {mergeSuites, prepareSuite, makeSuite} from 'ginny';
import {createOffer, mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';

import {createStories} from '@self/platform/spec/hermione/helpers/createStories';
// suites
import MetricaClickSuite from '@self/platform/spec/hermione/test-suites/blocks/Metrica/click';
import MetricaVisibleSuite from '@self/platform/spec/hermione/test-suites/blocks/Metrica/visible';
// page-objects
import ClickoutButton from '@self/platform/spec/page-objects/components/ClickoutButton';
import nodeConfig from '@self/platform/configs/current/node';

import metrikaConfig from './config';
import {offer, offerId} from '../fixtures/offerWithoutModel';

export default makeSuite('Метрика.', {
    environment: 'kadavr',
    story: mergeSuites(
        makeSuite('Кликаут с вкладки "Характеристики"', {
            story: prepareSuite(MetricaClickSuite, {
                meta: {
                    id: 'marketfront-3489',
                    issue: 'MARKETVERSTKA-34571',
                },
                params: {
                    expectedGoalName: 'offer-spec-page_default-offer_to-shop_go-to-shop',
                    counterId: nodeConfig.yaMetrika.market.id,
                    payloadSchema: () => true,
                    selector: ClickoutButton.root,
                },
                hooks: {
                    async beforeEach() {
                        await this.browser.setState('report', offer);
                        await this.browser.yaOpenPage('market:offer-spec', {offerId});
                    },
                },
            }),
        }),

        makeSuite('Кликаут с вкладки "Описание"', {
            story: prepareSuite(MetricaClickSuite, {
                meta: {
                    id: 'marketfront-3490',
                    issue: 'MARKETVERSTKA-34572',
                },
                params: {
                    expectedGoalName: 'offer-page_default-offer_to-shop_go-to-shop',
                    counterId: nodeConfig.yaMetrika.market.id,
                    payloadSchema: () => true,
                    selector: ClickoutButton.root,
                    scrollOffset: [0, -50],
                },
                hooks: {
                    async beforeEach() {
                        await this.browser.setState('report', offer);
                        await this.browser.yaOpenPage('market:offer', {offerId});
                    },
                },
            }),
        }),

        makeSuite('Кликаут с вкладки "Отзывы"', {
            story: prepareSuite(MetricaClickSuite, {
                meta: {
                    id: 'marketfront-3492',
                    issue: 'MARKETVERSTKA-34574',
                },
                params: {
                    expectedGoalName: 'offer-reviews-page_default-offer_to-shop_go-to-shop',
                    counterId: nodeConfig.yaMetrika.market.id,
                    payloadSchema: () => true,
                    selector: ClickoutButton.root,
                },

                hooks: {
                    async beforeEach() {
                        await this.browser.setState('report', offer);
                        await this.browser.yaOpenPage('market:offer-reviews', {offerId});
                    },
                },
            }),
        }),
        /*
        * Точка входа удалена. Задача на возврат тестов https://st.yandex-team.ru/MARKETFRONT-71298
        makeSuite('Кликаут с вкладки "Карта" ', {
            story: mergeSuites(
                {
                    async beforeEach() {
                        await this.browser.setState('report', offer);
                        await this.browser.yaOpenPage('market:offer-geo', {offerId});
                    },
                },
                makeSuite('Визитка', {
                    story: prepareSuite(MetricaClickSuite, {
                        meta: {
                            id: 'marketfront-3545',
                            issue: 'MARKETVERSTKA-34573',
                        },
                        params: {
                            expectedGoalName: 'offer-geo-page_default-offer_to-shop_go-to-shop',
                            counterId: nodeConfig.yaMetrika.market.id,
                            payloadSchema: () => true,
                            selector: ClickoutButton.root,
                        },
                    }),
                }),
                makeSuite('Оффер', {
                    story: prepareSuite(MetricaReactClickSuite, {
                        meta: {
                            id: 'marketfront-3491',
                            issue: 'MARKETVERSTKA-34573',
                        },
                        params: {
                            expectedGoalName: 'offer-geo-page_geo-panel_to-shop_go-to-shop',
                            counterId: nodeConfig.yaMetrika.market.id,
                            payloadSchema: () => true,
                        },
                        hooks: {
                            async beforeEach() {
                                this.setPageObjects({
                                    snippet: () => this.createPageObject(GeoSnippetPageObject),
                                });
                            },
                        },
                    }),
                })
            ),
        }),*/

        makeSuite('Вкладки', {
            story: mergeSuites(
                createStories(
                    metrikaConfig.elementClick,
                    ({
                        meta,
                        description,
                        pageId = 'market:offer',
                        ...restParams
                    }) => prepareSuite(MetricaClickSuite, {
                        hooks: {
                            async beforeEach() {
                                await this.browser.setState('report', offer);
                                await this.browser.yaOpenPage(pageId, {offerId});
                            },
                        },
                        meta,
                        params: {
                            counterId: nodeConfig.yaMetrika.market.id,
                            payloadSchema: () => true,
                            ...restParams,
                        },
                    })
                ),

                createStories(
                    metrikaConfig.snippetsVisible,
                    ({
                        meta,
                        pageId,
                        ...restParams
                    }) => prepareSuite(MetricaVisibleSuite, {
                        hooks: {
                            async beforeEach() {
                                const state = mergeState([
                                    offer,
                                    createOffer({
                                        urls: {
                                            encrypted: '/redir/encrypted',
                                            decrypted: '/redir/decrypted',
                                            offercard: '/redir/offercard',
                                            geo: '/redir/geo',
                                        },
                                        shop: {
                                            slug: 'shop',
                                            name: 'shop',
                                            id: 1,
                                        },
                                        seller: {
                                            comment: 'Comment',
                                        },
                                    }, 'aa11'),
                                    {
                                        data: {
                                            search: {
                                                total: 1,
                                            },
                                        },
                                    },
                                ]);

                                await this.browser.setState('report', state);
                                await this.browser.yaOpenPage(pageId, {offerId});
                            },
                        },
                        meta,
                        params: {
                            counterId: nodeConfig.yaMetrika.market.id,
                            payloadSchema: () => true,
                            scrollDown: true,
                            ...restParams,
                        },
                    })
                )
            ),
        })
    ),
});
