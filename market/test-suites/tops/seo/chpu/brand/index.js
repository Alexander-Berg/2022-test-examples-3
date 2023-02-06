import {prepareSuite, mergeSuites, makeSuite} from 'ginny';
import {createProduct} from '@yandex-market/kadavr/mocks/Report/helpers';
import mergeReportState from '@yandex-market/kadavr/mocks/Report/helpers/mergeState';

import {routes} from '@self/platform/spec/hermione/configs/routes';
// suites
import ShowcaseChpuSuite from '@self/platform/spec/hermione/test-suites/blocks/widgets/content/Showcase/chpu';
import ProductSummaryChpuSuite from '@self/platform/spec/hermione/test-suites/blocks/ProductSummary/chpu';
import PopularBrandsChpuBrandSuite from '@self/platform/spec/hermione/test-suites/blocks/n-w-popular-brands/chpu-brand';
import ISliderChpuSuite from '@self/platform/spec/hermione/test-suites/blocks/i-slider/chpu';
// page-objects
import Showcase from '@self/platform/spec/page-objects/Showcase';
import PopularBrands from '@self/platform/spec/page-objects/components/PopularBrands';
import ProductSummary from '@self/platform/spec/page-objects/ProductSummary';
import Scrollbox from '@self/platform/spec/page-objects/ScrollBox';
import HeaderCatalog from '@self/platform/widgets/content/HeaderCatalog/__pageObject';
import HeaderTabs from '@self/platform/widgets/content/HeaderTabs/__pageObject';
import Header2Menu from '@self/platform/spec/page-objects/header2-menu';
import AllVendorProductsLinkPageObject from '@self/platform/components/AllVendorProductsLink/__pageObject__';

import navigationRootMock from '@self/platform/spec/hermione/fixtures/navMenu/root.json';
import navigationCatalogMock from '@self/platform/spec/hermione/fixtures/navMenu/catalog.json';

import {phone} from './fixtures/product';
import indexPage from './fixtures/index-page';
import brandPage from './fixtures/brand-page';
import departmentPage from './fixtures/department-page';

const BRAND_ID = 153061;
const BRAND_SLUG = 'samsung';

export default makeSuite('Бренд', {
    environment: 'kadavr',
    story: mergeSuites(
        makeSuite('Главная страница.', {
            story: prepareSuite(ShowcaseChpuSuite, {
                pageObjects: {
                    showcase() {
                        return this.createPageObject(
                            Showcase,
                            {
                                root: '[data-zone-data*="Популярные бренды"]',
                            }
                        );
                    },
                },
                hooks: {
                    async beforeEach() {
                        await this.browser.setState('Tarantino.data.result', [indexPage]);
                        await this.browser.yaOpenPage('market:index');
                        await this.browser.yaExecAsyncClientScript('window.initAllLazyWidgets');
                    },
                },
            }),
        }),
        makeSuite('Департамент электроники.', {
            story: prepareSuite(ShowcaseChpuSuite, {
                pageObjects: {
                    showcase() {
                        return this.createPageObject(
                            Showcase,
                            {
                                root: '[data-zone-data*="Популярные бренды"]',
                            }
                        );
                    },
                },
                hooks: {
                    async beforeEach() {
                        await this.browser.setState('Tarantino.data.result', [departmentPage]);
                        return this.browser.yaOpenPage('market:catalog', routes.catalog.electronics);
                    },
                },
            }),
        }),
        makeSuite('Характеристики на КМ.', {
            story: prepareSuite(ProductSummaryChpuSuite, {
                pageObjects: {
                    allVendorProductLink() {
                        return this.createPageObject(AllVendorProductsLinkPageObject, {
                            parent: ProductSummary.root,
                        });
                    },
                },
                hooks: {
                    async beforeEach() {
                        const product = createProduct(phone, phone.id);
                        const reportState = mergeReportState([product]);

                        await this.browser.setState('report', reportState);

                        return this.browser.yaOpenPage('market:product', {
                            productId: phone.id,
                            slug: phone.slug,
                        });
                    },
                },
            }),
        }),
        makeSuite('Страница брендов.', {
            story: prepareSuite(PopularBrandsChpuBrandSuite, {
                pageObjects: {
                    popularBrands() {
                        return this.createPageObject(PopularBrands);
                    },
                },
                hooks: {
                    beforeEach() {
                        return this.browser.yaOpenPage('market:brands-list');
                    },
                },
            }),
        }),
        makeSuite('Страница брендов на букву А.', {
            story: prepareSuite(PopularBrandsChpuBrandSuite, {
                pageObjects: {
                    popularBrands() {
                        return this.createPageObject(PopularBrands);
                    },
                },
                hooks: {
                    beforeEach() {
                        return this.browser.yaOpenPage('market:brands-list', {char: 'a'});
                    },
                },
            }),
        }),
        makeSuite('Страница бренда.', {
            story: prepareSuite(ISliderChpuSuite, {
                pageObjects: {
                    scrollbox() {
                        return this.createPageObject(Scrollbox);
                    },
                },
                hooks: {
                    async beforeEach() {
                        await this.browser.setState('Tarantino.data.result', [brandPage]);
                        return this.browser.yaOpenPage('market:brands', {brandId: BRAND_ID, slug: BRAND_SLUG});
                    },
                },
            }),
        }),
        makeSuite('Меню.', {
            story: prepareSuite(ShowcaseChpuSuite, {
                pageObjects: {
                    showcase() {
                        return this.createPageObject(Showcase, {parent: HeaderCatalog.root});
                    },
                },
                hooks: {
                    async beforeEach() {
                        // увеличиваем окно, чтобы влез баннер
                        this.originalWindowSize = await this.browser.windowHandleSize();
                        await this.browser.windowHandleSize({width: 1600, height: 900});
                        await this.browser.setState(
                            'Tarantino.data.result',
                            [brandPage, navigationRootMock, navigationCatalogMock]
                        );

                        await this.browser.yaOpenPage('market:brands', {brandId: BRAND_ID, slug: BRAND_SLUG});

                        await this.browser.waitUntil(
                            () => this.browser.isExisting(HeaderTabs.root)
                            , 1000, 'Табы меню не загрузились');
                        const headerCatalogEntrypoint =
                            this.createPageObject(Header2Menu, {root: Header2Menu.catalogEntrypoint});
                        await headerCatalogEntrypoint.clickCatalogAndWaitForVisible();
                        // await Tab.moveToVerticalTabAndThenToContent(this.browser);
                    },
                    async afterEach() {
                        await this.browser.windowHandleSize(this.originalWindowSize.value);
                    },
                },
            }),
        })
    ),
});
