import {makeCase, makeSuite} from 'ginny';

import DeliveryTypeList from '@self/root/src/components/DeliveryTypes/__pageObject';
import AddressForm from '@self/root/src/components/AddressForm/__pageObject';
import {switchToSpecifiedDeliveryForm} from '@self/root/src/spec/hermione/scenarios/checkout';

export default makeSuite('Шаг 1.', {
    id: 'marketfront-4426',
    issue: 'MARKETFRONT-36074',
    feature: 'Шаг 1.',
    environment: 'kadavr',
    story: {
        async beforeEach() {
            this.setPageObjects({
                deliveryTypes: () => this.createPageObject(DeliveryTypeList, {
                    parent: this.deliveryEditorCheckoutWizard,
                }),
                addressForm: () => this.createPageObject(AddressForm, {
                    parent: this.deliveryEditorCheckoutWizard,
                }),
            });

            await this.deliveryTypes.waitForVisible();
        },
        'Открыть страницу чекаута.': {
            'Отображается экран "Как доставить заказ".': makeCase({
                async test() {
                    const titleText = 'Как доставить заказ?';

                    await this.deliveryEditorCheckoutWizard.getTitleText()
                        .should.eventually.to.be.equal(
                            titleText,
                            `Текст заголовка блока с оформлением заказа должен быть "${titleText}".`
                        );

                    await this.browser.allure.runStep(
                        'По дефолту отображается способ доставки "Курьером" с формой ввода адреса.',
                        async () => {
                            await this.deliveryTypes.isCheckedDeliveryTypeDelivery()
                                .should.eventually.to.be.equal(true, 'Должна отображаться доставка "Курьером."');
                            await this.courierSuggest.waitForVisible();
                        }
                    );

                    await this.browser.allure.runStep(
                        'Кнопка "Продолжить" заблокирована.',
                        async () => {
                            await this.deliveryEditorCheckoutWizard.isSubmitButtonDisabled()
                                .should.eventually.to.be.equal(true, 'Кнопка "Продолжить" должна быть заблокирована.');
                        }
                    );
                },
            }),
            'Указать в форме адреса произвольные данные.': makeCase({
                async test() {
                    await this.browser.yaScenario(this, switchToSpecifiedDeliveryForm);

                    await this.deliveryEditorCheckoutWizard.waitForEnabledSubmitButton(3000);

                    await this.browser.allure.runStep(
                        'Кнопка "Продолжить" активна.',
                        async () => {
                            await this.deliveryEditorCheckoutWizard.isSubmitButtonDisabled()
                                .should.eventually.to.be.equal(false, 'Кнопка "Продолжить" должна быть активна.');
                        }
                    );

                    await this.browser.allure.runStep(
                        'Нажать кнопку "Продолжить" для перехода к следующему шагу.',
                        async () => {
                            await this.deliveryEditorCheckoutWizard.submitButtonClick();
                            await this.deliveryEditorCheckoutWizard.waitForVisible();
                        }
                    );
                },
            }),
        },
    },
});
