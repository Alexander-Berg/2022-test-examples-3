import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на виджет Gallery
 * @param {PageObject.Gallery} gallery
 */
export default makeSuite('Gallery', {
    feature: 'Галерея картинок.',
    story: {
        'Атрибут title главного изображения.': {
            'По умолчанию': {
                'совпадает с названием товара': makeCase({
                    id: 'marketfront-2086',
                    issue: 'MOBMARKET-4626',
                    test() {
                        const {productCardTitle, galleryPicture} = this;

                        return Promise.all([
                            productCardTitle.getTitleText(),
                            galleryPicture.getTitleAttributeForPicture(),
                        ])
                            .then(([title, titleAttribute]) => this.expect(titleAttribute)
                                .be.equal(
                                    title,
                                    `Атрибут title изображения совпадает с названием ${title}`
                                )
                            );
                    },
                }),
            },
        },
        'Атрибут alt главного изображения.': {
            'По умолчанию': {
                'совпадает с названием товара': makeCase({
                    id: 'marketfront-2087',
                    issue: 'MOBMARKET-4626',
                    test() {
                        const {productCardTitle, galleryPicture} = this;

                        return Promise.all([
                            productCardTitle.getTitleText(),
                            galleryPicture.getAltAttributeForPicture(),
                        ])
                            .then(([title, altAttribute]) => this.expect(altAttribute)
                                .be.equal(
                                    title,
                                    `Атрибут alt изображения совпадает с названием ${title}`
                                )
                            );
                    },
                }),
            },
        },
    },
});
