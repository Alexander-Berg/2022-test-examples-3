'use strict';

import {makeSuite, importSuite, mergeSuites} from 'ginny';
import moment from 'moment';

import Filters from 'spec/page-objects/Filters';

/**
 * Тест на фильтры списка уведомлений
 * @param {PageObject.PagedList} list
 * @param {PageObject.Filters} filters
 */
export default makeSuite('Фильтры.', {
    story: mergeSuites(
        {
            async beforeEach() {
                await this.allure.runStep('Ожидаем появления фильтров', () => this.filters.waitForExist());
            },
        },
        importSuite('Filters/__search', {
            meta: {
                issue: 'VNDFRONT-2392',
                id: 'vendor_auto-785',
            },
            params: {
                initialCount: 6,
                expectedCount: 1,
                queryParamName: 'text',
                queryParamValue: 'Notification 6382',
            },
            pageObjects: {
                header() {
                    return this.createPageObject('NotificationsHeader');
                },
                input() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('InputB2b', this.header);
                },
            },
            hooks: {
                beforeEach() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.allure.runStep(
                        'Ожидаем появления шапки списка',
                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        () => this.header.waitForExist(),
                    );
                },
            },
        }),
        importSuite('Filters/__select', {
            suiteName: 'Фильтр по услуге',
            meta: {
                issue: 'VNDFRONT-2407',
                id: 'vendor_auto-783',
            },
            params: {
                initialFilterText: 'Все',
                expectedFilterText: 'Продвижение товаров',
                initialItemsCount: 6,
                expectedItemsCount: 1,
                queryParamName: 'type',
                queryParamValue: 'MODELBIDS',
            },
            pageObjects: {
                select() {
                    return this.createPageObject('SelectB2b', Filters.label(1));
                },
                popup() {
                    return this.createPageObject('PopupB2b');
                },
            },
        }),
        importSuite('Filters/__pager', {
            meta: {
                issue: 'VNDFRONT-2410',
                id: 'vendor_auto-789',
            },
            params: {
                expectedPage: 6,
                initialItemsCount: 10,
                expectedItemsCount: 6,
            },
            pageObjects: {
                pager() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('PagerB2b', Filters.footer);
                },
            },
        }),
        importSuite('Filters/__pageSize', {
            meta: {
                issue: 'VNDFRONT-2409',
                id: 'vendor_auto-788',
            },
            params: {
                queryParamName: 'pageSize',
                initialCount: 10,
                size: 50,
            },
            pageObjects: {
                pageSize() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('PageSize', Filters.footer);
                },
            },
        }),
        importSuite('Filters/__select', {
            suiteName: 'Фильтр по статусу прочтения',
            meta: {
                issue: 'VNDFRONT-2408',
                id: 'vendor_auto-784',
            },
            params: {
                initialFilterText: 'Все',
                expectedFilterText: 'Прочитанные',
                initialItemsCount: 6,
                expectedItemsCount: 1,
                queryParamName: 'status',
                queryParamValue: 'READ',
            },
            pageObjects: {
                select() {
                    return this.createPageObject('SelectB2b', Filters.label(2));
                },
                popup() {
                    return this.createPageObject('PopupB2b');
                },
            },
        }),
        importSuite('Filters/__period', {
            meta: {
                issue: 'VNDFRONT-2399',
                id: 'vendor_auto-782',
            },
            params: {
                fromParamName: 'from',
                fromParamValue: moment().subtract(3, 'd').format('YYYY-MM-DD'),
                toParamName: 'to',
                toParamValue: moment().format('YYYY-MM-DD'),
                initialItemsCount: 6,
                filteredItemsCount: 3,
            },
            pageObjects: {
                datePicker() {
                    return this.createPageObject('DatePicker', Filters.label(0));
                },
            },
        }),
        importSuite('Filters/__search', {
            suiteName: 'Поиск по отсутствующему тексту.',
            meta: {
                issue: 'VNDFRONT-2392',
                id: 'vendor_auto-785',
            },
            params: {
                initialCount: 6,
                expectedCount: 0,
                queryParamName: 'text',
                queryParamValue: 'Abracadabra',
            },
            pageObjects: {
                header() {
                    return this.createPageObject('NotificationsHeader');
                },
                input() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('InputB2b', this.header);
                },
            },
            hooks: {
                beforeEach() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.allure.runStep(
                        'Ожидаем появления шапки списка',
                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        () => this.header.waitForExist(),
                    );
                },
            },
        }),
        importSuite('Filters/__reset', {
            meta: {
                issue: 'VNDFRONT-2392',
                id: 'vendor_auto-785',
            },
            params: {
                initialItemsCount: 6,
                filteredItemsCount: 1,
                async setFilters() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    const {pageRouteName, vendor} = this.params;

                    // @ts-expect-error(TS2571) найдено в рамках VNDFRONT-4580
                    await this.browser.vndOpenPage(pageRouteName, {
                        vendor,
                        text: 'Notification 6386',
                        from: moment().subtract(6, 'd').format('YYYY-MM-DD'),
                        to: moment().format('YYYY-MM-DD'),
                        status: 'UNREAD',
                        type: 'MODELBIDS',
                    });
                },
            },
            pageObjects: {
                resetFilters() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('ResetFilters', this.filters);
                },
            },
        }),
        importSuite('Filters/__title', {
            meta: {
                issue: 'VNDFRONT-2392',
                id: 'vendor_auto-785',
            },
            params: {
                initialText: 'Найдено 6 уведомлений',
                expectedText: 'Найдено 1 уведомление',
                routeParams: {
                    text: 'Notification 6386',
                },
            },
            pageObjects: {
                header() {
                    return this.createPageObject('NotificationsHeader');
                },
                title() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('TitleB2b', this.header);
                },
            },
        }),
    ),
});
