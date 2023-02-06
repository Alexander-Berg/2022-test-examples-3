'use strict';

import _ from 'lodash';
import {mergeSuites, makeSuite, PageObject} from 'ginny';

import USERS from 'spec/lib/constants/users/users';
import makeUserStory from 'spec/hermione/lib/helpers/userStory';
// @ts-expect-error(TS2306) найдено в рамках VNDFRONT-4532
import PERMISSIONS from 'app/constants/permissions';
// @ts-expect-error(TS2306) найдено в рамках VNDFRONT-4532
import ROUTE_NAMES from 'app/constants/routeNames';

import initialState from '../ModelsOpinions/initialState';

const userStory = makeUserStory(ROUTE_NAMES.MODEL_OPINIONS);
const ModelOpinionsFilters = PageObject.get('ModelOpinionsFilters');
const Opinion = PageObject.get('Opinion');
const BackLink = PageObject.get('BackLink');

export default makeSuite('Страница Отзывы на модель.', {
    story: (() => {
        const modelId = 1722193751;
        const suites = USERS.map(user => {
            const vendor = _.get(user.permissions, [PERMISSIONS.opinions.read, 0], 3301);
            const params = {
                vendor,
                routeParams: {vendor, modelId},
            };

            return makeSuite(`${user.description}.`, {
                story: userStory({
                    user,
                    params,
                    onSetKadavrState({id}) {
                        const {browser} = this;

                        switch (id) {
                            case 'vendor_auto-512':
                                // кейс на ограничение бесплатных услуг
                                return this.browser.setState('virtualVendor', [
                                    {
                                        vendorId: vendor,
                                        hasFreeProducts: false,
                                    },
                                ]);
                            default:
                                return browser.setState('vendorsModelOpinions', initialState);
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
                                suite: 'ModelSnippet',
                                pageObjects: {
                                    snippet() {
                                        return this.createPageObject('ModelSnippet');
                                    },
                                },
                            },
                            {
                                suite: 'RestrictPanel',
                                pageObjects: {
                                    panel() {
                                        return this.createPageObject('RestrictPanel');
                                    },
                                },
                            },
                            {
                                suite: 'Link',
                                suiteName: 'Ссылка "Обратно к списку"',
                                meta: {
                                    id: 'vendor_auto-153',
                                    environment: 'kadavr',
                                    feature: 'Отзывы на модель',
                                },
                                params: {
                                    url: `/vendors/${vendor}/opinions`,
                                    caption: 'Обратно к списку',
                                    comparison: {
                                        skipHostname: true,
                                    },
                                },
                                pageObjects: {
                                    link() {
                                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                        return this.createPageObject('Link', this.browser, BackLink.root);
                                    },
                                },
                            },
                            {
                                suite: 'Link/ExternalLink',
                                suiteName: 'Ссылка на прикрепленное изображение',
                                meta: {
                                    id: 'vendor_auto-166',
                                    environment: 'kadavr',
                                    feature: 'Отзывы на модель',
                                },
                                params: {
                                    caption: '',
                                    url:
                                        'https://avatars.mdst.yandex.net/get-market-ugc/' +
                                        '3261/2a0000016165e416b4e82dd25b826d51e304/1920-1920',
                                },
                                pageObjects: {
                                    link() {
                                        return this.createPageObject(
                                            'Link',
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            this.browser,
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            this.firstOpinion.firstAttachedImageLink,
                                        );
                                    },
                                },
                                hooks: {
                                    beforeEach() {
                                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                        return this.list.waitForLoading();
                                    },
                                },
                            },
                            {
                                suite: 'Filters/__select',
                                suiteName: 'Сортировка старые/новые',
                                meta: {
                                    issue: 'VNDFRONT-1843',
                                    feature: 'Отзывы на модель',
                                    id: 'vendor_auto-147',
                                },
                                params: {
                                    initialItemText: '30 июля 2018 г.',
                                    expectedItemText: '1 апреля 2018 г.',
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
                                pageObjects: {
                                    select() {
                                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                        return this.createPageObject('SelectB2b', ModelOpinionsFilters.sort);
                                    },
                                    popup: 'PopupB2b',
                                },
                                hooks: {
                                    beforeEach() {
                                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                        return this.list.waitForLoading();
                                    },
                                },
                            },
                            {
                                suite: 'Filters/__period',
                                meta: {
                                    issue: 'VNDFRONT-1847',
                                    feature: 'Отзывы на модель',
                                    id: 'vendor_auto-150',
                                },
                                params: {
                                    fromParamName: 'from',
                                    fromParamValue: '2018-04-23',
                                    toParamName: 'to',
                                    toParamValue: '2018-07-01',
                                    initialItemsCount: 20,
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
                                suite: 'Filters/__checkbox',
                                suiteName: 'Фильтр "Неотвеченные"',
                                meta: {
                                    issue: 'VNDFRONT-3451',
                                    feature: 'Отзывы на модель',
                                    id: 'vendor_auto-148',
                                    environment: 'kadavr',
                                },
                                params: {
                                    initialItemsCount: 20,
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
                                suite: 'Filters/__checkbox',
                                suiteName: 'Фильтр "Только от проверенных покупателей"',
                                meta: {
                                    issue: 'VNDFRONT-3451',
                                    feature: 'Отзывы на модель',
                                    id: 'vendor_auto-164',
                                },
                                params: {
                                    initialItemsCount: 20,
                                    filteredItemsCount: 4,
                                    paramName: 'onlyCpa',
                                    paramValue: true,
                                },
                                pageObjects: {
                                    checkbox() {
                                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                        return this.createPageObject('CheckboxB2b', ModelOpinionsFilters.onlyCpa);
                                    },
                                },
                            },
                            {
                                suite: 'Filters/__select',
                                suiteName: 'Фильтр по оценке',
                                meta: {
                                    issue: 'VNDFRONT-3451',
                                    feature: 'Отзывы на модель',
                                    id: 'vendor_auto-149',
                                },
                                params: {
                                    initialItemText: '30 июля 2018 г.',
                                    expectedItemText: '17 мая 2018 г.',
                                    initialFilterText: 'любая',
                                    expectedFilterText: '3',
                                    queryParamName: 'grade',
                                    queryParamValue: '3',
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
                                        return this.createPageObject('SelectB2b', ModelOpinionsFilters.rating);
                                    },
                                    popup: 'PopupB2b',
                                },
                            },
                            {
                                suite: 'Page/title',
                                params: {
                                    title: 'Отзывы о товарах бренда',
                                },
                            },
                            {
                                suite: 'AnswerForm/autocompletion',
                                suiteName: 'Автоподстановка ответа на отзыв на конкретную модель. ',
                                meta: {
                                    issue: 'VNDFRONT-3451',
                                    feature: 'Отзывы на модель',
                                    id: 'vendor_auto-735',
                                },
                                params: {
                                    expectedTemplateText: 'elena.fireraven, ',
                                },
                                pageObjects: {
                                    answerForm() {
                                        return this.createPageObject(
                                            'AnswerForm',
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            this.firstOpinion,
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            this.firstOpinion.answerForm,
                                        );
                                    },
                                    toggle() {
                                        return this.createPageObject(
                                            'AnswerFormToggle',
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            this.firstOpinion,
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            this.firstOpinion.answerFormToggle,
                                        );
                                    },
                                },
                                hooks: {
                                    // @ts-expect-error(TS2322) найдено в рамках VNDFRONT-4580
                                    beforeEach() {
                                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                        this.params.exists = this.params.has(PERMISSIONS.opinions.write);
                                    },
                                },
                            },
                            {
                                suite: 'Comment/deleteConfirmation',
                                suiteName: 'Подтверждение удаления ответа на отзыв на конкретную модель. ',
                                meta: {
                                    issue: 'VNDFRONT-3451',
                                    feature: 'Отзывы на модель',
                                    id: 'vendor_auto-560',
                                },
                                params: {
                                    messageText: 'Комментарий успешно удален.',
                                },
                                pageObjects: {
                                    commentsList() {
                                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                        return this.createPageObject('CommentsList', this.list.getItemByIndex(4));
                                    },
                                    comment() {
                                        return this.createPageObject(
                                            'Comment',
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            this.commentsList,
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            this.commentsList.getItemByIndex(1),
                                        );
                                    },
                                },
                                hooks: {
                                    // @ts-expect-error(TS2322) найдено в рамках VNDFRONT-4580
                                    beforeEach() {
                                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                        this.params.editable = this.params.has(PERMISSIONS.opinions.write);
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
