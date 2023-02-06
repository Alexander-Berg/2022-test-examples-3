import {makeSuite, makeCase} from 'ginny';

const ADULT_COOKIE_NAME = 'adult';

/**
 * Тесты блока AdultWarning на странице с adult контентом, без подтверждения или отказа
 * @param {PageObject.AdultWarning} adultConfirmationPopup
 */
export default makeSuite('Блок 18+, без установленной adult куки', {
    story: {
        'По умолчанию': {
            'должен отображаться': makeCase({
                async test() {
                    await this.browser.deleteCookie(ADULT_COOKIE_NAME);
                    await this.browser.refresh();

                    return this.adultConfirmationPopup.isExisting()
                        .should.eventually.to.be.equal(true, 'Блок присутствует на странице');
                },
            }),
        },
    },
});
