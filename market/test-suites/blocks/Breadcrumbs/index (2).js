import {makeSuite, makeCase} from '@yandex-market/ginny';

const urlParsingParams = {
    skipProtocol: true,
    skipHostname: true,
    skipPathname: true,
    skipHash: true,
};

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Хлебные крошки', {
    story: {
        'В Shop-in-Shop': {
            'имеют необходимые параметры в ссылках': makeCase({
                id: 'marketfront-4370',
                async test() {
                    const firstBreadcrumbsUrl = await this.breadcrumbs.getItemLinkByIndex(1);

                    await this.browser.allure.runStep(
                        'Проверяем первую ссылку в хлебных крошках',
                        () => this.browser.expect(
                            firstBreadcrumbsUrl,
                            'Первая ссылка в хлебных крошках содержит правильный набор query-параметров'
                        )
                            .to.be.link({query: this.params.query}, urlParsingParams)
                    );

                    const hasLastItemLink = await this.breadcrumbs.hasLastItemLink();

                    if (hasLastItemLink) {
                        const linksCount = await this.breadcrumbs.getItemsCount();
                        const lastBreadcrumbsUrl = await this.breadcrumbs.getItemLinkByIndex(linksCount);

                        return this.browser.allure.runStep(
                            'Проверяем последнюю ссылку в хлебных крошках',
                            () => this.browser.expect(
                                lastBreadcrumbsUrl,
                                'Последняя ссылка в хлебных крошках содержит правильный набор query-параметров'
                            )
                                .to.be.link({query: this.params.query}, urlParsingParams)
                        );
                    }
                },
            }),
        },
    },
});
