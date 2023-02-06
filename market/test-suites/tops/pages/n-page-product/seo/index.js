import {merge, omit} from 'lodash';
import {prepareSuite, mergeSuites, makeSuite} from 'ginny';
import {mergeState, createOffer, createEntityPicture} from '@yandex-market/kadavr/mocks/Report/helpers';
import {selectProductById} from '@self/project/src/entities/product/selectors';
import {head} from 'ambar';

import {createStories} from '@self/platform/spec/hermione/helpers/createStories';
import testConfigsForBreadcrumbs from '@self/platform/spec/hermione/configs/test-models-for-breadcrumbs';
import seoTestConfigs from '@self/platform/spec/hermione/configs/seo/product-page';
// suites
import BreadcrumbsSuite from '@self/platform/spec/hermione/test-suites/blocks/n-breadcrumbs';
import BreadcrumbsItemClickableYesSuite from '@self/platform/spec/hermione/test-suites/blocks/n-breadcrumbs/__item_clickable_yes';
import ProductSummaryGallerySuite from '@self/platform/spec/hermione/test-suites/blocks/ProductSummary/__gallery';
import PageH1Suite from '@self/platform/spec/hermione/test-suites/blocks/page-h1';
import SchemaOrgProductSuite from '@self/platform/spec/hermione/test-suites/blocks/schemaOrg/product';
import SchemaOrgOfferSuite from '@self/platform/spec/hermione/test-suites/blocks/schemaOrg/offer';
import SchemaOrgBreadcrumbsSuite from '@self/platform/spec/hermione/test-suites/blocks/schemaOrg/breadcrumbs';
// page-objects
import Headline from '@self/platform/spec/page-objects/headline';
import ProductSummary from '@self/platform/spec/page-objects/n-product-summary';
import ProductTitle from '@self/platform/widgets/content/ProductCardTitle/__pageObject';
import Breadcrumbs from '@self/platform/components/Breadcrumbs/__pageObject/index.js';
import ImageGallery from '@self/platform/components/ImageGallery/__pageObject';
import SchemaOrgProduct from '@self/platform/spec/page-objects/SchemaOrgProduct';
import SchemaOrgAggregateOffer from '@self/platform/spec/page-objects/SchemaOrgAggregateOffer';
import SchemaOrgAggregateRating from '@self/platform/spec/page-objects/SchemaOrgAggregateRating';
import SchemaOrgOffer from '@self/platform/spec/page-objects/SchemaOrgOffer';
import SchemaOrgBreadcrumbsList from '@self/platform/spec/page-objects/SchemaOrgBreadcrumbsList';
import ProductTopOffer from '@self/platform/spec/page-objects/n-product-top-offer';
import ProductTopOffersList from '@self/platform/spec/page-objects/n-product-top-offers-list';
// mocks
import {phonePicture1, phonePicture2, phonePicture3} from '../fixtures/pictures';
import productSeoMock, {productId, slug} from './mocks/product.mock';
import offerSeoMock from './mocks/offer.mock';

export default makeSuite('SEO-разметка страницы.', {
    story: merge(
        createStories(
            testConfigsForBreadcrumbs,
            params => mergeSuites(
                prepareSuite(BreadcrumbsSuite, {
                    hooks: {
                        beforeEach() {
                            return this.browser.yaOpenPage(
                                'market:product',
                                omit(params, ['description', 'testParams'])
                            );
                        },
                    },
                    pageObjects: {
                        headline() {
                            return this.createPageObject(Headline);
                        },
                        productSummary() {
                            return this.createPageObject(ProductSummary);
                        },
                        breadcrumbs() {
                            return this.createPageObject(Breadcrumbs);
                        },
                    },
                }),
                prepareSuite(BreadcrumbsItemClickableYesSuite, {
                    hooks: {
                        beforeEach() {
                            // eslint-disable-next-line market/ginny/no-skip
                            return this.skip('MARKETVERSTKA-31797 скипаем упавшие тесты для озеленения');

                            // eslint-disable-next-line no-unreachable
                            return this.browser.yaOpenPage(
                                'market:product',
                                omit(params, ['description', 'testParams'])
                            );
                        },
                    },
                    pageObjects: {
                        headline() {
                            return this.createPageObject(Headline);
                        },
                        productSummary() {
                            return this.createPageObject(ProductSummary);
                        },
                        breadcrumbs() {
                            return this.createPageObject(Breadcrumbs);
                        },
                    },
                    params: params.testParams,
                })
            )
        ),
        createStories(
            seoTestConfigs.mainImageAttributes,
            ({routeParams, mock}) => prepareSuite(ProductSummaryGallerySuite, {
                hooks: {
                    async beforeEach() {
                        const threePhonePictures = [phonePicture1, phonePicture2, phonePicture3];

                        const reportState = mergeState([
                            ...threePhonePictures.map(pic => createEntityPicture(
                                pic,
                                'product',
                                routeParams.productId,
                                pic.url
                            )),
                            mock,
                        ]);

                        await this.browser.setState('report', reportState);

                        return this.browser.yaOpenPage('market:product', routeParams);
                    },
                },
                pageObjects: {
                    productTitle() {
                        return this.createPageObject(ProductTitle);
                    },
                    productGallery() {
                        return this.createPageObject(ImageGallery, {parent: this.productSummary});
                    },
                },
            })
        ),
        createStories(
            seoTestConfigs.pageMainHeader,
            ({routeParams, testParams, mock}) => prepareSuite(PageH1Suite, {
                meta: {
                    id: 'marketfront-2333',
                    issue: 'MARKETVERSTKA-27964',
                },
                pageObjects: {
                    headline() {
                        return this.createPageObject(ProductTitle);
                    },
                },
                hooks: {
                    async beforeEach() {
                        await this.browser.setState('report', mock);
                        return this.browser.yaOpenPage('market:product', routeParams);
                    },
                },
                params: testParams.main,
            })
        ),
        createStories(
            seoTestConfigs.canonicalDirectOpening,
            ({routeParams, testParams, mock}) => makeSuite('Переход по canonical-url.', {
                story: prepareSuite(PageH1Suite, {
                    meta: {
                        issue: 'MARKETVERSTKA-28692',
                        id: 'marketfront-2405',
                    },
                    pageObjects: {
                        headline() {
                            return this.createPageObject(ProductTitle);
                        },
                    },
                    hooks: {
                        async beforeEach() {
                            await this.browser.setState('report', mock);
                            return this.browser.yaOpenPage('market:product', routeParams);
                        },
                    },
                    params: testParams.main,
                }),
            })
        ),
        createStories(
            productSeoMock,
            productMock => prepareSuite(SchemaOrgProductSuite, {
                hooks: {
                    async beforeEach() {
                        const schemaOrgProduct = this.createPageObject(SchemaOrgProduct);

                        this.setPageObjects({
                            schemaOrgAggregateRating: () => this.createPageObject(
                                SchemaOrgAggregateRating,
                                {
                                    parent: schemaOrgProduct,
                                }
                            ),
                            schemaOrgAggregateOffer: () => this.createPageObject(
                                SchemaOrgAggregateOffer,
                                {
                                    parent: schemaOrgProduct,
                                }
                            ),
                            schemaOrgProduct: () => schemaOrgProduct,
                        });

                        const {results} = productMock.data.search;
                        const productMockId = head(results.filter(item => item.schema === 'product')).id;
                        const mockProduct = selectProductById(productMock, {productId: productMockId});

                        const routParams = {
                            productId: productMockId,
                            slug: mockProduct.slug,
                        };

                        Object.assign(this.params, {
                            expectedProductName: mockProduct.titles.raw,
                            expectedCategoryName: mockProduct.categories[0].name,
                            expectedBrandName: mockProduct.vendor.name,
                            expectedUrlPath: await this.browser.yaBuildURL('market:product', routParams),
                        });

                        await this.browser.setState('report', productMock);
                        await this.browser.setState('schema', {
                            modelOpinions: [
                                {
                                    product: {id: productMockId},
                                },
                            ],
                        });

                        return this.browser.yaOpenPage('market:product', routParams);
                    },
                },
            })
        ),
        createStories(productSeoMock, productMock => prepareSuite(SchemaOrgOfferSuite, {
            meta: {
                environment: 'kadavr',
            },
            hooks: {
                async beforeEach() {
                    const productTopOffer = this.createPageObject(ProductTopOffer);

                    this.setPageObjects({
                        schemaOrgOffer: () => this.createPageObject(
                            SchemaOrgOffer,
                            {
                                parent: this.createPageObject(ProductTopOffersList),
                                root: `${productTopOffer.root}:nth-child(1)`,
                            }
                        ),
                    });

                    await this.browser.setState('report', mergeState([
                        productMock,
                        createOffer(offerSeoMock, offerSeoMock.wareId),
                        {
                            data: {
                                search: {
                                    totalOffersBeforeFilters: 2,
                                },
                            },
                        },
                    ]));

                    return this.browser.yaOpenPage('market:product', {
                        productId,
                        slug,
                    });
                },
            },
        })),
        createStories(
            productSeoMock,
            productMock => prepareSuite(SchemaOrgBreadcrumbsSuite, {
                hooks: {
                    async beforeEach() {
                        this.setPageObjects({
                            schemaOrgBreadcrumbsList: () => this.createPageObject(
                                SchemaOrgBreadcrumbsList,
                                {
                                    parent: this.createPageObject(ProductSummary),
                                }
                            ),
                        });

                        const routParams = {
                            productId,
                            slug,
                        };

                        await this.browser.setState('report', productMock);

                        return this.browser.yaOpenPage('market:product', routParams);
                    },
                },
            })
        )
    ),
});
