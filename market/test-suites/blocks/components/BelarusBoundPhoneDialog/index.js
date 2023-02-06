import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.Form} form
 * @param {PageObject.QuestionSnippet} questionSnippet
 */
export default makeSuite('Диалог подтверждения телефона для домена .by', {
    story: {
        'При клике по кнопку "Привязать номер"': {
            'открывается новая вкладка паспорта с привязкой телефонов': makeCase({
                async test() {
                    const currentTabId = await this.browser.allure.runStep(
                        'Получаем идентификатор текущей вкладки',
                        () => this.browser.getCurrentTabId()
                    );

                    await this.boundPhoneDialog.clickSubmitButton();

                    const newTabId = await this.browser
                        .yaWaitForNewTab({startTabIds: [currentTabId], timeout: 2000});

                    await this.browser.yaWaitForPageReady();

                    await this.browser.allure.runStep(
                        'Переключаем вкладку на только что открытую',
                        () => this.browser.switchTab(newTabId)
                    );

                    const url = await this.browser.yaParseUrl();

                    return this.expect(url).to.be.link({
                        hostname: '^passport',
                        pathname: 'profile/phones',
                    }, {
                        mode: 'match',
                        skipProtocol: true,
                        skipQuery: true,
                    });
                },
            }),
            'диалог закрывается': makeCase({
                async test() {
                    const currentTabId = await this.browser.allure.runStep(
                        'Получаем идентификатор текущей вкладки',
                        () => this.browser.getCurrentTabId()
                    );

                    await this.boundPhoneDialog.clickSubmitButton();

                    await this.browser.yaWaitForPageReady();

                    await this.browser.allure.runStep(
                        'Переключаем вкладку обратно',
                        () => this.browser.switchTab(currentTabId)
                    );

                    await this.boundPhoneDialog.waitForContentHidden()
                        .should.eventually.equal(true, 'диалог привязки телефона закрылся');
                },
            }),
        },
        'При клике по кнопку "Отменить"': {
            'диалог закрывается': makeCase({
                async test() {
                    await this.boundPhoneDialog.waitForContentVisible()
                        .should.eventually.equal(true, 'диалог привязки телефона открылся');

                    await this.boundPhoneDialog.clickCancelButton();

                    await this.boundPhoneDialog.waitForContentHidden()
                        .should.eventually.equal(true, 'диалог привязки телефона закрылся');
                },
            }),
        },
        'При клике по крестику': {
            'диалог закрывается': makeCase({
                async test() {
                    await this.boundPhoneDialog.waitForContentVisible()
                        .should.eventually.equal(true, 'диалог привязки телефона открылся');

                    await this.boundPhoneDialog.clickCloseButton();

                    await this.boundPhoneDialog.waitForContentHidden()
                        .should.eventually.equal(true, 'диалог привязки телефона закрылся');
                },
            }),
        },
    },
});
