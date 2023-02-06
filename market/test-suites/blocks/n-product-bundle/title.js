import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на блок n-product-bundle
 * @param {PageObject.ProductBundle} productBundle
 */
export default makeSuite('Заголовок блока комплектов.', {
    feature: 'Карточка модели',
    story: {
        'По умолчанию': {
            'должен иметь ожидаемое значение': makeCase({
                test() {
                    return this.productBundle.getTitle().should.eventually.equal(
                        this.params.title,
                        'Заголовок соответствует ожидаемому'
                    );
                },
            }),
        },
    },
});
