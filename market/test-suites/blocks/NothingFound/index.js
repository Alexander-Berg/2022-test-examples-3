import {makeSuite, makeCase} from '@yandex-market/ginny';

const urlParsingParams = {
    skipProtocol: true,
    skipHostname: true,
    skipPathname: true,
    skipHash: true,
};

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Товар не найден', {
    story: {
        'При поиске в Shop-in-Shop': {
            'отображается': makeCase({
                id: 'marketfront-4369',
                async test() {
                    await this.search2.setInputValue('йцукен');

                    await this.browser.yaWaitForPageReloadedExtended(
                        () => this.search2.clickSearch(),
                        5000
                    );

                    await this.nothingFoundSins.waitForVisible();
                    const url = await this.browser.getUrl();
                    return this.browser.expect(url).to.be.link({query: this.params.query}, urlParsingParams);
                },
            }),
        },
    },
});
