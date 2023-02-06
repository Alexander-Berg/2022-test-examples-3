import {makeSuite, makeCase} from 'ginny';

/**
 * Тест на ЧПУ ссылки бренда на n-w-popular-brands.
 * @param {PageObject.PopularBrands} popularBrands
 */
export default makeSuite('ЧПУ ссылки на блоке популярных брендов.', {
    story: {
        'Ссылка на бренд.': {
            'По умолчанию': {
                'содержит slug и является ЧПУ': makeCase({
                    id: 'marketfront-3067',
                    issue: 'MARKETVERSTKA-32871',
                    test() {
                        return this.popularBrands.getAllBrandsUrls()
                            .then(links => this.browser.allure.runStep(
                                'Проверяем ссылки на страницы популярных брендов',
                                () => Promise.all(links.map(
                                    link => this.expect(link).to.be.link({
                                        pathname: 'brands--[\\w-]+/\\d+',
                                    }, {
                                        mode: 'match',
                                        skipProtocol: true,
                                        skipHostname: true,
                                    }))))
                            );
                    },
                }),
            },
        },
    },
});
