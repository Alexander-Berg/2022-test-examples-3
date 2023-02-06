import {makeCase} from 'ginny';

import {switchToSpecifiedDeliveryForm} from '@self/root/src/spec/hermione/scenarios/checkout';

const samaraAddress = 'Самара, Ленинская улица, д. 147';
const addressSuggest = 'Ленинская улица, 147';

export default makeCase({
    issue: 'MARKETFRONT-54555',
    id: 'marketfront-5032',

    async test() {
        await this.browser.allure.runStep(
            `В поле ввода указать адрес "${samaraAddress}".`,
            async () => {
                await this.browser.yaScenario(this, switchToSpecifiedDeliveryForm);
                await this.street.setText(samaraAddress);
                await this.street.selectSuggestion(addressSuggest);
                await this.street.waitForHideSuggestion(addressSuggest);

                await this.allure.runStep(
                    'Над пином указан адрес',
                    () => this.tooltipAddress.getText().should.eventually.to.be.equal(
                        samaraAddress,
                        `Адрес над пином "${samaraAddress}"`
                    )
                );

                await this.street.getText()
                    .should.eventually.to.be.equal(
                        samaraAddress,
                        'В поле ввода должен отображаться выбранный адрес'
                    );
            }
        );

        await this.browser.allure.runStep(
            'Нажать кнопку "Выбрать".',
            async () => {
                await this.browser.allure.runStep(
                    'Ожидаем доступности кнопки "Выбрать".',
                    async () => {
                        await this.deliveryEditorCheckoutWizard.waitForEnabledSubmitButton(10000);
                        await this.deliveryEditorCheckoutWizard.isSubmitButtonDisabled()
                            .should.eventually.to.be.equal(
                                false,
                                'Кнопка "Выбрать" должна быть активна.'
                            );
                    }
                );

                await this.deliveryEditorCheckoutWizard.submitButtonClick();
            }
        );

        await this.browser.allure.runStep(
            'Ожидаем появления главной страницы чекаута.',
            async () => {
                await this.confirmationPage.waitForVisible();
            }
        );

        await this.browser.allure.runStep(
            'Введенный адрес отображается в блоке информации о доставке.',
            async () => {
                await this.deliveryInfo.waitForVisible();
                await this.addressCard.getText()
                    .should.eventually.to.be.equal(
                        samaraAddress,
                        `Текст в поле адрес должен быть "${samaraAddress}".`
                    );
            }
        );
    },
});
