import {makeSuite, prepareSuite, mergeSuites} from 'ginny';
import {mergeState, createProduct} from '@yandex-market/kadavr/mocks/Report/helpers';

import {routes} from '@self/platform/spec/hermione/configs/routes';
import AgeConfirmationDefaultExistsSuite from '@self/platform/spec/hermione/test-suites/blocks/AgeConfirmation/parts/default-exists';
import AgeConfirmationDefaultAcceptSuite from '@self/platform/spec/hermione/test-suites/blocks/AgeConfirmation/parts/default-accept';
import AgeConfirmationSnippetDeclineSuite from
    '@self/platform/spec/hermione/test-suites/blocks/AgeConfirmation/parts/snippet-decline';
import AgeConfirmation from '@self/platform/spec/page-objects/widgets/parts/AgeConfirmation';

export default makeSuite('Подтверждение возраста', {
    environment: 'kadavr',
    story: mergeSuites(
        {
            async beforeEach() {
                this.setPageObjects({
                    ageConfirmation: () => this.createPageObject(AgeConfirmation),
                });

                const productsCount = 4;
                const products = [];

                for (let i = 0; i < productsCount; i++) {
                    products.push(createProduct({
                        slug: 'test-product',
                        categories: [
                            {
                                entity: 'category',
                                id: 91491,
                                name: 'Мобильные телефоны',
                                fullName: 'Мобильные телефоны',
                                slug: 'mobilnye-telefony',
                                type: 'guru',
                                isLeaf: true,
                            },
                        ],
                    }));
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

                await this.browser.setState('report', state);
                return this.browser.yaOpenPage('touch:search', routes.search.default);
            },
        },
        prepareSuite(AgeConfirmationDefaultExistsSuite, {
            meta: {
                id: 'm-touch-2628',
                issue: 'MOBMARKET-11112',
            },
        }),
        prepareSuite(AgeConfirmationDefaultAcceptSuite, {
            meta: {
                id: 'm-touch-2629',
                issue: 'MOBMARKET-11112',
            },
        }),
        prepareSuite(AgeConfirmationSnippetDeclineSuite, {
            meta: {
                id: 'm-touch-2630',
                issue: 'MOBMARKET-11112',
            },
        })
    ),
});
