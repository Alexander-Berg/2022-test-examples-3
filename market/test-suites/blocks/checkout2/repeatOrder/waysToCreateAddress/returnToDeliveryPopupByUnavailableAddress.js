import {makeCase} from 'ginny';

import {switchToSpecifiedDeliveryForm} from '@self/root/src/spec/hermione/scenarios/checkout';

import {ADDRESSES} from '../../constants';

const defaultAddress = ADDRESSES.MOSCOW_ADDRESS.address;
const address = 'Кызыл, Московская улица, д. 3';
const addressSuggest = 'Московская улица, 3';
const addressError = 'К сожалению, сюда доставки пока нет. Пожалуйста, выберите удобный адрес, куда доставка уже есть.';

export default makeCase({
    issue: 'MARKETFRONT-54555',
    id: 'marketfront-5032',

    async test() {
        await this.browser.allure.runStep(
            `В поле ввода указать адрес "${address}".`,
            async () => {
                await this.browser.yaScenario(this, switchToSpecifiedDeliveryForm);
                await this.street.setText(address);
                await this.street.selectSuggestion(addressSuggest);
                await this.street.waitForHideSuggestion(addressSuggest);

                await this.allure.runStep(
                    'Над пином указан адрес',
                    () => this.tooltipAddress.getText().should.eventually.to.be.equal(
                        address,
                        `Адрес над пином "${address}"`
                    )
                );

                await this.street.getText()
                    .should.eventually.to.be.equal(
                        address,
                        'В поле ввода должен отображаться выбранный адрес'
                    );
            }
        );

        await this.browser.allure.runStep(
            'Ожидаем доступности кнопки "Выбрать".',
            async () => {
                await this.deliveryEditorCheckoutWizard.waitForSubmitButton(10000);
                await this.deliveryEditorCheckoutWizard.isSubmitButtonDisabled()
                    .should.eventually.to.be.equal(
                        true,
                        'Кнопка "Выбрать" должна быть не активна.'
                    );
            }
        );

        await this.browser.allure.runStep(
            'Получаем текст ошибки под полем ввода.',
            async () => {
                await this.street.getErroredText()
                    .should.eventually.to.be.equal(
                        addressError,
                        'Должна корректно отображаться ошибка под полем ввода.'
                    );
            }
        );

        await this.browser.allure.runStep(
            'Нажать на кнопку "Назад".',
            async () => {
                await this.deliveryEditorCheckoutWizard.backButtonClick();

                await this.browser.allure.runStep(
                    'Происходит возврат к попапу "Изменить адрес".',
                    async () => {
                        await this.confirmationPage.waitForVisible();
                        await this.editPopup.waitForVisibleRoot();
                    }
                );

                await this.browser.allure.runStep(
                    'В попапе отображается предыдущий адрес.',
                    async () => {
                        await this.addressList.getActiveCardText()
                            .should.eventually.to.be.equal(
                                defaultAddress,
                                'Текст активного пресета должен соответствовать предыдущему адресу.'
                            );
                    }
                );
            }
        );

        await this.browser.allure.runStep(
            'Закрыть попап "Способ доставки".',
            async () => {
                await this.popupBase.clickOnCrossButton();
                await this.editPopup.waitForRootInvisible();
            }
        );

        await this.browser.allure.runStep(
            'Предыдущий адрес отображается в блоке информации о доставке.',
            async () => {
                await this.deliveryInfo.waitForVisible();
                await this.addressCard.getText()
                    .should.eventually.to.be.equal(
                        defaultAddress,
                        `Текст в поле адрес должен быть "${defaultAddress}".`
                    );
            }
        );
    },
});
