import {assign} from 'lodash/fp';
import {prepareSuite, makeSuite, mergeSuites} from 'ginny';
import {mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';
import mergeReportState from '@yandex-market/kadavr/mocks/Report/helpers/mergeState';

import {createStories} from '@self/platform/spec/hermione/helpers/createStories';
// configs
import {routes} from '@self/platform/spec/hermione/configs/routes';
import seoTestConfigs from '@self/platform/spec/hermione/configs/seo/product-page';
// suites
import BaseSuite from '@self/platform/spec/hermione/test-suites/blocks/n-base';
import BaseLinkCanonicalSuite from '@self/platform/spec/hermione/test-suites/blocks/n-base/__link-canonical';
import ProductOffersStaticListAllOffersSuite from
    '@self/platform/spec/hermione/test-suites/blocks/ProductOffersStaticList/allOffers';
import ProductOffersSnippetListSuite from '@self/platform/spec/hermione/test-suites/blocks/ProductOffersSnippetList';
import ProductOffersSnippetClickOutLinkSuite from
    '@self/platform/spec/hermione/test-suites/blocks/ProductOffersSnippet/clickOutLink';
import ProductOffersSnippetPriceSuite from '@self/platform/spec/hermione/test-suites/blocks/ProductOffersSnippet/price';
import ProductOffersSnippetShopRatingSuite from '@self/platform/spec/hermione/test-suites/blocks/ProductOffersSnippet/shopRating';
import DefaultOfferSuite from '@self/platform/spec/hermione/test-suites/blocks/DefaultOffer';
import ProductOrOfferComplaintSuite from '@self/platform/spec/hermione/test-suites/blocks/ProductOrOfferComplaint';

// page-objects
import DefaultOffer from '@self/platform/spec/page-objects/components/DefaultOffer';
import ProductOffersStaticList from '@self/platform/widgets/parts/ProductOffersStaticList/__pageObject';
import ProductOffersSnippetList from '@self/platform/containers/ProductOffersSnippetList/__pageObject';
import ProductOfferSnippet from '@self/platform/spec/page-objects/components/ProductOffersSnippet';
import Base from '@self/platform/spec/page-objects/n-base';
import ProductComplaintButton from '@self/platform/spec/page-objects/components/ProductComplaintButton';

import ComplaintForm from '@self/platform/spec/page-objects/components/ComplaintForm';
import ComplaintFormSubmitButton from '@self/platform/spec/page-objects/components/ComplaintForm/SubmitButton';
import ComplaintFormHeader from '@self/platform/spec/page-objects/components/ComplaintForm/Header';
import Notification from '@self/root/src/components/Notification/__pageObject';

import {productWithDefaultOffer, productWithPicture, phoneProductRoute} from '@self/platform/spec/hermione/fixtures/product';
import {
    buildProductOffersResultsState,
    URL_TO_SHOP,
    PRICE,
} from '@self/platform/spec/hermione/fixtures/product/productOffers';


import {picture, product} from './kadavrMocks';

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Страница "Характеристики" карточки модели.', {
    environment: 'testing',
    story: mergeSuites(
        makeSuite('Групповая модель.', {
            story: prepareSuite(BaseSuite, {
                hooks: {
                    beforeEach() {
                        const queryParams = routes.product.mattress;
                        return this.browser
                            .yaSimulateBot()
                            .yaOpenPage('touch:product-spec', queryParams)
                            .then(() => {
                                this.params = assign(
                                    {
                                        expectedCanonicalLink: 'https://market.yandex.ru/product--' +
                                            `${queryParams.slug}/${queryParams.productId}/spec`,
                                        expectedOpenGraphDescription: 'Подробные характеристики' +
                                            ' модели Матрас Аскона Victory' +
                                            ' — с описанием всех особенностей.' +
                                            ' А также цены, рейтинг магазинов и отзывы покупателей.',
                                        expectedOpenGraphTitle: 'Характеристики модели Матрас Аскона Victory' +
                                            ' — Матрасы — Яндекс.Маркет',
                                    },
                                    this.params
                                );
                            });
                    },
                },
                pageObjects: {
                    base() {
                        return this.createPageObject(Base);
                    },
                },
            }),
        }),
        makeSuite('Гуру-модель.', {
            environment: 'kadavr',
            story: prepareSuite(BaseSuite, {
                hooks: {
                    async beforeEach() {
                        const {routeParams, testParams, mocks} = product.guru;
                        const productWithPictureState = mergeReportState([mocks.report, picture]);

                        await this.browser.setState('report', productWithPictureState);
                        await this.browser.yaSimulateBot();
                        await this.browser.yaOpenPage('touch:product-spec', routeParams);

                        this.params = assign(
                            testParams,
                            this.params
                        );
                    },
                },
                pageObjects: {
                    base() {
                        return this.createPageObject(Base);
                    },
                },
            }),
        }),
        makeSuite('Кластер.', {
            environment: 'kadavr',
            story: prepareSuite(BaseSuite, {
                hooks: {
                    async beforeEach() {
                        const {routeParams, testParams, mocks} = product.guru;
                        const productWithPictureState = mergeReportState([mocks.report, picture]);

                        await this.browser.setState('report', productWithPictureState);
                        await this.browser.yaSimulateBot();
                        await this.browser.yaOpenPage('touch:product-spec', routeParams);

                        this.params = assign(
                            testParams,
                            this.params
                        );
                    },
                },
                pageObjects: {
                    base() {
                        return this.createPageObject(Base);
                    },
                },
            }),
        }),
        makeSuite('Книга.', {
            story: prepareSuite(BaseSuite, {
                params: {
                    // MOBMARKET-10116: skip падающих тестов на opengraph:image,
                    // тикет на починку MOBMARKET-9726
                    skipOpenGraphImage: true,
                },
                hooks: {
                    beforeEach() {
                        const queryParams = routes.product.book;
                        return this.browser
                            .yaSimulateBot()
                            .yaOpenPage('touch:product-spec', queryParams)
                            .then(() => {
                                this.params = assign(
                                    {
                                        expectedCanonicalLink: 'https://market.yandex.ru/product--' +
                                            `${queryParams.slug}/${queryParams.productId}/spec`,
                                        expectedOpenGraphDescription: 'Подробные характеристики' +
                                            ' книги Пауло Коэльо "Алхимик"' +
                                            ' — с описанием всех особенностей.' +
                                            ' А также цены, рейтинг магазинов и отзывы покупателей.',
                                        expectedOpenGraphTitle: 'Характеристики книги Пауло Коэльо "Алхимик"' +
                                            ' — Зарубежная проза и поэзия — Яндекс.Маркет',
                                    },
                                    this.params
                                );
                            });
                    },
                },
                pageObjects: {
                    base() {
                        return this.createPageObject(Base);
                    },
                },
            }),
        }),

        makeSuite('SEO-разметка страницы.', {
            story: createStories(
                seoTestConfigs.pageCanonical,
                ({routeConfig, testParams, description}) => prepareSuite(BaseLinkCanonicalSuite, {
                    hooks: {
                        beforeEach() {
                            if (description === 'Тип "Визуальная"') {
                                // eslint-disable-next-line market/ginny/no-skip
                                return this.skip(
                                    'MOBMARKET-10116: Скипаем падающие автотесты, ' +
                                    'тикет на починку MOBMARKET-9726'
                                );
                            }

                            return this.browser
                                .yaSimulateBot()
                                .yaOpenPage('touch:product-spec', routeConfig);
                        },
                    },
                    pageObjects: {
                        base() {
                            return this.createPageObject(Base);
                        },
                    },
                    params: testParams.spec,
                })
            ),
        }),
        makeSuite('Статичный виджет ДО и Топ6', {
            environment: 'kadavr',
            story: mergeSuites({
                async beforeEach() {
                    const state = buildProductOffersResultsState({
                        offersCount: 100,
                    });
                    await this.browser.setState('report', state);

                    await this.browser.yaOpenPage('touch:product-spec', phoneProductRoute);

                    // Виджет с ДО загружается лениво.
                    await this.browser.yaSlowlyScroll();
                },

                'Содержимое.': mergeSuites(
                    prepareSuite(ProductOffersStaticListAllOffersSuite, {
                        params: {
                            productId: phoneProductRoute.productId,
                            slug: phoneProductRoute.slug,
                        },
                        pageObjects: {
                            productOffersStaticList() {
                                return this.createPageObject(ProductOffersStaticList);
                            },
                        },

                        hooks: {
                            async beforeEach() {
                                await this.productOffersStaticList.waitForVisible();
                            },
                        },
                    }),
                    prepareSuite(ProductOffersSnippetListSuite, {
                        pageObjects: {
                            productOffersSnippetList() {
                                return this.createPageObject(ProductOffersSnippetList);
                            },
                        },
                        hooks: {
                            async beforeEach() {
                                await this.productOffersSnippetList.waitForVisible();
                            },
                        },
                    })
                ),
                'Сниппет Топ 6.': mergeSuites(
                    prepareSuite(ProductOffersSnippetClickOutLinkSuite, {
                        meta: {
                            id: 'm-touch-2886',
                            issue: 'MOBMARKET-12583',
                        },
                        params: {
                            expectedHref: URL_TO_SHOP,
                        },
                        pageObjects: {
                            offerSnippet() {
                                return this.createPageObject(ProductOfferSnippet);
                            },
                        },
                    }),
                    prepareSuite(ProductOffersSnippetPriceSuite, {
                        meta: {
                            id: 'm-touch-2888',
                            issue: 'MOBMARKET-12583',
                        },
                        params: {
                            expectedPriceValue: PRICE.value,
                            expectedPriceCurrency: PRICE.currency,
                        },
                        pageObjects: {
                            offerSnippet() {
                                return this.createPageObject(ProductOfferSnippet);
                            },
                        },
                    }),
                    prepareSuite(ProductOffersSnippetShopRatingSuite, {
                        meta: {
                            id: 'm-touch-2796',
                            issue: 'MOBMARKET-12244',
                        },
                        pageObjects: {
                            offerSnippet() {
                                return this.createPageObject(ProductOfferSnippet);
                            },
                        },
                    })
                ),
            }),
        }),
        makeSuite('Дефолтный оффер', {
            environment: 'kadavr',
            story: prepareSuite(DefaultOfferSuite, {
                pageObjects: {
                    defaultOffer() {
                        return this.createPageObject(DefaultOffer);
                    },
                },
                hooks: {
                    async beforeEach() {
                        const state = mergeState([
                            productWithDefaultOffer,
                            buildProductOffersResultsState({
                                offersCount: 6,
                            }),
                        ]);

                        await this.browser.setState('report', state);

                        await this.browser.yaOpenPage('touch:product-spec', phoneProductRoute);

                        // Виджет с ДО загружается лениво.
                        await this.browser.yaSlowlyScroll();

                        await this.defaultOffer.waitForVisible();
                    },
                },
            }),
        }),
        makeSuite('Жалобы', {
            environment: 'kadavr',
            story: prepareSuite(ProductOrOfferComplaintSuite, {
                hooks: {
                    async beforeEach() {
                        await this.browser.setState('report', productWithPicture);
                        await this.browser.yaOpenPage('touch:product-spec', phoneProductRoute);
                        await this.productComplaintButton.waitForVisible();
                        await this.productComplaintButton.clickProductComplaintButton();
                    },
                },
                pageObjects: {
                    productComplaintButton() {
                        return this.createPageObject(ProductComplaintButton);
                    },
                    notification() {
                        return this.createPageObject(Notification);
                    },
                    complaintForm() {
                        return this.createPageObject(ComplaintForm);
                    },
                    complaintFormSubmitButton() {
                        return this.createPageObject(ComplaintFormSubmitButton);
                    },
                    complaintFormHeader() {
                        return this.createPageObject(ComplaintFormHeader);
                    },
                },
            }),

        })
    ),
});
