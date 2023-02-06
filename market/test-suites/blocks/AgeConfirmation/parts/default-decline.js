import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.ageConfirmation} ageConfirmation
 */
export default makeSuite('Подтверждение возраста — кнопка «нет»', {
    feature: 'Подтверждение возраста',
    story: {
        async beforeEach() {
            await this.browser.deleteCookie('adult');
            await this.browser.refresh();
        },
        'При нажатии на кнопку «нет»': {
            'происходит редирект на главную': makeCase({
                async test() {
                    await this.ageConfirmation.clickDecline();
                    await this.ageConfirmation.clickRedirect();
                    await this.browser.yaWaitForPageReady();

                    const currentUrl = await this.browser.getUrl();
                    const expectedUrl = await this.browser.yaBuildURL('touch:index');

                    return this.expect(currentUrl).to.be.link(expectedUrl, {
                        skipProtocol: true,
                        skipHostname: true,
                    });
                },
            }),
        },
    },
});
