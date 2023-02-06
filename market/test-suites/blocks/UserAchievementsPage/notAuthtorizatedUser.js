import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на страницу для неавторизованного пользователя
 * @param {PageObject.NotAuthPage} notAuthPage
 */
export default makeSuite('В случае если пользователь неавторизован', {
    environment: 'testing',
    story: {
        'выводит предложение авторизоваться': makeCase({
            id: 'marketfront-2506',
            issue: 'MARKETVERSTKA-28955',
            test() {
                return this.notAuthPage.authLink.isVisible()
                    .should.eventually.be.equal(
                        true,
                        'Контрол для авторизации пользователя виден'
                    );
            },
        }),
    },
});
