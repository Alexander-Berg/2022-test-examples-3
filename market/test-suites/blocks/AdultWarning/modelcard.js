import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.AdultWarning} adultConfirmationPopup
 * @param {PageObject.ProductTopOffersList} topOffersList
 */
export default makeSuite('Подтверждение возраста на КМ', {
    feature: 'Подтверждение возраста',
    story: {
        async beforeEach() {
            await this.browser.deleteCookie('adult');
            await this.browser.refresh();
        },
        'По умолчанию': {
            'присутствует на странице': makeCase({
                id: 'marketfront-869',
                issue: 'MARKETVERSTKA-32569',
                async test() {
                    const {expectedText} = this.params;
                    const text = await this.adultConfirmationPopup.description.getText();

                    return this.expect(text).to.be.equal(expectedText, 'Описание соответствует ожидаемому');
                },
            }),
        },
        'При нажатии на кнопку «Да»': {
            'информер скрывается': makeCase({
                id: 'marketfront-873',
                issue: 'MARKETVERSTKA-32571',
                async test() {
                    await this.adultConfirmationPopup.clickAccept();
                    await this.browser.yaWaitForPageReady();

                    const isVisible = await this.adultConfirmationPopup.isExisting();

                    return this.expect(isVisible).to.be.equal(false, 'Информер отстуствует на странице');
                },
            }),
            'блок «Топ-6» присутствует на странице': makeCase({
                id: 'marketfront-876',
                issue: 'MARKETVERSTKA-32581',
                async test() {
                    await this.adultConfirmationPopup.clickAccept();
                    await this.browser.yaWaitForPageReady();

                    const isVisible = await this.topOffersList.isExisting();

                    return this.expect(isVisible).to.be.equal(true, 'Блок топ-6 присутствует на странице');
                },
            }),
        },
        'При нажатии на кнопку «Нет»': {
            'происходит редирект на главную': makeCase({
                id: 'marketfront-877',
                issue: 'MARKETVERSTKA-32570',
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
