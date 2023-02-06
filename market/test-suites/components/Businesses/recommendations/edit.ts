'use strict';

import {makeSuite, makeCase} from 'ginny';

/**
 * Кейс на редактирование рекомендаций для магазинов
 *
 * @param {PageObject.BusinessesListItem} item - строка списка магазинов
 * @param {PageObject.ButtonLevitan} editButton - кнопка настройки списка магазинов
 * @param {PageObject.ActionBar} actionBar - островок с кнопками сохранения и отмены настройки списка
 * @param {PageObject.ButtonLevitan} saveButton - кнопка сохранения изменений
 * @param {PageObject.CheckboxLevitan} officialCheckbox - чекбокс официального магазина
 * @param {PageObject.CheckboxLevitan} recommendationCheckbox - чекбокс представителя бренда
 * @param {PageObject.NotificationGroupLevitan} toasts - список тостов-нотификаций
 * @param {PageObject.NotificationLevitan} firstToast - первый тост-нотификация
 * @param {Object} params
 * @param {boolean} params.initialOfficialValue - начальное значение признака официального магазина
 * @param {boolean} params.initialRecommendationValue - начальное значение признака представителя бренда
 * @param {boolean} params.expectedOfficialValue - ожидаемое значение признака официального магазина
 * @param {boolean} params.expectedRecommendationValue - ожидаемое значение признака представителя бренда
 * @param {boolean} params.toggleOfficial - нужно ли кликать в чекбокс официального магазина
 * @param {boolean} params.toggleRecommendation - нужно ли кликать в чекбокс представителя бренда
 *
 */
export default makeSuite('Редактирование рекомендаций.', {
    issue: 'VNDFRONT-4301',
    params: {
        user: 'Пользователь',
    },
    story: {
        'При редактировании рекомендаций и сохранении изменений': {
            'изменения сохраняются корректно': makeCase({
                async test() {
                    const {
                        initialOfficialValue,
                        initialRecommendationValue,
                        expectedOfficialValue,
                        expectedRecommendationValue,
                        toggleOfficial,
                        toggleRecommendation,
                    } = this.params;

                    await this.item.officialBadge
                        .isVisible()
                        .should.eventually.be.equal(
                            initialOfficialValue,
                            'Иконка официального магазина отображается корректно',
                        );

                    await this.item.recommendationBadge
                        .isVisible()
                        .should.eventually.be.equal(
                            initialRecommendationValue,
                            'Иконка представителя бренда отображается корректно',
                        );

                    await this.editButton.click();

                    // Считаем, что список в режиме редактирования, когда скроется кнопка редактирования списка
                    await this.browser.allure.runStep('Дожидаемся режима редактирования списка', () =>
                        this.browser.waitUntil(
                            async () => {
                                const existing = await this.editButton.isExisting();

                                return existing === false;
                            },
                            this.browser.options.waitforTimeout,
                            'Кнопка "Отметить магазины" не скрылась',
                        ),
                    );

                    await this.actionBar
                        .isVisible()
                        .should.eventually.be.equal(true, 'Панель сохранения и отмены изменений отобразилась');

                    await this.officialCheckbox
                        .isExisting()
                        .should.eventually.be.equal(true, 'Чекбокс официального магазина отображается');

                    if (initialOfficialValue) {
                        await this.officialCheckbox.checked
                            .isExisting()
                            .should.eventually.be.equal(true, 'Чекбокс официального магазина выбран');
                    } else {
                        await this.officialCheckbox.unchecked
                            .isExisting()
                            .should.eventually.be.equal(true, 'Чекбокс официального магазина не выбран');
                    }

                    await this.recommendationCheckbox
                        .isExisting()
                        .should.eventually.be.equal(true, 'Чекбокс представителя бренда отображается');

                    if (initialRecommendationValue) {
                        await this.recommendationCheckbox.checked
                            .isExisting()
                            .should.eventually.be.equal(true, 'Чекбокс представителя бренда выбран');
                    } else {
                        await this.recommendationCheckbox.unchecked
                            .isExisting()
                            .should.eventually.be.equal(true, 'Чекбокс представителя бренда не выбран');
                    }

                    await this.browser.allure.runStep('Редактируем рекомендации', async () => {
                        if (toggleOfficial) {
                            await this.browser.allure.runStep('Кликаем по чекбоксу официального магазина', () =>
                                this.officialCheckbox.click(),
                            );
                        }

                        if (toggleRecommendation) {
                            await this.browser.allure.runStep('Кликаем по чекбоксу представителя бренда', () =>
                                this.recommendationCheckbox.click(),
                            );
                        }
                    });

                    await this.saveButton.click();

                    await this.browser.allure.runStep('Ожидаем показа всплывающего сообщения', () =>
                        this.firstToast.waitForVisible(),
                    );

                    await this.browser.allure.runStep('Проверяем текст всплывающего сообщения', () =>
                        this.firstToast
                            .getText()
                            .should.eventually.equal(
                                'Изменения сохранены. Значки появятся у выбранных продавцов на Маркете ' +
                                    'в течение 4 часов.',
                                'Текст всплывающего сообщения корректный',
                            ),
                    );

                    await this.browser.allure.runStep('Дожидаемся отображения кнопки редактирования списка', () =>
                        this.editButton.waitForVisible(),
                    );

                    await this.browser.allure.runStep('Проверяем, что рекомендации сохранились', async () => {
                        await this.item.officialBadge
                            .isVisible()
                            .should.eventually.be.equal(
                                expectedOfficialValue,
                                'Иконка официального магазина отображается корректно',
                            );

                        await this.item.recommendationBadge
                            .isVisible()
                            .should.eventually.be.equal(
                                expectedRecommendationValue,
                                'Иконка представителя бренда отображается корректно',
                            );
                    });
                },
            }),
        },
    },
});
