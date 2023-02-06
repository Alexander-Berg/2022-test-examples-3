'use strict';

import {mergeSuites, makeSuite} from 'ginny';

import USERS from 'spec/lib/constants/users/users';
import makeUserStory from 'spec/hermione/lib/helpers/userStory';
// @ts-expect-error(TS2306) найдено в рамках VNDFRONT-4532
import ROUTE_NAMES from 'app/constants/routeNames';

const userStory = makeUserStory(ROUTE_NAMES.NEW_VIRTUAL_VENDOR);

export default makeSuite('Страница подачи заявки внутри кабинета.', {
    story: (() => {
        const suites = USERS.map(user => {
            const vendor = 3301;
            const params = {
                vendor,
                routeParams: {vendor},
                checkBrandLink: false,
            };

            return makeSuite(`${user.description}.`, {
                story: userStory({
                    user,
                    params,
                    pageObjects: {
                        logo: 'Logo',
                        footer: 'Footer',
                        form: 'Form',
                        infoPanel: 'InfoPanel',
                        trademarkDocument() {
                            return this.createPageObject(
                                'DocumentUpload',
                                // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                this.form,
                                '[data-e2e=trademark-certificate-editable-document]',
                            );
                        },
                    },
                    suites: {
                        common: [
                            'NewVirtualVendor/Vendor',
                            {
                                suite: 'Page/title',
                                params: {
                                    title: 'Создание нового бренда',
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
