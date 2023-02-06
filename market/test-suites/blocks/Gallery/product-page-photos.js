import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на галерею с несколькими изображениями на визитке КМ
 * @param {PageObject.GalleryNumericCounter} galleryNumericCounter
 * @param {PageObject.GallerySlider} gallerySlider
 * @param {PageObject.GallerySlider} modalGallerySlider
 */
export default makeSuite('Галерея с несколькими изображениями', {
    feature: 'Продуктовая галерея',
    story: {
        'По умолчанию': {
            'работает правильно': makeCase({
                id: 'm-touch-2731',
                issue: 'MOBMARKET-11828',
                async test() {
                    await this.galleryNumericCounter.getText()
                        .should.eventually.to.be.equal('1 из 3 фото', 'Должен быть правильный текст в нумераторе');

                    await this.gallerySlider.swipeRight();
                    await this.gallerySlider.isSlideActive(2)
                        .should.eventually.to.be.equal(true, 'Второй слайд должен быть активным');

                    await this.gallerySlider.clickSlide(2);

                    await this.modalGallerySlider.waitForVisible();
                    await this.modalGallerySlider.swipeRight();

                    return this.modalGallerySlider.isSlideActive(3)
                        .should.eventually.to.be.equal(true, 'Третий слайд в модальном окне должен быть активным');
                },
            }),
        },
    },
});
