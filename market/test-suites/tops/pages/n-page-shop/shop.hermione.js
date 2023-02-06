import {makeSuite, prepareSuite, mergeSuites} from 'ginny';

import {mergeState, createShopInfo, createOffer} from '@yandex-market/kadavr/mocks/Report/helpers';

// configs
import {shopPages} from '@self/platform/spec/hermione/configs/seo/shop-page';
// suites
import ShopPageSuite from '@self/platform/spec/hermione/test-suites/blocks/widgets/pages/ShopPage';
import PageDescriptionSuite from '@self/platform/spec/hermione/test-suites/blocks/page-description';
import SchemaOrgOrganizationSuite from '@self/platform/spec/hermione/test-suites/blocks/schemaOrg/organization';
// page-objects
import ShopPage from '@self/platform/widgets/pages/ShopPage/__pageObject';
import PageMeta from '@self/platform/spec/page-objects/pageMeta';
import SchemaOrgOrganization from '@self/platform/spec/page-objects/SchemaOrgOrganization';
import SchemaOrgAggregateRating from '@self/platform/spec/page-objects/SchemaOrgAggregateRating';
import SchemaOrgPostalAddress from '@self/platform/spec/page-objects/SchemaOrgPostalAddress';
import ShopReviewsContainer from '@self/platform/spec/page-objects/n-shop-reviews-container';

import promoRecipes from './promoRecipes';
import shopInfoMock from './mocks/shopInfo.mock.json';
import shopReviewMock from './mocks/shopReview.mock.json';
import userMock from './mocks/user.mock.json';
import tarantinoMockData from './mocks/tarantino.mock.json';
import tarantinoWithoutDiscountMockData from './mocks/tarantinoWithoutDiscount.mock.json';

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Страница магазина.', {
    environment: 'testing',
    story: {
        'Содержимое.': mergeSuites(
            prepareSuite(ShopPageSuite, {
                pageObjects: {
                    shopPage() {
                        return this.createPageObject(ShopPage);
                    },
                },
                hooks: {
                    async beforeEach() {
                        const state = mergeState([
                            createShopInfo(shopInfoMock, shopInfoMock.id),
                            createOffer({
                                shop: {
                                    id: shopInfoMock.id,
                                },
                                cpc: 'cpcid',
                                navnodes: [],
                            }),
                        ]);

                        await this.browser.setState('report', state);
                        await this.browser.setState('Tarantino.data.result', [tarantinoWithoutDiscountMockData]);

                        const routeParams = shopPages.pageMainHeader.routeParams;
                        return this.browser.yaOpenPage('market:shop', routeParams);
                    },
                },
                params: {
                    shopId: shopInfoMock.id,
                    slug: shopInfoMock.slug,
                },
            }),
            promoRecipes
        ),
        'SEO-разметка страницы.': mergeSuites(
            prepareSuite(PageDescriptionSuite, {
                hooks: {
                    beforeEach() {
                        // eslint-disable-next-line market/ginny/no-skip
                        return this.skip('MARKETVERSTKA-31797 скипаем упавшие тесты для озеленения');

                        // eslint-disable-next-line no-unreachable
                        const {routeParams} = shopPages.pageDescription;

                        return this.browser.yaOpenPage('market:shop', routeParams);
                    },
                },
                params: shopPages.pageDescription.testParams,
                pageObjects: {
                    pageMeta() {
                        return this.createPageObject(PageMeta);
                    },
                },
            }),
            prepareSuite(SchemaOrgOrganizationSuite, {
                hooks: {
                    async beforeEach() {
                        Object.assign(this.params, {expectedName: shopInfoMock.shopName}, {
                            expectedAddress: shopInfoMock.juridicalAddress,
                        });

                        const schemaOrgOrganization = this.createPageObject(
                            SchemaOrgOrganization,
                            {
                                parent: ShopReviewsContainer,
                            }
                        );

                        const schemaOrgPostalAddress = this.createPageObject(
                            SchemaOrgPostalAddress,
                            {
                                parent: schemaOrgOrganization,
                            }
                        );

                        const schemaOrgAggregateRating = this.createPageObject(
                            SchemaOrgAggregateRating,
                            {
                                parent: schemaOrgOrganization,
                            }
                        );

                        this.setPageObjects({
                            schemaOrgOrganization: () => schemaOrgOrganization,
                            schemaOrgPostalAddress: () => schemaOrgPostalAddress,
                            schemaOrgAggregateRating: () => schemaOrgAggregateRating,
                        });

                        await this.browser
                            .setState(
                                'report',
                                createShopInfo(shopInfoMock, shopInfoMock.id)
                            )
                            .setState('schema', {
                                users: [userMock],
                                modelOpinions: [shopReviewMock],
                            })
                            .setState('Tarantino.data.result', [tarantinoMockData]);

                        return this.browser.yaOpenPage('market:shop', {
                            shopId: shopInfoMock.id,
                            slug: shopInfoMock.slug,
                        });
                    },
                },
            })
        ),
    },
});
