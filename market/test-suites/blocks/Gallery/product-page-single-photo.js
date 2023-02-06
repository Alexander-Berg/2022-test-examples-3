import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на галерею с одним изображением на визитке КМ
 * @param {PageObject.GalleryNumericCounter} galleryNumericCounter
 * @param {PageObject.GallerySlider} gallerySlider
 * @param {PageObject.GallerySlider} modalGallerySlider
 * @param {PageObject.GalleryModal} galleryModal
 */
export default makeSuite('Галерея с одним изображением', {
    feature: 'Продуктовая галерея',
    story: {
        'По умолчанию': {
            'работает правильно': makeCase({
                id: 'm-touch-2730',
                issue: 'MOBMARKET-11829',
                async test() {
                    await this.galleryNumericCounter.getText()
                        .should.eventually.to.be.equal('1 фото', 'Должен быть правильный текст в нумераторе');

                    await this.gallerySlider.clickSlide(1);

                    await this.modalGallerySlider.isExisting()
                        .should.eventually.to.be.equal(true, 'Модальное окно должно быть открыто');

                    await this.galleryModal.clickClose();

                    return this.galleryModal.isExisting()
                        .should.eventually.to.be.equal(false, 'Модальное окно должно быть закрыто');
                },
            }),
            'клик по кнопке назад закрывает галерею': makeCase({
                id: 'm-touch-3175',
                issue: 'MARKETFRONT-6786',
                async test() {
                    await this.galleryNumericCounter.getText()
                        .should.eventually.to.be.equal('1 фото', 'Должен быть правильный текст в нумераторе');

                    await this.gallerySlider.clickSlide(1);

                    await this.modalGallerySlider.isExisting()
                        .should.eventually.to.be.equal(true, 'Модальное окно должно быть открыто');

                    await this.browser.back();

                    return this.galleryModal.isExisting()
                        .should.eventually.to.be.equal(false, 'Модальное окно должно быть закрыто');
                },
            }),
        },
    },
});
