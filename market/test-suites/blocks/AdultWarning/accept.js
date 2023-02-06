import {makeSuite, makeCase} from 'ginny';

const ADULT_COOKIE_NAME = 'adult';

/**
 * Тесты блока AdultWarning на странице с adult контентом, с подтверждением 18+
 * @param {PageObject.AdultWarning} adultConfirmationPopup
 */
export default makeSuite('Блок 18+, без установленной adult куки. Подтверждение 18+', {
    story: {
        'После потверждения 18+ пользователя': {
            'не должен отображаться': makeCase({
                async test() {
                    await this.browser.deleteCookie(ADULT_COOKIE_NAME);
                    await this.browser.refresh();

                    await this.adultConfirmationPopup.clickAccept();
                    return this.adultConfirmationPopup.isExisting()
                        .should.eventually.to.be.equal(false, 'Блок отсутствует на странице');
                },
            }),
        },
    },
});
