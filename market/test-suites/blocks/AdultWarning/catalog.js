import {makeSuite, makeCase} from 'ginny';
import {getLastReportRequestParams} from './helpers';

/**
 * @param {PageObject.AdultWarning} adultConfirmationPopup
 * @param {PageObject.SnippetList} snippetList
 */
export default makeSuite('Подтверждение возраста на странице каталога', {
    feature: 'Подтверждение возраста',
    story: {
        async beforeEach() {
            await this.browser.deleteCookie('adult');
            await this.browser.refresh();
        },
        'По умолчанию': {
            'присутствует на странице': makeCase({
                id: 'marketfront-3072',
                issue: 'MARKETVERSTKA-32587',
                async test() {
                    const isVisible = await this.adultConfirmationPopup.isVisible();

                    return this.expect(isVisible).to.be.equal(true, 'Информер присутствует на странице');
                },
            }),
        },
        'При нажатии на кнопку «Да»': {
            'параметр adult отправляется в репорт': makeCase({
                id: 'marketfront-3076',
                issue: 'MARKETVERSTKA-32590',
                async test() {
                    await this.browser.yaWaitForPageReady();
                    await this.adultConfirmationPopup.clickAccept();
                    await this.browser.yaWaitForPageReady();

                    const params = await getLastReportRequestParams(this);
                    const {adult} = params;

                    return this.expect(Number(adult)).to.be.equal(1, 'параметр adult присутствует в запросе');
                },
            }),
        },
        'При нажатии на кнопку «Нет»': {
            'происходит редирект на главную': makeCase({
                id: 'marketfront-3073',
                issue: 'MARKETVERSTKA-32588',
                async test() {
                    await this.browser.yaWaitForPageReady();
                    await this.adultConfirmationPopup.clickDecline();
                    await this.browser.yaWaitForPageReady();

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
