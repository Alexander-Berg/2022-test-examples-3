'use strict';

import {makeCase, makeSuite, importSuite, mergeSuites} from 'ginny';

import buildUrl from 'spec/lib/helpers/buildUrl';
// @ts-expect-error(TS2306) найдено в рамках VNDFRONT-4532
import P from 'app/constants/permissions';
// @ts-expect-error(TS2306) найдено в рамках VNDFRONT-4532
import ROUTE_NAMES from 'app/constants/routeNames';
import {isAllowed} from 'shared/permissions';

/**
 * Тесты на колокольчик
 * @param {PageObject.Bell} bell – колокольчик
 * @param {PageObject.PagedList} list – список уведомлений
 * @param {Object} params
 * @param {number} params.unreadCount - количество непрочитанных уведомлений
 * @param {string} params.readAllLinkCaption - текст ссылки прочтения всех уведомлений
 */
export default makeSuite('Колокольчик.', {
    feature: 'Уведомления',
    environment: 'testing',
    params: {
        user: 'Пользователь',
    },
    story: mergeSuites(
        {
            async beforeEach() {
                await this.allure.runStep('Ожидаем появления колокольчика', () => this.bell.waitForExist());
                await this.allure.runStep('Кликаем на колокольчик', () => this.bell.root.click());
                await this.allure.runStep('Ожидаем появления выпадающего меню', () =>
                    this.browser.waitUntil(
                        () => this.bell.dropdown.isVisible(),
                        this.browser.options.waitforTimeout,
                        'Выпадающее меню появилось',
                    ),
                );
            },
        },
        importSuite('Link', {
            suiteName: 'Ссылка "Настройки"',
            meta: {
                id: 'vendor_auto-777',
                issue: 'VNDFRONT-2391',
            },
            params: {
                caption: 'Настройки',
                comparison: {
                    skipHostname: true,
                },
            },
            hooks: {
                // @ts-expect-error(TS2322) найдено в рамках VNDFRONT-4580
                beforeEach() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    const {vendor, permissionsByVendor} = this.params;

                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    this.params.exist = isAllowed(permissionsByVendor, P.subscribers.read);
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    this.params.url = buildUrl(ROUTE_NAMES.SETTINGS, {
                        vendor,
                        tab: 'notifications',
                    });

                    // Грязный хак, который подменяет ID-кейса для отсутствующей ссылки.
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    if (!this.params.exist) {
                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        this.currentTest._meta.id = 'vendor_auto-775';
                    }
                },
            },
            pageObjects: {
                link() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('Link', this.browser, this.bell.settingsLink);
                },
            },
        }),
        importSuite('Link', {
            suiteName: 'Ссылка "Посмотреть все"',
            meta: {
                id: 'vendor_auto-779',
                issue: 'VNDFRONT-2393',
            },
            params: {
                caption: 'Посмотреть все',
                comparison: {
                    skipHostname: true,
                },
            },
            hooks: {
                // @ts-expect-error(TS2322) найдено в рамках VNDFRONT-4580
                beforeEach() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    const {vendor} = this.params;

                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    this.params.url = buildUrl(ROUTE_NAMES.NOTIFICATIONS, {vendor});
                },
            },
            pageObjects: {
                link() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('Link', this.browser, this.bell.viewAllLink);
                },
            },
        }),
        importSuite('Notifications/__navigateNotification', {
            meta: {
                id: 'vendor_auto-778',
                issue: 'VNDFRONT-2394',
                environment: 'kadavr',
            },
            hooks: {
                async beforeEach() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    const {vendor, unreadCount} = this.params;

                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    this.params.pageUrl = buildUrl(ROUTE_NAMES.NOTIFICATION, {
                        vendor,
                        notificationId: 6382,
                    });
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    this.params.expectedUnreadCount = unreadCount - 1;

                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    await this.allure.runStep('Ожидаем появления списка уведомлений', () =>
                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        this.list.waitForExist(),
                    );
                },
            },
        }),
        {
            'При клике на "Прочитать все"': {
                'все уведомления отмечаются прочитанными': makeCase({
                    id: 'vendor_auto-776',
                    issue: 'VNDFRONT-2400',
                    environment: 'kadavr',
                    async test() {
                        const {unreadCount, readAllLinkCaption} = this.params;

                        await this.allure.runStep(`Ожидаем появления ссылки "${readAllLinkCaption}"`, () =>
                            this.browser.waitUntil(
                                () => this.bell.readAllLink.isVisible(),
                                this.browser.options.waitforTimeout,
                                'Не удалось дождаться появления ссылки',
                            ),
                        );
                        await this.allure.runStep('Получаем название ссылки', () =>
                            this.bell.readAllLink
                                .getText()
                                .should.eventually.be.equal(
                                    readAllLinkCaption,
                                    `Текст ссылки соответствует "${readAllLinkCaption}"`,
                                ),
                        );
                        await this.bell.waitForCounterVisible();
                        await this.bell
                            .getUnreadCount()
                            .should.eventually.be.equal(unreadCount, `Непрочитано ${unreadCount} уведомлений`);
                        await this.allure.runStep(`Кликаем на ссылку "${readAllLinkCaption}"`, () =>
                            this.bell.readAllLink.click(),
                        );
                        await this.allure.runStep('Ожидаем скрытия счетчика непрочитанных уведомлений', () =>
                            this.browser.waitUntil(
                                () =>
                                    this.bell.counter
                                        .vndIsExisting()
                                        // @ts-expect-error(TS7006) найдено в рамках VNDFRONT-4580
                                        .then(isExisting => !isExisting),
                                this.browser.options.waitforTimeout,
                                'Не удалось дождаться скрытия счетчика',
                            ),
                        );
                    },
                }),
            },
        },
    ),
});
