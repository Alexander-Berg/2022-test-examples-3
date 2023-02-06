import {makeCase, makeSuite, mergeSuites, prepareSuite} from 'ginny';

/**
 * @ifLose заменить на старые импорты из .../components/DeliveryIntervals/__pageObject
 * @expFlag all_checkout_new_intervals [обратный эксперимент]
 * @ticket MARKETFRONT-58113
 * @start
 */
import {
    TimeSelect,
    // eslint-disable-next-line max-len
} from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/TimeIntervalSelector/__pageObject';
import DeliveryActionButton
    from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/DeliveryActionButton/__pageObject';
import {region} from '@self/root/src/spec/hermione/configs/geo';

import EditPopup
    from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/EditPopup/__pageObject';
import DeliveryTypeList from '@self/root/src/components/DeliveryTypes/__pageObject';
import AddressList
    from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/EditPopup/components/AddressList/__pageObject';
import CheckoutWizard from '@self/root/src/widgets/content/checkout/layout/components/wizard/__pageObject';
import ConfirmationPage
    from '@self/root/src/widgets/content/checkout/layout/CheckoutLayoutConfirmationPage/view/__pageObject';
import DeliveryInfo from '@self/root/src/components/Checkout/DeliveryInfo/__pageObject';

import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';
import {
    fillAllDeliveryAddressFields,
    prepareCheckouterPageWithCartsForRepeatOrder,
    switchToSpecifiedDeliveryForm,
} from '@self/root/src/spec/hermione/scenarios/checkout';
import * as kettle from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';
import {deliveryOnDemandMock, deliveryDeliveryMock} from '@self/root/src/spec/hermione/kadavr-mock/checkouter/delivery';

export default makeSuite('Доставка по клику', {
    environment: 'kadavr',
    issue: 'MARKETFRONT-53509',
    feature: 'Доставка по клику',
    params: {
        region: 'Регион',
        isAuthWithPlugin: 'Авторизован ли пользователь',
    },
    defaultParams: {
        region: region['Москва'],
        isAuthWithPlugin: true,
    },
    story: mergeSuites(
        {
            async beforeEach() {
                this.setPageObjects({
                    deliveryEditorCheckoutWizard: () => this.createPageObject(CheckoutWizard),
                    timeSelect: () => this.createPageObject(TimeSelect),
                    deliveryActionButton: () => this.createPageObject(DeliveryActionButton),
                    editPopup: () => this.createPageObject(EditPopup),
                    confirmationPage: () => this.createPageObject(ConfirmationPage),
                    popupDeliveryTypeList: () => this.createPageObject(
                        DeliveryTypeList,
                        {parent: this.editPopup}
                    ),
                    deliveryInfo: () => this.createPageObject(DeliveryInfo),
                    addressList: () => this.createPageObject(AddressList, {
                        parent: this.editPopup,
                    }),
                });
            },
        },

        prepareSuite(makeSuite('Опции доставки "По клику в удобное время"', {
            id: 'bluemarket-4087',
            story: {
                async beforeEach() {
                    await this.browser.yaScenario(
                        this,
                        prepareCheckouterPageWithCartsForRepeatOrder,
                        {
                            carts: [
                                buildCheckouterBucket({
                                    items: [{
                                        skuMock: kettle.skuMock,
                                        offerMock: kettle.offerMock,
                                        count: 1,
                                    }],
                                    deliveryOptions: [deliveryDeliveryMock, deliveryOnDemandMock],
                                }),
                            ],
                            orders: [{
                                id: 123,
                                paymentType: 'PREPAID',
                            }],
                            options: {
                                region: this.params.region,
                                checkout2: true,
                            },
                        }
                    );
                },
                'При наличии опции ондеманда она выбирается автоматически': makeCase({
                    async test() {
                        await this.deliveryActionButton.isButtonVisible()
                            .should.eventually.to.be.equal(
                                true,
                                'На карточке блока доставки должна отображатся кнопка "Выбрать адрес доставки".'
                            );

                        await this.deliveryActionButton.click();

                        await this.editPopup.waitForVisibleRoot();
                        await this.editPopup.deliveryChooseButtonClick();

                        await this.deliveryEditorCheckoutWizard.waitForVisible();

                        await this.browser.yaScenario(this, switchToSpecifiedDeliveryForm);

                        await this.browser.yaScenario(
                            this,
                            fillAllDeliveryAddressFields,
                            {
                                address: {
                                    suggest: 'Усачёва улица, 62',
                                    apartament: '12',
                                    floor: '15',
                                    entrance: '1',
                                    intercom: '12test',
                                    comment: 'Тестирование',
                                    fullDeliveryInfo: ['Москва, Усачёва улица, д. 62, 12\n'] +
                                    ['1 подъезд, 15 этаж, домофон 12test, "Тестирование"'],
                                },
                            }
                        );

                        await this.allure.runStep(
                            'Нажать кнопку "Выбрать".', async () => {
                                await this.deliveryEditorCheckoutWizard.waitForEnabledSubmitButton();
                                await this.deliveryEditorCheckoutWizard.isSubmitButtonDisabled()
                                    .should.eventually.to.be.equal(false, 'Кнопка "Выбрать" должна быть активна.');

                                return this.deliveryEditorCheckoutWizard.submitButtonClick();
                            }
                        );

                        await this.confirmationPage.waitForVisible();
                        await this.deliveryInfo.waitForVisible();

                        await this.timeSelect.isOnDemand()
                            .should.eventually.to.be.equal('true', 'Должна быть выбрана опция "По клику"');
                        await this.timeSelect.getText()
                            .should.eventually.to.be.equal(null, 'Должен быть выбран любой временной интервал');
                    },
                }),
            },
        }))
    ),
});
/**
 * @expFlag all_checkout_new_intervals [обратный эксперимент]
 * @ticket MARKETFRONT-58113
 * @end
 */
