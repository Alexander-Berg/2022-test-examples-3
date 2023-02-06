import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.UgcMediaGallery} ugcMediaGallery
 */

export default makeSuite('Блок UGC медиа галереи.', {
    environment: 'kadavr',
    story: {
        'По умолчанию': {
            'виден': makeCase({
                feature: 'Видимость блока',
                id: 'marketfront-4074',
                async test() {
                    await this.ugcMediaGallery.isVisible()
                        .should.eventually.equal(true, 'Карусель UGC медиа галереи видна');
                },
            }),
        },
    },
});
