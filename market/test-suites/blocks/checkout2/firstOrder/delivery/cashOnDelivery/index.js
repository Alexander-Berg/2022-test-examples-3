import {
    prepareSuite,
    makeSuite,
    mergeSuites,
} from 'ginny';

import {prepareCheckoutPage} from '@self/root/src/spec/hermione/scenarios/checkout';
import {prepareMultiCartState} from '@self/root/src/spec/hermione/scenarios/cartResource';

import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';
import * as kettle from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';

import AddressForm from '@self/root/src/components/AddressForm/__pageObject/index.js';
import GeoSuggest from '@self/root/src/components/GeoSuggest/__pageObject';
import RecipientForm from '@self/root/src/components/RecipientForm/__pageObject';
import RecipientInfo from '@self/root/src/widgets/content/checkout/layout/components/recipientInfo/__pageObject';
import CheckoutCourierMap
    from '@self/root/src/widgets/content/checkout/common/CheckoutVectorPinMap/components/VectorMap/__pageObject';
import CourierSuggest from
    '@self/root/src/widgets/content/checkout/common/CheckoutDeliveryEditor/components/CourierSuggest/__pageObject';

import firstStepDeliveryAndCash from './firstStep';
import firstStepDeliveryAndCashA11y from './firstStep.a11y';
import secondStepDeliveryAndCash from './secondStep';
import secondStepDeliveryAndCashA11y from './secondStep.a11y';
import thirdStepDeliveryAndCash from './thirdStep';

export default makeSuite('Оформление заказа с доставкой курьером и оплатой наличными при получении', {
    id: 'marketfront-4425',
    issue: 'MARKETFRONT-45602',
    feature: 'Оформление заказа с доставкой курьером и оплатой наличными при получении',
    environment: 'kadavr',
    story: mergeSuites(
        {
            async beforeEach() {
                const carts = [
                    buildCheckouterBucket({
                        items: [{
                            skuMock: kettle.skuMock,
                            offerMock: kettle.offerMock,
                            count: 1,
                        }],
                    }),
                ];


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
                    street: () => this.createPageObject(GeoSuggest, {
                        parent: this.addressForm,
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
                    recipientInfo: () => this.createPageObject(RecipientInfo, {
                        parent: this.recipientWizard,
                    }),
                    map: () => this.createPageObject(CheckoutCourierMap, {
                        parent: this.deliveryEditorCheckoutWizard,
                    }),
                });
            },
        },
        prepareSuite(firstStepDeliveryAndCash),
        prepareSuite(firstStepDeliveryAndCashA11y),

        prepareSuite(secondStepDeliveryAndCash),
        prepareSuite(secondStepDeliveryAndCashA11y),

        prepareSuite(thirdStepDeliveryAndCash)
    ),
});
