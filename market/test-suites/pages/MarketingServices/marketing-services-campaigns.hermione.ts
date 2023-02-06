'use strict';

import {get} from 'lodash';
import {mergeSuites, makeSuite, PageObject} from 'ginny';

import USERS from 'spec/lib/constants/users/users';
import makeUserStory from 'spec/hermione/lib/helpers/userStory';
import buildUrl from 'spec/lib/helpers/buildUrl';
import {combinePermissions} from 'spec/hermione/lib/helpers/permissions';
// @ts-expect-error(TS2306) найдено в рамках VNDFRONT-4532
import ROUTE_NAMES from 'app/constants/routeNames';
// @ts-expect-error(TS2306) найдено в рамках VNDFRONT-4532
import PERMISSIONS from 'app/constants/permissions';
import {isManager, getAllowedPermissions} from 'shared/permissions';

import categoriesData from './categories.json';
import noCutoffData from './noCutoff.json';

const userStory = makeUserStory(ROUTE_NAMES.MARKETING_SERVICES_CAMPAIGNS);

const MarketingServicesListItem = PageObject.get('MarketingServicesListItem');
const Link = PageObject.get('Link');
const ButtonLevitan = PageObject.get('ButtonLevitan');

export default makeSuite('Страница создания маркетинговой кампании.', {
    feature: 'Маркетинговые услуги',
    story: (() => {
        const suites = USERS.map(user => {
            const vendor = get(user.permissions, [PERMISSIONS.marketingBanners.read, 0], 3301);
            const permissionsByVendor = getAllowedPermissions(user.permissions, vendor);
            const params = {
                vendor,
                routeParams: {vendor},
            };

            return makeSuite(`${user.description}.`, {
                story: userStory({
                    user,
                    params,
                    async onSetKadavrState({id}) {
                        // Кейс с доступом к конкретной услуге и кейс с успешным созданием кампании
                        if (id === 'vendor_auto-1136' || id === 'vendor_auto-1144') {
                            await this.browser.setState('vendorsCategories', categoriesData);

                            return this.browser.setState('vendorProductsData', noCutoffData);
                        }
                    },
                    pageObjects: {
                        logo: 'Logo',
                        footer: 'Footer',
                    },
                    suites: {
                        common: [
                            {
                                suite: 'Page/title',
                                params: {
                                    title: 'Создать кампанию',
                                },
                            },
                            {
                                suite: 'Link',
                                suiteName: 'Ссылка по кнопке [Отмена]',
                                meta: {
                                    id: 'vendor_auto-1142',
                                    issue: 'VNDFRONT-3312',
                                    environment: 'kadavr',
                                },
                                params: {
                                    url: buildUrl(ROUTE_NAMES.MARKETING_SERVICES, {vendor}),
                                    caption: 'Отмена',
                                    comparison: {
                                        skipHostname: true,
                                    },
                                },
                                pageObjects: {
                                    link() {
                                        return this.createPageObject(
                                            'Link',
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            this.browser,
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            ButtonLevitan.getByText('Отмена'),
                                        );
                                    },
                                },
                            },
                            {
                                suite: 'Link',
                                suiteName: 'Ссылка "Все кампании"',
                                meta: {
                                    id: 'vendor_auto-1143',
                                    issue: 'VNDFRONT-3312',
                                    environment: 'kadavr',
                                },
                                params: {
                                    url: buildUrl(ROUTE_NAMES.MARKETING_SERVICES, {vendor}),
                                    caption: 'Все кампании',
                                    comparison: {
                                        skipHostname: true,
                                    },
                                },
                                pageObjects: {
                                    link() {
                                        return this.createPageObject(
                                            'Link',
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            this.browser,
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            Link.getByText('Все кампании'),
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
                                PERMISSIONS.entries.write,
                            )]: [
                                {
                                    suite: 'MarketingServicesCampaigns/create',
                                    params: {
                                        marketingServicesPageUrl: buildUrl(ROUTE_NAMES.MARKETING_SERVICES, {vendor}),
                                        serviceStatusText: isManager(permissionsByVendor)
                                            ? 'Необходимо подтверждение производителя'
                                            : 'Необходимо подтверждение менеджера',
                                    },
                                    pageObjects: {
                                        popup: 'PopupB2b',
                                        form: 'MarketingCampaignForm',
                                        campaignSelect() {
                                            return this.createPageObject(
                                                'SelectAdvanced',
                                                // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                                this.browser,
                                                // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                                this.form.getFieldByLabelText('Услуга', 'div'),
                                            );
                                        },
                                        categorySuggest() {
                                            return this.createPageObject(
                                                'Suggest',
                                                // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                                this.form,
                                                // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                                this.form.getFieldByLabelText('Категория', 'div'),
                                            );
                                        },
                                        businessModelSelect() {
                                            return this.createPageObject(
                                                'SelectAdvanced',
                                                // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                                this.browser,
                                                // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                                this.form.getFieldByLabelText('Бизнес модель', 'div'),
                                            );
                                        },
                                        datePicker() {
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            return this.createPageObject('DatePicker').setCustomToggler(
                                                // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                                this.form.getFieldByLabelText('Период проведения', 'div'),
                                            );
                                        },
                                        list() {
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            return this.createPageObject('PagedList').setItemSelector(
                                                MarketingServicesListItem.root,
                                            );
                                        },
                                        listItem() {
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            return this.createPageObject('MarketingServicesListItem', this.list);
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
