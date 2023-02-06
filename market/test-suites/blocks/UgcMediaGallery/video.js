import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.UgcMediaGallery} ugcMediaGallery
 * @param {PageObject.UgcGalleryFloatingReview} ugcGalleryFloatingReview
 */

export default makeSuite('Блок UGC медиа галереи, содержащий видео.', {
    environment: 'kadavr',
    story: {
        'Кнопка "Комментировать видео"': {
            'по умолчанию': {
                'содержит корректную ссылку на страницу обсуждения видео': makeCase({
                    feature: 'Видимость блока',
                    id: 'marketfront-4215',
                    async test() {
                        await this.browser.yaWaitForChangeValue({
                            action: () => this.ugcMediaGallery.clickFirstSlide(),
                            valueGetter: () => this.ugcGalleryFloatingReview.isVisible(),
                        });
                        const actualPath = this.ugcGalleryFloatingReview.getCommentLinkHref();
                        const expectedPath = await this.browser.yaBuildURL('market:product-video', {
                            slug: this.params.slug,
                            productId: this.params.productId,
                            videoId: this.params.videoId,
                        });
                        return this.expect(actualPath).to.be.link(
                            expectedPath,
                            {
                                skipProtocol: true,
                                skipHostname: true,
                            }
                        );
                    },
                }),
            },
        },
    },
});
