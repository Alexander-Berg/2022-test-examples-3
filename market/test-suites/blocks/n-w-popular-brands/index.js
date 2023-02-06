import {makeSuite, makeCase} from 'ginny';


/**
 * Тесты на блок n-w-pupular-brands.
 *
 * @param {PageObject.PopularBrands} popularBrands
 */
export default makeSuite('Блок с популярными брендами.', {
    feature: 'Бренды',
    id: 'marketfront-835',
    issue: 'MARKETVERSTKA-24632',
    story: {
        'Каждый элемент с брендом': {
            'должен содержать ссылку на страницу бренда': makeCase({
                test() {
                    return this.popularBrands.getAllBrandsUrls()
                        .then(links => this.browser.allure.runStep(
                            'Проверяем ссылки на страницы популярных брендов',
                            () => Promise.all(links.map(
                                link => this.expect(link).to.be.link({
                                    pathname: '/brands--.*/\\d+',
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
});
