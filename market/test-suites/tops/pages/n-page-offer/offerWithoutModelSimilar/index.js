import {prepareSuite, makeSuite, mergeSuites} from 'ginny';
import {createOffer, createProduct, mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';

import SearchSimilarSuite from '@self/platform/spec/hermione/test-suites/blocks/n-search-similar';
import SearchSimilarTitleSuite from '@self/platform/spec/hermione/test-suites/blocks/n-search-similar/__more-offers-title';
import SearchSimilarButtonSuite from '@self/platform/spec/hermione/test-suites/blocks/n-search-similar/__more-offers-button';
import SearchSimilar from '@self/platform/widgets/content/SearchSimilar/__pageObject';

const offerId = 'uQizLmsYjkLixn5SRhgitQ';
const categoryMock = {
    entity: 'category',
    id: 7757500,
    nid: 57713,
    name: 'Топоры',
    slug: 'topory',
    fullName: 'Топоры',
    type: 'guru',
    cpaType: 'cpa_non_guru',
    isLeaf: true,
    kinds: [],
};

/**
 * Создает мок оффера с необходимыми для теста данными
 *
 * @param {Object} options - свойства оффера
 * @param {String} wareId - id оффера
 * @returns {Object}
 */
function createOfferMockWithRequiredData(options, wareId) {
    return createOffer({
        ...options,
        shop: {
            name: 'Test shop',
        },
        urls: {
            encrypted: '/redir/ololo',
            offercard: 'an_encoded_url',
            U_DIRECT_OFFER_CARD_URL: `/offer/${wareId}`,
            decrypted: '/redir/decrypted',
            geo: '/redir/geo',
        },
    }, wareId);
}

/**
 * Создает мок продукта с необходимыми для теста данными
 *
 * @param {Object} options - свойства продукта
 * @returns {Object}
 */
function createProductMockWithRequiredData(options) {
    return createProduct({
        ...options,
        type: 'model',
    });
}

export default makeSuite('Блок "Похожие товары" на КО без модели', {
    environment: 'kadavr',
    story: mergeSuites(
        {
            async beforeEach() {
                const entityWithCategory = {
                    categories: [categoryMock],
                    urls: {
                        encrypted: '/redir/encrypted',
                        decrypted: '/redir/decrypted',
                        geo: '/redir/geo',
                        offercard: '/redir/offercard',
                    },
                    seller: {
                        comment: 'Comment',
                    },
                };
                const mainOffer = createOffer(entityWithCategory, offerId);
                const similarProducts = [
                    createProductMockWithRequiredData(entityWithCategory),
                    createProductMockWithRequiredData(entityWithCategory),
                    createProductMockWithRequiredData(entityWithCategory),
                ];
                const similarOffers = [
                    createOfferMockWithRequiredData(entityWithCategory, 'aaa111'),
                    createOfferMockWithRequiredData(entityWithCategory, 'aaa222'),
                    createOfferMockWithRequiredData(entityWithCategory, 'aaa333'),
                ];

                await this.browser.setState('report', mergeState([
                    mainOffer,
                    ...similarProducts,
                    ...similarOffers,
                    {
                        data: {
                            search: {
                                total: 6,
                            },
                        },
                    },
                ]));

                const serachLinksPath = await this.browser.yaBuildURL('market:list', {
                    'nid': categoryMock.nid,
                    'slug': categoryMock.slug,
                });
                const searchLinksQuery = {
                    'hid': String(categoryMock.id),
                    'cvredirect': '3',
                    'local-offers-first': '1',
                };
                this.params = {
                    goalsPageId: 'OFFER',
                    moreButtonText: 'Показать ещё',
                    moreButtonLinkPath: serachLinksPath,
                    moreButtonLinkQuery: searchLinksQuery,
                    titleText: 'Похожие товары',
                    titleLinkPath: serachLinksPath,
                    titleLinkQuery: searchLinksQuery,
                    zonePrefix: 'offer-page',
                };

                this.setPageObjects({
                    searchSimilar: () => this.createPageObject(SearchSimilar),
                });
            },
        },

        makeSuite('Без поискового запроса', {
            story: prepareSuite(SearchSimilarSuite, {
                hooks: {
                    async beforeEach() {
                        this.params = {
                            ...this.params,
                            offerSnippetTrack: 'offer_card_similar',
                            productSnippetTrack: 'offer_card_similar',
                            snippetsCount: 5,
                        };

                        await this.browser.yaOpenPage('market:offer', {
                            offerId,
                        });
                        await this.searchSimilar.waitForLoaded();
                    },
                },
            }),
        }),

        makeSuite('C поисковым запросом', {
            story: mergeSuites(
                {
                    async beforeEach() {
                        const textQuery = 'Топор туристический';

                        this.params.titleLinkQuery = {
                            ...this.params.titleLinkQuery,
                            text: textQuery,
                        };

                        await this.browser.yaOpenPage('market:offer', {
                            offerId,
                            text: textQuery,
                        });
                        await this.searchSimilar.waitForLoaded();
                    },
                },
                prepareSuite(SearchSimilarTitleSuite),
                prepareSuite(SearchSimilarButtonSuite)
            ),
        })
    ),
});
