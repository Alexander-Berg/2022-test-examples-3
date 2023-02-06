import {makeSuite, prepareSuite, mergeSuites} from 'ginny';
import {mergeState, createProduct} from '@yandex-market/kadavr/mocks/Report/helpers';

import {routes} from '@self/platform/spec/hermione/configs/routes';
// suites
import AgeConfirmationDefaultExistsSuite from '@self/platform/spec/hermione/test-suites/blocks/AgeConfirmation/parts/default-exists';
import AgeConfirmationDefaultAcceptSuite from '@self/platform/spec/hermione/test-suites/blocks/AgeConfirmation/parts/default-accept';
import AgeConfirmationDefaultDeclineSuite from
    '@self/platform/spec/hermione/test-suites/blocks/AgeConfirmation/parts/default-decline';
// page-objects
import AgeConfirmation from '@self/platform/spec/page-objects/widgets/parts/AgeConfirmation';

export default makeSuite('Подтверждение возраста', {
    environment: 'kadavr',
    story: mergeSuites(
        {
            async beforeEach() {
                this.setPageObjects({
                    ageConfirmation: () => this.createPageObject(AgeConfirmation),
                });

                const productsCount = 0;
                const products = [];

                for (let i = 0; i < productsCount; i++) {
                    products.push(createProduct());
                }

                const state = mergeState([
                    ...products,
                    {
                        data: {
                            search: {
                                adult: true,
                                total: productsCount,
                                totalOffers: productsCount,
                            },
                        },
                    },
                ]);

                await this.browser.deleteCookie('adult');
                await this.browser.setState('report', state);

                return this.browser.yaOpenPage('touch:list', routes.catalog.list);
            },
        },
        prepareSuite(AgeConfirmationDefaultExistsSuite, {
            meta: {
                id: 'm-touch-2625',
                issue: 'MOBMARKET-11112',
            },
        }),
        prepareSuite(AgeConfirmationDefaultAcceptSuite, {
            meta: {
                id: 'm-touch-2626',
                issue: 'MOBMARKET-11112',
            },
        }),
        prepareSuite(AgeConfirmationDefaultDeclineSuite, {
            meta: {
                id: 'm-touch-2627',
                issue: 'MOBMARKET-11112',
            },
        })
    ),
});
