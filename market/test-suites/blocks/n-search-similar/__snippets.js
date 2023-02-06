import {prepareSuite, makeSuite, makeCase, mergeSuites} from 'ginny';
import schema from 'js-schema';
import nodeConfig from '@self/platform/configs/current/node';
import {createStories} from '@self/platform/spec/hermione/helpers/createStories';

import MetricaVisibleSuite from '@self/platform/spec/hermione/test-suites/blocks/Metrica/visible';
import MetricaClickSuite from '@self/platform/spec/hermione/test-suites/blocks/Metrica/click';
import SearchSimilar from '@self/platform/widgets/content/SearchSimilar/__pageObject';

const metrikaClickConfig = [
    {
        description: 'Клик по картинке продуктового сниппета',
        meta: {
            issue: 'MARKETVERSTKA-33759',
            id: 'marketfront-3357',
        },
        selector: `${SearchSimilar.productSnippet} ${SearchSimilar.productSnippetImageLink}`,
        goalSuffix: 'similar_search-results_search-results-paged_search-partition_snippet-list_snippet-card_picture_click',
    },
    {
        description: 'Клик по заголовку продуктового сниппета',
        meta: {
            issue: 'MARKETVERSTKA-33760',
            id: 'marketfront-3358',
        },
        selector: `${SearchSimilar.productSnippet} ${SearchSimilar.snippetTitleLink}`,
        goalSuffix: 'similar_search-results_search-results-paged_search-partition_snippet-list_snippet-card_title_click',
    },
    {
        description: 'Клик по цене продуктового сниппета',
        meta: {
            issue: 'MARKETVERSTKA-33761',
            id: 'marketfront-3359',
        },
        selector: `${SearchSimilar.productSnippet} ${SearchSimilar.snippetPriceLink}`,
        goalSuffix: 'similar_search-results_search-results-paged_search-partition_snippet-list_snippet-card_price_click',
    },
    {
        description: 'Клик по цене офферного сниппета',
        meta: {
            issue: 'MARKETVERSTKA-33762',
            id: 'marketfront-3360',
        },
        selector: `${SearchSimilar.offerSnippet} ${SearchSimilar.snippetPriceLink}`,
        goalSuffix: 'similar_search-results_search-results-paged_search-partition_snippet-list_snippet-card_price_offer-link_click',
    },
    {
        description: 'Клик по картинке офферного сниппета',
        meta: {
            issue: 'MARKETVERSTKA-33763',
            id: 'marketfront-3361',
        },
        selector: `${SearchSimilar.offerSnippet} ${SearchSimilar.offerSnippetImageLink}`,
        goalSuffix: 'similar_search-results_search-results-paged_search-partition_snippet-list_snippet-card_picture_offer-link_click',
    },
    {
        description: 'Клик по заголовку офферного сниппета',
        meta: {
            issue: 'MARKETVERSTKA-33764',
            id: 'marketfront-3362',
        },
        selector: `${SearchSimilar.offerSnippet} ${SearchSimilar.snippetTitleLink}`,
        goalSuffix: 'similar_search-results_search-results-paged_search-partition_snippet-list_snippet-card_title_offer-link_click',
    },
    {
        description: 'Клик по названию магазина офферного сниппета',
        meta: {
            issue: 'MARKETVERSTKA-33765',
            id: 'marketfront-3363',
        },
        selector: `${SearchSimilar.offerSnippet} ${SearchSimilar.snippetShopNameLink}`,
        goalSuffix: 'similar_search-results_search-results-paged_search-partition_snippet-list_snippet-card_shop-name_go-to-shop',
    },
];

/**
 * @param {PageObject.SearchSimilar} searchSimilar
 */
export default makeSuite('Список сниппетов', {
    params: {
        goalsPageId: 'Префикс событий Метрики, соответствующий текущей странице (см. goalsPoints.js) – ' +
            'OFFER | SEARCH | ...',
        offerSnippetTrack: 'Параметр ссылки track, который должен присустствовать в ссылке офферного сниппета',
        productSnippetTrack: 'Параметр ссылки track, который должен присустствовать в ссылке продуктового сниппета',
        snippetsCount: 'Количество сниппетов в списке товаров',
        zonePrefix: 'Префикс зоны Метрики, в которой находится текущий блок',
    },
    story: {
        'Содержит заданное количество сниппетов товаров': makeCase({
            issue: 'MARKETVERSTKA-33753',
            id: 'marketfront-3351',
            async test() {
                const snippetsCount = await this.searchSimilar.getSnippetsCount();

                await this.expect(snippetsCount).to.be.equal(
                    this.params.snippetsCount,
                    'Количество сниппетов совпадает с заданным'
                );
            },
        }),

        'Cниппет продукта': {
            'содержит ссылки на КМ': makeCase({
                issue: 'MARKETVERSTKA-33749',
                id: 'marketfront-3347',
                test() {
                    const expectedLink = {
                        pathname: '/product--[\\w-]+/\\d+',
                    };
                    const assertionParams = {
                        mode: 'match',
                        skipHostname: true,
                        skipProtocol: true,
                    };

                    return Promise.all([
                        this.searchSimilar.getProductSnippetImageLink(),
                        this.searchSimilar.getProductSnippetPriceLink(),
                        this.searchSimilar.getProductSnippetTitleLink(),
                    ]).then(productSnippetLinks => Promise.all([
                        this.expect(productSnippetLinks[0], 'Ссылка c картинки продуктового сниппета ведет на КМ')
                            .to.be.link(expectedLink, assertionParams),
                        this.expect(productSnippetLinks[1], 'Ссылка c цены продуктового сниппета ведет на КМ')
                            .to.be.link(expectedLink, assertionParams),
                        this.expect(productSnippetLinks[2], 'Ссылка c заголовка продуктового сниппета ведет на КМ')
                            .to.be.link(expectedLink, assertionParams),
                    ]));
                },
            }),
        },

        'Cниппет оффера': {
            'содержит ссылки на КО': makeCase({
                issue: 'MARKETVERSTKA-33750',
                id: 'marketfront-3348',
                test() {
                    // eslint-disable-next-line market/ginny/no-skip
                    return this.skip(
                        'MARKETFRONT-8739 скипаем упавшие тесты для озеленения, ' +
                        'тикет на починку MARKETVERSTKA-32345'
                    );

                    // eslint-disable-next-line no-unreachable
                    const expectedLink = {
                        pathname: '/offer/\\w+',
                    };
                    const assertionParams = {
                        mode: 'match',
                        skipHostname: true,
                        skipProtocol: true,
                    };

                    return Promise.all([
                        this.searchSimilar.getOfferSnippetImageLink(),
                        this.searchSimilar.getOfferSnippetPriceLink(),
                        this.searchSimilar.getOfferSnippetTitleLink(),
                    ]).then(offerSnippetLinks => Promise.all([
                        this.expect(offerSnippetLinks[0], 'Ссылка c картинки офферного сниппета ведет на КО')
                            .to.be.link(expectedLink, assertionParams),
                        this.expect(offerSnippetLinks[1], 'Ссылка c цены офферного сниппета ведет на КО')
                            .to.be.link(expectedLink, assertionParams),
                        this.expect(offerSnippetLinks[2], 'Ссылка c заголовка офферного сниппета ведет на КО')
                            .to.be.link(expectedLink, assertionParams),
                    ]));
                },
            }),
        },

        'Метрика.': mergeSuites(
            {
                'Видимость виджета.': prepareSuite(MetricaVisibleSuite, {
                    meta: {
                        issue: 'MARKETVERSTKA-33756',
                        id: 'marketfront-3354',
                    },
                    hooks: {
                        beforeEach() {
                            this.params = {
                                ...this.params,
                                counterId: nodeConfig.yaMetrika.market.id,
                                expectedGoalName: `${this.params.zonePrefix}_similar_search-results_search-results-paged_search-partition_snippet-list_snippet-card_visible`,
                                payloadSchema: schema({}),
                                selector: SearchSimilar.snippet,
                            };
                        },
                    },
                }),
            },

            createStories(metrikaClickConfig, ({meta, goalSuffix, selector}) => mergeSuites(
                {
                    'Общий goal сниппета.': prepareSuite(MetricaClickSuite, {
                        meta,
                        hooks: {
                            beforeEach() {
                                this.params = {
                                    ...this.params,
                                    counterId: nodeConfig.yaMetrika.market.id,
                                    expectedGoalName: `${this.params.zonePrefix}_similar_search-results_search-results-paged_search-partition_snippet-list_snippet-card_click`,
                                    payloadSchema: schema({}),
                                    selector,
                                    scrollOffset: [0, -50],
                                };
                            },
                        },
                    }),

                    'Специфичный goal области сниппета.': prepareSuite(MetricaClickSuite, {
                        meta,
                        hooks: {
                            beforeEach() {
                                this.params = {
                                    ...this.params,
                                    counterId: nodeConfig.yaMetrika.market.id,
                                    expectedGoalName: `${this.params.zonePrefix}_${goalSuffix}`,
                                    payloadSchema: schema({}),
                                    selector,
                                    scrollOffset: [0, -50],
                                };
                            },
                        },
                    }),
                }
            ))
        ),
    },
});
