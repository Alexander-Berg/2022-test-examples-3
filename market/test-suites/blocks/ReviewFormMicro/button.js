import {makeCase, makeSuite} from 'ginny';

/**
 * Тест на блок ProductReviewFormMicro
 * @param {PageObject.ProductReviewFormMicro} productReviewFormMicro
 */

export default makeSuite('Кнопка оставления отзыва', {
    environment: 'kadavr',
    story: {
        'По умолчанию': {
            'содержит правильный текст': makeCase({
                params: {
                    buttonText: 'Текст кнопки',
                },
                id: 'm-touch-3574',
                issue: 'MARKETFRONT-36998',
                async test() {
                    return this.productReviewFormMicro.getButtonText()
                        .should.eventually.to.be.equal(this.params.buttonText, 'Кнопка содержит правильный текст');
                },
            }),
        },
        'При нажатии': {
            'ведет на страницу оставления отзыва': makeCase({
                params: {
                    productId: 'Идентификатор товара',
                    slug: 'Slug товара',
                },
                id: 'm-touch-3575',
                issue: 'MARKETFRONT-36998',
                async test() {
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.productReviewFormMicro.buttonClick(),
                        valueGetter: () => this.browser.getUrl(),
                    });

                    const currentUrl = await this.browser.getUrl();
                    const expectedUrl = await this.browser.yaBuildURL(
                        'market:product-reviews-add',
                        {
                            productId: String(this.params.productId),
                            slug: this.params.slug,
                        }
                    );
                    return this.expect(currentUrl).to.be.link(expectedUrl, {
                        skipProtocol: true,
                        skipHostname: true,
                    });
                },
            }),
        },
    },
});
