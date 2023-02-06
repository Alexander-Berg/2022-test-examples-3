import {makeSuite, prepareSuite} from 'ginny';
import DefaultOfferIdSuite from '@self/project/src/spec/hermione/test-suites/blocks/DefaultOffer/offer-id';
import DefaultOfferPageObject from '@self/platform/spec/page-objects/components/DefaultOffer';

/**
 * Тесты на блок DefaultOffer.
 *
 * @property {PageObject.DefaultOffer} this.defaultOffer
 */
export default makeSuite('Невалидный идентификатор оффера', {
    story: {
        before() {
            this.setPageObjects({
                defaultOffer: () => this.createPageObject(DefaultOfferPageObject),
            });
        },
        'Проверка': prepareSuite(DefaultOfferIdSuite, {
            meta: {
                id: 'm-touch-3507',
            },
        }),
    },
});
