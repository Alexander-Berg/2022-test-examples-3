import {makeSuite, makeCase} from '@yandex-market/ginny';

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Категории', {
    story: {
        'В Shop-in-Shop': {
            'имеют необходимые параметры в ссылках': makeCase({
                id: 'marketfront-4371',
                async test() {
                    const categoryUrl = await this.intentsTree.getSearchIntentLinkHref();

                    return this.browser.allure.runStep(
                        'Проверяем ссылку на категорию',
                        () => this.browser.expect(
                            categoryUrl,
                            'Ссылка на категорию содержит правильный набор query-параметров'
                        )
                            .to.be.link({
                                query: this.params.query,
                            }, {
                                skipProtocol: true,
                                skipHostname: true,
                                skipPathname: true,
                                skipHash: true,
                            })
                    );
                },
            }),
        },
    },
});
