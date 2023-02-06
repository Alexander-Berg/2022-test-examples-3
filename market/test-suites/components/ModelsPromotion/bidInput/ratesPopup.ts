'use strict';

import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на всплывающую подсказку со ставками
 * @param {PageObject.TextFieldLevitan} textField - поле ввода ставки
 * @param {PageObject.ModelBidRecommendationTooltip} modelBidRecommendationTooltip - тултип с рекомендованными ставками
 * @param {PageObject.LinkLevitan} link - ссылка установки ставки
 * @param {PageObject.RatesControlBar} bar - ссылка установки ставки
 * @param {Object} params
 * @param {string} params.bid – рекомендованная ставка
 */
export default makeSuite('Всплывающая подсказка.', {
    issue: 'VNDFRONT-2249',
    environment: 'kadavr',
    feature: 'Прогнозатор',
    params: {
        user: 'Пользователь',
    },
    story: {
        'При клике по ставке': {
            'поле ввода предзаполняется': makeCase({
                async test() {
                    const {expectedBid} = this.params;

                    await this.textField.setFocus();

                    await this.browser.allure.runStep('Ожидаем появления всплывающего окна со ставками', () =>
                        this.modelBidRecommendationTooltip.waitForVisible(),
                    );

                    await this.browser.allure.runStep(`Кликаем на ставку "${expectedBid} у.е."`, () =>
                        this.link.root.click(),
                    );

                    await this.browser.allure.runStep('Проверяем изменение значения поля ввода', () =>
                        this.textField.value.should.eventually.be.equal(
                            expectedBid,
                            `Значение изменилось на "${expectedBid}"`,
                        ),
                    );

                    await this.browser.allure.runStep('Ожидаем появления сайдбара управления ставками', () =>
                        this.bar.waitForVisible(),
                    );
                },
            }),
        },
    },
});
