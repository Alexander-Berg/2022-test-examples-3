'use strict';

import {makeSuite, mergeSuites, importSuite, PageObject} from 'ginny';
import buildUrl from 'spec/lib/helpers/buildUrl';

// @ts-expect-error(TS7016) найдено в рамках VNDFRONT-4580
import REPORTS from 'app/constants/reports';
// @ts-expect-error(TS2306) найдено в рамках VNDFRONT-4532
import ROUTE_NAMES from 'app/constants/routeNames';

const FlexGroup = PageObject.get('FlexGroup');

const reports = [
    {
        reportKey: REPORTS.SHOWS,
        caption: 'Показы',
        text:
            'Суммарное количество показов всех форматов продвижения бренда в рамках спецпроекта — например, ' +
            'баннеров, выделенных предложений на страницах' +
            ' категорий или специальных блоков на карточках товаров.',
    },
    {
        reportKey: REPORTS.CTR,
        caption: 'CTR',
        // eslint-disable-next-line max-len
        text: 'Показатель кликабельности баннеров, выделенных предложений и других форматов продвижения бренда в рамках спецпроекта. Измеряется в процентах и рассчитывается по формуле: CTR = (количество кликов / количество показов) х 100%.',
    },
    {
        reportKey: REPORTS.INCREMENTAL,
        caption: 'Сводная статистика по спецпроектам',
        // eslint-disable-next-line max-len
        text: 'Первый график — количество кликов по предложениям с товарами бренда, которые продвигаются в спецпроектах. Второй — количество заказов, совершённых пользователями Маркета в течение суток после клика по этим предложениям. Третий — выручка от продажи товаров, которые были куплены в рамках спецпроекта. Заказы и выручка рассчитываются по усреднённой статистике магазинов на Маркете.',
    },
    {
        reportKey: REPORTS.CAMPAIGNS,
        caption: 'Результаты спецпроектов',
        // eslint-disable-next-line max-len
        text: 'Отчёт позволяет сравнить запланированное и фактическое количество показов всех форматов продвижения бренда в рамках спецпроекта и кликов по предложениям товаров, которые продвигаются в спецпроекте, а также оценить выполнение плана по каждому месяцу и за всё время действия спецпроекта.',
    },
];

/**
 * @param {PageObject.FlexGroup} list
 * @param {number} vendor - идентификатор вендора
 */
export default makeSuite('Список превью отчетов.', {
    feature: 'Спецпроекты',
    environment: 'testing',
    params: {
        user: 'Пользователь',
    },
    story: mergeSuites(
        {
            async beforeEach() {
                // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                await this.browser.yaWaitForPageObject(FlexGroup);
            },
        },
        ...reports.map(({caption, reportKey, text}, index) =>
            makeSuite(`Отчет ${caption}`, {
                story: mergeSuites(
                    {
                        beforeEach() {
                            this.setPageObjects({
                                reportPreview() {
                                    return this.createPageObject(
                                        'ReportPreview',
                                        this.list,
                                        this.list.getItemByIndex(index),
                                    );
                                },
                            });
                        },
                    },
                    importSuite('Link', {
                        meta: {
                            id: 'vendor_auto-997',
                            issue: 'VNDFRONT-2565',
                        },
                        suiteName: 'Ссылка',
                        params: {
                            caption,
                            comparison: {
                                skipHostname: true,
                            },
                        },
                        pageObjects: {
                            link() {
                                return this.createPageObject(
                                    'Link',
                                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                    this.reportPreview,
                                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                    this.reportPreview.headerLink,
                                );
                            },
                        },
                        hooks: {
                            // @ts-expect-error(TS2322) найдено в рамках VNDFRONT-4580
                            beforeEach() {
                                // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                this.params.url = buildUrl(ROUTE_NAMES.SPECIAL_PROJECTS_REPORT, {
                                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                    vendor: this.params.vendor,
                                    reportKey,
                                });
                            },
                        },
                    }),
                    importSuite('Hint', {
                        meta: {
                            feature: 'Спецпроекты',
                            id: 'vendor_auto-883',
                            environment: 'testing',
                        },
                        suiteName: 'Подсказка у графика.',
                        params: {
                            text,
                        },
                        pageObjects: {
                            hint() {
                                return this.createPageObject(
                                    'Hint',
                                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                    this.reportPreview.header,
                                );
                            },
                        },
                    }),
                ),
            }),
        ),
    ),
});
