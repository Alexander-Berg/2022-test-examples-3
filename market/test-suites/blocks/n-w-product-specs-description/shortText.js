import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на блок n-w-product-specs-description.
 * @param {PageObject.ProductSpecsDescrition} productSpecsDescrition
 */
export default makeSuite('Маркетинговое описание с коротким текстом', {
    feature: 'Маркетинговое описание',
    environment: 'kadavr',
    issue: 'MARKETVERSTKA-35712',
    id: 'marketfront-3683',
    story: {
        'По умолчанию': {
            'заголовок отображается': makeCase({
                async test() {
                    const titleText = this.productSpecsDescrition.getTitleText();
                    await this.expect(titleText).to.be.equal(this.params.expectedTitle);
                },
            }),
            'описание отображается': makeCase({
                async test() {
                    const descriptionVisible = this.productSpecsDescrition.isDescriptionVisible();
                    await this.expect(descriptionVisible).to.be.equal(true, 'Маркетинговое описание отображается');
                },
            }),
        },
    },
});
