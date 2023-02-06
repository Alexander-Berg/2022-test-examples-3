import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на попап в профиле пользователя
 * @param {PageObject.ProductReview} productReview
 */
export default makeSuite('Профиль удаленного пользователя', {
    feature: 'Тултип профиля пользователя',
    id: 'marketfront-2530',
    issue: 'MARKETVERSTKA-29016',
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
