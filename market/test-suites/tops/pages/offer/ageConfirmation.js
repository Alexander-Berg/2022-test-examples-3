import {makeSuite, prepareSuite, mergeSuites} from 'ginny';
import {mergeState, createOffer} from '@yandex-market/kadavr/mocks/Report/helpers';

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

                this.params.reportPlace = 'offerinfo';

                const offerId = 420;
                const state = mergeState([
                    createOffer({}, offerId),
                    {
                        data: {
                            search: {
                                adult: true,
                            },
                        },
                    },
                ]);

                await this.browser.setState('report', state);
                return this.browser.yaOpenPage('touch:offer', {offerId});
            },
        },
        prepareSuite(AgeConfirmationDefaultExistsSuite, {
            meta: {
                id: 'm-touch-2631',
                issue: 'MOBMARKET-11112',
            },
        }),
        prepareSuite(AgeConfirmationDefaultAcceptSuite, {
            meta: {
                id: 'm-touch-2632',
                issue: 'MOBMARKET-11112',
            },
        }),
        prepareSuite(AgeConfirmationDefaultDeclineSuite, {
            meta: {
                id: 'm-touch-2633',
                issue: 'MOBMARKET-11112',
            },
        })
    ),
});
