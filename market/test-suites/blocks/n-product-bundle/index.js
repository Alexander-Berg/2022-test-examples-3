import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на блок n-product-bundle
 * @param {PageObject.ProductBundle} productBundle
 */
export default makeSuite('Блок комплектов.', {
    feature: 'Карточка модели',
    story: {
        'По умолчанию': {
            'должен отображаться': makeCase({
                test() {
                    return this.productBundle.root.isVisible()
                        .should.eventually.equal(true, 'Блок отображается');
                },
            }),
        },
    },
});
