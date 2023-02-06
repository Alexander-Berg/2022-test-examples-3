import {makeSuite, mergeSuites, prepareSuite} from 'ginny';

import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import {prepareCheckoutPage} from '@self/root/src/spec/hermione/scenarios/checkout';
import {lensesBausch} from '@self/root/src/spec/hermione/configs/checkout/items';
import userFormData from '@self/root/src/spec/hermione/configs/checkout/formData/user-prepaid';
import {goToConfirmationPage} from '@self/root/src/spec/hermione/scenarios/checkout/goToConfirmationPage';
import CheckoutWizard from '@self/root/src/widgets/content/checkout/layout/components/wizard/__pageObject';
import DeliveryTypeList from '@self/root/src/components/DeliveryTypes/__pageObject';
import EditPopup
    from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/EditPopup/__pageObject';
import EditPaymentOption from '@self/root/src/components/EditPaymentOption/__pageObject';
import EditableCard from '@self/root/src/components/EditableCard/__pageObject';
import PaymentOptions
    from '@self/root/src/components/PaymentOptionsList/__pageObject';
import CheckoutOrderButton
    from '@self/root/src/widgets/content/checkout/common/CheckoutOrderButton/components/View/__pageObject';
import GroupedParcels
    from '@self/root/src/widgets/content/checkout/common/CheckoutParcels/components/View/__pageObject';
import GroupedParcel
    from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/GroupedParcel/__pageObject';
// eslint-disable-next-line max-len
import AddressList from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/EditPopup/components/AddressList/__pageObject';
import OrderConfirmation from '@self/root/src/spec/page-objects/OrderConfirmation';
import {region} from '@self/root/src/spec/hermione/configs/geo';

import newCard from './newCard';
import cancelPopup from './cancelPopup';
import existingCard from './existingCard';
import {LOGIN, ADDRESS} from './constants';

module.exports = makeSuite('Оплата', {
    environment: 'testing',
    feature: 'Оплата',
    story: mergeSuites(
        {
            async beforeEach() {
                this.setPageObjects({
                    deliveryEditorCheckoutWizard: () => this.createPageObject(CheckoutWizard),
                    deliveryTypes: () => this.createPageObject(DeliveryTypeList),
                    editPopup: () => this.createPageObject(EditPopup),
                    paymentOptionsBlock: () => this.createPageObject(EditPaymentOption),
                    paymentOptionsEditableCard: () => this.createPageObject(EditableCard, {
                        parent: this.paymentOptionsBlock,
                    }),
                    paymentOptions: () => this.createPageObject(PaymentOptions),
                    orderButton: () => this.createPageObject(CheckoutOrderButton),
                    orderConfirmation: () => this.createPageObject(OrderConfirmation),
                });
            },
        },

        makeSuite('Незалогин', {
            story: mergeSuites({
                async beforeEach() {
                    await this.browser.yaOpenPage(PAGE_IDS_COMMON.ORDER_CONDITIONS);
                    await this.browser.yaScenario(this, prepareCheckoutPage, {
                        items: [lensesBausch],
                        region: region['Москва'],
                    });
                    await this.deliveryTypes.setDeliveryTypeDelivery();
                    await this.browser.yaScenario(this, goToConfirmationPage, {userFormData});
                },
                Оплата: prepareSuite(newCard, {
                    meta: {
                        id: 'marketfront-5207',
                        issue: 'MARKETFRONT-58142',
                    },
                }),
                Отмена: prepareSuite(cancelPopup, {
                    meta: {
                        issue: 'MARKETFRONT-60438',
                    },
                }),
            }),
        }),

        makeSuite('Залогин', {
            story: mergeSuites({
                async beforeEach() {
                    this.setPageObjects({
                        addressBlocks: () => this.createPageObject(GroupedParcels, {
                            parent: this.confirmationPage,
                        }),
                        addressBlock: () => this.createPageObject(GroupedParcel, {
                            parent: this.addressBlocks,
                        }),
                        addressEditableCard: () => this.createPageObject(EditableCard, {
                            parent: this.addressBlock,
                        }),
                        addressList: () => this.createPageObject(AddressList, {
                            parent: this.editPopup,
                        }),
                    });

                    await this.browser.yaProfile(LOGIN, PAGE_IDS_COMMON.ORDER_CONDITIONS);
                    await this.browser.yaScenario(this, prepareCheckoutPage, {
                        items: [lensesBausch],
                        region: region['Москва'],
                    });
                    await this.allure.runStep('Устанавливаем курьерскую доставку', async () => {
                        await this.addressEditableCard.changeButtonClick();
                        await this.deliveryTypes.setDeliveryTypeDelivery();
                        await this.addressList.clickAddressListItemByAddress(ADDRESS);
                        await this.editPopup.waitForChooseButtonEnabled();
                        return this.editPopup.chooseButtonClick();
                    });
                },
                Оплата: prepareSuite(newCard, {
                    meta: {
                        id: 'marketfront-4836',
                        issue: 'MARKETFRONT-60245',
                    },
                }),
                Отмена: prepareSuite(cancelPopup, {
                    meta: {
                        issue: 'MARKETFRONT-60438',
                    },
                }),
                Одноклик: prepareSuite(existingCard),
            }),
        })
    ),
});
