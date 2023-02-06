import {prepareSuite, mergeSuites, makeSuite} from 'ginny';
// suites
import mainUnitSuite from '@self/root/src/spec/hermione/test-suites/blocks/unitInfo/mainUnit/secondDO';
import referenceUnitSuite from '@self/root/src/spec/hermione/test-suites/blocks/unitInfo/referenceUnit';
// page-objects
import DefaultOfferPrice from '@self/platform/components/DefaultOffer/PriceInfo/Price/__pageObject/';
// mocks
import {PAGE_IDS_TOUCH} from '@self/root/src/constants/pageIds';
import {
    reportState,
    pageRoute,
    expectedFirstDOMainPriceText,
    expectedSecondDOMainPriceText,
    expectedReferencePriceText,
} from './fixtures/stateMock';

/**
 * Тесты на единицы измерения продажи товара в ДО
 * @param {PageObject.DefaultOffer} DefaultOffer
 */
export default makeSuite('Дефолтный оффер. Единицы измерения.', {
    id: 'm-touch-3954',
    issue: 'MARKETFRONT-77123',
    environment: 'kadavr',
    story: {
        'ДО с единицами измерения.': mergeSuites(
            {
                async beforeEach() {
                    await this.browser.setState('report', reportState);
                    return this.browser.yaOpenPage(
                        PAGE_IDS_TOUCH.YANDEX_MARKET_PRODUCT,
                        pageRoute
                    );
                },
            },
            prepareSuite(mainUnitSuite, {
                pageObjects: {
                    price() {
                        return this.createPageObject(DefaultOfferPrice);
                    },
                },
                params: {
                    expectedFirstDOMainPriceText: expectedFirstDOMainPriceText,
                    expectedSecondDOMainPriceText: expectedSecondDOMainPriceText,
                },
            }),
            prepareSuite(referenceUnitSuite, {
                pageObjects: {
                    parent() {
                        return this.createPageObject(DefaultOfferPrice);
                    },
                },
                params: {
                    expectedPriceText: expectedReferencePriceText,
                },
            })
        ),
    },
});
