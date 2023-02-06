import {
    makeSuite,
    mergeSuites,
    prepareSuite,
} from 'ginny';

import {region} from '@self/root/src/spec/hermione/configs/geo';
import {commonParams} from '@self/root/src/spec/hermione/configs/params';

import Checkout2Page from '@self/root/src/widgets/pages.touch/Checkout2Page/__pageObject';
import DeliveryEditor
    from '@self/root/src/widgets/content/checkout/common/CheckoutTouchSimpleDeliveryEditor/components/View/__pageObject';
import MedicalCartDeliveryEditor
    from '@self/root/src/widgets/content/checkout/common/CheckoutMedicalCartDeliveryEditor/components/View/__pageObject';
import {Preloader} from '@self/root/src/components/Preloader/__pageObject';
import {SummaryPlaceholder} from '@self/root/src/components/OrderTotalV2/components/SummaryPlaceholder/__pageObject';

import EditPaymentOption from '@self/root/src/components/EditPaymentOption/__pageObject';
import EditableCard from '@self/root/src/components/EditableCard/__pageObject/index.touch';

import firstOrder from '@self/platform/spec/hermione/test-suites/blocks/checkout2/firstOrder';
import repeatOrder from '@self/platform/spec/hermione/test-suites/blocks/checkout2/repeatOrder';
import hsch from '@self/platform/spec/hermione/test-suites/blocks/checkout2/hsch';
import cashbackOptions from '@self/root/src/spec/hermione/test-suites/blocks/checkout/cashback/options';
import YandexHelpSummaryTotal from '@self/root/src/spec/hermione/test-suites/blocks/checkout/confirmation/yandexHelpSummaryTotal';
import medicalFirstOrder from '@self/platform/spec/hermione/test-suites/blocks/checkout2/firstOrder/medical';

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Новый чекаут', {
    environment: 'kadavr',
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
                    deliveryEditor: () => this.createPageObject(DeliveryEditor),
                    medicalCartDeliveryEditor: () => this.createPageObject(MedicalCartDeliveryEditor),
                });
            },
        },
        prepareSuite(YandexHelpSummaryTotal, {
            params: {
                region: region['Москва'],
                isAuthWithPlugin: true,
            },
        }),
        prepareSuite(firstOrder),
        prepareSuite(medicalFirstOrder),
        prepareSuite(repeatOrder),
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
        })
    ),
});
