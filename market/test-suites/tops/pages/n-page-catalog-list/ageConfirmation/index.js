import {makeSuite, prepareSuite} from 'ginny';
import {mergeState, createOffer} from '@yandex-market/kadavr/mocks/Report/helpers';

import AdultConfirmationPopupSuite from '@self/platform/spec/hermione/test-suites/blocks/AdultWarning/catalog';
import SnippetList from '@self/root/src/widgets/content/search/Serp/components/Page/__pageObject';
import AdultConfirmationPopup from '@self/platform/widgets/content/AdultWarning/components/AdultWarning/__pageObject';

export default makeSuite('Подтверждение возраста', {
    environment: 'kadavr',
    story: prepareSuite(AdultConfirmationPopupSuite, {
        params: {
            expectedSnippetsCount: 2,
        },
        pageObjects: {
            adultConfirmationPopup() {
                return this.createPageObject(AdultConfirmationPopup);
            },
            snippetList() {
                return this.createPageObject(SnippetList);
            },
        },
        hooks: {
            async beforeEach() {
                const routeParams = {
                    nid: 82910,
                    hid: 16155448,
                    slug: 'koniak-armaniak-brendi',
                };

                const state = mergeState([
                    createOffer({
                        categories: [{
                            id: 16155448,
                            nid: 82910,
                            slug: 'koniak-armaniak-brendi',
                            name: 'koniak-armaniak-brendi',
                            entity: 'category',
                        }],
                        prices: {
                            currency: 'RUB',
                            value: 42,
                        },
                        shop: {
                            id: 1,
                            name: 'shop',
                            slug: 'shop',
                        },
                        urls: {
                            encrypted: '/redir/test',
                            decrypted: '/redir/test',
                            geo: '/redir/test',
                            offercard: '/redir/test',
                        },
                    }),
                    createOffer({
                        categories: [{
                            id: 16155448,
                            nid: 82910,
                            slug: 'koniak-armaniak-brendi',
                            name: 'koniak-armaniak-brendi',
                            entity: 'category',
                        }],
                        prices: {
                            currency: 'RUB',
                            value: 420,
                        },
                        shop: {
                            id: 1,
                            name: 'shop',
                            slug: 'shop',
                        },
                        urls: {
                            encrypted: '/redir/test',
                            decrypted: '/redir/test',
                            geo: '/redir/test',
                            offercard: '/redir/test',
                        },
                    }),
                    {
                        data: {
                            search: {
                                total: 3,
                                totalOffers: 2,
                                totalOffersBeforeFilters: 3,
                                totalModels: 1,
                                adult: true,
                                shops: 2,
                                totalShopsBeforeFilters: 2,
                                isDeliveryIncluded: false,
                            },
                        },
                    },
                ]);

                await this.browser.setState('report', state);
                await this.browser.yaOpenPage('market:list', routeParams);
            },
        },
    }),
});
