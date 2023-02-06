'use strict';

import {PageObject, makeSuite, makeCase} from 'ginny';

const ModelsPromotionStatisticsFiltersGroup = PageObject.get('ModelsPromotionStatisticsFiltersGroup');

/**
 * Общая статистика по товарам (график)
 * @param {PageObject.StatisticsReport} report - блок отчёта
 * @param {PageObject.Filters} filters - общие фильтры
 */
export default makeSuite('Общая статистика по товарам.', {
    story: {
        async beforeEach() {
            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
            this.setPageObjects({
                reportFilters() {
                    return this.createPageObject('Filters', this.report, ModelsPromotionStatisticsFiltersGroup.root);
                },
            });

            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
            await this.allure.runStep('Ожидаем появления блока общих фильтров', () =>
                // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                this.filters.waitForVisible(),
            );

            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
            await this.allure.runStep('Ожидаем появления блока общей статистики по товарам', () =>
                // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                this.report.waitForVisible(),
            );

            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
            await this.allure.runStep('Ожидаем появления фильтров блока общей статистики по товарам', () =>
                // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                this.reportFilters.waitForVisible(),
            );
        },
        'Фильтры.': {
            'Детализация.': {
                'При изменении периода': {
                    'масштабирует виды детализации': makeCase({
                        id: 'vendor_auto-1052',
                        async test() {
                            this.setPageObjects({
                                popup() {
                                    return this.createPageObject('PopupB2b');
                                },
                                periodSelect() {
                                    return this.createPageObject(
                                        'SelectAdvanced',
                                        this.filters.elem('label:nth-child(1)'),
                                    );
                                },
                                scaleSelect() {
                                    return this.createPageObject('SelectAdvanced', this.reportFilters.label(1));
                                },
                            });

                            await this.allure.runStep('Проверяем значение фильтра "Период"', () =>
                                this.periodSelect
                                    .getText()
                                    .should.eventually.be.equal(
                                        'За последние 30 дней',
                                        'Выбран период "За последние 30 дней"',
                                    ),
                            );

                            await this.allure.runStep('Проверяем значение фильтра "Детализация"', async () => {
                                await this.scaleSelect
                                    .getText()
                                    .should.eventually.be.equal('По дням', 'Выбрана детализация "По дням"');
                                await this.scaleSelect.click();
                                await this.popup.waitForPopupShown();
                                await this.scaleSelect
                                    .isItemByTitleDisabled('По месяцам')
                                    .should.eventually.be.equal(true, 'Детализация "По месяцам" недоступна');
                            });

                            await this.allure.runStep('Выбираем период "За последний год"', async () => {
                                await this.periodSelect.click();
                                await this.popup.waitForPopupShown();
                                await this.periodSelect.selectItem('За последний год');
                            });

                            await this.browser.vndScrollToBottom();

                            await this.allure.runStep('Выбираем детализацию "По месяцам"', async () => {
                                await this.scaleSelect.click();
                                await this.popup.waitForPopupShown();
                                await this.scaleSelect.selectItem('По месяцам');
                            });

                            await this.allure.runStep('Проверяем значение фильтра "Детализация"', () =>
                                this.scaleSelect
                                    .getText()
                                    .should.eventually.be.equal('По месяцам', 'Выбрана детализация "По месяцам"'),
                            );

                            await this.allure.runStep('Выбираем период "За последние 30 дней"', async () => {
                                await this.periodSelect.click();
                                await this.popup.waitForPopupShown();
                                await this.periodSelect.selectItem('За последние 30 дней');
                            });

                            await this.allure.runStep('Проверяем значение фильтра "Детализация"', () =>
                                this.scaleSelect
                                    .getText()
                                    .should.eventually.be.equal('По дням', 'Выбрана детализация "По дням"'),
                            );
                        },
                    }),
                },
            },
        },
    },
});
