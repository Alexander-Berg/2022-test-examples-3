'use strict';

import moment from 'moment';
import _ from 'lodash';
import {mergeSuites, makeSuite, PageObject} from 'ginny';

import USERS from 'spec/lib/constants/users/users';
import makeUserStory from 'spec/hermione/lib/helpers/userStory';
import initialStateShort from 'spec/lib/page-mocks/modelOpinions.json';
// @ts-expect-error(TS2306) найдено в рамках VNDFRONT-4532
import PERMISSIONS from 'app/constants/permissions';
// @ts-expect-error(TS2306) найдено в рамках VNDFRONT-4532
import ROUTE_NAMES from 'app/constants/routeNames';

import initialState from './initialState';
import modelsState from './models.json';

const userStory = makeUserStory(ROUTE_NAMES.MODELS_OPINIONS);
const ModelOpinionsHeader = PageObject.get('ModelOpinionsHeader');
const ModelOpinionsFilters = PageObject.get('ModelOpinionsFilters');
const Opinion = PageObject.get('Opinion');

export default makeSuite('Страница Отзывы.', {
    story: (() => {
        const suites = USERS.map(user => {
            const vendor = _.get(user.permissions, [PERMISSIONS.opinions.read, 0], 3301);
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
                            case 'vendor_auto-512':
                                // кейс на ограничение бесплатных услуг
                                return this.browser.setState('virtualVendor', [
                                    {
                                        vendorId: vendor,
                                        hasFreeProducts: false,
                                    },
                                ]);

                            case 'vendor_auto-158':
                                return this.browser.setState('vendorsModelOpinions', initialState);

                            // Фильтрация по моделям
                            case 'vendor_auto-133':
                                await this.browser.setState('vendorModels', modelsState);

                                return this.browser.setState('vendorsModelOpinions', initialStateShort);

                            default:
                                return this.browser.setState('vendorsModelOpinions', initialStateShort);
                        }
                    },
                    pageObjects: {
                        logo: 'Logo',
                        footer: 'Footer',
                        modelOpinions: 'ModelOpinions',
                        firstOpinion() {
                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                            return this.createPageObject('Opinion', this.browser, this.modelOpinions.getItemByIndex(0));
                        },
                        list() {
                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                            return this.createPageObject('ListContainer').setItemSelector(Opinion.root);
                        },
                    },
                    suites: {
                        common: [
                            {
                                suite: 'RestrictPanel',
                                pageObjects: {
                                    panel() {
                                        return this.createPageObject('RestrictPanel');
                                    },
                                },
                            },
                            {
                                suite: 'SuggestFilter',
                                suiteName: 'Фильтр по моделям.',
                                meta: {
                                    issue: 'VNDFRONT-1255',
                                    environment: 'kadavr',
                                    feature: 'Отзывы на модель',
                                    id: 'vendor_auto-133',
                                },
                                hooks: {
                                    beforeEach() {
                                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                        return this.browser.yaWaitForPageObject(ModelOpinionsHeader);
                                    },
                                },
                                params: {
                                    url: `/vendors/${vendor}/opinions/models/[0-9]+`,
                                    text: 'Телевизор Samsung T32E310EX',
                                },
                                pageObjects: {
                                    suggest() {
                                        return this.createPageObject(
                                            'Suggest',
                                            // @ts-expect-error(TS2345) найдено в рамках VNDFRONT-4580
                                            this.browser,
                                            ModelOpinionsHeader.root,
                                        );
                                    },
                                    popup: 'PopupB2b',
                                },
                            },
                            {
                                suite: 'ModelOpinions',
                                pageObjects: {
                                    list() {
                                        return this.createPageObject('ModelOpinions');
                                    },
                                },
                            },
                            {
                                suite: 'Filters/__select',
                                suiteName: 'Сортировка старые/новые',
                                meta: {
                                    issue: 'VNDFRONT-1843',
                                    feature: 'Отзывы на модель',
                                    id: 'vendor_auto-134',
                                },
                                params: {
                                    initialItemText: '30 июля 2018 г.',
                                    expectedItemText: '11 апреля 2018 г.',
                                    initialFilterText: 'новые',
                                    expectedFilterText: 'старые',
                                    queryParamName: 'sortBy',
                                    queryParamValue: 'ASC',
                                    async getItemText() {
                                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                        await this.firstOpinion.waitForExist();
                                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                        return this.firstOpinion.getCreationDate();
                                    },
                                },
                                hooks: {
                                    beforeEach() {
                                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                        return this.select.waitForExist();
                                    },
                                },
                                pageObjects: {
                                    select() {
                                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                        return this.createPageObject('SelectB2b', ModelOpinionsFilters.sort);
                                    },
                                    popup: 'PopupB2b',
                                },
                            },
                            {
                                suite: 'Filters/__period',
                                meta: {
                                    issue: 'VNDFRONT-1847',
                                    feature: 'Отзывы на модель',
                                    id: 'vendor_auto-137',
                                },
                                params: {
                                    fromParamName: 'from',
                                    fromParamValue: '2018-04-12',
                                    toParamName: 'to',
                                    toParamValue: '2018-07-01',
                                    initialItemsCount: 5,
                                    filteredItemsCount: 3,
                                },
                                pageObjects: {
                                    datePicker() {
                                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                        return this.createPageObject('DatePicker', ModelOpinionsFilters.range);
                                    },
                                },
                            },
                            {
                                suite: 'Filters/__reset',
                                meta: {
                                    id: 'vendor_auto-693',
                                    issue: 'VNDFRONT-3435',
                                    feature: 'Отзывы на модель',
                                },
                                params: {
                                    initialItemsCount: 5,
                                    filteredItemsCount: 0,
                                    async setFilters() {
                                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                        await this.list.waitForLoading();

                                        // Открываем попап
                                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                        await this.datePicker.open();

                                        // Выбираем предыдущий месяц
                                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                        await this.datePicker.selectPrevMonth();

                                        const currentMonth = moment();
                                        const prevMonth = moment().startOf('month').subtract(1, 'months');

                                        // Выбираем диапазон дат с первого числа предыдущего месяца по текущий день
                                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                        await this.datePicker.selectDate(prevMonth);
                                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                        await this.datePicker.selectDate(currentMonth);
                                    },
                                },
                                pageObjects: {
                                    datePicker() {
                                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                        return this.createPageObject('DatePicker', ModelOpinionsFilters.range);
                                    },
                                    resetFilters() {
                                        return this.createPageObject('ResetFilters', ModelOpinionsFilters.root);
                                    },
                                },
                            },
                            {
                                suite: 'Filters/__checkbox',
                                suiteName: 'Фильтр "Неотвеченные"',
                                meta: {
                                    issue: 'VNDFRONT-3451',
                                    feature: 'Отзывы на модель',
                                    id: 'vendor_auto-135',
                                },
                                params: {
                                    initialItemsCount: 5,
                                    filteredItemsCount: 3,
                                    paramName: 'withoutAnswer',
                                    paramValue: true,
                                },
                                pageObjects: {
                                    checkbox() {
                                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                        return this.createPageObject('CheckboxB2b', ModelOpinionsFilters.answered);
                                    },
                                },
                            },
                            {
                                suite: 'Page/title',
                                params: {
                                    title: 'Отзывы о товарах бренда',
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
