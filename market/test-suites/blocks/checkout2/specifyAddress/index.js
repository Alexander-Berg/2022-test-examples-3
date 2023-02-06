import {makeSuite, makeCase} from 'ginny';

import {
    prepareCheckouterPageWithCartsForRepeatOrder,
    prepareCheckoutPage,
    fillRecipientForm,
} from '@self/root/src/spec/hermione/scenarios/checkout';
import {prepareMultiCartState} from '@self/root/src/spec/hermione/scenarios/cartResource';

import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';
import * as kettle from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';

import PinMap from '@self/root/src/components/VectorPinMap/__pageObject';
import CourierSuggest
    from '@self/root/src/widgets/content/checkout/common/CheckoutDeliveryEditor/components/CourierSuggest/__pageObject';
import GeoSuggest from '@self/root/src/components/GeoSuggest/__pageObject';
import Tooltip from '@self/root/src/widgets/content/checkout/common/CheckoutPin/components/Tooltip/__pageObject';
import {Button} from '@self/root/src/uikit/components/Button/__pageObject';
import Text from '@self/root/src/uikit/components/Text/__pageObject';
import AddressForm from '@self/root/src/components/AddressForm/__pageObject';
import {TextFieldWithValidationErrorText} from '@self/root/src/components/TextFieldWithValidation/__pageObject';
import RecipientForm from '@self/root/src/components/RecipientForm/__pageObject';
import GroupedParcel
    from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/GroupedParcel/__pageObject';
import EditableCard from '@self/root/src/components/EditableCard/__pageObject/index.desktop';
import DeliveryInfo from '@self/root/src/components/Checkout/DeliveryInfo/__pageObject';
import AddressCard from '@self/root/src/components/AddressCard/__pageObject';
import DeliveryActionButton from
    '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/DeliveryActionButton/__pageObject';
import EditPopup from
    '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/EditPopup/__pageObject';
import Clickable from '@self/root/src/components/Clickable/__pageObject';
import {region} from '@self/root/src/spec/hermione/configs/geo';

import {CONTACTS} from '../constants';

export default makeSuite('Уточнение адреса.', {
    environment: 'kadavr',
    feature: 'Уточнение адреса',
    params: {
        region: 'Регион',
        isAuth: 'Авторизован ли пользователь',
    },
    defaultParams: {
        region: region['Москва'],
        isAuth: true,
    },
    story: {
        async beforeEach() {
            this.setPageObjects({
                courierSuggest: () => this.createPageObject(CourierSuggest, {
                    parent: this.deliveryEditorCheckoutWizard,
                }),
                courierSuggestInput: () => this.createPageObject(GeoSuggest, {
                    parent: this.courierSuggest,
                }),
                pinMap: () => this.createPageObject(PinMap, {}),
                tooltip: () => this.createPageObject(Tooltip, {
                    parent: this.pinMap,
                }),
                tooltipDescription: () => this.createPageObject(Text, {
                    parent: this.tooltip,
                    root: `${Text.root}[data-auto="description"]`,
                }),
                specifyButton: () => this.createPageObject(Button, {
                    parent: this.tooltip,
                }),
                addressForm: () => this.createPageObject(AddressForm, {
                    parent: this.deliveryEditorCheckoutWizard,
                }),
                regionSuggest: () => this.createPageObject(GeoSuggest, {
                    parent: this.addressForm,
                }),
                houseErrorText: () => this.createPageObject(Text, {
                    parent: this.addressForm,
                    root: `[data-auto="house"] ${TextFieldWithValidationErrorText.root} ${Text.root}`,
                }),
                recipientForm: () => this.createPageObject(RecipientForm),
                addressBlock: () => this.createPageObject(GroupedParcel, {
                    parent: this.confirmationPage,
                }),
                addressEditableCard: () => this.createPageObject(EditableCard, {
                    parent: this.addressBlock,
                }),
                deliveryInfo: () => this.createPageObject(DeliveryInfo, {
                    parent: this.addressEditableCard,
                }),
                addressCard: () => this.createPageObject(AddressCard, {
                    parent: this.deliveryInfo,
                }),
                deliveryActionButton: () => this.createPageObject(DeliveryActionButton),
                editPopup: () => this.createPageObject(EditPopup),
            });


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

            if (this.params.isAuthWithPlugin) {
                await this.browser.setState(`persAddress.contact.${CONTACTS.DEFAULT_CONTACT.id}`, CONTACTS.DEFAULT_CONTACT);
                await this.browser.yaScenario(
                    this,
                    prepareCheckouterPageWithCartsForRepeatOrder,
                    {
                        carts,
                        options: {
                            region: this.params.region,
                            checkout2: true,
                        },
                    }
                );
                await this.deliveryActionButton.waitForVisible();
                await this.deliveryActionButton.click();

                await this.editPopup.waitForVisibleRoot();
                await this.editPopup.deliveryChooseButtonClick();

                await this.deliveryEditorCheckoutWizard.waitForVisible();
            } else {
                await this.browser.yaScenario(
                    this,
                    prepareCheckoutPage,
                    {
                        items: testState.checkoutItems,
                        reportSkus: testState.reportSkus,
                        checkout2: true,
                    }
                );
            }

            await this.courierSuggestInput.setTextAndSelect('Писцовая улица');
        },

        'Отображение кнопки Привезти в эту точку': makeCase({
            id: 'marketfront-5357',
            issue: 'MARKETFRONT-73797',
            async test() {
                await this.allure.runStep(
                    'Алерт об уточнении адреса отображается.', () =>
                        this.courierSuggestInput.getErroredText()
                            .should.eventually.to.be.equal(
                                'Не удалось определить точный адрес',
                                'Должно отображаться сообщение "Не удалось определить точный адрес"'
                            )
                );
                await this.allure.runStep(
                    'Кнопка "Привезти в эту точку" отображается',
                    async () => {
                        await this.specifyButton.isVisible().should.eventually.to.be.equal(
                            true,
                            'Отображается кнопка уточнения адреса'
                        );
                        return this.specifyButton.getButtonText().should.eventually.to.be.equal(
                            'Привезти в эту точку',
                            'Текст на кнопке "Привезти в эту точку"'
                        );
                    }
                );
            },
        }),
        'После нажатия на кнопку "Привезти в эту точку"': {
            async beforeEach() {
                this.setPageObjects({
                    closeSpecification: () => this.createPageObject(Clickable, {
                        parent: this.deliveryEditorCheckoutWizard,
                        root: '[data-auto="denyAddressSpecification"]',
                    }),
                });

                await this.specifyButton.click();
                await this.allure.runStep(
                    'Кнопка "Привезти в эту точку" пропадает',
                    async () => {
                        await this.specifyButton.isVisible()
                            .catch(() => false)
                            .should.eventually.to.be.equal(
                                false,
                                'Кнопка уточнения адреса не отображается'
                            );
                    }
                );
                await this.allure.runStep(
                    'Над пином надпись "Доставим в эту точку"',
                    () => this.tooltipDescription.getText().should.eventually.to.be.equal(
                        'Доставим в эту точку',
                        'Надпись над пином "Доставим в эту точку"'
                    )
                );
                await this.allure.runStep(
                    'Слева открылась форма уточнения адреса',
                    async () => {
                        await this.regionSuggest.getText().should.eventually.to.be.equal(
                            'Москва',
                            'В поле город указана Москва'
                        );
                        await this.addressForm.streetFormField.getValue().should.eventually.to.be.equal(
                            'Писцовая улица',
                            'В поле улица указана "Писцовая улица"'
                        );
                        await this.houseErrorText.getText().should.eventually.to.be.equal(
                            'Не удалось определить точный адрес',
                            'Поле "Дом, корпус, строение" выделено красным, снизу надпись "Не удалось определить точный адрес"'
                        );
                        await this.addressForm.apartamentFormField.getValue().should.eventually.to.be.equal(
                            '',
                            'Поле квартиры пустое'
                        );
                        await this.addressForm.floorFormField.getValue().should.eventually.to.be.equal(
                            '',
                            'Поле этажа пустое'
                        );
                        await this.addressForm.entranceFormField.getValue().should.eventually.to.be.equal(
                            '',
                            'Поле подъезда пустое'
                        );
                        await this.addressForm.intercomFormField.getValue().should.eventually.to.be.equal(
                            '',
                            'Поле домофона пустое'
                        );
                        await this.addressForm.commentFormField.getValue().should.eventually.to.be.equal(
                            '',
                            'Поле комментария пустое'
                        );
                    }
                );
                await this.deliveryEditorCheckoutWizard.isSubmitButtonDisabled()
                    .should.eventually.to.be.equal(
                        true,
                        'Кнопка "Выбрать" должна быть неактивна.'
                    );
            },
            'Можно успешно дополнить адрес': makeCase({
                id: 'marketfront-5358',
                issue: 'MARKETFRONT-73797',
                async test() {
                    await this.allure.runStep(
                        'Вводим номер дома',
                        () => this.addressForm.setHouseField(1)
                    );
                    await this.deliveryEditorCheckoutWizard.waitForEnabledSubmitButton();
                    await this.deliveryEditorCheckoutWizard.isSubmitButtonDisabled()
                        .should.eventually.to.be.equal(
                            false,
                            'Кнопка "Выбрать" должна быть активна.'
                        );
                    await this.allure.runStep(
                        'Вводим остальные поля адреса',
                        async () => {
                            await this.addressForm.setApartamentField(1);
                            await this.addressForm.setEntranceField(1);
                            await this.addressForm.setIntercomField(1);
                            await this.addressForm.setCommentField('тест');
                        }
                    );
                    await this.deliveryEditorCheckoutWizard.isSubmitButtonDisabled()
                        .should.eventually.to.be.equal(
                            false,
                            'Кнопка "Выбрать" должна быть активна.'
                        );
                    await this.deliveryEditorCheckoutWizard.submitButtonClick();

                    if (!this.params.isAuthWithPlugin) {
                        await this.recipientWizard.waitForVisible();
                        await this.browser.yaScenario(this, fillRecipientForm, {
                            formName: 'user-prepaid',
                            recipientForm: this.recipientForm,
                        });
                        await this.recipientWizard.submitButtonClick();
                    }

                    await this.confirmationPage.waitForVisible();
                    await this.allure.runStep(
                        'На главном экране отображается адрес "Москва, Писцовая улица, д. 1, 1"', async () => {
                            const newAddress = 'Москва, Писцовая улица, д. 1, 1\n1 подъезд, домофон 1, "тест"';
                            await this.addressCard.getText()
                                .should.eventually.to.be.equal(
                                    newAddress,
                                    `Отображается, который был выбран "${newAddress}"`
                                );
                        }
                    );
                },
            }),
            'Можно выйти из режима уточнения': makeCase({
                id: 'marketfront-5678',
                issue: 'MARKETFRONT-73797',
                async test() {
                    await this.closeSpecification.click();
                    await this.allure.runStep(
                        'Кнопка "Привезти в эту точку" отображается',
                        async () => {
                            await this.specifyButton.isVisible().should.eventually.to.be.equal(
                                true,
                                'Отображается кнопка уточнения адреса'
                            );
                            return this.specifyButton.getButtonText().should.eventually.to.be.equal(
                                'Привезти в эту точку',
                                'Текст на кнопке "Привезти в эту точку"'
                            );
                        }
                    );
                    await this.allure.runStep(
                        'Отображается стандартная форма "Как доставить заказ?"',
                        async () => {
                            await this.addressForm.isVisible().catch(() => false)
                                .should.eventually.to.be.equal(
                                    false,
                                    'Форма уточнения адреса не отображается'
                                );
                            return this.courierSuggest.isVisible()
                                .should.eventually.to.be.equal(
                                    true,
                                    'Саджест курьерки отображается'
                                );
                        }
                    );
                },
            }),
            'Нажатие на кнопку "Назад" в форме уточнения адреса': makeCase({
                id: 'marketfront-5679',
                issue: 'MARKETFRONT-73797',
                async test() {
                    const {isAuthWithPlugin} = this.params;
                    await this.deliveryEditorCheckoutWizard.backButtonClick();
                    if (isAuthWithPlugin) {
                        await this.allure.runStep(
                            'Ожидаем попап на странице подтверждения чекаута',
                            () => this.editPopup.waitForVisibleRoot()
                        );
                    } else {
                        await this.allure.runStep(
                            'Ожидаем перехода на страницу корзины',
                            () => this.browser.getUrl()
                                .should.eventually.to.be.link({
                                    pathname: 'my/cart',
                                }, {
                                    mode: 'match',
                                    skipProtocol: true,
                                    skipHostname: true,
                                })
                        );
                    }
                },
            }),
        },
    },
});
