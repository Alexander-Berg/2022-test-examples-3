'use strict';

import {mergeSuites, importSuite, PageObject, makeSuite, makeCase} from 'ginny';

const ButtonLevitan = PageObject.get('ButtonLevitan');

/**
 * Кейсы на редактирование рекомендаций для бизнесов
 *
 * @param {PageObject.BusinessesList} list - таблица с магазинами
 * @param {PageObject.BusinessesListItem} item - строка списка магазинов
 */
export default makeSuite('Редактирование рекомендаций.', {
    issue: 'VNDFRONT-4301',
    params: {
        user: 'Пользователь',
    },
    story: mergeSuites(
        {
            async beforeEach() {
                this.setPageObjects({
                    actionBar() {
                        return this.createPageObject('ActionBar');
                    },
                    saveButton() {
                        return this.createPageObject(
                            'ButtonLevitan',
                            this.actionBar,
                            `${ButtonLevitan.root}:nth-child(1)`,
                        );
                    },
                    cancelButton() {
                        return this.createPageObject(
                            'ButtonLevitan',
                            this.actionBar,
                            `${ButtonLevitan.root}:nth-child(2)`,
                        );
                    },
                    editButton() {
                        return this.createPageObject('ButtonLevitan', this.list, this.list.editButton);
                    },
                    officialCheckbox() {
                        return this.createPageObject('CheckboxLevitan', this.item, this.item.officialBadge);
                    },
                    recommendationCheckbox() {
                        return this.createPageObject('CheckboxLevitan', this.item, this.item.recommendationBadge);
                    },
                    toasts() {
                        return this.createPageObject('NotificationGroupLevitan');
                    },
                    firstToast() {
                        return this.createPageObject('NotificationLevitan', this.toasts, this.toasts.getItemByIndex(0));
                    },
                });

                await this.allure.runStep('Ожидаем появления списка бизнесов', () => this.list.waitForVisible());

                await this.allure.runStep('Ожидаем загрузки списка бизнесов', () => this.list.waitForLoading());

                await this.allure.runStep('Ожидаем появления строки таблицы', () => this.item.waitForVisible());
            },
        },
        importSuite('Businesses/recommendations/edit', {
            suiteName: 'Проставление рекомендаций.',
            meta: {
                id: 'vendor_auto-1450',
                environment: 'kadavr',
            },
            params: {
                initialOfficialValue: false,
                initialRecommendationValue: false,
                expectedOfficialValue: true,
                expectedRecommendationValue: true,
                toggleOfficial: true,
                toggleRecommendation: true,
            },
        }),
        importSuite('Businesses/recommendations/edit', {
            suiteName: 'Снятие рекомендации.',
            meta: {
                id: 'vendor_auto-1451',
                environment: 'kadavr',
            },
            params: {
                initialOfficialValue: true,
                initialRecommendationValue: true,
                expectedOfficialValue: false,
                expectedRecommendationValue: false,
                toggleOfficial: true,
                toggleRecommendation: true,
            },
        }),
        importSuite('Businesses/recommendations/edit', {
            suiteName: 'Снятие всех рекомендаций.',
            meta: {
                id: 'vendor_auto-1453',
                environment: 'kadavr',
            },
            params: {
                initialOfficialValue: true,
                initialRecommendationValue: true,
                expectedOfficialValue: false,
                expectedRecommendationValue: false,
                toggleOfficial: true,
                toggleRecommendation: true,
            },
        }),
        {
            'При редактировании рекомендаций и отмене изменений': {
                'изменения сбрасываются': makeCase({
                    id: 'vendor_auto-1449',
                    environment: 'kadavr',
                    async test() {
                        await this.item.officialBadge
                            .isVisible()
                            .should.eventually.be.equal(true, 'Иконка официального магазина отображается корректно');

                        await this.item.recommendationBadge
                            .isVisible()
                            .should.eventually.be.equal(true, 'Иконка представителя бренда отображается корректно');

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

                        await this.officialCheckbox.checked
                            .isExisting()
                            .should.eventually.be.equal(true, 'Чекбокс официального магазина выбран');

                        await this.recommendationCheckbox
                            .isExisting()
                            .should.eventually.be.equal(true, 'Чекбокс представителя бренда отображается');

                        await this.recommendationCheckbox.checked
                            .isExisting()
                            .should.eventually.be.equal(true, 'Чекбокс представителя бренда выбран');

                        await this.browser.allure.runStep('Кликаем по чекбоксу официального магазина', () =>
                            this.officialCheckbox.click(),
                        );

                        await this.officialCheckbox.unchecked
                            .isExisting()
                            .should.eventually.be.equal(true, 'Чекбокс официального магазина не выбран');

                        await this.browser.allure.runStep('Кликаем по чекбоксу представителя бренда', () =>
                            this.recommendationCheckbox.click(),
                        );

                        await this.recommendationCheckbox.unchecked
                            .isExisting()
                            .should.eventually.be.equal(true, 'Чекбокс представителя бренда не выбран');

                        await this.cancelButton.click();

                        await this.browser.allure.runStep('Дожидаемся отображения кнопки редактирования списка', () =>
                            this.editButton.waitForVisible(),
                        );

                        await this.browser.allure.runStep('Проверяем, что рекомендации сбросились', async () => {
                            await this.item.officialBadge
                                .isVisible()
                                .should.eventually.be.equal(
                                    true,
                                    'Иконка официального магазина отображается корректно',
                                );

                            await this.item.recommendationBadge
                                .isVisible()
                                .should.eventually.be.equal(true, 'Иконка представителя бренда отображается корректно');
                        });
                    },
                }),
            },
        },
    ),
});
