import {
    makeCase,
    makeSuite,
} from 'ginny';

/**
 * @property {PageObject.SeoVersusComparisons} seoVersusComparisons
 */
export default makeSuite('Блок "С этим товаром сравнивают".', {
    environment: 'kadavr',
    story: {
        'Названия сравниваемых товаров.': {
            'По умолчанию': {
                'содержат корректную ссылку': makeCase({
                    id: 'marketfront-3794',
                    issue: 'MARKETFRONT-5618',
                    params: {
                        expectedId: 'ID версуса',
                        expectedSlug: 'Слаг часть версуса',
                    },
                    async test() {
                        const {expectedId, expectedSlug} = this.params;

                        const expectedUrl = await this.browser.yaBuildURL('market:versus', {
                            id: expectedId,
                            slug: expectedSlug,
                        });
                        const actualUrl = await this.seoVersusComparisons.getComparisonLink();

                        await this.expect(actualUrl).to.be.link(expectedUrl, {
                            skipProtocol: true,
                            skipHostname: true,
                        });
                    },
                }),
            },
        },
    },
});
