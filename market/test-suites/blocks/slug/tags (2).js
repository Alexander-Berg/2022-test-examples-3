import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.Base} base
 * @param {PageObject.Footer} footer
 * @param {string} params.entity
 */
export default makeSuite('Проверка соответствия slug в тегах', {
    environment: 'kadavr',
    story: {
        'По умолчанию': {
            'slug есть в урле, каноникле, og:url, ссылке “Полная версия”': makeCase({
                id: 'm-touch-2413',
                issue: 'MOBMARKET-10847',
                async test() {
                    const bindCheckUrl = checkUrl.bind(this);

                    await this.browser.allure.runStep(
                        'Проверяем ссылку из урла',
                        () => this.browser.getUrl().then(bindCheckUrl)
                    );

                    await this.browser.allure.runStep(
                        'Проверяем ссылку из canonical',
                        () => this.base.getCanonicalLinkContent().then(bindCheckUrl)
                    );

                    await this.browser.allure.runStep(
                        'Проверяем ссылку из og:url',
                        () => this.base.getOpenGraphUrlContent().then(bindCheckUrl)
                    );

                    await this.browser.allure.runStep(
                        'Проверяем ссылку «Полная версия» из футера',
                        () => this.footer.getDesktopLinkHref().then(bindCheckUrl)
                    );
                },
            }),
        },
    },
});

function checkUrl(url) {
    return this.expect(url).to.be.link({
        pathname: `${this.params.entity}--[\\w-]+\\/[\\d]+(\\/.*)?`,
    }, {
        mode: 'match',
        skipProtocol: true,
        skipHostname: true,
    });
}
