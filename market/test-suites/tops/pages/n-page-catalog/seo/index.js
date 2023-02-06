import {prepareSuite, mergeSuites, makeSuite} from 'ginny';

import seoTestConfigs from '@self/platform/spec/hermione/configs/seo/catalog-page';
import {routes} from '@self/platform/spec/hermione/configs/routes';
// suites
import PageTitleSuite from '@self/platform/spec/hermione/test-suites/blocks/page-title';
import PageDescriptionSuite from '@self/platform/spec/hermione/test-suites/blocks/page-description';
import BreadcrumbsItemClickSuite from '@self/platform/spec/hermione/test-suites/blocks/Breadcrumbs/__item_clickable_yes';
import BreadcrumbsSuite from '@self/platform/spec/hermione/test-suites/blocks/Breadcrumbs';
import PageH1Suite from '@self/platform/spec/hermione/test-suites/blocks/page-h1';
import CanonicalUrlSuite from '@self/platform/spec/hermione/test-suites/blocks/canonical-url';
import AlternateUrlSuite from '@self/platform/spec/hermione/test-suites/blocks/alternate-url';
// page-objects
import PageMeta from '@self/platform/spec/page-objects/pageMeta';
import AlternateUrl from '@self/platform/spec/page-objects/alternate-url';
import Headline from '@self/platform/spec/page-objects/widgets/content/CatalogHeader';
import CanonicalUrl from '@self/platform/spec/page-objects/canonical-url';
import Breadcrumbs from '@self/platform/components/Breadcrumbs/__pageObject';

import departmentPage from '../fixtures/department-page';
import navigationTreeDepartment from '../fixtures/navigation-tree-department';

export default makeSuite('SEO-разметка страницы.', {
    story: mergeSuites(
        makeSuite('Департамент. SEO.', {
            environment: 'testing',
            story: mergeSuites(
                {
                    async beforeEach() {
                        this.setPageObjects({
                            breadcrumbs: () => this.createPageObject(Breadcrumbs),
                        });

                        return this.browser.yaOpenPage('market:catalog', routes.catalog.kukhnya);
                    },
                },
                prepareSuite(BreadcrumbsItemClickSuite),
                prepareSuite(BreadcrumbsSuite)
            ),
        }),
        makeSuite('Департамент.', {
            environment: 'kadavr',
            story: mergeSuites(
                {
                    async beforeEach() {
                        this.setPageObjects({
                            headline: () => this.createPageObject(Headline),
                        });

                        await this.browser.setState('Tarantino.data.result', [departmentPage]);
                        await this.browser.setState('Cataloger.tree', navigationTreeDepartment);
                        await this.browser.setState('Cataloger.path', navigationTreeDepartment);

                        return this.browser.yaOpenPage('market:catalog', routes.catalog.kukhnya);
                    },
                },
                makeSuite('Переход по canonical-url.', {
                    story: prepareSuite(PageH1Suite, {
                        meta: {
                            issue: 'MARKETVERSTKA-31865',
                            id: 'marketfront-2405',
                        },
                        pageObjects: {
                            headline() {
                                return this.createPageObject(Headline);
                            },
                        },
                        params: {
                            expectedHeaderText: seoTestConfigs.department.testParams.expectedTitleText,
                        },
                    }),
                }),
                makeSuite('Title и Description.', {
                    issue: 'MARKETVERSTKA-31865',
                    story: mergeSuites(
                        prepareSuite(PageDescriptionSuite, {
                            meta: {
                                issue: 'MARKETVERSTKA-31865',
                            },
                            params: {
                                expectedDescription: seoTestConfigs.department.testParams.expectedDescription,
                            },
                            pageObjects: {
                                pageMeta() {
                                    return this.createPageObject(PageMeta);
                                },
                            },
                        }),
                        prepareSuite(PageTitleSuite, {
                            meta: {
                                issue: 'MARKETVERSTKA-31865',
                            },
                            params: {
                                expectedTitle: seoTestConfigs.department.testParams.expectedTitle,
                            },
                            pageObjects: {
                                pageMeta() {
                                    return this.createPageObject(PageMeta);
                                },
                            },
                        })
                    ),
                }),
                makeSuite('Мета-тэги', {
                    story: mergeSuites(
                        prepareSuite(CanonicalUrlSuite, {
                            meta: {
                                id: 'marketfront-2404',
                                issue: 'MARKETVERSTKA-31330',
                            },
                            pageObjects: {
                                canonicalUrl() {
                                    return this.createPageObject(CanonicalUrl);
                                },
                            },
                            params: {
                                expectedUrl: seoTestConfigs.department.testParams.expectedCanonicalUrl,
                            },
                        }),
                        prepareSuite(AlternateUrlSuite, {
                            meta: {
                                id: 'marketfront-2331',
                                issue: 'MARKETVERSTKA-31865',
                            },
                            pageObjects: {
                                alternateUrl() {
                                    return this.createPageObject(AlternateUrl);
                                },
                            },
                            params: {
                                expectedUrl: seoTestConfigs.department.testParams.expectedAlternateUrl,
                            },
                        })
                    ),
                })
            ),
        })
    ),
});
