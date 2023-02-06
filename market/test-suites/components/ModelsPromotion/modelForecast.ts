'use strict';

import {mergeSuites, importSuite, PageObject, makeSuite, makeCase} from 'ginny';

import buildUrl from 'spec/lib/helpers/buildUrl';
// @ts-expect-error(TS2306) найдено в рамках VNDFRONT-4532
import {MODELS_PROMOTION_STATISTICS} from 'app/constants/routeNames';

const PopupB2b = PageObject.get('PopupB2b');

/**
 * Тесты на продвигаемый товар
 * @param {PageObject.ModelsPromotionListItem} item - товар из списка
 */
export default makeSuite('Прогнозатор.', {
    issue: 'VNDFRONT-2249',
    environment: 'kadavr',
    feature: 'Прогнозатор',
    params: {
        user: 'Пользователь',
    },
    story: mergeSuites(
        {
            beforeEach() {
                return this.browser.allure.runStep('Ожидаем появления товара', () => this.item.waitForExist());
            },
        },
        importSuite('Link', {
            suiteName: 'Ссылка на модель на Маркете',
            meta: {
                id: 'vendor_auto-676',
                environment: 'kadavr',
            },
            params: {
                url: '/product/10667722',
                caption: 'Видеорегистратор CARCAM Q7',
                comparison: {
                    skipHostname: true,
                },
                external: true,
                target: '_blank',
            },
            pageObjects: {
                link() {
                    // Непонятно, почему this.createPageObject('Link', this.modelSnippet) находит все ссылки на странице
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('Link', this.item, this.item.modelLink);
                },
            },
        }),
        importSuite('Link', {
            suiteName: 'График. Ссылка на статистику',
            meta: {
                id: 'vendor_auto-674',
                issue: 'VNDFRONT-3245',
                environment: 'kadavr',
            },
            params: {
                caption: 'Статистика',
                comparison: {
                    skipHostname: true,
                },
            },
            pageObjects: {
                chart() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('ModelsPromotionChart', this.item);
                },
                popup() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('PopupB2b', this.chart);
                },
                link() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('Link', this.popup);
                },
            },
            hooks: {
                async beforeEach() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    const {vendor} = this.params;

                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    this.params.url = buildUrl(MODELS_PROMOTION_STATISTICS, {
                        vendor,
                        modelId: 10667722,
                        period: 'INTERVAL',
                        report: 'MODELS',
                        from: '2018-12-09',
                        to: '2019-01-07',
                    });

                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    await this.browser.allure.runStep('Наводим курсор на первую точку', async () => {
                        // Стрим с флагом отображения тултипа нужно проинициализировать, наведя курсор сначала на график
                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        await this.chart.root.moveToObject();
                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        await this.chart.getPointByIndex(0).moveToObject();
                    });

                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    await this.browser.allure.runStep('Ожидаем появления попапа', () =>
                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        this.popup.waitForVisible(),
                    );
                },
            },
        }),
        {
            'При наведении курсора на звезду': {
                'появляется подсказка': makeCase({
                    id: 'vendor_auto-677',
                    issue: 'VNDFRONT-3336',
                    async test() {
                        this.setPageObjects({
                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                            popup: this.createPageObject('PopupB2b', this.browser, PopupB2b.activeBodyPopup),
                            icon: this.createPageObject('IconB2b', this.item.recommendedShopsLabel),
                        });

                        await this.icon.root.vndHoverToElement();

                        await this.browser.allure.runStep('Ожидаем появления подсказки', () =>
                            this.popup.waitForVisible(),
                        );

                        await this.popup
                            .getActiveText()
                            .should.eventually.be.equal('Рекомендованные магазины', 'Текст подсказки корректный');

                        await this.popup.selfClick();

                        await this.allure.runStep('Ожидаем скрытия подсказки', () =>
                            this.browser.waitUntil(
                                async () => {
                                    const existing = await this.popup.activeBodyPopup.vndIsExisting();

                                    return existing === false;
                                },
                                this.browser.options.waitforTimeout,
                                'Не удалось дождаться скрытия подсказки',
                            ),
                        );
                    },
                }),
            },
        },
    ),
});
