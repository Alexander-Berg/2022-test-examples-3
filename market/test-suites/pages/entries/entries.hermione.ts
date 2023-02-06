'use strict';

import {mergeSuites, makeSuite, PageObject} from 'ginny';

import USERS from 'spec/lib/constants/users/users';
import makeUserStory from 'spec/hermione/lib/helpers/userStory';
import initialState from 'spec/lib/page-mocks/entries.json';
// @ts-expect-error(TS2306) найдено в рамках VNDFRONT-4532
import ROUTE_NAMES from 'app/constants/routeNames';
// @ts-expect-error(TS2306) найдено в рамках VNDFRONT-4532
import PERMISSIONS from 'app/constants/permissions';

import editInitialState from './editInitialState.json';
import createFromEntryInitialState from './createFromEntryInitialState.json';

const vendor = 3301;
const userStory = makeUserStory(ROUTE_NAMES.ENTRIES);
const EntryPO = PageObject.get('Entry');
const CampaignsFormPO = PageObject.get('CampaignsForm');

export default makeSuite('Страница Заявок.', {
    story: (() => {
        // @ts-expect-error(TS7034) найдено в рамках VNDFRONT-4580
        const suites = [];

        const EntryContainer = {
            suite: 'EntryContainer',
            meta: {
                environment: 'kadavr',
            },
            params: {
                searchText: '$',
                searchItemsCount: 1,
                acceptedItemsCount: 7,
                cancelledItemsCount: 7,
                newItemsCount: 19,
                inWorkItemsCount: 7,
                onlyMyItemsCount: 1,
            },
        };

        const PageTitle = {
            suite: 'Page/title',
            params: {
                title: 'Заявки',
            },
        };

        const Entry = {
            suite: 'Entry',
            pageObjects: {
                // @ts-expect-error(TS7023) найдено в рамках VNDFRONT-4580
                item() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject(
                        'Entry',
                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        this.list,
                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        this.list.getItemByIndex(0),
                    );
                },
                form: 'Form',
                // @ts-expect-error(TS7023) найдено в рамках VNDFRONT-4580
                campaignsForm() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('FinalForm', this.browser, CampaignsFormPO.root);
                },
                // @ts-expect-error(TS7023) найдено в рамках VNDFRONT-4580
                checkbox() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('CheckboxB2b', this.campaignsForm);
                },
            },
            hooks: {
                async beforeEach() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    await this.allure.runStep('Ожидаем появления списка заявок', () =>
                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        this.list.waitForExist(),
                    );
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    await this.list.waitForLoading();
                },
            },
        };

        USERS.forEach(user =>
            suites.push(
                makeSuite(`${user.description} без вендора.`, {
                    story: userStory({
                        user,
                        meta: {
                            feature: 'Заявки',
                            environment: 'kadavr',
                        },
                        pageObjects: {
                            logo: 'Logo',
                            list() {
                                // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                return this.createPageObject('ListContainer').setItemSelector(EntryPO.root);
                            },
                        },
                        onSetKadavrState({id}) {
                            switch (id) {
                                case 'vendor_auto-39':
                                case 'vendor_auto-245':
                                case 'vendor_auto-246':
                                    return this.browser.setState('vendorsEntries', editInitialState);
                                case 'vendor_auto-247':
                                case 'vendor_auto-248':
                                    return this.browser.setState('vendorsEntries', createFromEntryInitialState);
                                default:
                                    return this.browser.setState('vendorsEntries', initialState);
                            }
                        },
                        suites: {
                            common: [
                                PageTitle,
                                // @ts-expect-error(TS2322) найдено в рамках VNDFRONT-4580
                                EntryContainer,
                                {
                                    suite: 'Bell/__unavailable',
                                    meta: {
                                        environment: 'kadavr',
                                    },
                                    pageObjects: {
                                        bell() {
                                            return this.createPageObject('Bell');
                                        },
                                    },
                                },
                            ],
                            byPermissions: {
                                [PERMISSIONS.entries.write]: Entry,
                                [PERMISSIONS.entries.read]: {
                                    suite: 'VendorsSearch',
                                    suiteName: 'Саджест в шапке приложения для менеджера.',
                                    meta: {
                                        issue: 'VNDFRONT-3898',
                                        id: 'vendor_auto-650',
                                    },
                                    params: {
                                        expectedCount: 20,
                                        withElementClick: true,
                                        checkPlaceholder: true,
                                    },
                                },
                            },
                        },
                    }),
                }),
            ),
        );

        USERS.forEach(user =>
            suites.push(
                makeSuite(`${user.description} с вендором.`, {
                    story: userStory({
                        user,
                        params: {
                            vendor,
                            routeParams: {vendor},
                        },
                        /* eslint-disable-next-line max-len */
                        checkPageAvailability: false, // не будем повторно прогонять кейсы "Страница доступна/недоступна"
                        meta: {
                            feature: 'Заявки',
                            environment: 'kadavr',
                        },
                        pageObjects: {
                            logo: 'Logo',
                            list() {
                                // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                return this.createPageObject('ListContainer').setItemSelector(EntryPO.root);
                            },
                        },
                        onSetKadavrState({id}) {
                            switch (id) {
                                case 'vendor_auto-39':
                                case 'vendor_auto-245':
                                case 'vendor_auto-246':
                                    return this.browser.setState('vendorsEntries', editInitialState);
                                case 'vendor_auto-247':
                                case 'vendor_auto-248':
                                    return this.browser.setState('vendorsEntries', createFromEntryInitialState);
                                default:
                                    return this.browser.setState('vendorsEntries', initialState);
                            }
                        },
                        suites: {
                            // @ts-expect-error(TS2322) найдено в рамках VNDFRONT-4580
                            common: [PageTitle, EntryContainer],
                            byPermissions: {
                                [PERMISSIONS.entries.write]: Entry,
                            },
                        },
                    }),
                }),
            ),
        );

        // @ts-expect-error(TS7005) найдено в рамках VNDFRONT-4580
        return mergeSuites(...suites);
    })(),
});
