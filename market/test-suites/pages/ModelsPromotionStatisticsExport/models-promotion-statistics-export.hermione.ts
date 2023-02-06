'use strict';

import _ from 'lodash';
import {mergeSuites, makeSuite} from 'ginny';

import USERS from 'spec/lib/constants/users/users';
import makeUserStory from 'spec/hermione/lib/helpers/userStory';
import buildUrl from 'spec/lib/helpers/buildUrl';
// @ts-expect-error(TS2306) найдено в рамках VNDFRONT-4532
import PERMISSIONS from 'app/constants/permissions';
// @ts-expect-error(TS2306) найдено в рамках VNDFRONT-4532
import ROUTE_NAMES from 'app/constants/routeNames';

const userStory = makeUserStory(ROUTE_NAMES.MODELS_PROMOTION_STATISTICS_EXPORT);

export default makeSuite('Страница Формирования отчёта статистики продвижения товаров.', {
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
                    // @ts-expect-error(TS7031) найдено в рамках VNDFRONT-4580
                    onSetKadavrState({id}) {
                        // Кейс с отключённой услугой продвижения товаров
                        if (id === 'vendor_auto-1008') {
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
                    },
                    suites: {
                        common: [
                            {
                                suite: 'Page/title',
                                params: {
                                    title: 'Формирование отчёта',
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
                                suiteName: 'Ссылка "Назад"',
                                meta: {
                                    issue: 'VNDFRONT-3871',
                                    id: 'vendor_auto-1037',
                                },
                                params: {
                                    url: buildUrl(ROUTE_NAMES.MODELS_PROMOTION_STATISTICS, {vendor}),
                                    caption: 'Назад',
                                    comparison: {
                                        skipHostname: true,
                                        skipQuery: true,
                                    },
                                },
                                pageObjects: {
                                    link: 'BackLink',
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
