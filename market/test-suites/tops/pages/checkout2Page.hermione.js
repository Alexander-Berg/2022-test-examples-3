import {
    makeSuite,
    mergeSuites,
    prepareSuite,
} from 'ginny';

import {region} from '@self/root/src/spec/hermione/configs/geo';
import {commonParams} from '@self/root/src/spec/hermione/configs/params';

import {Preloader} from '@self/root/src/components/Preloader/__pageObject';
import {SummaryPlaceholder} from '@self/root/src/components/OrderTotalV2/components/SummaryPlaceholder/__pageObject';
import Checkout2Page from '@self/root/src/widgets/pages.desktop/Checkout2Page/__pageObject';
import DeliveryEditor from '@self/root/src/widgets/content/checkout/common/CheckoutDeliveryEditor/components/View/__pageObject';
import MedicalCartDeliveryEditor
    from '@self/root/src/widgets/content/checkout/common/CheckoutMedicalCartDeliveryEditor/components/View/__pageObject';
import CheckoutWizard from '@self/root/src/widgets/content/checkout/layout/components/wizard/__pageObject';
import RecipientPage from '@self/root/src/widgets/content/checkout/layout/CheckoutLayoutRecipientPage/view/__pageObject';
import ConfirmationPage from '@self/root/src/widgets/content/checkout/layout/CheckoutLayoutConfirmationPage/view/__pageObject';
import EditPaymentOption from '@self/root/src/components/EditPaymentOption/__pageObject';
import EditableCard from '@self/root/src/components/EditableCard/__pageObject/index.desktop';

import confirmationSuite from '@self/platform/spec/hermione/test-suites/blocks/checkout2/confirmation';
import firstOrder from '@self/platform/spec/hermione/test-suites/blocks/checkout2/firstOrder';
import largeCargoType from '@self/platform/spec/hermione/test-suites/blocks/checkout2/largeCargoType';
import repeatOrder from '@self/platform/spec/hermione/test-suites/blocks/checkout2/repeatOrder';
import hsch from '@self/platform/spec/hermione/test-suites/blocks/checkout2/hsch';
import payment from '@self/root/src/spec/hermione/test-suites/blocks/checkout/payment';
import cashbackOptions from '@self/root/src/spec/hermione/test-suites/blocks/checkout/cashback/options';
import YandexHelpSummaryTotal from '@self/root/src/spec/hermione/test-suites/blocks/checkout/confirmation/yandexHelpSummaryTotal';
import specifyAddress from '@self/platform/spec/hermione/test-suites/blocks/checkout2/specifyAddress';
import medicalFirstOrder from '@self/root/src/spec/hermione/test-suites/blocks/checkout/medicalDeliveryEditor/firstOrder';
import medicalRepeatOrder from '@self/root/src/spec/hermione/test-suites/blocks/checkout/medicalDeliveryEditor/repeatOrder';

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Новый чекаут', {
    environment: 'testing',
    params: {
        ...commonParams.description,
    },
    defaultParams: {
        ...commonParams.value,
    },
    story: mergeSuites(
        {
            async beforeEach() {
                this.setPageObjects({
                    preloader: () => this.createPageObject(Preloader),
                    summaryPlaceholder: () => this.createPageObject(SummaryPlaceholder),
                    checkoutPage: () => this.createPageObject(Checkout2Page),
                    confirmationPage: () => this.createPageObject(ConfirmationPage),
                    deliveryEditor: () => this.createPageObject(DeliveryEditor),
                    recipientEditor: () => this.createPageObject(RecipientPage),
                    deliveryEditorCheckoutWizard: () => this.createPageObject(CheckoutWizard, {
                        parent: this.deliveryEditor,
                    }),
                    recipientWizard: () => this.createPageObject(CheckoutWizard, {
                        parent: this.recipientEditor,
                    }),
                    medicalCartDeliveryEditor: () => this.createPageObject(MedicalCartDeliveryEditor),
                    medicalCartDeliveryEditorCheckoutWizard: () => this.createPageObject(CheckoutWizard, {
                        parent: this.medicalCartDeliveryEditor,
                    }),
                });
            },
        },
        prepareSuite(firstOrder),

        prepareSuite(medicalFirstOrder),

        prepareSuite(confirmationSuite, {
            params: {
                region: region['Москва'],
                isAuthWithPlugin: true,
            },
        }),

        prepareSuite(confirmationSuite, {
            suiteName: 'Гость. Страница подтверждения заказа',
            params: {
                region: region['Москва'],
                isAuthWithPlugin: false,
            },
        }),

        prepareSuite(YandexHelpSummaryTotal, {
            params: {
                region: region['Москва'],
                isAuthWithPlugin: true,
            },
        }),

        prepareSuite(largeCargoType),

        prepareSuite(repeatOrder, {
            params: {
                region: region['Москва'],
                isAuthWithPlugin: false,
            },
        }),

        prepareSuite(medicalRepeatOrder),

        prepareSuite(hsch),
        prepareSuite(cashbackOptions, {
            hooks: {
                async beforeEach() {
                    this.setPageObjects({
                        etidPaymentOptionBlock: () => this.createPageObject(EditPaymentOption, {
                            parent: this.checkoutPage,
                        }),
                        editableCard: () => this.createPageObject(EditableCard, {
                            parent: this.etidPaymentOptionBlock,
                        }),
                    });
                },
            },
        }),
        prepareSuite(payment),
        prepareSuite(specifyAddress, {
            suiteName: 'Гость. Уточнение адреса.',
            params: {
                isAuthWithPlugin: false,
            },
        }),
        prepareSuite(specifyAddress, {
            suiteName: 'Залогин. Уточнение адреса.',
            params: {
                isAuthWithPlugin: true,
            },
        })
    ),
});
