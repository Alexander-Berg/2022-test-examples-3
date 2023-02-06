import {makeCase, makeSuite} from 'ginny';

/**
 * Тесты на отсутствие блока n-w-shop-info.
 * @param {PageObject.ShopsInfo} shopsInfo
 */
export default makeSuite('Блок с информацией о продавце.', {
    feature: 'Карточка модели',
    story: {
        'Ссылка о продавце': {
            'для товара не в продаже': {
                'должна отсутствовать на странице': makeCase({
                    id: 'marketfront-1866',
                    test() {
                        return this.shopsInfo.isBlockExists()
                            .should.eventually.to.be.equal(false, 'Блок отсутствует на странице');
                    },

                }),
            },
        },
    },
});
