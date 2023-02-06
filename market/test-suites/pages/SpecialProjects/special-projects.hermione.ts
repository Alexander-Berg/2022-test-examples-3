'use strict';

import _ from 'lodash';
import {mergeSuites, makeSuite} from 'ginny';

import USERS from 'spec/lib/constants/users/users';
import makeUserStory from 'spec/hermione/lib/helpers/userStory';
// @ts-expect-error(TS2306) найдено в рамках VNDFRONT-4532
import P from 'app/constants/permissions';
// @ts-expect-error(TS2306) найдено в рамках VNDFRONT-4532
import ROUTE_NAMES from 'app/constants/routeNames';
import ReportPreview from 'spec/page-objects/ReportPreview';

const userStory = makeUserStory(ROUTE_NAMES.SPECIAL_PROJECTS);

export default makeSuite('Страница Спецпроектов.', {
    // @ts-expect-error(TS2345) найдено в рамках VNDFRONT-4580
    meta: {
        issue: 'VNDFRONT-2565',
        feature: 'Спецпроекты',
        environment: 'testing',
        id: 'vendor_auto-882',
    },
    story: (() => {
        const suites = USERS.map(user => {
            const vendor = _.get(user.permissions, [P.specialProjects.read, 0], 3301);
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
                        list() {
                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                            return this.createPageObject('FlexGroup').setItemSelector(ReportPreview.root);
                        },
                    },
                    suites: {
                        common: [
                            {
                                suite: 'Page/title',
                                params: {
                                    title: 'Статистика спецпроектов',
                                },
                            },
                            {
                                suite: 'ReportPreviews',
                            },
                        ],
                    },
                }),
            });
        });

        return mergeSuites(...suites);
    })(),
});
