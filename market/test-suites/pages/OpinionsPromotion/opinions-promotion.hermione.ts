'use strict';

import _ from 'lodash';
import {mergeSuites, makeSuite, PageObject} from 'ginny';

import USERS from 'spec/lib/constants/users/users';
import makeUserStory from 'spec/hermione/lib/helpers/userStory';
import {excludePermissions} from 'spec/hermione/lib/helpers/permissions';
import buildUrl from 'spec/lib/helpers/buildUrl';
// @ts-expect-error(TS2306) найдено в рамках VNDFRONT-4532
import PERMISSIONS from 'app/constants/permissions';
// @ts-expect-error(TS2306) найдено в рамках VNDFRONT-4532
import ROUTE_NAMES from 'app/constants/routeNames';

import emptyStatsState from './emptyStats.json';
import categoriesState from './categories.json';
import initialStatsState from './initialStats.json';
import opinionsPromotionState from './opinionsPromotion.json';
import withoutTargetsStatsState from './withoutTargetsStats.json';
import targetOpinionsAchievedState from './targetOpinionsAchieved.json';
import disabledOpinionsPromotionState from './disabledOpinionsPromotion.json';

const OpinionsPromotionListItem = PageObject.get('OpinionsPromotionListItem');
const ModelSnippetPromotion = PageObject.get('ModelSnippetPromotion');
const Link = PageObject.get('Link');

const userStory = makeUserStory(ROUTE_NAMES.OPINIONS_PROMOTION);

export default makeSuite('Страница Отзывы за баллы.', {
    story: (() => {
        const suites = USERS.map(user => {
            const vendor = _.get(user.permissions, [PERMISSIONS.paidOpinions.read, 0], 3301);
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
                        popup: 'PopupB2b',
                        filters: 'Filters',
                        agitationStats: 'OpinionsPromotionAgitationStats',

                        /**
                         * Таблица с товарами с отзывами за баллы
                         */
                        list() {
                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                            return this.createPageObject('OpinionsPromotionList').setItemSelector(
                                OpinionsPromotionListItem.root,
                            );
                        },
                    },
                    async onSetKadavrState({id}) {
                        const {isManager} = this.params;

                        switch (id) {
                            /**
                             * Кейсы на блок статистики
                             */
                            case 'vendor_auto-1308':
                                return this.browser.setState('vendorsOpinionsPromotionAgitationStats', emptyStatsState);
                            case 'vendor_auto-1309':
                                return this.browser.setState(
                                    'vendorsOpinionsPromotionAgitationStats',
                                    withoutTargetsStatsState,
                                );
                            case 'vendor_auto-1310':
                                return this.browser.setState(
                                    'vendorsOpinionsPromotionAgitationStats',
                                    initialStatsState,
                                );

                            /**
                             * Кейсы на просмотр, фильтрацию и масс-селект списка товаров с отзывами за баллы
                             */
                            case 'vendor_auto-1313':
                            case 'vendor_auto-1314':
                            case 'vendor_auto-1315':
                            case 'vendor_auto-1316':
                            case 'vendor_auto-1317':
                            case 'vendor_auto-1318':
                            case 'vendor_auto-1334':
                            case 'vendor_auto-1335':
                            case 'vendor_auto-1336':
                            case 'vendor_auto-1337':
                            case 'vendor_auto-1338':
                            case 'vendor_auto-1339':
                                await this.browser.setState('vendorsCategories', categoriesState);

                                await this.browser.setState(
                                    'vendorsOpinionsPromotionAgitationStats',
                                    initialStatsState,
                                );

                                return this.browser.setState('vendorsOpinionsPromotion', opinionsPromotionState);
                            case 'vendor_auto-1333':
                                return this.browser.setState('vendorsOpinionsPromotion', targetOpinionsAchievedState);

                            /**
                             * Проставление баллов и целей
                             */
                            case 'vendor_auto-1327':
                            case 'vendor_auto-1328':
                            case 'vendor_auto-1329':
                                await this.browser.setState('vendorsOpinionsPromotion', disabledOpinionsPromotionState);

                                return this.browser.setState('vendorsOpinionsPromotionAgitationStats', emptyStatsState);
                            case 'vendor_auto-1330':
                            case 'vendor_auto-1332':
                            case 'vendor_auto-1341':
                                await this.browser.setState(
                                    'vendorsOpinionsPromotionAgitationStats',
                                    initialStatsState,
                                );

                                return this.browser.setState('vendorsOpinionsPromotion', opinionsPromotionState);

                            // Кейс с отключённой услугой
                            case 'vendor_auto-1301':
                                return this.browser.setState('virtualVendor', [
                                    {
                                        vendorId: vendor,
                                        products: [],
                                    },
                                ]);

                            case 'vendor_auto-1305':
                                await this.browser.setState('vendorProductsData', {
                                    list: {
                                        paidOpinions: {},
                                    },
                                });

                                return this.browser.setState('vendorCurrentUserInfo', {
                                    isManager,
                                });

                            default:
                                await this.browser.setState('vendorsOpinionsPromotion', targetOpinionsAchievedState);

                                return this.browser.setState(
                                    'vendorsOpinionsPromotionAgitationStats',
                                    initialStatsState,
                                );
                        }
                    },
                    suites: {
                        common: [
                            'OpinionsPromotion/hints',
                            'OpinionsPromotion/filters',
                            'OpinionsPromotion/stats',
                            {
                                suite: 'Page/title',
                                params: {
                                    title: 'Отзывы за баллы',
                                },
                            },
                            {
                                suite: 'Link',
                                suiteName: 'Ссылка на справку в заголовке страницы',
                                meta: {
                                    id: 'vendor_auto-1304',
                                    issue: 'VNDFRONT-4004',
                                    environment: 'testing',
                                },
                                params: {
                                    url: buildUrl('external:help:opinions-promotion'),
                                    caption: 'Справка',
                                    target: '_blank',
                                    external: true,
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
                                hooks: {
                                    beforeEach() {
                                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                        return this.allure.runStep('Ожидаем появления заголовка страницы', () =>
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            this.pageHeading.waitForExist(),
                                        );
                                    },
                                },
                            },
                            {
                                suite: 'Link',
                                suiteName: 'Ссылка на информацию об услуге «Отзывы за баллы» в рекламной растяжке',
                                meta: {
                                    id: 'vendor_auto-1304',
                                    issue: 'VNDFRONT-4004',
                                    environment: 'testing',
                                },
                                params: {
                                    url: buildUrl('external:help:opinions-promotion'),
                                    caption: 'Узнать больше',
                                    target: '_blank',
                                    external: true,
                                },
                                pageObjects: {
                                    cover() {
                                        return this.createPageObject('Cover');
                                    },
                                    link() {
                                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                        return this.createPageObject('Link', this.cover, 'a');
                                    },
                                },
                                hooks: {
                                    beforeEach() {
                                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                        return this.allure.runStep('Ожидаем появления рекламной растяжки', () =>
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            this.cover.waitForExist(),
                                        );
                                    },
                                },
                            },
                            {
                                suite: 'InfoPanel',
                                suiteName: 'Уведомление об отключённой услуге',
                                meta: {
                                    feature: 'Отзывы за баллы',
                                    issue: 'VNDFRONT-3987',
                                    id: 'vendor_auto-1301',
                                    environment: 'kadavr',
                                },
                                params: {
                                    title: 'Услуга недоступна',
                                    text: 'Подключите услугу "Отзывы за баллы", чтобы получить доступ к странице.',
                                },
                                pageObjects: {
                                    panel() {
                                        return this.createPageObject('InfoPanel');
                                    },
                                },
                            },
                        ],
                        byPermissions: {
                            [PERMISSIONS.paidOpinions.write]: [
                                {
                                    suite: 'OpinionsPromotion/balance/placementChange',
                                    pageObjects: {
                                        balance() {
                                            return this.createPageObject('OpinionsPromotionBalance');
                                        },
                                    },
                                },
                                {
                                    suite: 'OpinionsPromotion/listItem',
                                    pageObjects: {
                                        item() {
                                            return this.createPageObject(
                                                'OpinionsPromotionListItem',
                                                // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                                this.list,
                                                // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                                this.list.getItemByIndex(0),
                                            );
                                        },
                                        nextItem() {
                                            return this.createPageObject(
                                                'OpinionsPromotionListItem',
                                                // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                                this.list,
                                                // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                                this.list.getItemByIndex(1),
                                            );
                                        },
                                        thirdItem() {
                                            return this.createPageObject(
                                                'OpinionsPromotionListItem',
                                                // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                                this.list,
                                                // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                                this.list.getItemByIndex(2),
                                            );
                                        },
                                    },
                                    hooks: {
                                        async beforeEach() {
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            await this.allure.runStep('Ожидаем появления списка товаров', () =>
                                                // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                                this.list.waitForExist(),
                                            );

                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            await this.list.waitForLoading();
                                        },
                                    },
                                },
                                'OpinionsPromotion/massSelect',
                            ],
                            [PERMISSIONS.paidOpinions.read]: {
                                suite: 'Link',
                                suiteName: 'Ссылка в названии модели',
                                meta: {
                                    id: 'vendor_auto-1331',
                                    issue: 'VNDFRONT-4001',
                                    environment: 'kadavr',
                                },
                                params: {
                                    url: buildUrl('external:market-product', {
                                        modelId: 2107270,
                                    }),
                                    caption: 'Haier HSU-18LEA03',
                                    target: '_blank',
                                    external: true,
                                },
                                pageObjects: {
                                    modelSnippetPromotion() {
                                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                        return this.createPageObject('ModelSnippetPromotion', this.item);
                                    },
                                    link() {
                                        return this.createPageObject(
                                            'Link',
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            this.modelSnippetPromotion,
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            `${ModelSnippetPromotion.info} ${Link.root}`,
                                        );
                                    },
                                },
                                hooks: {
                                    async beforeEach() {
                                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                        await this.allure.runStep('Ожидаем появления списка товаров', () =>
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            this.list.waitForExist(),
                                        );

                                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                        await this.list.waitForLoading();
                                    },
                                },
                            },
                            [excludePermissions(PERMISSIONS.paidOpinions.write)]: {
                                suite: 'OpinionsPromotion/balance/placementUnavailable',
                                pageObjects: {
                                    balance() {
                                        return this.createPageObject('OpinionsPromotionBalance');
                                    },
                                },
                            },
                        },
                    },
                }),
            });
        });

        return mergeSuites(...suites);
    })(),
});
