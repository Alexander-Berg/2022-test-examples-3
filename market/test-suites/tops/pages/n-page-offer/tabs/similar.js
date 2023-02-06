import {prepareSuite, makeSuite, mergeSuites} from 'ginny';
import {createOffer, mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';

// suites
import MoreOffersButtonSuite from '@self/platform/spec/hermione/test-suites/blocks/n-search-similar/__more-offers-button';
import OfferCardSuite from '@self/platform/spec/hermione/test-suites/blocks/n-product-default-offer/offer-card';
// page-objects
import DefaultOfferMini from '@self/platform/components/DefaultOfferMini/__pageObject';
import ClickoutButton from '@self/platform/spec/page-objects/components/ClickoutButton';
import SearchSimilar from '@self/platform/widgets/content/SearchSimilar/__pageObject';

import {offer, offerId, categoryMock} from '../fixtures/offerWithoutModel';

export default makeSuite('Контент вкладки "Похожие товары"', {
    id: 'marketfront-3484',
    issue: 'MARKETVERSTKA-34566',
    environment: 'kadavr',
    story: mergeSuites(
        {
            async beforeEach() {
                this.params.moreButtonLinkPath = await this.browser.yaBuildURL('market:list', {
                    'nid': categoryMock.nid,
                    'slug': categoryMock.slug,
                });
                await this.browser.setState('report', mergeState([
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
                    {data: {
                        search: {
                            total: 2,
                        },
                    }},
                ]));
                await this.browser.yaOpenPage('market:offer-similar', {offerId});
            },
        },
        prepareSuite(MoreOffersButtonSuite, {
            params: {
                moreButtonText: 'Показать ещё',
            },
            pageObjects: {
                searchSimilar() {
                    return this.createPageObject(SearchSimilar);
                },
            },
            only: ['Всегда'],
        }),
        prepareSuite(OfferCardSuite, {
            pageObjects: {
                productDefaultOfferActionButton() {
                    return this.createPageObject(ClickoutButton, {
                        parent: DefaultOfferMini.root,
                    });
                },
                productDefaultOffer() {
                    return this.createPageObject(DefaultOfferMini);
                },
            },
        })
        /** MARKETFRONT-58887: Скип автотестов в релизе 2021.375.0
        prepareSuite(WishlistControlSuite, {
            pageObjects: {
                wishlistControl() {
                    return this.createPageObject(WishlistTumbler, {
                        parent: MiniCard.root,
                    });
                },
            },
        })
        */
    ),
});
