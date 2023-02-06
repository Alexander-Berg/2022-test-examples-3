import {makeSuite, prepareSuite} from 'ginny';
import {mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';

import ModelcardSuite from '@self/platform/spec/hermione/test-suites/blocks/AdultWarning/modelcard';
import MiniTopOffers from '@self/platform/spec/page-objects/widgets/content/MiniTopOffers';
import AdultConfirmationPopup from '@self/platform/widgets/content/AdultWarning/components/AdultWarning/__pageObject';

import productWithTop6Offer from '../fixtures/productWithTop6Offer';

export default makeSuite('Подтверждение возраста', {
    environment: 'kadavr',
    story: prepareSuite(ModelcardSuite, {
        params: {
            expectedText: 'Найдены товары, для доступа к которым нужно подтвердить совершеннолетний возраст',
        },
        pageObjects: {
            adultConfirmationPopup() {
                return this.createPageObject(AdultConfirmationPopup);
            },
            topOffersList() {
                return this.createPageObject(MiniTopOffers);
            },
        },
        hooks: {
            async beforeEach() {
                const state = mergeState([
                    productWithTop6Offer.state,
                    {
                        data: {
                            search: {
                                adult: true,
                            },
                        },
                    },
                ]);

                await this.browser.setState('report', state);
                return this.browser.yaOpenPage('market:product', productWithTop6Offer.route);
            },
        },
    }),
});
