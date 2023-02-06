import {mergeSuites, prepareSuite, makeSuite} from 'ginny';

import seoTestConfigs from '@self/platform/spec/hermione/configs/seo/brand-page';
import {routes} from '@self/platform/spec/hermione/configs/routes';
// suites
import BrandHeadlineSuite from '@self/platform/spec/hermione/test-suites/blocks/BrandHeadline';
import BrandHeadlineAboutSuite from '@self/platform/spec/hermione/test-suites/blocks/BrandHeadline/__about';
import LogoCarouselSuite from '@self/platform/spec/hermione/test-suites/blocks/LogoCarousel';
import ScrollBoxSuite from '@self/platform/spec/hermione/test-suites/blocks/ScrollBox';
import RecommendedShopsInformingSuite from '@self/platform/spec/hermione/test-suites/blocks/RecommendedShopsInforming';
import CanonicalUrlSuite from '@self/platform/spec/hermione/test-suites/blocks/canonical-url';
import PageTitleSuite from '@self/platform/spec/hermione/test-suites/blocks/page-title';
import PageDescriptionSuite from '@self/platform/spec/hermione/test-suites/blocks/page-description';
// page-objects
import BrandHeadline from '@self/platform/spec/page-objects/BrandHeadline';
import LogoCarousel from '@self/platform/widgets/content/LogoCarousel/__pageObject';
import BrandNavnodes from '@self/platform/spec/page-objects/n-brand-navnodes';
import RecommendedShopsInforming from '@self/platform/spec/page-objects/RecommendedShopsInforming';
import CanonicalUrl from '@self/platform/spec/page-objects/canonical-url';
import PageMeta from '@self/platform/spec/page-objects/pageMeta';
import ScrollBox from '@self/platform/spec/page-objects/ScrollBox';
// mocks
import brandPageMock from './brand.mock';

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Страница бренда.', {
    environment: 'testing',
    feature: 'Структура страницы',
    story: mergeSuites(
        makeSuite('Бесплатная страница бренда.', {
            issue: 'AUTOTESTMARKET-4132',
            environment: 'kadavr',
            story: mergeSuites(
                {
                    async beforeEach() {
                        await this.browser.setState('Tarantino.data.result', [brandPageMock.Tarantino]);
                        await this.browser.setState('report', brandPageMock.report);
                        this.setPageObjects({
                            brandHeadline: () => this.createPageObject(BrandHeadline),
                        });

                        const params = routes.brands.free;
                        return this.browser.yaOpenPage('market:brands', params);
                    },
                },

                prepareSuite(BrandHeadlineSuite),

                prepareSuite(BrandHeadlineAboutSuite),


                // TODO: MARKETFRONT-11725 написать PO и Suite для RecommendationFeedSuite
                // prepareSuite(RecommendationFeedSuite, {
                //     pageObjects: {
                //         feed() {
                //             return this.createPageObject(RecommendationFeed);
                //         },
                //     },
                // }),

                makeSuite('Блок "Смотрите также"', {
                    story: mergeSuites(
                        {
                            beforeEach() {
                                this.setPageObjects({
                                    popularBrandsCarousel: () => (this.createPageObject(LogoCarousel)),
                                    scrollBox: () => this.createPageObject(ScrollBox, {parent: LogoCarousel.root}),
                                });
                            },
                        },
                        prepareSuite(LogoCarouselSuite, {
                            params: {
                                title: 'Смотрите также',
                                url: {
                                    pathname: 'brands--.+/[0-9]+',
                                },
                            },
                        }),

                        prepareSuite(ScrollBoxSuite, {
                            hooks: {
                                beforeEach() {
                                    // eslint-disable-next-line market/ginny/no-skip
                                    return this.skip(
                                        'MARKETFRONT-12460 Тест на скроллбоксы падает из-за стиля с отменой анимации'
                                    );
                                    // eslint-disable-next-line no-unreachable
                                    this.logoCarousel.scrollIntoViewport();
                                },
                            },
                        })
                    ),
                })
            ),
        }),

        makeSuite('Платная страница бренда.', {
            issue: 'AUTOTESTMARKET-4133',
            story: mergeSuites(
                {
                    beforeEach() {
                        this.setPageObjects({
                            brandHeadline: () => this.createPageObject(BrandHeadline),
                            navNodes: () => this.createPageObject(BrandNavnodes),
                        });

                        const params = routes.brands.paid;
                        return this.browser.yaOpenPage('market:brands', params);
                    },
                },

                prepareSuite(BrandHeadlineSuite),

                prepareSuite(BrandHeadlineAboutSuite),


                prepareSuite(RecommendedShopsInformingSuite, {
                    pageObjects: {
                        shopsInforming() {
                            return this.createPageObject(RecommendedShopsInforming);
                        },
                    },
                })
            ),
        }),

        makeSuite('SEO-разметка страницы.', {
            story: mergeSuites(
                prepareSuite(CanonicalUrlSuite, {
                    pageObjects: {
                        canonicalUrl() {
                            return this.createPageObject(CanonicalUrl);
                        },
                    },
                    hooks: {
                        beforeEach() {
                            const {routeParams} = seoTestConfigs.canonicalUrl;

                            return this.browser.yaOpenPage('market:brands', routeParams);
                        },
                    },
                    params: seoTestConfigs.canonicalUrl.testParams,
                }),
                prepareSuite(PageTitleSuite, {
                    meta: {
                        environment: 'kadavr',
                    },
                    hooks: {
                        async beforeEach() {
                            await this.browser.setState('Tarantino.data.result', [{
                                entity: 'page',
                                id: 153061,
                                type: 'brand',
                                info: {
                                    seo: {
                                        // Этот заголовок будет устанавливать только в качестве fallback,
                                        // если странице не сможет построить его по данным. В данном тесте
                                        // он устанавливаться не должен.
                                        title: 'Samsung (Самсунг) — Каталог товаров — Яндекс.Маркет',
                                    },
                                },
                            }]);

                            await this.browser.yaOpenPage('market:brands', {
                                brandId: 153061,
                                slug: 'samsung',
                            });
                        },
                    },
                    params: {
                        expectedTitle: 'Samsung — Каталог товаров бренда — Купить Samsung на Яндекс Маркете',
                    },
                }),
                prepareSuite(PageDescriptionSuite, {
                    hooks: {
                        beforeEach() {
                            const {routeParams} = seoTestConfigs.pageDescription;

                            return this.browser.yaOpenPage('market:brands', routeParams);
                        },
                    },
                    meta: {
                        environment: 'testing',
                    },
                    pageObjects: {
                        pageMeta() {
                            return this.createPageObject(PageMeta);
                        },
                    },
                    params: seoTestConfigs.pageDescription.testParams,
                })
            ),
        })
    ),
});
