import {makeSuite, prepareSuite} from 'ginny';
import {mergeState, createOffer} from '@yandex-market/kadavr/mocks/Report/helpers';

import AdultConfirmationPopupSearchSuite from '@self/platform/spec/hermione/test-suites/blocks/AdultWarning/search';
import SnippetList from '@self/root/src/widgets/content/search/Serp/components/Page/__pageObject';
import AdultConfirmationPopup from '@self/platform/widgets/content/AdultWarning/components/AdultWarning/__pageObject';
import ProductWarning from '@self/project/src/components/ProductWarning/__pageObject/index.desktop';

export default makeSuite('Подтверждение возраста', {
    environment: 'kadavr',
    story: prepareSuite(AdultConfirmationPopupSearchSuite, {
        params: {
            expectedSnippetsCount: 2,
            expectedSafeSnippetsCount: 3,
        },
        pageObjects: {
            adultConfirmationPopup() {
                return this.createPageObject(AdultConfirmationPopup);
            },
            snippetList() {
                return this.createPageObject(SnippetList);
            },
            ageWarning() {
                return this.createPageObject(ProductWarning, {
                    root: ProductWarning.age,
                });
            },
        },
        hooks: {
            async beforeEach() {
                const routeParams = {
                    lr: 213,
                    text: 'красный',
                    onstock: 1,
                };

                const warningsMixin = {
                    warnings: {
                        common: [{
                            type: 'adult',
                            value: {
                                full: 'Возрастное ограничение',
                                short: 'Возрастное ограничение',
                            },
                        }],
                    },
                };

                const state = mergeState([
                    createOffer({
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
                        ...warningsMixin,
                    }),
                    createOffer({
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
                        ...warningsMixin,
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
                await this.browser.yaOpenPage('market:search', routeParams);
            },
        },
    }),
});
