/*
// Прячем обзоры до лучших времён см.MARKETFRONT-76293
import {makeSuite, mergeSuites, prepareSuite} from 'ginny';
import {mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';
import {stateProductWithDO} from '@self/platform/spec/hermione/configs/seo/mocks';

import HeadBannerProductAbsenceSuite from '@self/platform/spec/hermione/test-suites/blocks/HeadBanner/productAbsence';
import AdultWarningDefaultSuite from '@self/platform/spec/hermione/test-suites/blocks/AdultWarning/default';
import AdultWarningAcceptSuite from '@self/platform/spec/hermione/test-suites/blocks/AdultWarning/accept';
import AdultWarningDeclineSuite from '@self/platform/spec/hermione/test-suites/blocks/AdultWarning/decline';

import AdultConfirmationPopup from '@self/platform/widgets/content/AdultWarning/components/AdultWarning/__pageObject';

import seo from './seo';

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Страница обзоров на товар.', {
    environment: 'testing',
    story: mergeSuites(
        prepareSuite(HeadBannerProductAbsenceSuite, {
            meta: {
                id: 'marketfront-3389',
                issue: 'MARKETVERSTKA-33961',
            },
            params: {
                pageId: 'market:product-articles',
            },
        }),

        makeSuite('Диалог подтверждения возраста. Adult контент.', {
            environment: 'kadavr',
            feature: 'Диалог подтверждения возраста',
            story: mergeSuites(
                {
                    async beforeEach() {
                        this.setPageObjects({
                            adultConfirmationPopup() {
                                return this.createPageObject(AdultConfirmationPopup);
                            },
                        });

                        const productId = 12345;
                        const state = mergeState([
                            stateProductWithDO(productId, {
                                type: 'model',
                                titles: {
                                    raw: 'Смартфон Apple iPhone 7 256GB',
                                },
                            }),
                            {
                                data: {
                                    search: {adult: true},
                                },
                            },
                        ]);

                        await this.browser.setState('report', state);

                        return this.browser.yaOpenPage('market:product-offers', {
                            slug: 'haha',
                            productId,
                        });
                    },
                },
                prepareSuite(AdultWarningDefaultSuite, {
                    meta: {
                        issue: 'MARKETFRONT-7130',
                        id: 'marketfront-4035',
                    },
                }),
                prepareSuite(AdultWarningAcceptSuite, {
                    meta: {
                        issue: 'MARKETFRONT-7130',
                        id: 'marketfront-4040',
                    },
                }),
                prepareSuite(AdultWarningDeclineSuite, {
                    meta: {
                        issue: 'MARKETFRONT-7130',
                        id: 'marketfront-4045',
                    },
                })
            ),
        }),

        seo
    ),
});
*/
