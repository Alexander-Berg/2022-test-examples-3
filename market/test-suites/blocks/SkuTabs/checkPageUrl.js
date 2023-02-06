import {makeCase, makeSuite} from 'ginny';

/**
 * Тесты на проверку содержания sku в url страницы.
 * @param {string} this.params.infoLink
 * @param {PageObject} productCard
 */
export default makeSuite('Проверка url на наличие sku в query параметрах.', {
    environment: 'kadavr',
    params: {
        infoLink: 'имя метода предоставляещего нужную ссылку',
    },
    story: {
        'По умолчанию': {
            'параметр sku находится в URL страницы': makeCase({
                async test() {
                    const skuId = await getSku.call(this);
                    return this.expect(skuId).to.be.a('string').that.is.not.empty;
                },
            }),
        },
        'После переходе по ссылке': {
            'параметр sku находится в URL страницы': makeCase({
                async test() {
                    await this.productCardLinks.clickOnInfoLink(this.params.infoLink);
                    const skuId = await getSku.call(this);
                    return this.expect(skuId).to.be.a('string').that.is.not.empty;
                },
            }),
        },
    },
});


async function getSku() {
    const {query: {sku}} = await this.browser.yaParseUrl();
    return sku;
}
