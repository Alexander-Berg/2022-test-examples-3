import {makeSuite, prepareSuite} from 'ginny';
import {mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';

import ModelcardOffersSuite from '@self/platform/spec/hermione/test-suites/blocks/AdultWarning/modelcard-offers';
import SnippetList from '@self/platform/widgets/content/productOffers/Results/__pageObject';
import AdultConfirmationPopup from '@self/platform/widgets/content/AdultWarning/components/AdultWarning/__pageObject';

import oneOffer from '../fixtures/oneOffer';

export default makeSuite('Подтверждение возраста', {
    environment: 'kadavr',
    story: prepareSuite(ModelcardOffersSuite, {
        params: {
            expectedSnippetsCount: 1,
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
                const state = mergeState([
                    oneOffer.reportState,
                    {
                        data: {
                            search: {
                                total: 3,
                                totalOffers: 1,
                                totalOffersBeforeFilters: 3,
                                totalModels: 1,
                                adult: true,
                                shops: 1,
                                totalShopsBeforeFilters: 2,
                                isDeliveryIncluded: false,
                            },
                        },
                    }]);

                await this.browser.setState('report', state);
                await this.browser.yaOpenPage('market:product-offers', oneOffer.params);
            },
        },
    }),
});
