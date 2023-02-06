import {prepareSuite, mergeSuites, makeSuite} from 'ginny';
// suites
import mainUnitSuite from '@self/root/src/spec/hermione/test-suites/blocks/unitInfo/mainUnit';
import referenceUnitSuite from '@self/root/src/spec/hermione/test-suites/blocks/unitInfo/referenceUnit';
// page-objects
import DefaultOffer from '@self/platform/components/DefaultOffer/__pageObject';
// mocks
import {offer, offerId} from './fixtures/offer';

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
                    await this.browser.setState('report', offer);
                    await this.browser.yaOpenPage('market:offer', {offerId});
                },
            },
            prepareSuite(mainUnitSuite, {
                meta: {
                    id: 'marketfront-5682',
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
                    id: 'marketfront-5682',
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
