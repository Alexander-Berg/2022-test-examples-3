import {prepareSuite, mergeSuites, makeSuite} from 'ginny';
import {createOffer} from '@yandex-market/kadavr/mocks/Report/helpers/searchResult';
import mergeReportState from '@yandex-market/kadavr/mocks/Report/helpers/mergeState';

import {routes} from '@self/platform/spec/hermione/configs/routes';
import OrderMinCostSuite from '@self/platform/spec/hermione/test-suites/blocks/n-snippet-card/order-min-cost';
import SnippetCard2 from '@self/project/src/components/Search/Snippet/Card/__pageObject';

export default makeSuite('Минимальная сумма заказа.', {
    environment: 'kadavr',
    story: mergeSuites(
        {
            beforeEach() {
                const offer = createOffer({
                    orderMinCost: {
                        value: 5500,
                        currency: 'RUR',
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
                    seller: {},
                });
                const dataMixin = {
                    data: {
                        search: {
                            total: 1,
                            totalOffers: 1,
                        },
                    },
                };
                const reportState = mergeReportState([
                    offer,
                    dataMixin,
                ]);

                return this.browser.setState('report', reportState)
                    .then(() => this.browser.yaOpenPage('market:search', routes.search.default));
            },
        },
        prepareSuite(OrderMinCostSuite,
            {
                pageObjects: {
                    snippetCard() {
                        return this.createPageObject(SnippetCard2);
                    },
                },
            }
        )
    ),
});
