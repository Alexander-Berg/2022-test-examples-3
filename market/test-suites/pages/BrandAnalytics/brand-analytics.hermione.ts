'use strict';

import {mergeSuites, makeSuite} from 'ginny';
import _ from 'lodash';

import makeUserStory from 'spec/hermione/lib/helpers/userStory';
import USERS from 'spec/lib/constants/users/users';
// @ts-expect-error(TS2306) найдено в рамках VNDFRONT-4532
import PERMISSIONS from 'app/constants/permissions';
// @ts-expect-error(TS2306) найдено в рамках VNDFRONT-4532
import ROUTE_NAMES from 'app/constants/routeNames';

import categoriesState from './categories.json';
import brandsState from './brands.json';

const userStory = makeUserStory(ROUTE_NAMES.BRAND_ANALYTICS);

export default makeSuite('Страница Бренда на Маркете.', {
    story: (() => {
        const suites = USERS.map(user => {
            const vendor = _.get(user.permissions, [PERMISSIONS.reports.read, 0], 3301);
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
                            // Кейс с поиском брендов
                            case 'vendor_auto-1400':
                                await this.browser.setState('vendorsCategories', categoriesState);

                                return this.browser.setState('vendorsBrands', brandsState);

                            default:
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
                                    title: 'Бренд на Маркете',
                                },
                            },
                            'BrandAnalytics/reportDownload',
                            'BrandAnalytics/filters',
                        ],
                    },
                }),
            });
        });

        return mergeSuites(...suites);
    })(),
});
