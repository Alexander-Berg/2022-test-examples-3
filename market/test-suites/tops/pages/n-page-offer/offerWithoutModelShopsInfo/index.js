import {prepareSuite, makeSuite, mergeSuites} from 'ginny';
import {createOffer, mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';

import {createStories} from '@self/platform/spec/hermione/helpers/createStories';
import ShopsInfoLinkSuite from '@self/platform/spec/hermione/test-suites/blocks/n-shops-info/__link';
import LegalInfo from '@self/platform/spec/page-objects/components/LegalInfo';
import SearchSimilar from '@self/platform/widgets/content/SearchSimilar/__pageObject';

const sellerInfoText = 'Информация о продавце';
const sellersInfoText = 'Информация о продавцах';
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
const entityWithCategory = {
    categories: [categoryMock],
};
const shopIds = [1337, 1338, 1339];
const createSimilarOffers = (shopId, id) => createOffer({
    ...entityWithCategory,
    shop: {id: shopId},
    urls: {
        encrypted: '/redir/encrypted',
        decrypted: '/redir/decrypted',
        offercard: '/redir/offercard',
        geo: '/redir/geo',
    },
    seller: {
        comment: 'Comment',
    },
    cpc: id,
}, id);

export default makeSuite('Информация о продавцах', {
    environment: 'kadavr',
    story: mergeSuites(
        makeSuite('Несколько продавцов', {
            story: createStories({
                similar: {
                    description: 'Вкладка "Похожие товары"',
                    expectedIds: shopIds.join(','),
                    expectedText: sellersInfoText,
                    pageId: 'market:offer-similar',
                },
            },
            ({expectedIds, expectedText, pageId}) => prepareSuite(ShopsInfoLinkSuite, {
                meta: {
                    id: 'marketfront-3487',
                    issue: 'MARKETVERSTKA-34569',
                },
                params: {
                    expectedText,
                    expectedIds,
                },
                hooks: {
                    async beforeEach() {
                        const mainOffer = createOffer({
                            ...entityWithCategory,
                            shop: {
                                id: shopIds[0],
                            },
                            urls: {
                                encrypted: '/redir/encrypted',
                                decrypted: '/redir/decrypted',
                                offercard: '/redir/offercard',
                                geo: '/redir/geo',
                            },
                            seller: {
                                comment: 'Comment',
                            },
                        }, offerId);
                        const similarOffers = shopIds.map((shopId, index) => createSimilarOffers(shopId, index));

                        await this.browser.setState('report', mergeState([
                            mainOffer,
                            ...similarOffers,
                            {data: {
                                search: {
                                    total: 3,
                                },
                            }},
                        ]));
                        await this.browser.yaOpenPage(pageId, {offerId});
                        await this.browser.yaExecAsyncClientScript('window.initAllLazyWidgets');
                    },
                },
                pageObjects: {
                    shopsInfo() {
                        return this.createPageObject(LegalInfo);
                    },
                    searchSimilar() {
                        return this.createPageObject(SearchSimilar);
                    },
                },
            })),
        }),
        makeSuite('Один продавец', {
            story: createStories({
                spec: {
                    description: 'Вкладка "Характиристики"',
                    expectedIds: String(shopIds[0]),
                    expectedText: sellerInfoText,
                    pageId: 'market:offer-spec',
                },
                geo: {
                    description: 'Вкладка "Карта"',
                    expectedIds: String(shopIds[0]),
                    expectedText: sellerInfoText,
                    pageId: 'market:offer-geo',
                },
                reviews: {
                    description: 'Вкладка "Отзывы"',
                    expectedIds: String(shopIds[0]),
                    expectedText: sellerInfoText,
                    pageId: 'market:offer-reviews',
                },
            },
            ({expectedIds, expectedText, pageId}) => prepareSuite(ShopsInfoLinkSuite, {
                meta: {
                    id: 'marketfront-3488',
                    issue: 'MARKETVERSTKA-34570',
                },
                params: {
                    expectedText,
                    expectedIds,
                },
                hooks: {
                    async beforeEach() {
                        const offer = createOffer({
                            ...entityWithCategory,
                            shop: {id: shopIds[0]},
                            urls: {
                                encrypted: '/redir/encrypted',
                                decrypted: '/redir/decrypted',
                                offercard: '/redir/offercard',
                                geo: '/redir/geo',
                            },
                        }, offerId);

                        await this.browser.setState('report', offer);
                        await this.browser.yaOpenPage(pageId, {offerId});
                    },
                },
                pageObjects: {
                    shopsInfo() {
                        return this.createPageObject(LegalInfo);
                    },
                },
            })),
        })
    ),
});
