import {mergeSuites, makeSuite, prepareSuite} from 'ginny';
import {PAGE_IDS_TOUCH} from '@self/root/src/constants/pageIds/index.js';

import {allLineLinkSuite} from '@self/platform/spec/hermione/test-suites/blocks/AllLineProductsLink';
import ScrollBox from '@self/root/src/widgets/content/RootScrollBox/__pageObject';
import WidgetWrapper from '@self/root/src/components/WidgetWrapper/__pageObject';

import {
    productId,
    slug,
    routeParams,
    state,
} from './mocks';

export default makeSuite('Ссылка товаров одной линейки.', {
    environment: 'kadavr',
    feature: 'Ссылка товаров одной линейки',
    id: 'm-touch-3906',
    issue: 'MARKETFRONT-69409',
    story: mergeSuites(
        {
            async beforeEach() {
                await this.browser.setState('report', state);
                this.browser.yaOpenPage(PAGE_IDS_TOUCH.YANDEX_MARKET_PRODUCT, {
                    productId,
                    slug,
                });
                this.setPageObjects({
                    productsFromTheLineScrollbox() {
                        return this.createPageObject(ScrollBox, {
                            root: '[data-apiary-widget-id="/content/productsFromTheLine"]',
                        });
                    },
                    allLineProductsWidgetWrapper: () => this.createPageObject(WidgetWrapper, {
                        parent: this.productsFromTheLineScrollbox,
                    }),
                });
            },
        },
        prepareSuite(allLineLinkSuite, {
            params: {
                expectedLinkText: 'смотреть все',
                routeParams,
            },
        })
    ),
});
