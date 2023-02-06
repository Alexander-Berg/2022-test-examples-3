'use strict';

import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на сброс значения в поле «Цель»
 *
 * @param {Object} params - параметры сьюта
 * @param {string} params.cashBackInitialValue - изначальное значение поля Баллы
 * @param {string} params.targetOpinionsInitialValue - изначальное значение поля Цель
 * @param {string} params.targetOpinionsResetValue -  значение для сброса в поле Цель
 * @param {PageObject.OpinionsPromotionListItem} item - товар
 * @param {PageObject.InputB2b} cashbackInput - инпут ввода баллов
 * @param {PageObject.InputB2b} targetInput - инпут ввода целей
 * @param {PageObject.ButtonLevitan} submitButton - кнопка сохранения баллов и целей в списке товаров
 * @param {PageObject.NotificationGroupLevitan} toasts - список нотификаций-тостов
 * @param {PageObject.NotificationLevitan} toast - первый тост из списка нотификаций
 */
export default makeSuite('Поле «Цель».', {
    feature: 'Отзывы за баллы',
    environment: 'kadavr',
    params: {
        user: 'Пользователь',
    },
    story: {
        'Значение в поле «Цель»': {
            сбрасывается: {
                корректно: makeCase({
                    async test() {
                        this.setPageObjects({
                            targetOpinionsHint() {
                                return this.createPageObject('TextLevitan', this.item, this.item.targetOpinionsHint);
                            },
                        });

                        const {cashBackInitialValue, targetOpinionsResetValue, targetOpinionsInitialValue} =
                            this.params;

                        await this.cashbackInput.value.should.eventually.be.equal(
                            cashBackInitialValue,
                            'Поле «Баллы» у товара заполнено',
                        );

                        await this.targetInput.value.should.eventually.be.equal(
                            targetOpinionsInitialValue,
                            'Поле «Цель» у товара заполнено',
                        );

                        await this.targetInput.setValue(targetOpinionsResetValue);

                        await this.submitButton
                            .isVisible()
                            .should.eventually.be.equal(true, 'Кнопка сохранения баллов и ставок отображается');

                        await this.cashbackInput.value.should.eventually.be.equal(
                            '100',
                            'Поле «Баллы» у товара осталось прежним',
                        );

                        await this.submitButton
                            .isVisible()
                            .should.eventually.be.equal(true, 'Кнопка сохранения баллов и ставок отображается');

                        await this.submitButton.click();

                        await this.browser.allure.runStep('Ожидаем показа группы всплывающих сообщений', () =>
                            this.toasts.waitForVisible(),
                        );

                        await this.browser.allure.runStep(
                            'Ожидаем показа всплывающего сообщения с подтверждением',
                            () => this.toast.waitForVisible(),
                        );
                        await this.toast
                            .getText()
                            .should.eventually.equal(
                                'Настроен сбор отзывов для 1 товара',
                                'Текст всплывающего сообщения верный',
                            );

                        await this.cashbackInput.value.should.eventually.be.equal(
                            cashBackInitialValue,
                            'Поле «Баллы» у товара осталось прежним',
                        );

                        await this.targetInput.placeholder.should.eventually.be.equal(
                            'Нет цели',
                            'Плейсхолдер у поля «Цель» корректный',
                        );

                        await this.targetInput.value.should.eventually.be.equal(
                            '0',
                            'Значение у поля «Цель» корректное',
                        );

                        await this.targetOpinionsHint
                            .getText()
                            .should.eventually.be.equal(
                                'Отзывы собираются бесконечно',
                                'Текст подсказки у поля «Цель» корректный',
                            );
                    },
                }),
            },
        },
    },
});
