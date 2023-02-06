'use strict';

import _ from 'lodash';
import {mergeSuites, makeSuite, PageObject} from 'ginny';

import USERS from 'spec/lib/constants/users/users';
import makeUserStory from 'spec/hermione/lib/helpers/userStory';
import buildUrl from 'spec/lib/helpers/buildUrl';
// @ts-expect-error(TS2306) найдено в рамках VNDFRONT-4532
import PERMISSIONS from 'app/constants/permissions';
// @ts-expect-error(TS2306) найдено в рамках VNDFRONT-4532
import ROUTE_NAMES from 'app/constants/routeNames';

import initialState from './initialState';
import categoriesState from './categories.json';
import modelsState from './models.json';
import groupsState from './groups.json';

const userStory = makeUserStory(ROUTE_NAMES.MODELS_PROMOTION_STATISTICS);

const ModelsPromotionStatisticsFilters = PageObject.get('ModelsPromotionStatisticsFilters');

export default makeSuite('Страница Статистики продвижения товаров.', {
    feature: 'Статистика продвижения товаров',
    environment: 'testing',
    params: {
        user: 'Пользователь',
    },
    story: (() => {
        const suites = USERS.map(user => {
            const vendor = _.get(user.permissions, [PERMISSIONS.modelbids.read, 0], 3301);
            const params = {
                vendor,
                routeParams: {vendor},
            };

            return makeSuite(`${user.description}.`, {
                story: userStory({
                    user,
                    params,
                    async onSetKadavrState({id}) {
                        switch (id) {
                            // Кейс с отключённой услугой продвижения товаров
                            case 'vendor_auto-1008':
                                return this.browser.setState('virtualVendor', [
                                    {
                                        vendorId: vendor,
                                        products: [],
                                    },
                                ]);

                            // Кейсы фильтрации по товарам
                            case 'vendor_auto-1374':
                            case 'vendor_auto-1375':
                            case 'vendor_auto-1376':
                                await this.browser.setState('vendorModelsPromotionGroups', groupsState);
                                await this.browser.setState('vendorsCategories', categoriesState);
                                await this.browser.setState('vendorModels', modelsState);

                                return this.browser.setState('vendorsModelsPromotionStatistics', initialState);

                            default:
                                return this.browser.setState('vendorsModelsPromotionStatistics', initialState);
                        }
                    },
                    pageObjects: {
                        logo: 'Logo',
                        footer: 'Footer',
                        page: 'ModelsPromotionStatisticsPage',
                    },
                    suites: {
                        common: [
                            {
                                suite: 'Page/title',
                                params: {
                                    title: 'Статистика продвижения товаров',
                                },
                            },
                            {
                                suite: 'ModelsPromotionStatistics/unavailable',
                                meta: {
                                    environment: 'kadavr',
                                },
                                params: {
                                    title: 'Статистика недоступна',
                                    text:
                                        'Мы не можем показать статистику, потому что вы ещё не продвигали товары ' +
                                        'на Маркете. Начать продвижение',
                                },
                                pageObjects: {
                                    panel: 'InfoPanel',
                                },
                            },
                            {
                                suite: 'Link',
                                suiteName: 'Ссылка при переходе по кнопке [Назначить ставки]',
                                meta: {
                                    id: 'vendor_auto-1009',
                                    issue: 'VNDFRONT-3182',
                                },
                                params: {
                                    url: buildUrl(ROUTE_NAMES.MODELS_PROMOTION, {vendor}),
                                    caption: 'Назначить ставки',
                                    comparison: {
                                        skipHostname: true,
                                    },
                                },
                                pageObjects: {
                                    link() {
                                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                        return this.createPageObject('Link', this.page, this.page.bidsButton);
                                    },
                                },
                            },
                            {
                                suite: 'Link',
                                suiteName: 'Кнопка [Сформировать отчёт]',
                                meta: {
                                    issue: 'VNDFRONT-3871',
                                    id: 'vendor_auto-1035',
                                },
                                params: {
                                    url: buildUrl(ROUTE_NAMES.MODELS_PROMOTION_STATISTICS_EXPORT, {vendor}),
                                    caption: 'Сформировать отчёт',
                                    comparison: {
                                        skipHostname: true,
                                    },
                                },
                                pageObjects: {
                                    link() {
                                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                        return this.createPageObject('Link', this.page, this.page.exportButton);
                                    },
                                },
                            },
                            {
                                suite: 'ModelsPromotionStatistics/modelsTab',
                                pageObjects: {
                                    tabGroup() {
                                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                        return this.createPageObject('TabGroupLevitan', this.page);
                                    },
                                    filters() {
                                        return this.createPageObject(
                                            'Filters',
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            this.page,
                                            ModelsPromotionStatisticsFilters.root,
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
