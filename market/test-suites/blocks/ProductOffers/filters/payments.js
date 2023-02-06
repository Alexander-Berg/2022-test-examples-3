import {makeCase, makeSuite} from 'ginny';

/**
 * Тесты на фильтр способов оплаты.
 *
 * @property {PageObject.ProductOffers} this.productOffers
 * @property {PageObject.FilterCompound} this.filterCompound
 */
export default makeSuite('Фильтр на «Способы оплаты»', {
    params: {
        filterName: 'Название фильтра',
    },
    story: {
        'При открытии фильтров': {
            'должен присутствовать фильтр «Способы оплаты»': makeCase({
                id: 'm-touch-2856',
                issue: 'MOBMARKET-12541',
                async test() {
                    await this.productOffers.waitForFiltersBtnVisible();
                    return this.productOffers.clickOnAllFiltersButton()
                        .then(() => this.filterCompound.getFilterNameValue()
                            .should.eventually.to.equal(
                                this.params.filterName,
                                `фильтр должен быть равен - ${this.params.filterName}`
                            )
                        );
                },
            }),
        },
    },
});
