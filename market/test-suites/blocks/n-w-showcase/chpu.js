import {makeSuite, makeCase} from 'ginny';

/**
 * Тест на ЧПУ ссылки на n-w-showcase.
 * @param {PageObject.Showcase} showcaseMenu
 */
export default makeSuite('ЧПУ ссылки брендов в меню.', {
    environment: 'testing',
    story: {
        'Ссылка на бренд.': {
            'По умолчанию': {
                'содержит slug и является ЧПУ': makeCase({
                    id: 'marketfront-3067',
                    issue: 'MARKETVERSTKA-32871',
                    test() {
                        return this.showcaseMenu.getAllBrandsUrls()
                            .then(links => this.browser.allure.runStep(
                                'Проверяем ссылки на страницы брендов',
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
