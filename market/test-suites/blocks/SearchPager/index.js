import {makeSuite, makeCase} from '@yandex-market/ginny';

const urlParsingParams = {
    skipProtocol: true,
    skipHostname: true,
    skipPathname: true,
    skipHash: true,
};

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Пагинация', {
    story: {
        'В Shop-in-Shop': {
            'имеет необходимые параметры в ссылках': makeCase({
                id: 'marketfront-4359',
                async test() {
                    const numberButtonUrl = await this.searchPager.getFirstNumberButtonUrl();
                    const nextButtonUrl = await this.searchPager.getNextButtonUrl();

                    await this.browser.allure.runStep(
                        'Проверяем ссылку на первую страницу в пагинации',
                        () => this.browser.expect(
                            numberButtonUrl,
                            'Ссылка на первую страницу содержит правильный набор query-параметров'
                        )
                            .to.be.link({query: this.params.query}, urlParsingParams)
                    );

                    return this.browser.allure.runStep(
                        'Проверяем ссылку "Вперед" в пагинации',
                        () => this.browser.expect(
                            nextButtonUrl,
                            'Ссылка "Вперед" в пагинации содержит правильный набор query-параметров'
                        )
                            .to.be.link({query: this.params.query}, urlParsingParams)
                    );
                },
            }),
        },
    },
});
