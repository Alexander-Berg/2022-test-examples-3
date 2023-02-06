'use strict';

import {mergeSuites, makeSuite, PageObject} from 'ginny';
import _ from 'lodash';

import makeUserStory from 'spec/hermione/lib/helpers/userStory';
import USERS from 'spec/lib/constants/users/users';
// @ts-expect-error(TS2306) найдено в рамках VNDFRONT-4532
import PERMISSIONS from 'app/constants/permissions';
// @ts-expect-error(TS2306) найдено в рамках VNDFRONT-4532
import ROUTE_NAMES from 'app/constants/routeNames';

import businessesState from './businesses.json';
import recommendationsState from './recommendations.json';

const userStory = makeUserStory(ROUTE_NAMES.BUSINESSES);

const BusinessesListItem = PageObject.get('BusinessesListItem');

export default makeSuite('Страница Рекомендованные бизнесы.', {
    story: (() => {
        const suites = USERS.map(user => {
            const vendor = _.get(user.permissions, [PERMISSIONS.recommended.read, 0], 3301);
            const params = {
                vendor,
                routeParams: {vendor},
            };

            const virtualVendorState = [
                {
                    vendorId: vendor,
                    products: [1],
                },
            ];

            return makeSuite(`${user.description}.`, {
                story: userStory({
                    user,
                    params,
                    async onSetKadavrState({id}) {
                        switch (id) {
                            // Кейс с панелькой о недоступности услуги
                            case 'vendor_auto-1452':
                                return this.browser.setState('virtualVendor', [
                                    {
                                        vendorId: vendor,
                                        products: [],
                                    },
                                ]);
                            // Кейсы с поиском, фильтрацией и редактированием рекомендаций для бизнесов
                            case 'vendor_auto-1447':
                            case 'vendor_auto-1448':
                            case 'vendor_auto-1449':
                            case 'vendor_auto-1451':
                            case 'vendor_auto-1453':
                            case 'vendor_auto-1458':
                            case 'vendor_auto-1459':
                                await this.browser.setState('virtualVendor', virtualVendorState);

                                await this.browser.setState('vendorRecommendedBusinesses', recommendationsState);

                                return this.browser.setState('priceLabs', {businesses: businessesState});
                            case 'vendor_auto-1450':
                                await this.browser.setState('virtualVendor', virtualVendorState);

                                return this.browser.setState('priceLabs', {businesses: businessesState});
                            default:
                        }
                    },
                    pageObjects: {
                        logo: 'Logo',
                        footer: 'Footer',
                        list() {
                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                            return this.createPageObject('BusinessesList').setItemSelector(BusinessesListItem.root);
                        },
                        item() {
                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                            return this.createPageObject('BusinessesListItem', this.list, this.list.getItemByIndex(0));
                        },
                    },
                    suites: {
                        common: [
                            {
                                suite: 'Page/title',
                                params: {
                                    title: 'Рекомендованные магазины',
                                },
                            },
                            'Businesses/filters',
                            {
                                suite: 'InfoPanel',
                                suiteName: 'При не подключённой услуге',
                                meta: {
                                    issue: 'VNDFRONT-4301',
                                    id: 'vendor_auto-1452',
                                    environment: 'kadavr',
                                },
                                params: {
                                    title: 'Инструмент не подключён',
                                    text:
                                        'Пожалуйста, обратитесь к вашему менеджеру или в службу поддержки, ' +
                                        'чтобы начать настраивать рекомендованные магазины.',
                                },
                                pageObjects: {
                                    panel: 'InfoPanel',
                                },
                            },
                            {
                                suite: 'Hint',
                                suiteName: 'Подсказка официального магазина',
                                meta: {
                                    issue: 'VNDFRONT-4384',
                                    id: 'vendor_auto-1458',
                                    environment: 'kadavr',
                                },
                                params: {
                                    text:
                                        'Магазин бренда на Маркете. Если у продавца одновременно есть статус ' +
                                        'официального магазина и представителя бренда, показываться будет только ' +
                                        'значок официального магазина.',
                                },
                                pageObjects: {
                                    header: 'TableHeadLevitan',
                                    cell() {
                                        return this.createPageObject(
                                            'TableCellLevitan',
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            this.header,
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            this.header.getCellByIndex(2),
                                        );
                                    },
                                    hint() {
                                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                        return this.createPageObject('Hint', this.cell);
                                    },
                                },
                                hooks: {
                                    async beforeEach() {
                                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                        await this.allure.runStep('Ожидаем появления списка бизнесов', () =>
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            this.list.waitForVisible(),
                                        );

                                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                        await this.allure.runStep('Ожидаем загрузки списка бизнесов', () =>
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            this.list.waitForLoading(),
                                        );
                                    },
                                },
                            },
                            {
                                suite: 'Hint',
                                suiteName: 'Подсказка представителя бренда',
                                meta: {
                                    issue: 'VNDFRONT-4384',
                                    id: 'vendor_auto-1459',
                                    environment: 'kadavr',
                                },
                                params: {
                                    text:
                                        'Значок представителя бренда — дополнительное подтверждение качества ' +
                                        'товаров вашей марки в магазине и сервиса продавца. Можно выбрать ' +
                                        'неограниченное число представителей.',
                                },
                                pageObjects: {
                                    header: 'TableHeadLevitan',
                                    cell() {
                                        return this.createPageObject(
                                            'TableCellLevitan',
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            this.header,
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            this.header.getCellByIndex(3),
                                        );
                                    },
                                    hint() {
                                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                        return this.createPageObject('Hint', this.cell);
                                    },
                                },
                                hooks: {
                                    async beforeEach() {
                                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                        await this.allure.runStep('Ожидаем появления списка бизнесов', () =>
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            this.list.waitForVisible(),
                                        );

                                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                        await this.allure.runStep('Ожидаем загрузки списка бизнесов', () =>
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            this.list.waitForLoading(),
                                        );
                                    },
                                },
                            },
                        ],
                        byPermissions: {
                            [PERMISSIONS.recommended.write]: ['Businesses/recommendations'],
                        },
                    },
                }),
            });
        });

        return mergeSuites(...suites);
    })(),
});
