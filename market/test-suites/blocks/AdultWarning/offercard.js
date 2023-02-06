import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.AdultWarning} adultConfirmationPopup
 */
export default makeSuite('Подтверждение возраста на КО', {
    feature: 'Подтверждение возраста',
    story: {
        async beforeEach() {
            await this.browser.deleteCookie('adult');
            await this.browser.refresh();
        },
        'По умолчанию': {
            'информер присутствует на странице': makeCase({
                id: 'marketfront-3068',
                issue: 'MARKETVERSTKA-32573',
                async test() {
                    const isVisible = await this.adultConfirmationPopup.isExisting();

                    return this.expect(isVisible).to.be.equal(true, 'Информер присутствует на странице');
                },
            }),
        },
        'При нажатии на кнопку «Нет»': {
            'происходит редирект на главную': makeCase({
                id: 'marketfront-3069',
                issue: 'MARKETVERSTKA-32574',
                async test() {
                    await this.adultConfirmationPopup.clickDecline();
                    await this.browser.yaWaitForReactPageLoaded();

                    const currentUrl = await this.browser.getUrl();
                    const expectedUrl = await this.browser.yaBuildURL('market:index');

                    return this.expect(currentUrl).to.be.link(expectedUrl, {
                        skipProtocol: true,
                        skipHostname: true,
                    });
                },
            }),
        },
    },
});
