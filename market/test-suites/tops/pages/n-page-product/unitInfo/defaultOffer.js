import {prepareSuite, mergeSuites, makeSuite} from 'ginny';
import {mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';

// suites
import mainUnitSuite from '@self/root/src/spec/hermione/test-suites/blocks/unitInfo/mainUnit';
import referenceUnitSuite from '@self/root/src/spec/hermione/test-suites/blocks/unitInfo/referenceUnit';
// page-objects
import DefaultOffer from '@self/platform/components/DefaultOffer/__pageObject';
// mocks
import {product, productId, slug} from './fixtures/product';
import {offer} from './fixtures/offer';

/**
 * Тесты на единицы измерения продажи товара в ДО
 * @param {PageObject.DefaultOffer} DefaultOffer
 */
export default makeSuite('Дефолтный оффер. Единицы измерения.', {
    environment: 'kadavr',
    story: {
        'ДО с единицами измерения.': mergeSuites(
            {
                async beforeEach() {
                    await this.browser.setState('report', mergeState([
                        product,
                        offer,
                    ]));
                    return this.browser.yaOpenPage('market:product', {productId, slug});
                },
            },
            prepareSuite(mainUnitSuite, {
                meta: {
                    id: 'marketfront-5401',
                    issue: 'MARKETFRONT-72689',
                },
                pageObjects: {
                    parent() {
                        return this.createPageObject(DefaultOffer);
                    },
                },
            }),
            prepareSuite(referenceUnitSuite, {
                meta: {
                    id: 'marketfront-5401',
                    issue: 'MARKETFRONT-72689',
                },
                pageObjects: {
                    parent() {
                        return this.createPageObject(DefaultOffer);
                    },
                },
            })
        ),
    },
});
