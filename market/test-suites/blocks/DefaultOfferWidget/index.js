import {makeSuite, mergeSuites, makeCase} from 'ginny';

import {
    createProductWithExpressCpaOfferState,
} from '@self/project/src/spec/hermione/fixtures/express';

import DefaultOfferWidget from '@self/platform/widgets/parts/DefaultOffer/__pageObject';
import DefaultOffer from '@self/platform/spec/page-objects/components/DefaultOffer';

export default makeSuite('Офферы модели', {
    story: mergeSuites(
        makeSuite('Есть express-cpa оффер', {
            story: {
                async beforeEach() {
                    this.setPageObjects({
                        recommendedOffers() {
                            return this.createPageObject(DefaultOfferWidget);
                        },
                        firstDefaultOffer() {
                            return this.createPageObject(DefaultOffer, {parent: DefaultOfferWidget.getDefaultOfferContainerByIndex(0)});
                        },
                    });
                },
                'Дефолтный оффер без экспресс доставки.': {
                    async beforeEach() {
                        const {pageId, pageRoute} = this.params;

                        await this.browser.setState('report', createProductWithExpressCpaOfferState({isExpressDefaultOffer: false}));
                        await this.browser.yaOpenPage(pageId, pageRoute);

                        this.setPageObjects({
                            secondDefaultOffer() {
                                return this.createPageObject(DefaultOffer, {parent: DefaultOfferWidget.getDefaultOfferContainerByIndex(1)});
                            },
                        });
                    },
                    'Показываем 2 ДО': makeCase({
                        id: 'marketfront-4935',
                        issue: 'MARKETFRONT-54425',
                        async test() {
                            await this.firstDefaultOffer.isExisting().should.eventually.be.equal(true, 'Есть основной дефолтный оффер');
                            await this.secondDefaultOffer.isExisting().should.eventually.be.equal(true, 'Есть дополнительный дефолтный оффер');

                            await this.expect(this.firstDefaultOffer.isExpress()).to.be.equal(false, 'Основной оффер без шильдика экспресса');
                            await this.expect(this.secondDefaultOffer.isExpress()).to.be.equal(true, 'Дополнительный оффер с шильдиком экспресса');
                        },
                    }),
                },
                'Дефолтный оффер c экспресс доставкой.': {
                    async beforeEach() {
                        const {pageId, pageRoute} = this.params;

                        await this.browser.setState('report', createProductWithExpressCpaOfferState({isExpressDefaultOffer: true}));
                        await this.browser.yaOpenPage(pageId, pageRoute);
                    },
                    'Показываем 1 ДО': makeCase({
                        id: 'marketfront-4935',
                        issue: 'MARKETFRONT-54425',
                        async test() {
                            await this.recommendedOffers.getDefaultOffersCount().should.eventually.be.equal(1, 'Есть всего 1 дефолтный оффер');

                            await this.firstDefaultOffer.isExpress().should.eventually.be.equal(true, 'Дефолтный оффер с шильдиком экспресса');
                        },
                    }),

                },
            },
        })
    ),
});

