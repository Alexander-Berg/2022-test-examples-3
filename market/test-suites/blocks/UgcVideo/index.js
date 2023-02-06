import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.UgcVideoPlayer} ugcVideoPlayer
 * @param {PageObject.UgcVideoPlayerFooter} ugcVideoPlayerFooter
 */

export default makeSuite('Виджет UGC видео.', {
    story: {
        'Видеоплеер': {
            'по умолчанию': {
                'должен присутствовать.': makeCase({
                    id: 'marketfront-4216',
                    async test() {
                        await this.ugcVideoPlayer.isVisible()
                            .should.eventually.be.equal(true, 'блок присутствует на странице.');
                    },
                }),
            },
        },
        'Футер видео': {
            'по умолчанию': {
                'должен присутствовать.': makeCase({
                    id: 'marketfront-4217',
                    async test() {
                        await this.ugcVideoPlayerFooter.isVisible()
                            .should.eventually.be.equal(true, 'блок присутствует на странице.');
                    },
                }),
            },
        },
    },
});
