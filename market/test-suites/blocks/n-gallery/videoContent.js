
import {makeCase, makeSuite} from 'ginny';

export default makeSuite('Видео в галерее', {
    story: {
        'По умолчанию': {
            'работает правильно': makeCase({
                id: 'marketfront-4350',
                issue: 'MARKETFRONT-32541',
                async test() {
                    await this.galleryVideoContent.waitForVisible();

                    await this.galleryVideoContent.isExistingThumbnail()
                        .should.eventually.to.be.equal(true, 'Показывается превью видео');

                    await this.galleryVideoContent.playVideo();

                    await this.galleryVideoContent.isExistingThumbnail()
                        .should.eventually.to.be.equal(false, 'Превью видео должно отсутствовать');
                },
            }),
        },
    },
});
