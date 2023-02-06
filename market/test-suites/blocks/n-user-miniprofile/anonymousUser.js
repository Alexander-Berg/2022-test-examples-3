import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на попап в профиле пользователя
 * @param {PageObject.UserMiniprofile} userMiniprofile
 */
export default makeSuite('Профиль анонимного пользователя', {
    feature: 'Тултип профиля пользователя',
    story: {
        'при наведении': {
            'не появляется тултип профиля пользователя': makeCase({
                id: 'marketfront-2531',
                issue: 'MARKETVERSTKA-29017',
                test() {
                    return this.userMiniprofile.isExisting()
                        .should.eventually.to.equal(false, 'Реакт виджет тултипа отсутствует');
                },
            }),
        },
    },
});
