import {makeSuite, makeCase} from 'ginny';

const ADULT_COOKIE_NAME = 'adult';

/**
 * Тесты блока AdultWarning на странице с adult контентом, после отказа 18+
 * @param {PageObject.AdultWarning} adultConfirmationPopup
 */
export default makeSuite('Блок 18+, без установленной adult куки. Отказ 18+', {
    story: {
        'После отказа 18+ пользователя': {
            'не должен отображаться': makeCase({
                async test() {
                    await this.browser.deleteCookie(ADULT_COOKIE_NAME);
                    await this.browser.refresh();

                    await this.adultConfirmationPopup.clickDecline();
                    return this.adultConfirmationPopup.isExisting()
                        .should.eventually.to.be.equal(false, 'Блок отсутствует на странице');
                },
            }),
        },
    },
});
