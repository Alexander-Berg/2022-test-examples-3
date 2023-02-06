import {prepareSuite, mergeSuites, makeSuite} from 'ginny';
import {mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';

// suites
import mainUnitSuite from '@self/root/src/spec/hermione/test-suites/blocks/unitInfo/mainUnit';
// page-objects
import MiniTopOffers from '@self/platform/spec/page-objects/widgets/content/MiniTopOffers';
// mocks
import {product, productId, slug} from './fixtures/product';
import {offer} from './fixtures/offerWithTop6';

/**
 * Тесты на единицы измерения продажи товара в TOP6
 * @param {PageObject.MiniTopOffers}
 */
export default makeSuite('TOP6. Единицы измерения.', {
    environment: 'kadavr',
    story: {
        'TOP6 с единицами измерения.': mergeSuites(
            {
                async beforeEach() {
                    await this.browser.setState('report', mergeState([
                        {
                            data: {
                                search: {
                                    // нужно чтобы отобразился блок топ-6
                                    totalOffersBeforeFilters: 2,
                                },
                            },
                        },
                        product,
                        offer,
                    ]));
                    return this.browser.yaOpenPage('market:product', {productId, slug});
                },
            },
            prepareSuite(mainUnitSuite, {
                meta: {
                    id: 'marketfront-5681',
                    issue: 'MARKETFRONT-75554',
                },
                pageObjects: {
                    parent() {
                        return this.createPageObject(MiniTopOffers);
                    },
                },
            })
        ),
    },
});
