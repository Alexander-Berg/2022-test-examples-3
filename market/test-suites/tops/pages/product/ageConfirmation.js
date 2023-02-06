import {makeSuite, prepareSuite, mergeSuites} from 'ginny';
import {mergeState, createProduct} from '@yandex-market/kadavr/mocks/Report/helpers';

import AgeConfirmationDefaultExistsSuite from '@self/platform/spec/hermione/test-suites/blocks/AgeConfirmation/parts/default-exists';
import AgeConfirmationDefaultAcceptSuite from '@self/platform/spec/hermione/test-suites/blocks/AgeConfirmation/parts/default-accept';
import AgeConfirmationDefaultDeclineSuite from
    '@self/platform/spec/hermione/test-suites/blocks/AgeConfirmation/parts/default-decline';

import AgeConfirmation from '@self/platform/spec/page-objects/widgets/parts/AgeConfirmation';

export default makeSuite('Подтверждение возраста', {
    environment: 'kadavr',
    story: mergeSuites(
        {
            async beforeEach() {
                this.setPageObjects({
                    ageConfirmation: () => this.createPageObject(AgeConfirmation),
                });

                const productId = 420;
                const slug = 'weed';

                this.params.reportPlace = 'modelinfo';

                const state = mergeState([
                    createProduct({slug}, productId),
                    {
                        data: {
                            search: {
                                adult: true,
                            },
                        },
                    },
                ]);

                await this.browser.setState('report', state);
                return this.browser.yaOpenPage('touch:product', {slug, productId});
            },
        },
        prepareSuite(AgeConfirmationDefaultExistsSuite, {
            meta: {
                id: 'm-touch-2634',
                issue: 'MOBMARKET-11112',
            },
        }),
        prepareSuite(AgeConfirmationDefaultAcceptSuite, {
            meta: {
                id: 'm-touch-2635',
                issue: 'MOBMARKET-11112',
            },
        }),
        prepareSuite(AgeConfirmationDefaultDeclineSuite, {
            meta: {
                id: 'm-touch-2636',
                issue: 'MOBMARKET-11112',
            },
        })
    ),
});
