'use strict';

import {mergeSuites, importSuite, PageObject, makeSuite} from 'ginny';

const StatisticsReport = PageObject.get('StatisticsReport');

/**
 * Таб "Товары"
 * @param {PageObject.TabGroupLevitan} tabGroup - табы
 * @param {PageObject.Filters} filters - общие фильтры
 */
export default makeSuite('Таб "Товары".', {
    issue: 'VNDFRONT-3871',
    feature: 'Статистика продвижения товаров',
    story: mergeSuites(
        {
            async beforeEach() {
                // По умолчанию на странице активен таб "Товары", поэтому только сверяем название
                await this.allure.runStep('Проверяем название активного таба', () =>
                    this.tabGroup.activeTabText.should.eventually.be.equal(
                        'Товары',
                        'Название таба соответствует "Товары"',
                    ),
                );
            },
        },
        importSuite('ModelsPromotionStatistics/modelsTab/chart', {
            pageObjects: {
                report() {
                    return this.createPageObject(
                        'StatisticsReport',
                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        this.browser,
                        `${StatisticsReport.root}:nth-child(2)`,
                    );
                },
            },
        }),
        importSuite('ModelsPromotionStatistics/modelsTab/tableReport', {
            pageObjects: {
                report() {
                    return this.createPageObject(
                        'StatisticsReport',
                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        this.browser,
                        `${StatisticsReport.root}:nth-child(3)`,
                    );
                },
            },
        }),
    ),
});
