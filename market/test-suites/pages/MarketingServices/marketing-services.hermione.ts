'use strict';

import {get} from 'lodash';
import {mergeSuites, makeSuite, PageObject} from 'ginny';

import USERS from 'spec/lib/constants/users/users';
import makeUserStory from 'spec/hermione/lib/helpers/userStory';
import {combinePermissions} from 'spec/hermione/lib/helpers/permissions';
// @ts-expect-error(TS2306) найдено в рамках VNDFRONT-4532
import ROUTE_NAMES from 'app/constants/routeNames';
// @ts-expect-error(TS2306) найдено в рамках VNDFRONT-4532
import PERMISSIONS from 'app/constants/permissions';

import offerDocumentsCutoffData from './offerDocumentsCutoff.json';
import categoriesData from './categories.json';
import noCutoffData from './noCutoff.json';
import {getMarketingCampaignsMock} from './marketingCampaignsMock';

const userStory = makeUserStory(ROUTE_NAMES.MARKETING_SERVICES);

const MarketingServicesListItem = PageObject.get('MarketingServicesListItem');

export default makeSuite('Страница Маркетинговых услуг.', {
    feature: 'Маркетинговые услуги',
    story: (() => {
        const suites = USERS.map(user => {
            const vendor = get(user.permissions, [PERMISSIONS.marketingBanners.read, 0], 3301);
            const params = {
                vendor,
                routeParams: {vendor},
            };

            return makeSuite(`${user.description}`, {
                story: userStory({
                    user,
                    params,
                    async onSetKadavrState({id}) {
                        switch (id) {
                            // Кейс с истекшим сроком документов
                            case 'vendor_auto-1120':
                                return this.browser.setState('vendorProductsData', offerDocumentsCutoffData);

                            // Кейсы с доступом к конкретной услуге и доступом на страницу
                            case 'vendor_auto-1136':
                            case 'vendor_auto-1137':
                                return this.browser.setState('vendorProductsData', noCutoffData);

                            // Кейс с пагинацией
                            case 'vendor_auto-1160':
                                return this.browser.setState(
                                    'vendorMarketingCampaigns',
                                    getMarketingCampaignsMock({length: 25}),
                                );

                            // Кейсы с подсказкой с бюджетом кампании, подтверждением и отменой удаления кампании
                            case 'vendor_auto-1161':
                            case 'vendor_auto-1158':
                            case 'vendor_auto-1159':
                                return this.browser.setState(
                                    'vendorMarketingCampaigns',
                                    getMarketingCampaignsMock({length: 1}),
                                );

                            case 'vendor_auto-1152':
                            case 'vendor_auto-1153':
                            case 'vendor_auto-1154':
                                return this.browser.setState(
                                    'vendorMarketingCampaigns',
                                    getMarketingCampaignsMock({length: 1, approvedBy: null}),
                                );

                            // Кейсы с редактированием созданной кампании
                            case 'vendor_auto-1146':
                            case 'vendor_auto-1147':
                            case 'vendor_auto-1148':
                            case 'vendor_auto-1149':
                            case 'vendor_auto-1150':
                            case 'vendor_auto-1151':
                                await this.browser.setState('vendorsCategories', categoriesData);

                                return this.browser.setState(
                                    'vendorMarketingCampaigns',
                                    getMarketingCampaignsMock({length: 1}),
                                );

                            case 'vendor_auto-1214':
                            case 'vendor_auto-1215':
                                await this.browser.setState('vendorsCategories', categoriesData);

                                return this.browser.setState(
                                    'vendorMarketingCampaigns',
                                    getMarketingCampaignsMock({
                                        length: 1,
                                        approvedBy: null,
                                        status: 'SCHEDULED',
                                    }),
                                );

                            default:
                        }

                        // Кейс с отключенными маркетинговыми услугами
                        if (id === 'vendor_auto-1119') {
                            return this.browser.setState('virtualVendor', [
                                {
                                    vendorId: vendor,
                                    products: [],
                                },
                            ]);
                        }
                    },
                    pageObjects: {
                        logo: 'Logo',
                        footer: 'Footer',
                        list() {
                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                            return this.createPageObject('PagedList').setItemSelector(MarketingServicesListItem.root);
                        },
                    },
                    suites: {
                        common: [
                            {
                                suite: 'Page/title',
                                params: {
                                    title: 'Маркетинговые услуги',
                                },
                            },
                            'MarketingServices/suspended',
                            'MarketingServices/unactivated',
                            'MarketingServices/campaignsList',
                            {
                                suite: 'MarketingServices/budgetHint',
                                hooks: {
                                    async beforeEach() {
                                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                        await this.list
                                            .isExisting()
                                            .should.eventually.be.equal(true, 'Список услуг отображается');

                                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                        return this.list.waitForLoading();
                                    },
                                },
                            },
                            {
                                suite: 'Link',
                                suiteName: 'Ссылка на описание (Подробно об услугах)',
                                meta: {
                                    issue: 'VNDFRONT-3292',
                                    id: 'vendor_auto-1137',
                                    environment: 'kadavr',
                                },
                                params: {
                                    url: 'https://yadi.sk/d/Zw_dHUglNOQ0nA',
                                    caption: 'Подробно об услугах',
                                    target: '_blank',
                                    external: true,
                                },
                                pageObjects: {
                                    link() {
                                        return this.createPageObject(
                                            'Link',
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            this.browser,
                                            'a=Подробно об услугах',
                                        );
                                    },
                                },
                            },
                        ],
                        byPermissions: {
                            [combinePermissions(
                                PERMISSIONS.marketingBanners.write,
                                PERMISSIONS.marketingLandings.write,
                                PERMISSIONS.marketingPromo.write,
                                PERMISSIONS.marketingEmail.write,
                                PERMISSIONS.marketingShopInShop.write,
                                PERMISSIONS.marketingProductPlacement.write,
                                PERMISSIONS.marketingLogo.write,
                                PERMISSIONS.marketingTv.write,
                                PERMISSIONS.marketingExternalPlatforms.write,
                            )]: [
                                {
                                    suite: 'MarketingServices/edit',
                                    hooks: {
                                        async beforeEach() {
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            await this.list
                                                .isExisting()
                                                .should.eventually.be.equal(true, 'Список услуг отображается');

                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            return this.list.waitForLoading();
                                        },
                                    },
                                },
                                {
                                    suite: 'MarketingServices/approve',
                                    hooks: {
                                        async beforeEach() {
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            await this.list
                                                .isExisting()
                                                .should.eventually.be.equal(true, 'Список услуг отображается');

                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            return this.list.waitForLoading();
                                        },
                                    },
                                },
                            ],
                            [PERMISSIONS.entries.write]: [
                                'MarketingServices/createCampaignButton',
                                {
                                    suite: 'MarketingServices/delete',
                                    hooks: {
                                        async beforeEach() {
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            await this.list
                                                .isExisting()
                                                .should.eventually.be.equal(true, 'Список услуг отображается');

                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            return this.list.waitForLoading();
                                        },
                                    },
                                },
                            ],
                        },
                    },
                }),
            });
        });

        return mergeSuites(...suites);
    })(),
});
