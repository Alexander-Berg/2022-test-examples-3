'use strict';

import url from 'url';

import moment from 'moment';
import {makeSuite, mergeSuites, importSuite} from 'ginny';

import buildUrl from 'spec/lib/helpers/buildUrl';
// @ts-expect-error(TS2306) найдено в рамках VNDFRONT-4532
import ROUTE_NAMES from 'app/constants/routeNames';
import IconB2b from 'spec/page-objects/IconB2b';
import Filters from 'spec/page-objects/Filters';
import CheckboxB2b from 'spec/page-objects/CheckboxB2b';
import PreviewItem from 'spec/page-objects/PreviewItem';

import openNotificationFromList from './hooks/openNotificationFromList';

const statusSelectTogglerSelector = `${IconB2b.root}:not(${CheckboxB2b.icon})`;

/**
 * Тесты на список уведомлений
 * @param {PageObject.PagedList} list
 * @param {PageObject.Bell} bell
 */
export default makeSuite('Список уведомлений.', {
    issue: 'VNDFRONT-2383',
    environment: 'kadavr',
    feature: 'Уведомления',
    params: {
        user: 'Пользователь',
    },
    story: mergeSuites(
        {
            async beforeEach() {
                await this.allure.runStep('Ожидаем появления списка уведомлений', () => this.list.waitForExist());
                await this.allure.runStep('Ожидаем загрузки списка', () => this.list.waitForLoading());
            },
        },
        importSuite('Notifications/__filters', {
            pageObjects: {
                filters() {
                    return this.createPageObject('Filters');
                },
            },
        }),
        importSuite('Notifications/__navigateNotification', {
            meta: {
                id: 'vendor_auto-790',
                issue: 'VNDFRONT-2403',
            },
            params: {
                expectedUnreadCount: 4,
            },
            hooks: {
                async beforeEach() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    this.params.pageUrl = buildUrl(ROUTE_NAMES.NOTIFICATION, {
                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        vendor: this.params.vendor,
                        notificationId: 6382,
                    });

                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    await this.allure.runStep('Ожидаем появления колокольчика', () =>
                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        this.bell.waitForExist(),
                    );
                },
            },
        }),
        importSuite('Notifications/__navigateNotification', {
            suiteName: 'Переход на прочитанное уведомление.',
            meta: {
                id: 'vendor_auto-791',
                issue: 'VNDFRONT-2404',
            },
            params: {
                expectedUnreadCount: 5,
                itemIndex: 1,
            },
            hooks: {
                async beforeEach() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    this.params.pageUrl = buildUrl(ROUTE_NAMES.NOTIFICATION, {
                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        vendor: this.params.vendor,
                        notificationId: 6383,
                    });

                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    await this.allure.runStep('Ожидаем появления колокольчика', () =>
                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        this.bell.waitForExist(),
                    );
                },
            },
        }),
        importSuite('Notifications/__markNotification', {
            suiteName: 'Выбрать в списке и отметить уведомление прочитанным.',
            meta: {
                id: 'vendor_auto-787',
                issue: 'VNDFRONT-2412',
            },
            pageObjects: {
                item() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('NotificationsListItem', this.list, this.list.getItemByIndex(0));
                },
                checkbox() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('CheckboxB2b', this.item.getCell(0));
                },
                header() {
                    return this.createPageObject('NotificationsListHeader');
                },
                button() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('ButtonB2bNext', this.header.mark);
                },
                text() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('TextB2b', this.header.mark);
                },
            },
        }),
        importSuite('Notifications/__selectionByStatus', {
            suiteName: 'Выбор прочитанных.',
            params: {
                selectOptionTitle: 'Прочитанные',
                resetOptionTitle: 'Ни одного',
                initialCount: 0,
                selectedText: 'Выбрано 1 уведомление',
                selectedCount: 1,
            },
            pageObjects: {
                header() {
                    return this.createPageObject('NotificationsListHeader');
                },
                text() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('TextB2b', this.header.mark);
                },
                select() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('SelectB2b', this.header).setCustomToggler(
                        statusSelectTogglerSelector,
                    );
                },
                popup() {
                    return this.createPageObject('PopupB2b');
                },
            },
        }),
        importSuite('Notifications/__selectionByStatus', {
            suiteName: 'Выбор непрочитанных.',
            params: {
                selectOptionTitle: 'Непрочитанные',
                resetOptionTitle: 'Ни одного',
                initialCount: 0,
                selectedText: 'Выбрано 5 уведомлений',
                selectedCount: 5,
            },
            pageObjects: {
                header() {
                    return this.createPageObject('NotificationsListHeader');
                },
                text() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('TextB2b', this.header.mark);
                },
                select() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('SelectB2b', this.header).setCustomToggler(
                        statusSelectTogglerSelector,
                    );
                },
                popup() {
                    return this.createPageObject('PopupB2b');
                },
            },
        }),
        importSuite('Notifications/backToList', {
            suiteName:
                'Открытие уведомления через список и возврат к списку уведомлений ' +
                'по ссылке «Вернуться к уведомлениям». ',
            meta: {
                id: 'vendor_auto-797',
                issue: 'VNDFRONT-3460',
            },
            params: {
                notificationId: 6382,
                elementCaption: 'Вернуться к уведомлениям',
                withFiltersCheck: true,
                periodValue: moment(),
                productValue: 'Рекомендованные магазины',
                statusValue: 'Непрочитанные',
                expectedPeriodValue: '',
                expectedProductValue: 'Все',
                expectedStatusValue: 'Все',
            },
            pageObjects: {
                notification() {
                    return this.createPageObject('Notification');
                },
                clickable() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('Link', this.notification);
                },
                datePicker() {
                    return this.createPageObject('DatePicker', Filters.label(0));
                },
                productSelect() {
                    return this.createPageObject('SelectB2b', Filters.label(1));
                },
                statusSelect() {
                    return this.createPageObject('SelectB2b', Filters.label(2));
                },
                popup() {
                    return this.createPageObject('PopupB2b');
                },
            },
            hooks: openNotificationFromList(),
        }),
        importSuite('Notifications/backToList', {
            suiteName:
                'Открытие уведомления через список и возврат к списку уведомлений ' +
                'при нажатии на кнопку «Закрыть». ',
            meta: {
                id: 'vendor_auto-798',
                issue: 'VNDFRONT-3460',
            },
            params: {
                notificationId: 6382,
                elementCaption: 'Закрыть',
            },
            pageObjects: {
                notification() {
                    return this.createPageObject('Notification');
                },
                clickable() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('ButtonLevitan', this.notification);
                },
            },
            hooks: openNotificationFromList(),
        }),
        importSuite('Notifications/backToList', {
            suiteName:
                'Открытие уведомления через список и возврат к списку уведомлений ' +
                'при нажатии на кнопку «Закрыть» с сохранением фильтров. ',
            meta: {
                id: 'vendor_auto-800',
                issue: 'VNDFRONT-3460',
            },
            params: {
                notificationId: 6382,
                elementCaption: 'Закрыть',
                withFiltersCheck: true,
                periodValue: moment(),
                productValue: 'Рекомендованные магазины',
                statusValue: 'Непрочитанные',
                expectedProductValue: 'Рекомендованные магазины',
                expectedStatusValue: 'Непрочитанные',
            },
            pageObjects: {
                notification() {
                    return this.createPageObject('Notification');
                },
                clickable() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('ButtonLevitan', this.notification);
                },
                datePicker() {
                    return this.createPageObject('DatePicker', Filters.label(0));
                },
                productSelect() {
                    return this.createPageObject('SelectB2b', Filters.label(1));
                },
                statusSelect() {
                    return this.createPageObject('SelectB2b', Filters.label(2));
                },
                popup() {
                    return this.createPageObject('PopupB2b');
                },
            },
            hooks: openNotificationFromList(),
        }),
        importSuite('Notifications/backToList', {
            suiteName:
                'Открытие уведомления через колокольчик и возврат на предыдущую страницу ' +
                'при нажатии на кнопку «Закрыть». ',
            meta: {
                id: 'vendor_auto-799',
                issue: 'VNDFRONT-3460',
            },
            params: {
                notificationId: 6382,
                elementCaption: 'Закрыть',
            },
            pageObjects: {
                notification() {
                    return this.createPageObject('Notification');
                },
                clickable() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('ButtonLevitan', this.notification);
                },
                popup() {
                    return this.createPageObject('PopupB2b');
                },
                previewList() {
                    return this.createPageObject(
                        'PagedList',
                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        this.popup.activeBodyPopup,
                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    ).setItemSelector(PreviewItem.root);
                },
                previewItem() {
                    return this.createPageObject(
                        'PreviewItem',
                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        this.previewList,
                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        this.previewList.getItemByIndex(0),
                    );
                },
            },
            hooks: {
                async beforeEach() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    const {vendor, notificationId} = this.params;

                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    this.params.backPageUrl = buildUrl(ROUTE_NAMES.MARKET_ANALYTICS, {
                        vendor,
                    });

                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    await this.browser.vndOpenPage(ROUTE_NAMES.MARKET_ANALYTICS, {
                        vendor,
                    });

                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    await this.allure.runStep('Ожидаем появления колокольчика', () =>
                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        this.bell.waitForExist(),
                    );

                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    await this.allure.runStep('Нажимаем на колокольчик', () =>
                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        this.bell.root.click(),
                    );

                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    await this.browser.allure.runStep('Ожидаем открытия попапа', () =>
                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        this.popup.waitForPopupShown(),
                    );

                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    await this.allure.runStep('Ожидаем появления списка уведомлений', () =>
                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        this.previewList.waitForVisible(),
                    );

                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    await this.allure.runStep('Дожидаемся загрузки списка уведомлений', async () => {
                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        await this.previewList.waitForLoading(20000);

                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        await this.allure.runStep('Ожидаем появления уведомления', () =>
                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                            this.previewItem.waitForVisible(),
                        );
                    });

                    const pageUrl = buildUrl(ROUTE_NAMES.NOTIFICATION, {
                        vendor,
                        notificationId,
                    });

                    const parsedUrl = url.parse(pageUrl, true, true);

                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    await this.allure.runStep('Переходим на страницу одного уведомления', () =>
                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        this.browser
                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                            .vndWaitForChangeUrl(() => this.previewItem.click())
                            .should.eventually.be.link(parsedUrl, {
                                skipProtocol: true,
                                skipHostname: true,
                            }),
                    );
                },
            },
        }),
    ),
});
