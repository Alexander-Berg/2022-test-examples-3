'use strict';

import _ from 'lodash';
import {mergeSuites, makeSuite, PageObject} from 'ginny';

import USERS from 'spec/lib/constants/users/users';
import makeUserStory from 'spec/hermione/lib/helpers/userStory';
// @ts-expect-error(TS2306) найдено в рамках VNDFRONT-4532
import PERMISSIONS from 'app/constants/permissions';
// @ts-expect-error(TS2306) найдено в рамках VNDFRONT-4532
import ROUTE_NAMES from 'app/constants/routeNames';
import ListContainerPo from 'spec/page-objects/ListContainer';

import initialState from './initialState.json';

const userStory = makeUserStory(ROUTE_NAMES.MODELS_PROMOTION);

const ModelsPromotionListItem = PageObject.get('ModelsPromotionListItem');
const ButtonB2b = PageObject.get('ButtonB2b');

export default makeSuite('Страница Продвижение товаров.', {
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
                    onSetKadavrState() {
                        return this.browser.setState('vendorsModelsPromotion', initialState);
                    },
                    pageObjects: {
                        logo: 'Logo',
                        footer: 'Footer',
                        // @ts-expect-error(TS2322) найдено в рамках VNDFRONT-4580
                        list() {
                            return this.createPageObject<ListContainerPo>('ListContainer').setItemSelector(
                                ModelsPromotionListItem.root,
                            );
                        },
                    },
                    suites: {
                        common: [
                            'ModelsPromotion',
                            {
                                suite: 'Page/title',
                                params: {
                                    title: 'Продвижение товаров на Маркете',
                                },
                            },
                        ],
                        byPermissions: {
                            [PERMISSIONS.modelbids.write]: {
                                suite: 'ModelsPromotion/bidInput',
                                pageObjects: {
                                    modelBidRecommendationTooltip: 'ModelBidRecommendationTooltip',
                                    toasts: 'NotificationGroupLevitan',
                                    bar: 'RatesControlBar',
                                    item() {
                                        return this.createPageObject(
                                            'ModelsPromotionListItem',
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            this.list,
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            this.list.getItemByIndex(0),
                                        );
                                    },
                                    textField() {
                                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                        return this.createPageObject('TextFieldLevitan', this.item.bidContainer);
                                    },
                                    submitButton() {
                                        return this.createPageObject(
                                            'ButtonB2b',
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            this.bar,
                                            `${ButtonB2b.root}:first-child`,
                                        );
                                    },
                                    cancelButton() {
                                        return this.createPageObject(
                                            'ButtonB2b',
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            this.bar,
                                            `${ButtonB2b.root}:last-child`,
                                        );
                                    },
                                    toast() {
                                        return this.createPageObject(
                                            'NotificationLevitan',
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            this.toasts,
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            this.toasts.getItemByIndex(0),
                                        );
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
