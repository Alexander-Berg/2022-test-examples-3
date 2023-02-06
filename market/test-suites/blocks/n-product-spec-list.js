import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на блок n-product-spec-list
 * @param {PageObject.ProductSpecList} specList
 */
export default makeSuite('Блок "Характеристики".', {
    feature: 'Карточка модели',
    story: {
        'По умолчанию': {
            'должен отображаться': makeCase({
                test() {
                    return this.specList.isExists()
                        .should.eventually.to.be.equal(true, 'Блок отображается');
                },
            }),

            'должен содержать хотя бы одну характеристику': makeCase({
                test() {
                    return this.specList.getItemsCount().should.eventually.be.greaterThan(0, 'Есть характеристики');
                },
            }),
        },

        'В групповой модели': {
            'должен содержать список характеристик': makeCase({
                test() {
                    return this.specList.getSpecs().then(specs => (
                        this.expect(specs.join('')).not.to.be.empty
                    ));
                },
            }),
        },
    },
});
