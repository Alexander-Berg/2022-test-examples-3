import {makeSuite, makeCase} from 'ginny';


/**
 * Тест на компонент Filters
 * @property {PageObject.Filters} filters - попап с фильтрами
 * @property {PageObject.ProductOffers} productOffers - блок с предложениями магазинов
 */
export default makeSuite('Попап с фильтрами', {
    story: {
        'По умолчанию': {
            'отображается': makeCase({
                issue: 'MARKETFRONT-5212',
                id: 'm-touch-3072',
                feature: 'Фильтры',
                async test() {
                    await this.productOffers.waitForFiltersBtnVisible();
                    await this.productOffers.clickOnAllFiltersButton();
                    return this.filters.isVisible();
                },
            }),
        },
    },
});
