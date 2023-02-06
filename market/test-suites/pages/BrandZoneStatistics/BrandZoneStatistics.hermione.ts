'use strict';

import _ from 'lodash';
import {mergeSuites, makeSuite, PageObject} from 'ginny';
import moment from 'moment';

import buildURL from 'spec/lib/helpers/buildUrl';
import USERS from 'spec/lib/constants/users/users';
import makeUserStory from 'spec/hermione/lib/helpers/userStory';
// @ts-expect-error(TS2306) найдено в рамках VNDFRONT-4532
import {DAY_OF_WEEK} from 'app/constants/date';
// @ts-expect-error(TS2306) найдено в рамках VNDFRONT-4532
import PERMISSIONS from 'app/constants/permissions';
// @ts-expect-error(TS2306) найдено в рамках VNDFRONT-4532
import ROUTE_NAMES from 'app/constants/routeNames';

const userStory = makeUserStory(ROUTE_NAMES.BRAND_ZONE_STATISTICS);

const ButtonLevitan = PageObject.get('ButtonLevitan');
const SummaryEntry = PageObject.get('SummaryEntry');
const EntryMetric = PageObject.get('EntryMetric');
const BrandZoneStatisticsAbout = PageObject.get('BrandZoneStatisticsAbout');

const todayDate = moment().format(DAY_OF_WEEK);
const BRAND_ZONE_SURVEY_ID = '10021881.9627f7c691b08541081968734b747fe9a8ba75fd';

export default makeSuite('Страница Статистики брендзоны.', {
    story: (() => {
        const suites = USERS.map(user => {
            const vendor = _.get(user.permissions, [PERMISSIONS.brandzone.read, 0], 13143);
            const params = {
                vendor,
                routeParams: {vendor},
            };

            return makeSuite(`${user.description}.`, {
                story: userStory({
                    user,
                    params,
                    pageObjects: {
                        logo: 'Logo',
                        footer: 'Footer',
                        generateReportButton() {
                            return this.createPageObject(
                                'ButtonLevitan',
                                // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                this.browser,
                                // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                ButtonLevitan.getByText('Сформировать отчёт'),
                            );
                        },
                        asyncReportsList() {
                            return this.createPageObject('AsyncReportsList');
                        },
                        firstReportLink() {
                            return this.createPageObject(
                                'Link',
                                // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                this.asyncReportsList.getItemByIndex(0),
                            );
                        },
                        brandZoneStatisticsMonthSummaryEntries() {
                            return this.createPageObject('BrandZoneStatisticsMonthSummaryEntries');
                        },
                        summaryEntry() {
                            return this.createPageObject(
                                'SummaryEntry',
                                // @ts-expect-error(TS2345) найдено в рамках VNDFRONT-4580
                                this.brandZoneStatisticsMonthSummaryEntries,
                                `${SummaryEntry.root}:nth-child(1)`,
                            );
                        },
                        vendorTariffTitle() {
                            return this.createPageObject(
                                'TextLevitan',
                                // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                this.browser,
                                BrandZoneStatisticsAbout.root,
                            );
                        },
                        entryMetricSecondBox() {
                            return this.createPageObject(
                                'EntryMetric',
                                // @ts-expect-error(TS2345) найдено в рамках VNDFRONT-4580
                                this.summaryEntry,
                                `${EntryMetric.root}:nth-child(2)`,
                            );
                        },
                        metricValueElem() {
                            return this.createPageObject(
                                'TextLevitan',
                                // @ts-expect-error(TS2345) найдено в рамках VNDFRONT-4580
                                this.entryMetricSecondBox,
                                // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                EntryMetric.metricValue,
                            );
                        },
                    },
                    suites: {
                        common: [
                            'BrandZoneStatistics',
                            {
                                suite: 'Link',
                                suiteName: 'Показы баннеров. Ссылка на форму по кнопке "Купить"',
                                meta: {
                                    id: 'vendor_auto-1166',
                                    issue: 'VNDFRONT-3918',
                                },
                                pageObjects: {
                                    link() {
                                        return this.createPageObject(
                                            'Link',
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            this.summaryEntry,
                                            ButtonLevitan.root,
                                        );
                                    },
                                },
                                params: {
                                    external: true,
                                    target: '_blank',
                                    caption: 'Купить',
                                    url: buildURL('external:forms-survey', {
                                        surveyId: BRAND_ZONE_SURVEY_ID,
                                    }),
                                },
                            },
                            {
                                suite: 'Hint',
                                suiteName: 'Показы баннеров. Хинт "Было показов".',
                                meta: {
                                    issue: 'VNDFRONT-3614',
                                    id: 'vendor_auto-1167',
                                },
                                params: {
                                    text: `${todayDate}\nПо тарифу\nСверх тарифа`,
                                },
                                pageObjects: {
                                    entryMetric() {
                                        return this.createPageObject(
                                            'EntryMetric',
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            this.summaryEntry,
                                            `${EntryMetric.root}:nth-child(1)`,
                                        );
                                    },
                                    hint() {
                                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                        return this.createPageObject('Hint', this.entryMetric);
                                    },
                                },
                            },
                            {
                                suite: 'Hint',
                                suiteName: 'Показы баннеров. Хинт "Осталось показов".',
                                meta: {
                                    issue: 'VNDFRONT-3614',
                                    id: 'vendor_auto-1167',
                                },
                                params: {
                                    text: `${todayDate}\nПо тарифу`,
                                },
                                pageObjects: {
                                    entryMetric() {
                                        return this.createPageObject(
                                            'EntryMetric',
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            this.summaryEntry,
                                            `${EntryMetric.root}:nth-child(2)`,
                                        );
                                    },
                                    hint() {
                                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                        return this.createPageObject('Hint', this.entryMetric);
                                    },
                                },
                            },
                            {
                                suite: 'Link',
                                suiteName: 'Ссылка на справку',
                                meta: {
                                    id: 'vendor_auto-1362',
                                    feature: 'Статистика Брендзоны',
                                    environment: 'testing',
                                },
                                pageObjects: {
                                    pageHeading() {
                                        return this.createPageObject('PageHeading');
                                    },
                                    link() {
                                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                        return this.createPageObject('Link', this.pageHeading);
                                    },
                                },
                                params: {
                                    url: buildURL('external:help:brand-stats'),
                                    external: true,
                                    target: '_blank',
                                    caption: 'Справка',
                                },
                            },
                        ],
                    },
                }),
            });
        });
        return mergeSuites(...suites);
    })(),
});
