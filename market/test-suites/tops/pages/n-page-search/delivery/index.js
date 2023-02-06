import {prepareSuite, mergeSuites, makeSuite} from 'ginny';
import {mergeState, createOffer} from '@yandex-market/kadavr/mocks/Report/helpers';

import {routes} from '@self/platform/spec/hermione/configs/routes';
// suites
import DeliveryFromSuite from '@self/platform/spec/hermione/test-suites/blocks/n-delivery-popup/__delivery-from';
import DeliveryImpreciseConditionsSuite from '@self/platform/spec/hermione/test-suites/blocks/n-delivery/impreciseConditions';
// page-objects
import SnippetCard2 from '@self/project/src/components/Search/Snippet/Card/__pageObject';
// eslint-disable-next-line max-len
import DeliveryInfo from '@self/project/src/components/Search/Snippet/Offer/common/DeliveryInfo/components/DeliveryInfoContent/__pageObject';
// eslint-disable-next-line max-len
import DeliveryPopup from '@self/project/src/components/Search/Snippet/Offer/common/DeliveryInfo/components/DeliveryPopupContent/__pageObject';

import offerMock from './offer.mock';

export default makeSuite('Доставка на сниппетах.', {
    environment: 'kadavr',
    story: mergeSuites(
        {
            async beforeEach() {
                const offer = createOffer(offerMock);
                const dataMixin = {
                    data: {
                        search: {
                            total: 1,
                            totalOffers: 1,
                        },
                    },
                };
                const reportState = mergeState([
                    offer,
                    dataMixin,
                ]);
                await this.browser.setState('report', reportState);
                await this.browser.yaOpenPage('market:search', routes.search.default);
            },
        },
        prepareSuite(DeliveryFromSuite,
            {
                pageObjects: {
                    snippetCard() {
                        return this.createPageObject(SnippetCard2);
                    },
                    delivery() {
                        return this.createPageObject(DeliveryInfo);
                    },
                    deliveryPopup() {
                        return this.createPageObject(DeliveryPopup);
                    },
                },
                params: {
                    expectedDeliveryTo: 'Доставка в Москву',
                },
            }
        ),
        prepareSuite(DeliveryImpreciseConditionsSuite,
            {
                pageObjects: {
                    snippetCard() {
                        return this.createPageObject(SnippetCard2);
                    },
                    delivery() {
                        return this.createPageObject(DeliveryInfo);
                    },
                },
                params: {
                    expectedDeliveryText: 'Есть доставка',
                },
            }
        )
    ),
});
