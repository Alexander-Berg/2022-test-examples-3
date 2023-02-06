import {makeSuite, makeCase} from 'ginny';


/**
 * Тесты на блок n-product-review-item, элемент заголовок
 *
 * @param {PageObject.ProductReviewItem} productReviewItem
 */
export default makeSuite('Заголовок блока с отзывом.', {
    feature: 'Отображение отзыва',
    id: 'marketfront-2327',
    issue: 'MARKETVERSTKA-27668',
    params: {
        path: 'Имя роута',
        query: 'Параметры роута',
    },
    environment: 'kadavr',
    story: {
        'Ссылка в заголовке отзыва': {
            'ведет на нужную страницу': makeCase({
                async test() {
                    const actualUrl = await this.productReviewItem.fetchCaptureLink();
                    const expectedUrl = await this.browser.yaBuildURL(this.params.path, this.params.query);

                    return this.expect(actualUrl).to.be.link(expectedUrl, {
                        skipProtocol: true,
                        skipHostname: true,
                    });
                },
            }),
        },
    },
});
