import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на галерею с видео на визитке КМ
 * @param {PageObject.GalleryNumericCounter} galleryNumericCounter
 * @param {PageObject.GallerySlider} gallerySlider
 */
export default makeSuite('Видео в галерее', {
    feature: 'Продуктовая галерея',
    story: {
        'По умолчанию': {
            'работает правильно': makeCase({
                id: 'm-touch-2732',
                issue: 'MOBMARKET-11827',
                async test() {
                    await this.galleryNumericCounter.getText()
                        .should.eventually.to.be.equal(
                            '1 из 1 фото - 1 видео',
                            'Должен быть правильный текст в нумераторе'
                        );

                    await this.gallerySlider.swipeRight();

                    await this.gallerySlider.clickSlide(2);

                    const url = await this.browser.getUrl();
                    return this.browser.allure.runStep(
                        'Проверяем что перешли на /videos',
                        () => this.expect(url).to.contain('/videos')
                    );
                },
            }),
        },
    },
});
