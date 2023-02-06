'use strict';

import _ from 'lodash';
import {mergeSuites, makeSuite} from 'ginny';

import USERS from 'spec/lib/constants/users/users';
import makeUserStory from 'spec/hermione/lib/helpers/userStory';
// @ts-expect-error(TS2306) найдено в рамках VNDFRONT-4532
import P from 'app/constants/permissions';
// @ts-expect-error(TS2306) найдено в рамках VNDFRONT-4532
import ROUTE_NAMES from 'app/constants/routeNames';
import NotificationsListItem from 'spec/page-objects/NotificationsListItem';
import PreviewItem from 'spec/page-objects/PreviewItem';
import Layout from 'spec/page-objects/LayoutBase';

import initialState from './initialState';
import initialStateExtended from './initialStateExtended';

const userStory = makeUserStory(ROUTE_NAMES.NOTIFICATIONS);

export default makeSuite('Страница Уведомления.', {
    story: (() => {
        const suites = USERS.map(user => {
            const vendor = _.get(user.permissions, [P.notifications.read, 0], 3301);
            const params = {
                vendor,
                routeParams: {
                    vendor,
                    /*
                     * Ряд ссылок ведут на эту же страницу списка через onClick.
                     * Сьют Link пытается сравнить href, которого нет, с эталонным URL.
                     * Тест падает. В качестве решения добавляем неиспользуемый параметр в URL страницы.
                     */
                    test: true,
                },
            };

            return makeSuite(`${user.description}.`, {
                story: userStory({
                    user,
                    // @ts-expect-error(TS2322) найдено в рамках VNDFRONT-4580
                    params,
                    async onSetKadavrState({id}) {
                        switch (id) {
                            case 'vendor_auto-788':
                            case 'vendor_auto-789':
                                // Увеличенное количество элементов для тестов постраничной навигации
                                return this.browser.setState('vendorsNotifications', initialStateExtended);
                            case 'vendor_auto-799':
                                await this.browser.setState('vendorsNotifications', initialState);

                                return this.browser.setState('marketAnalytics', {
                                    balance: {
                                        actualBalance: 666,
                                        allowedSpendSum: 555,
                                        datasourceId: 1,
                                        nextMonthLackSum: 34,
                                        summaryCost: {
                                            currentPeriodCost: {
                                                period: {
                                                    startDate: '05-12-2020',
                                                    endDate: '04-03-2021',
                                                },
                                                price: 123,
                                            },
                                            freezeNextPeriodCost: {
                                                period: {
                                                    startDate: '05-12-2020',
                                                    endDate: '04-03-2021',
                                                },
                                                price: 123,
                                            },
                                            nextPeriodCost: {
                                                period: {
                                                    startDate: '05-12-2020',
                                                    endDate: '04-03-2021',
                                                },
                                                price: 123,
                                            },
                                            totalCost: 222,
                                        },
                                    },
                                });
                            default:
                                // Простой список из статичного файла по дефолту
                                return this.browser.setState('vendorsNotifications', initialState);
                        }
                    },
                    pageObjects: {
                        logo: 'Logo',
                        bell: 'Bell',
                        footer: 'Footer',
                    },
                    suites: {
                        common: [
                            {
                                suite: 'Page/title',
                                params: {
                                    title: 'Уведомления',
                                },
                            },
                            {
                                suite: 'Bell',
                                params: {
                                    unreadCount: 5,
                                    readAllLinkCaption: 'Прочитать все',
                                },
                                pageObjects: {
                                    list() {
                                        return this.createPageObject(
                                            'PagedList',
                                            // @ts-expect-error(TS2345) найдено в рамках VNDFRONT-4580
                                            this.browser,
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            this.bell.list,
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                        ).setItemSelector(PreviewItem.root);
                                    },
                                },
                            },
                            {
                                suite: 'Notifications',
                                pageObjects: {
                                    list() {
                                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                        return this.createPageObject('PagedList', Layout.main).setItemSelector(
                                            NotificationsListItem.root,
                                        );
                                    },
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
