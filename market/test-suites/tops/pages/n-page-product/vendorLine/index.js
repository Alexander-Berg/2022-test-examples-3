import {mergeSuites, makeSuite, prepareSuite} from 'ginny';
import {PAGE_IDS_DESKTOP} from '@self/root/src/constants/pageIds';

import {defaultSuite, redirectSuite} from '@self/platform//spec/hermione/test-suites/blocks/AllLineProductsLink';
import AllLineProductsLink from '@self/platform/components/AllLineProductsLink/__pageObject';
import ScrollBox from '@self/platform/components/ScrollBox/__pageObject';
import {productId, lineName, productSlug, routeParams, state} from './mocks';

const allLineLinkSuite = makeSuite('Ссылка "Все товары линейки <имя_линейки>"', {
    story: mergeSuites(
        {
            async beforeEach() {
                this.setPageObjects({
                    allLineProductsLink: () => this.createPageObject(AllLineProductsLink),
                });
            },
        },
        prepareSuite(defaultSuite, {
            meta: {
                id: 'marketfront-5291',
                issue: 'MARKETFRONT-67247',
            },
            params: {
                expectedLinkText: `Все товары линейки ${lineName}`,
                routeParams,
            },
        }),
        prepareSuite(redirectSuite, {
            meta: {
                id: 'marketfront-5309',
                issue: 'MARKETFRONT-69109',
            },
            params: {
                routeParams,
            },
        })
    ),
});

const scrollBoxSuite = makeSuite('Ссылка "Смотреть все" в карусели "Товары из той же линейки"', {
    id: 'marketfront-5309',
    issue: 'MARKETFRONT-69109',
    story: mergeSuites(
        {
            async beforeEach() {
                this.setPageObjects({
                    allLineProductsLink: () => this.createPageObject(ScrollBox),
                });
            },
        },
        prepareSuite(defaultSuite, {
            params: {
                expectedLinkText: 'Смотреть все',
                routeParams,
            },
        }),
        prepareSuite(redirectSuite, {
            params: {
                routeParams,
            },
        })
    ),
});

export default makeSuite('Ссылка товаров одной линейки', {
    environment: 'kadavr',
    feature: 'Ссылка товаров одной линейки',
    story: mergeSuites(
        {
            async beforeEach() {
                await this.browser.setState('report', state);
                return this.browser.yaOpenPage(PAGE_IDS_DESKTOP.YANDEX_MARKET_PRODUCT, {
                    productId,
                    slug: productSlug,
                });
            },
        },
        prepareSuite(allLineLinkSuite),
        prepareSuite(scrollBoxSuite)
    ),
});
