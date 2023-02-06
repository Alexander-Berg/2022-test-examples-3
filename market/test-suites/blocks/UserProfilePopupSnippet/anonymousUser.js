import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на попап в профиле пользователя
 * @param {PageObject.UserMiniprofile} userMiniprofile
 */
export default makeSuite('Профиль анонимного пользователя', {
    feature: 'Тултип профиля пользователя',
    id: 'marketfront-2531',
    issue: 'MARKETVERSTKA-29017',
    story: {
        'при наведении на заголовок': {
            'не появляется тултип профиля пользователя': makeCase({
                async test() {
                    await this.header.hoverOnUserName();
                    return this.userProfilePopupSnippet
                        .isUserProfileSnippetVisible()
                        .should.eventually
                        .to.be.equal(false, 'Тултип под заголовком не видно');
                },
            }),
        },
    },
});
