import {makeCase, makeSuite, prepareSuite} from 'ginny';
import DefaultOfferIdSuite from '@self/project/src/spec/hermione/test-suites/blocks/DefaultOffer/offer-id';
import DefaultOfferPageObject from '@self/platform/spec/page-objects/components/DefaultOffer';
import VisualEnumFilterPageObject from '@self/platform/containers/VisualEnumFilter/_pageObject';

/**
 * Тесты на блок DefaultOffer.
 *
 * @property {PageObject.DefaultOffer} this.defaultOffer
 * @property {PageObject.VisualEnumFilter} this.visualEnumFilter
 */
export default makeSuite('Валидный идентификатор оффера', {
    story: {
        before() {
            this.setPageObjects({
                defaultOffer: () => this.createPageObject(DefaultOfferPageObject),
                visualEnumFilter: () => this.createPageObject(VisualEnumFilterPageObject),
            });
        },
        'Проверка': prepareSuite(DefaultOfferIdSuite, {
            meta: {
                id: 'm-touch-3477',
            },
        }),
        'При переключении фильтра и возврате на начальное значение': {
            'offerId соответствует ожидаемому': makeCase({
                id: 'm-touch-3478',
                params: {
                    expectedOfferId: 'ожидаемый offerId в ДО',
                    defaultFilterValueId: 'начальное значение фильтра',
                    nextFilterValueId: 'следующее значение фильтра',
                },
                async test() {
                    await this.defaultOffer.isExisting()
                        .should.eventually.to.equal(true, 'Проверяем, что ДО присутствует на странице');
                    await this.defaultOffer.getOfferId()
                        .should.eventually.to.equal(this.params.expectedOfferId, `Проверяем, что offerId в ДО равен "${this.params.expectedOfferId}"`);

                    await this.browser.allure.runStep(
                        'Скроллим до фильтра',
                        () => this.visualEnumFilter
                            .getSelector()
                            .then(selector => this.browser.scroll(selector, 0, -200))
                    );

                    await this.visualEnumFilter.clickOnValueById(this.params.nextFilterValueId);
                    await this.visualEnumFilter.clickOnValueById(this.params.defaultFilterValueId);

                    await this.defaultOffer.isExisting()
                        .should.eventually.to.equal(true, 'Проверяем, что ДО присутствует на странице');
                    return this.defaultOffer.getOfferId()
                        .should.eventually.to.equal(this.params.expectedOfferId, `Проверяем, что offerId в ДО равен "${this.params.expectedOfferId}"`);
                },
            }),
        },
    },
});
