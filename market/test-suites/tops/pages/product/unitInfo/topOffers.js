import {mergeSuites, prepareSuite, makeSuite} from 'ginny';
// suits
import mainUnitSuite from '@self/root/src/spec/hermione/test-suites/blocks/unitInfo/mainUnit';
// page-objects
import ProductOffersSnippetPrice from '@self/project/src/components/ProductOffersSnippetPrice/__pageObject__';
// mocks
import {PAGE_IDS_TOUCH} from '@self/root/src/constants/pageIds';
import {
    topState,
    pageRoute,
    expectedOfferPriceText,
} from './fixtures/stateMock';

export default makeSuite('Дефолтный оффер. Единицы измерения.', {
    id: 'm-touch-3954',
    issue: 'MARKETFRONT-77123',
    environment: 'kadavr',
    story: {
        'блок еще предложения.': mergeSuites(
            {
                async beforeEach() {
                    await this.browser.setState('report', topState);
                    return this.browser.yaOpenPage(
                        PAGE_IDS_TOUCH.YANDEX_MARKET_PRODUCT,
                        pageRoute
                    );
                },
            },
            prepareSuite(mainUnitSuite, {
                pageObjects: {
                    parent() {
                        return this.createPageObject(ProductOffersSnippetPrice);
                    },
                },
                params: {
                    expectedPriceText: expectedOfferPriceText,
                },
            })
        ),
    },
});
