import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на переход по блоку HeadBanner.
 * @param {PageObject.HeadBanner} headBanner
 */
export default makeSuite('Блок баннера.', {
    story: {
        'При клике': {
            'должен приводить к переходу на целевую страницу': makeCase({
                params: {
                    expectedUrl: 'Ссылка, ожидаемая после перехода по баннеру',
                },

                async test() {
                    await this.headBanner.click();
                    await this.browser.yaWaitForPageLoaded();

                    const {expectedUrl} = this.params;
                    const currentUrl = await this.browser.getUrl();

                    return this.browser.allure.runStep(
                        'Сравниваем текущую и ожидаемую ссылку',
                        () => this.expect(currentUrl).to.be.link(expectedUrl, {
                            skipProtocol: true,
                            skipHostname: true,
                        })
                    );
                },
            }),
        },
    },
});
