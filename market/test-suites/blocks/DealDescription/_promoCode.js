import {makeCase, makeSuite} from 'ginny';

/**
 * Тесты на блок DealDescription (промокод)
 * @property {PageObject.DealDescription} dealDescription
 */
export default makeSuite('Блок описания отдельной акции (промокод).', {
    story: {
        'По клику на кнопку "копировать"': {
            'копирует промокод': makeCase({
                issue: 'MARKETFRONT-6639',
                id: 'm-touch-3128',
                async test() {
                    await this.browser.allure.runStep(
                        'Проверяем видимость кнопки копирования промокода',
                        () => {
                            const isVisible = this.dealDescription.copyButton.isVisible();

                            return this.expect(isVisible).to.equal(true);
                        }
                    );

                    await this.dealDescription.copyPromoCode();

                    // Небольшая пауза для смены текста кнопки
                    // eslint-disable-next-line market/ginny/no-pause
                    await this.browser.pause(100);

                    return this.browser.allure.runStep(
                        'Проверяем текст кнопки копирования промокода',
                        () => {
                            const copyButtonText = this.dealDescription.copyButton.getText();

                            return this.expect(copyButtonText).to.equal('Промокод скопирован');
                        }
                    );
                },
            }),
        },
    },
});
