import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на блок Banner
 * @param {PageObject.StaticBanner} banner
 */
export default makeSuite('Adfox-баннер', {
    feature: 'Баннер.',
    story: {
        'Имя хоста баннера совпадает с "fenek.market.yandex.ru"': makeCase({
            /**
             * Нужно проверить тот факт, что ссылка правильно проксируется,
             * поэтому нет смысла делать тест в кадавре.
             */
            environment: 'testing',
            issue: 'MARKETVERSTKA-31702',
            id: 'marketfront-2965',
            async test() {
                const bannerUrl = await this.banner.getUrl();
                const expectedHost = 'fenek.market.yandex.ru';

                return this.browser.allure.runStep(
                    'Проверяем, что баннер ведёт на страницу, хост которой "fenek.market.yandex.ru"',
                    () => this.expect(bannerUrl)
                        .to.be.link({
                            hostname: expectedHost,
                        }, {
                            skipProtocol: true,
                            skipPathname: true,
                            skipQuery: true,
                        }, 'Имя хоста - "fenek.market.yandex.ru"')
                );
            },
        }),
    },
});
