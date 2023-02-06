import {makeSuite, mergeSuites, prepareSuite} from 'ginny';
import mergeReportState from '@yandex-market/kadavr/mocks/Report/helpers/mergeState';
import {routes} from '@self/platform/spec/hermione/configs/routes';
// suites
import SearchOfferPreorder from '@self/platform/spec/hermione/test-suites/blocks/SearchOffer/preorder';
// page-objects
import SearchOfferTile from '@self/platform/spec/page-objects/SearchOfferTile';
import {offerPreorder} from '@self/platform/spec/hermione/test-suites/tops/pages/search/fixtures/offerPreorder';
const dataMixin = {
    data: {
        search: {
            total: 1,
            totalOffers: 1,
        },
    },
};

const offerGridSuite = makeSuite('Предзаказ. Сниппет оффера (грид)', {
    environment: 'kadavr',
    story: prepareSuite(SearchOfferPreorder, {
        meta: {
            id: 'm-touch-3684',
            issue: 'MARKETFRONT-51879',
        },
        pageObjects: {
            searchOffer() {
                return this.createPageObject(SearchOfferTile, {
                    parent: this.searchResults,
                });
            },
        },
        hooks: {
            async beforeEach() {
                const reportState = mergeReportState([offerPreorder, dataMixin]);
                await this.browser.setState('report', reportState);
                return this.browser.yaOpenPage('touch:list', routes.catalog.phones)
                    .yaClosePopup(this.regionPopup);
            },
        },
    }),
});

export default mergeSuites(
    offerGridSuite
);
