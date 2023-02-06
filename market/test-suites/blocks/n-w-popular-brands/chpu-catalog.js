
import {makeSuite, makeCase} from 'ginny';

/**
 * Тест на ЧПУ ссылки каталога на n-w-popular-brands.
 * @param {PageObject.PopularBrands} popularBrands
 */
export default makeSuite('ЧПУ ссылки на блоке популярных брендов.', {
    story: {
        'Ссылка на категорию.': {
            'По умолчанию': {
                'содержит slug и является ЧПУ': makeCase({
                    id: 'marketfront-3096',
                    issue: 'MARKETVERSTKA-31865',
                    async test() {
                        const url = await this.popularBrands.getSnippetUrl();

                        return this.expect(url).to.be.link({
                            pathname: '/catalog--.*/\\d+',
                        }, {
                            mode: 'match',
                            skipProtocol: true,
                            skipHostname: true,
                        });
                    },
                }),
            },
        },
    },
});

