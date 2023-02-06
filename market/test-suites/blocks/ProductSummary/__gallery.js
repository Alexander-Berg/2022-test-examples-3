import _ from 'lodash';
import {makeSuite, makeCase} from 'ginny';

const makeTestForAttribute = function (attributeName) {
    return async function () {
        const productName = await this.productTitle.getHeaderTitleText();
        const attributeValue = await this.productGallery.getMainImageAttribute(attributeName);

        return this.expect(attributeValue).to.be.equal(
            productName,
            `Атрибут "${attributeName}" главного изображения товара должен быть равен имени ` +
            `товара: "${productName}"`
        ).then(async () => {
            const thumbs = await this.productGallery.thumbs.then(({value}) => value);

            // INFO: первое изображение пропускаем специально.
            thumbs.shift();

            return _.reduce(thumbs,
                (acc, thumbId, index) => acc.then(() => this.productGallery.clickThumbByIndex(index + 2)
                    .then(() => this.productGallery.getMainImageAttribute(attributeName))
                    .then(imageAttribute => this.expect(imageAttribute).to.be.equal(
                        productName,
                        `Атрибут "${attributeName}" открытого изображения товара должен быть равен имени ` +
                            `товара: "${productName}"`
                    ))), Promise.resolve()
            );
        });
    };
};

/**
 * Тесты на галерею визитки карточки товара.
 * @property {PageObject.ProductSummary} this.productSummary
 * @property {PageObject.ProductGallery} this.productGallery
 */
export default makeSuite('Визитка карточки модели. Галерея.', {
    environment: 'kadavr',
    feature: 'SEO',
    story: {
        'По умолчанию': {
            'у главного изображения товара': {
                'в атрибуте "alt" содержится название товара.': makeCase({
                    id: 'marketfront-2251',
                    issue: 'MARKETVERSTKA-28712',
                    test: makeTestForAttribute('alt'),
                }),
                'в атрибуте "title" содержится название товара.': makeCase({
                    id: 'marketfront-1255',
                    issue: 'MARKETVERSTKA-28712',
                    test: makeTestForAttribute('title'),
                }),
            },
        },
    },
});
