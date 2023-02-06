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
            'slug есть в урле, каноникле, og:url, ссылке “Мобильная версия”': makeCase({
                id: 'marketfront-2802',
                issue: 'MARKETVERSTKA-33220',
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
                        'Проверяем ссылку «Мобильная версия» из футера',
                        () => this.footer.getMobileLinkHref().then(bindCheckUrl)
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
