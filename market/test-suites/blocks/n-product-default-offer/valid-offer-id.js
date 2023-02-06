import {makeSuite, prepareSuite, makeCase} from 'ginny';
import DefaultOfferIdSuite from '@self/project/src/spec/hermione/test-suites/blocks/DefaultOffer/offer-id';
import DefaultOfferPageObject from '@self/platform/components/DefaultOffer/__pageObject';
import ColorFilterPageObject from '@self/platform/components/ColorFilter/__pageObject__';
import PreloadablePageObject from '@self/platform/spec/page-objects/preloadable';

export default makeSuite('Валидный идентификатор оффера', {
    story: {
        before() {
            this.setPageObjects({
                defaultOffer: () => this.createPageObject(DefaultOfferPageObject),
                colorFilter: () => this.createPageObject(ColorFilterPageObject),
                preloadable: () => this.createPageObject(PreloadablePageObject, {parent: this.defaultOffer}),
            });
        },
        'Проверка': prepareSuite(DefaultOfferIdSuite, {
            meta: {
                id: 'marketfront-4392',
            },
        }),
        'При переключении фильтра и возврате на начальное значение': {
            'offerId соответствует ожидаемому': makeCase({
                id: 'marketfront-4394',
                params: {
                    expectedOfferId: 'ожидаемый offerId в ДО',
                },
                async test() {
                    await this.defaultOffer.isExisting()
                        .should.eventually.to.equal(true, 'Проверяем, что ДО присутствует на странице');
                    await this.defaultOffer.getOfferId()
                        .should.eventually.to.equal(this.params.expectedOfferId, `Проверяем, что offerId в ДО равен "${this.params.expectedOfferId}"`);

                    await this.colorFilter.selectColor(1);
                    await this.preloadable.waitForLoaded();

                    await this.colorFilter.clickReset();
                    await this.preloadable.waitForLoaded();

                    await this.defaultOffer.isExisting()
                        .should.eventually.to.equal(true, 'Проверяем, что ДО присутствует на странице');
                    return this.defaultOffer.getOfferId()
                        .should.eventually.to.equal(this.params.expectedOfferId, `Проверяем, что offerId в ДО равен "${this.params.expectedOfferId}"`);
                },
            }),
        },
    },
});
