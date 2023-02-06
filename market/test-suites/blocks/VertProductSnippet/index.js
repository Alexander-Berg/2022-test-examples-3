import {makeSuite, makeCase} from 'ginny';
import ProductImage from '@self/platform/spec/page-objects/ProductImage';
import ProductName from '@self/platform/spec/page-objects/ProductName';
import ProductPrice from '@self/platform/spec/page-objects/ProductPrice';

/**
 * Тесты для блока VertProductSnippet на главной
 * @param {PageObject.VertProductSnippet} snippet
 */
export default makeSuite('Сниппет продукта на главной.', {
    environment: 'kadavr',
    story: {
        beforeEach() {
            this.setPageObjects({
                productImage: () => this.createPageObject(ProductImage, {parent: this.snippet}),
                productName: () => this.createPageObject(ProductName, {parent: this.snippet}),
                productPrice: () => this.createPageObject(ProductPrice, {parent: this.snippet}),
            });
        },
        'По умолчанию': {
            'имеет фото, цену и название': makeCase({
                id: 'marketfront-2592',
                issue: 'MARKETVERSTKA-29326',
                async test() {
                    const image = await this.productImage.isExisting();
                    const price = await this.productPrice.isExisting();
                    const name = await this.productName.isExisting();
                    return this.expect(image && price && name)
                        .to.be.equal(true, 'Фото, цена и название товара присутвуют на сниппете');
                },
            }),
        },
    },
});
