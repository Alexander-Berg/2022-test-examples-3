import {makeSuite, makeCase} from 'ginny';


/**
 * Тесты на блок n-w-all-brands.
 *
 * @param {PageObject.AllBrands} allBrands
 */
export default makeSuite('Блок со списком всех брендов.', {
    feature: 'Бренды',
    id: 'marketfront-836',
    issue: 'MARKETVERSTKA-24631',
    story: {
        'Каждый элемент списка': {
            'должен содержать ссылку на страницу бренда': makeCase({
                test() {
                    return this.allBrands
                        // Проверяем ТОЛЬКО первую ссылкупотому что их на странице >1200
                        // И проверка такого числа ссылок занимает очень много времени
                        .getFirstItemUrl()
                        .should.eventually.be.link({
                            pathname: '/brands--.*/\\d+',
                        }, {
                            mode: 'match',
                            skipProtocol: true,
                            skipHostname: true,
                        });
                },
            }),
        },
    },
});
