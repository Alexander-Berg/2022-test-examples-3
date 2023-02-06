import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на галерею с видео на визитке КМ
 * @param {PageObject.GalleryNumericCounter} galleryNumericCounter
 * @param {PageObject.GallerySlider} gallerySlider
 */
export default makeSuite('Видео в галерее на КМ', {
    story: {
        'По умолчанию': {
            'работает правильно': makeCase({
                id: 'marketfront-4349',
                issue: 'MARKETFRONT-32533',
                async test() {
                    await this.galleryVideoContent.isExisting()
                        .should.eventually.to.be.equal(true, 'Видео слайд существует');

                    await this.galleryVideoContent.waitForVisiblePreview();

                    await this.galleryVideoContent.playVideo();

                    await this.galleryVideoContent.waitForInvisiblePreview();
                },
            }),
        },
    },
});
