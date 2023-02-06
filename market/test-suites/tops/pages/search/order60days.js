import {makeSuite, prepareSuite} from 'ginny';
import {mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';
import {routes} from '@self/platform/spec/hermione/configs/routes';
// suites
import OfferDeliverySuite from '@self/platform/spec/hermione/test-suites/blocks/AutomaticallyCalculatedDelivery';
// page-objects
import SearchSnippetDelivery from '@self/platform/spec/page-objects/containers/SearchSnippet/Delivery';
// fixtures
import {offer60days} from './fixtures/offer';

export default makeSuite('Сниппет оффера (лист). До 60 дней', {
    environment: 'kadavr',
    story: prepareSuite(OfferDeliverySuite, {
        meta: {
            id: 'm-touch-3335',
            issue: 'MARKETFRONT-11608',
        },
        pageObjects: {
            delivery() {
                return this.createPageObject(SearchSnippetDelivery);
            },
        },
        hooks: {
            async beforeEach() {
                const dataMixin = {
                    data: {
                        search: {
                            total: 1,
                            totalOffers: 1,
                        },
                    },
                };

                const reportState = mergeState([
                    offer60days,
                    dataMixin,
                ]);

                await this.browser.setState('report', reportState);

                this.params = {
                    expectedText: 'Доставка от продавца',
                };

                return this.browser
                    .yaOpenPage('touch:list', routes.catalog.list)
                    .yaClosePopup(this.regionPopup);
            },
        },
    }),
});
