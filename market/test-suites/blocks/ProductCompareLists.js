import {makeCase, makeSuite} from 'ginny';

/**
 * Тесты на компонент ProductCompareLists.
 * @param {PageObject.ProductCompareLists} productCompareLists
 */
export default makeSuite('Компонент ProductCompareLists.', {
    story: {
        'При попадании на пустой список сравнения': {
            'видим кнопку "Войти"': makeCase({
                id: 'm-touch-1140',
                issue: 'MOBMARKET-5012',
                test() {
                    return this.productCompareLists
                        .loginLinkIsVisibleIsVisible()
                        .should.eventually.be.equal(true, 'Проверяем видимость кнопки "Войти"');
                },
            }),
        },

        'При клике на кнопку "Войти" из пустого списка сравнения': {
            'видим страницу авторизации': makeCase({
                id: 'm-touch-1141',
                issue: 'MOBMARKET-5013',
                test() {
                    return this.productCompareLists.loginLinkClick()
                        .then(() => this.browser.getUrl())
                        .should.eventually.be.link({
                            hostname: 'passport-rc.yandex.ru',
                            pathname: '/auth',
                        }, {
                            skipProtocol: true,
                        });
                },
            }),
        },
    },
});
