import {makeSuite, makeCase} from '@yandex-market/ginny';

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Уточняющие категории', {
    story: {
        'В Shop-in-Shop': {
            'имеют необходимые параметры в ссылках': makeCase({
                id: 'marketfront-4373',
                async test() {
                    const categoryUrl = await this.clarifyCategory.getHref();

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
