import {makeSuite, makeCase} from '@yandex-market/ginny';

const urlParsingParams = {
    skipProtocol: true,
    skipHostname: true,
    skipPathname: true,
    skipHash: true,
};

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Поисковая выдача', {
    story: {
        'В листовой выдаче': {
            'сниппеты имеют необходимые параметры в ссылках': makeCase({
                id: 'marketfront-4372',
                async test() {
                    const imageUrl = await this.snippetCard.getImageUrl();
                    const priceHref = await this.snippetCard.getMainPriceHref();
                    const titleUrl = await this.snippetCard.getTitleUrl();

                    await this.browser.allure.runStep(
                        'Проверяем ссылку на изображении',
                        () => this.browser.expect(imageUrl, 'Ссылка на изображении содержит правильный набор query-параметров')
                            .to.be.link({query: this.params.query}, urlParsingParams)
                    );

                    await this.browser.allure.runStep(
                        'Проверяем ссылку на цене',
                        () => this.browser.expect(priceHref, 'Ссылка на цене содержит правильный набор query-параметров')
                            .to.be.link({query: this.params.query}, urlParsingParams)
                    );

                    await this.browser.allure.runStep(
                        'Проверяем ссылку в заголовке',
                        () => this.browser.expect(titleUrl, 'Ссылка в заголовке содержит правильный набор query-параметров')
                            .to.be.link({query: this.params.query}, urlParsingParams)
                    );
                },
            }),
        },
        'В гридовой выдаче': {
            'сниппеты имеют необходимые параметры в ссылках': makeCase({
                id: 'marketfront-4372',
                async test() {
                    await this.viewSwitcher.clickSwitcherByValue('grid');

                    await this.spin.waitForHidden(5000);

                    const imageUrl = await this.snippetCell.getImageUrl();
                    const priceHref = await this.snippetCell.getMainPriceHref();
                    const titleUrl = await this.snippetCell.getTitleUrl();

                    await this.browser.allure.runStep(
                        'Проверяем ссылку на изображении',
                        () => this.browser.expect(imageUrl, 'Ссылка на изображении содержит правильный набор query-параметров')
                            .to.be.link({query: this.params.query}, urlParsingParams)
                    );

                    await this.browser.allure.runStep(
                        'Проверяем ссылку на цене',
                        () => this.browser.expect(priceHref, 'Ссылка на цене содержит правильный набор query-параметров')
                            .to.be.link({query: this.params.query}, urlParsingParams)
                    );

                    await this.browser.allure.runStep(
                        'Проверяем ссылку в заголовке',
                        () => this.browser.expect(titleUrl, 'Ссылка в заголовке содержит правильный набор query-параметров')
                            .to.be.link({query: this.params.query}, urlParsingParams)
                    );
                },
            }),
        },
    },
});
