import {
    prepareSuite,
    makeSuite,
    mergeSuites,
} from 'ginny';

import {prepareCheckoutPage} from '@self/root/src/spec/hermione/scenarios/checkout';
import {prepareMultiCartState} from '@self/root/src/spec/hermione/scenarios/cartResource';

import AddressForm from '@self/root/src/components/AddressForm/__pageObject/index.js';
import GeoSuggest from '@self/root/src/components/GeoSuggest/__pageObject';
import RecipientForm from '@self/root/src/components/RecipientForm/__pageObject';
import PlacemarkMap
    from '@self/root/src/widgets/content/checkout/common/CheckoutVectorPlacemarkMap/components/VectorPlacemarkMap/__pageObject';
import CourierSuggest from
    '@self/root/src/widgets/content/checkout/common/CheckoutDeliveryEditor/components/CourierSuggest/__pageObject';

import firstStep from './firstStep';
import secondStep from './secondStep';
import thirdStep from './thirdStep';
import forthStep from './forthStep';
import {carts} from './helpers';

export default makeSuite('С&C и КГТ', {
    id: 'marketfront-4426',
    issue: 'MARKETFRONT-36074',
    feature: 'С&C и КГТ',
    environment: 'kadavr',
    story: mergeSuites(
        {
            async beforeEach() {
                const testState = await this.browser.yaScenario(
                    this,
                    prepareMultiCartState,
                    carts
                );

                await this.browser.yaScenario(
                    this,
                    prepareCheckoutPage,
                    {
                        items: testState.checkoutItems,
                        reportSkus: testState.reportSkus,
                        checkout2: true,
                    }
                );

                this.setPageObjects({
                    addressForm: () => this.createPageObject(AddressForm, {
                        parent: this.deliveryEditorCheckoutWizard,
                    }),
                    courierSuggest: () => this.createPageObject(CourierSuggest, {
                        parent: this.deliveryEditorCheckoutWizard,
                    }),
                    courierSuggestInput: () => this.createPageObject(GeoSuggest, {
                        parent: this.courierSuggest,
                    }),
                    recipientForm: () => this.createPageObject(RecipientForm, {
                        parent: this.recipientWizard,
                    }),
                    placemarkMap: () => this.createPageObject(PlacemarkMap, {
                        parent: this.deliveryEditorCheckoutWizard,
                    }),
                });
            },
        },
        prepareSuite(firstStep),
        prepareSuite(secondStep),
        prepareSuite(thirdStep),
        prepareSuite(forthStep)
    ),
});
