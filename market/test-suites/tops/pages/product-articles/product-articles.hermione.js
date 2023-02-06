/*
// Прячем обзоры до лучших времён см.MARKETFRONT-76293
import {makeSuite, prepareSuite, mergeSuites} from 'ginny';
import {mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';

import {buildProductOffersResultsState} from '@self/platform/spec/hermione/fixtures/product/productOffers';
// suites
import ProductVideoListSuite from '@self/platform/spec/hermione/test-suites/blocks/ProductVideoList';
import ProductArticleListSuite from '@self/platform/spec/hermione/test-suites/blocks/ProductArticleList';
import ScrollBoxSuite from '@self/platform/spec/hermione/test-suites/blocks/ScrollBox';
import ProductOffersSnippetListSuite from '@self/platform/spec/hermione/test-suites/blocks/ProductOffersSnippetList';
import DefaultOfferSuite from '@self/platform/spec/hermione/test-suites/blocks/DefaultOffer';
// page-objects
import ProductVideoList from '@self/platform/spec/page-objects/widgets/content/ProductVideoList';
import ProductArticleList from '@self/platform/spec/page-objects/widgets/content/ProductArticleList';
import ScrollBox from '@self/root/src/components/ScrollBox/__pageObject';
import DefaultOffer from '@self/platform/spec/page-objects/components/DefaultOffer';
import Separator from '@self/platform/spec/page-objects/components/Separator';
import ProductOffersSnippetList from '@self/platform/containers/ProductOffersSnippetList/__pageObject';
import {productWithDefaultOffer, phoneProductRoute} from '@self/platform/spec/hermione/fixtures/product';

import {tarantinoState as productWithVideoTarantinoState} from '../product/fixtures/productWithVideo';
import relevantPagesMock from '../journal-article/fixtures/relevant-pages';

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Страница "Обзоры" карточки модели.', {
    environment: 'kadavr',
    story: mergeSuites(
        {
            async beforeEach() {
                this.setPageObjects({
                    productOffersSnippetList: () => this.createPageObject(ProductOffersSnippetList),
                });

                const state = mergeState([
                    productWithDefaultOffer,
                    buildProductOffersResultsState({
                        offersCount: 100,
                    }),
                ]);
                await this.browser.setState('report', state);
            },
        },
        prepareSuite(ProductVideoListSuite, {
            hooks: {
                async beforeEach() {
                    await this.browser.setState('Tarantino.data.result', productWithVideoTarantinoState);
                    return this.browser.yaOpenPage('touch:product-articles', phoneProductRoute);
                },
            },
            pageObjects: {
                productVideoList() {
                    return this.createPageObject(ProductVideoList);
                },
            },
        }),
        prepareSuite(ProductArticleListSuite, {
            hooks: {
                beforeEach() {
                    return this.browser.yaOpenPage('touch:product-articles', phoneProductRoute);
                },
            },
            pageObjects: {
                productArticleList() {
                    return this.createPageObject(ProductArticleList);
                },
            },
        }),
        prepareSuite(ScrollBoxSuite, {
            hooks: {
                async beforeEach() {
                    await this.browser.setState('Tarantino.data.result', relevantPagesMock);
                    return this.browser.yaOpenPage('touch:product-articles', phoneProductRoute);
                },
            },
            meta: {
                id: 'm-touch-2962',
                issue: 'MOBMARKET-13174',
            },
            pageObjects: {
                ScrollBox() {
                    return this.createPageObject(ScrollBox, {
                        parent: `${Separator.root}:nth-child(4)`,
                    });
                },
            },
        }),
        prepareSuite(ProductOffersSnippetListSuite, {
            hooks: {
                beforeEach() {
                    return this.browser.yaOpenPage('touch:product-articles', phoneProductRoute);
                },
            },
        }),
        prepareSuite(DefaultOfferSuite, {
            hooks: {
                async beforeEach() {
                    await this.browser.yaOpenPage('touch:product-articles', phoneProductRoute);
                    // Виджет с ДО загружается лениво.
                    await this.browser.yaSlowlyScroll();
                    return this.productOffersSnippetList.waitForVisible();
                },
            },
            pageObjects: {
                defaultOffer() {
                    return this.createPageObject(DefaultOffer);
                },
            },
        })
    ),
});
*/
