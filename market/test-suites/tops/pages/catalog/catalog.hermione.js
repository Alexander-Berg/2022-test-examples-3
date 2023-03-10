import dayjs from 'dayjs';
import {prepareSuite, mergeSuites, makeSuite} from 'ginny';
import {createOffer} from '@yandex-market/kadavr/mocks/Report/helpers/searchResult';
import mergeReportState from '@yandex-market/kadavr/mocks/Report/helpers/mergeState';
import {NBSP} from '@self/platform/constants/string';

import {createStories} from '@self/platform/spec/hermione/helpers/createStories';
import {makeCatalogerTree} from '@self/project/src/spec/hermione/helpers/metakadavr';
// configs
import seoTestConfigs from '@self/platform/spec/hermione/configs/seo/catalog-page';
import {routes} from '@self/platform/spec/hermione/configs/routes';
import {availableHid, availableNid, commonCatalogQuizMock, availableHidsAndNidsMock} from '@self/root/src/widgets/content/CatalogQuiz/__spec__/mock';
// suite
import BaseLinkCanonicalSuite from '@self/platform/spec/hermione/test-suites/blocks/n-base/__link-canonical';
import BaseOpenGraphSuite from '@self/platform/spec/hermione/test-suites/blocks/n-base/__open-graph';
import BreadcrumbsUnifiedSuite from '@self/platform/spec/hermione/test-suites/blocks/breadcrumbsUnified';
import BillboardSuite from '@self/platform/spec/hermione/test-suites/blocks/n-w-billboard';
import SearchResultsListSisSuite from '@self/platform/spec/hermione/test-suites/blocks/SearchResults/list-sis';
import RetailShopsIncutSuite from '@self/root/src/spec/hermione/test-suites/touch.blocks/retailShopsIncut';
import catalogQuizSuite from '@self/root/src/spec/hermione/test-suites/blocks/catalogQuiz';
// page-objects
import RegionPopup from '@self/platform/spec/page-objects/widgets/parts/RegionPopup';
import Base from '@self/platform/spec/page-objects/n-base';
import BreadcrumbsUnified from '@self/platform/spec/page-objects/BreadcrumbsUnified';
import Billboard from '@self/platform/spec/page-objects/n-w-billboard';
import SearchSnippetDelivery from '@self/platform/spec/page-objects/containers/SearchSnippet/Delivery';
import StartScreen from '@self/root/src/widgets/content/CatalogQuiz/components/StartScreen/__pageObject';

// fixtures
import delivery350r from './fixtures/delivery/delivery350r';
import delivery350rToday from './fixtures/delivery/delivery350rToday';
import deliveryFreeTomorrow from './fixtures/delivery/deliveryFreeTomorrow';
import deliveryFree3days from './fixtures/delivery/deliveryFree3days';

// imports
import deals from './deals';
import dealsDiscount from './deals/discount';
import footerSubscription from './subscription';
import ageConfirmation from './ageConfirmation';
import filtersPayment from './filters/payments';
import cashback from './deals/cashback';
import genericBundle from './deals/genericBundle';
import {breadcrumbsExpressSuite} from './breadcrumbs/express';

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('???????????????? ????????????????.', {
    story: mergeSuites(
        {
            beforeEach() {
                this.setPageObjects({
                    regionPopup: () => this.createPageObject(RegionPopup),
                });
            },
        },
        makeSuite('SEO-???????????????? ????????????????.', {
            environment: 'testing',
            story: mergeSuites(
                makeSuite('?????? Canonical.', {
                    story: createStories(
                        seoTestConfigs.pageCanonical,
                        ({
                            routeConfig,
                            testParams,
                        }) => prepareSuite(BaseLinkCanonicalSuite, {
                            meta: {
                                id: 'm-touch-1040',
                                issue: 'MOBMARKET-4778',
                            },
                            params: testParams,
                            hooks: {
                                beforeEach() {
                                    return this.browser
                                        .yaSimulateBot()
                                        .yaOpenPage('touch:list', routeConfig);
                                },
                            },
                            pageObjects: {
                                base() {
                                    return this.createPageObject(Base);
                                },
                            },
                        })
                    ),
                }),
                createStories(
                    seoTestConfigs.pageOpenGraph,
                    ({
                        testParams,
                        routeParams,
                    }) => prepareSuite(BaseOpenGraphSuite, {
                        meta: {
                            id: 'm-touch-1041',
                            issue: 'MOBMARKET-5949',
                        },
                        hooks: {
                            beforeEach() {
                                return this.browser
                                    .yaSimulateBot()
                                    .yaOpenPage('touch:list', routeParams);
                            },
                        },
                        pageObjects: {
                            base() {
                                return this.createPageObject(Base);
                            },
                        },
                        params: testParams,
                    })
                )
            ),
        }),

        makeSuite('???????????????? ????????????????.', {
            environment: 'testing',
            story: mergeSuites(
                {
                    async beforeEach() {
                        await this.browser.yaSetCookie({
                            name: 'currentRegionId',
                            value: '213',
                        });
                        return this.browser.yaOpenPage('touch:catalog', routes.catalog.phones);
                    },
                },

                prepareSuite(BreadcrumbsUnifiedSuite, {
                    pageObjects: {
                        breadcrumbsUnified() {
                            return this.createPageObject(BreadcrumbsUnified);
                        },
                    },
                    params: {
                        track: 'pieces',
                        // ?????????????????? ?????????????????? ??????????????????????
                        name: '??????????????????????',
                        ...routes.catalog.electronics,
                        links: [{pathname: '/catalog--elektronika/54440'}],
                    },
                })
            ),
        }),

        makeSuite('?????????????????? ????????????', {
            environment: 'testing',
            story: mergeSuites(
                {
                    async beforeEach() {
                        await this.browser.yaOpenPage('touch:list', {...routes.catalog.phones, text: '?????????????? ??????????'});
                    },
                },

                prepareSuite(BreadcrumbsUnifiedSuite, {
                    meta: {
                        id: 'm-touch-3177',
                        issue: 'MARKETFRONT-6927',
                    },
                    pageObjects: {
                        breadcrumbsUnified() {
                            return this.createPageObject(BreadcrumbsUnified);
                        },
                    },
                    params: {
                        links: [
                            {pathname: '/search'},
                            {pathname: '/catalog--elektronika/54440', query: {hid: '198119'}},
                            {pathname: '/catalog--smartfony-i-aksessuary/54437', query: {hid: '91461'}},
                            '?????????????? ??????????',
                        ],
                    },
                })
            ),
        }),

        makeSuite('?????????????????????? ??????????????????????.', {
            environment: 'testing',
            story: mergeSuites(
                {
                    beforeEach() {
                        return this.browser.yaOpenPage('touch:list', routes.catalog.electronics);
                    },
                },

                prepareSuite(BillboardSuite, {
                    pageObjects: {
                        billboard() {
                            return this.createPageObject(Billboard);
                        },
                    },
                })
            ),
        }),

        makeSuite('?????????? ?? ?????????????????? ???????????????? ???? ???????????? ???????? List.', {
            environment: 'kadavr',
            story: createStories([
                {
                    description: '?????? ?????????????? "+????? ????????????????, ??? ????????"',
                    params: {
                        // market/platform.touch/containers/SearchSnippet/connectors/helpers/getDeliveryFormattedText.js
                        deliveryText: `${dayjs().add(2, 'day').format(`D${NBSP}MMMM`)}, ???? ????????????????`,
                        offerOptions: delivery350r,
                    },
                    meta: {
                        id: 'm-touch-2431',
                        issue: 'MOBMARKET-10209',
                    },
                },
                {
                    description: '?????? ?????????????? "+????? ????????????????, ?????????????? (????????????)"',
                    params: {
                        deliveryText: '??????????????, ???? ????????????????',
                        offerOptions: delivery350rToday,
                    },
                    meta: {
                        id: 'm-touch-2430',
                        issue: 'MOBMARKET-10210',
                    },
                },
                {
                    description: '?????? ?????????????? "???????????????????? ????????????????, ?????????????? (????????????)"',
                    params: {
                        deliveryText: '????????????, ???? ????????????????',
                        offerOptions: deliveryFreeTomorrow,
                    },
                    meta: {
                        id: 'm-touch-2429',
                        issue: 'MOBMARKET-10211',
                    },
                },
                {
                    description: '?????? ?????????????? "???????????????????? ???????????????? ??? ????????"',
                    params: {
                        // market/platform.touch/containers/SearchSnippet/connectors/helpers/getDeliveryFormattedText.js
                        deliveryText: `${dayjs().add(3, 'day').format(`D${NBSP}MMMM`)}, ???? ????????????????`,
                        offerOptions: deliveryFree3days,
                    },
                    meta: {
                        id: 'm-touch-2428',
                        issue: 'MOBMARKET-10212',
                    },
                },
                // ???????? ???????????????????????? ?????????? ?? ??????????????. ?????????? ?????????? ???????????????? ?? ???????????? ?? ?????????? ??????????????.
                // ???????? ?????? ?? ?????????????? ??????????????????, ???? ???????????? ?????????????????????????? ???????? ?? ???????????????? ???????????? hasDelivery
                // {
                //     description: '?????? ?????????????? "???????? ????????????????"',
                //     params: {
                //         deliveryText: '???????? ????????????????',
                //         offerOptions: hasDelivery,
                //     },
                //     meta: {
                //         id: 'MOBMARKET-10208',
                //         issue: 'm-touch-2432',
                //     },
                // },
            ], ({meta, params}) => prepareSuite(SearchResultsListSisSuite, {
                meta,
                params,
                hooks: {
                    beforeEach() {
                        const {offerOptions} = params;

                        const offersCount = 1;
                        const offers = [createOffer(offerOptions)];

                        const totalMixin = {
                            data: {
                                search: {
                                    total: offersCount,
                                    totalOffers: offersCount,
                                },
                            },
                        };

                        const reportState = mergeReportState([
                            ...offers,
                            totalMixin,
                        ]);

                        return this.browser
                            .setState('report', reportState)
                            .yaOpenPage('touch:list', routes.catalog.list)
                            .yaClosePopup(this.regionPopup);
                    },
                },
                pageObjects: {
                    snippetDelivery() {
                        return this.createPageObject(SearchSnippetDelivery);
                    },
                },
            })),
        }),

        deals,
        dealsDiscount,
        footerSubscription,
        ageConfirmation,
        filtersPayment,
        cashback,
        genericBundle,
        breadcrumbsExpressSuite,
        RetailShopsIncutSuite,
        prepareSuite(catalogQuizSuite, {
            hooks: {
                async beforeEach() {
                    await this.browser.setState('Tarantino.data.result', [
                        commonCatalogQuizMock,
                        availableHidsAndNidsMock,
                    ]);
                    const catalogerMock = makeCatalogerTree('categoryName', availableHid, availableNid);
                    await this.browser.setState('Cataloger.tree', catalogerMock);
                },
            },
            pageObjects: {
                StartScreen() {
                    return this.createPageObject(StartScreen);
                },
            },
            params: {
                pageId: 'touch:list',
            },
        })
    ),
});
