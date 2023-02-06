import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на meta robots открытой страницы.
@property {PageObject.PageMeta} pageMeta
 */
export default makeSuite('Meta robots страницы.', {
    feature: 'Meta robots страницы.',
    environment: 'kadavr',
    story: {
        'По умолчанию': {
            'должен соответствовать ожидаемому значению': makeCase({
                params: {
                    robotsContent: 'контент для всех роботов',
                    yandexBot: 'контент только для робота яндекса',
                    googleBot: 'контент только для робота гугла',
                },
                async test() {
                    const {
                        robotsContent,
                        yandexBot,
                        googleBot,
                    } = this.params;


                    const allRobotsMeta = await this.pageMeta.getRobotsMeta();
                    const googleBotMeta = await this.pageMeta.getGoogleBotMeta();
                    const yandexBotMeta = await this.pageMeta.getYandexBotMeta();

                    await this.expect(allRobotsMeta).to.be.equal(robotsContent);
                    await this.expect(googleBotMeta).to.be.equal(googleBot);
                    return this.expect(yandexBotMeta).to.be.equal(yandexBot);
                },
            }),
        },
    },
});
