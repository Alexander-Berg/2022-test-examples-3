'use strict';

import {mergeSuites, makeSuite, PageObject} from 'ginny';

import USERS from 'spec/lib/constants/users/users';
import makeUserStory from 'spec/hermione/lib/helpers/userStory';
import initialState from 'spec/lib/page-mocks/moderation.json';
import {combinePermissions, excludePermissions} from 'spec/hermione/lib/helpers/permissions';
// @ts-expect-error(TS2306) найдено в рамках VNDFRONT-4532
import ROUTE_NAMES from 'app/constants/routeNames';
// @ts-expect-error(TS2306) найдено в рамках VNDFRONT-4532
import PERMISSIONS from 'app/constants/permissions';

import revertedRequestState from './revertedRequest.json';
import requestWithoutOldBrandDataState from './requestWithoutOldBrandData.json';

const userStory = makeUserStory(ROUTE_NAMES.MODERATION);
const BrandEditRequest = PageObject.get('BrandEditRequest');

export default makeSuite('Страница Модерации.', {
    story: (() => {
        const suites = USERS.map(user =>
            makeSuite(`${user.description}.`, {
                story: userStory({
                    user,
                    // @ts-expect-error(TS2739) найдено в рамках VNDFRONT-4580
                    params: {},
                    meta: {
                        feature: 'Модерация',
                        environment: 'kadavr',
                    },
                    pageObjects: {
                        logo: 'Logo',
                        footer: 'Footer',
                        filters: 'ModerationFilters',
                        moderationList: 'ModerationList',
                        brandEditRequest: 'BrandEditRequest',
                        list() {
                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                            return this.createPageObject('ListContainer').setItemSelector(BrandEditRequest.root);
                        },
                    },
                    // @ts-expect-error(TS7031) найдено в рамках VNDFRONT-4580
                    onSetKadavrState({id}) {
                        switch (id) {
                            case 'vendor_auto-289':
                            case 'vendor_auto-290':
                            case 'vendor_auto-771':
                                return this.browser.setState('vendorsBrandEditRequests', initialState);

                            /**
                             * Кейс с невозможностью подачи корректирующей заявки
                             */
                            case 'vendor_auto-292':
                                return this.browser.setState(
                                    'vendorsBrandEditRequests',
                                    requestWithoutOldBrandDataState,
                                );

                            /**
                             * Кейсы с корректирующими заявками
                             */
                            case 'vendor_auto-291':
                            case 'vendor_auto-293':
                                return this.browser.setState('vendorsBrandEditRequests', revertedRequestState);
                            default:
                                break;
                        }
                    },
                    suites: {
                        common: [
                            {
                                suite: 'Page/title',
                                params: {
                                    title: 'Модерация',
                                },
                            },
                            'Moderation',
                            {
                                suite: 'Bell/__unavailable',
                                pageObjects: {
                                    bell: 'Bell',
                                },
                            },
                        ],
                        byPermissions: {
                            [PERMISSIONS.moderation.write]: {
                                suite: 'BrandEditRequest',
                                pageObjects: {
                                    item() {
                                        return this.createPageObject(
                                            'BrandEditRequest',
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            this.list,
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            this.list.getItemByIndex(0),
                                        );
                                    },
                                    checkbox() {
                                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                        return this.createPageObject('CheckboxLevitan', this.item);
                                    },
                                    saveButton() {
                                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                        return this.createPageObject('ButtonLevitan', this.item, this.item.saveButton);
                                    },
                                    closeButton() {
                                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                        return this.createPageObject('ButtonLevitan', this.item, this.item.closeButton);
                                    },
                                    revertButton() {
                                        return this.createPageObject(
                                            'ButtonLevitan',
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            this.item,
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            this.item.revertButton,
                                        );
                                    },
                                },
                                hooks: {
                                    async beforeEach() {
                                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                        await this.allure.runStep(
                                            'Ожидаем появления списка заявок',
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            () => this.list.waitForExist(),
                                        );
                                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                        await this.list.waitForLoading();
                                    },
                                },
                            },
                            [combinePermissions(
                                PERMISSIONS.moderation.read,
                                excludePermissions(PERMISSIONS.entries.read),
                            )]: {
                                suite: 'Moderation/vendorsSuggestUnavailable',
                            },
                        },
                    },
                }),
            }),
        );

        return mergeSuites(...suites);
    })(),
});
