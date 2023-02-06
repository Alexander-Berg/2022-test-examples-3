import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.GalleryUgcSlider} galleryUgcSlider
 */

export default makeSuite('Блок UGC медиа галереи.', {
    environment: 'kadavr',
    story: {
        'По умолчанию': {
            'виден': makeCase({
                feature: 'Видимость блока',
                id: 'm-touch-3355',
                async test() {
                    await this.galleryUgcSlider.isVisible()
                        .should.eventually.equal(true, 'Слайдер UGC медиа галереи виден');
                },
            }),
        },
    },
});
