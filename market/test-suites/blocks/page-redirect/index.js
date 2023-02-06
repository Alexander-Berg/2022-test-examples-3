import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на блок page-redirect.
 * @param {PageObject.PageRedirect} pageRedirect
 */
export default makeSuite('Редирект.', {
    environment: 'testing',
    story: {
        'При переходе по ссылке': {
            'получаем корректный редирект': makeCase({
                test() {
                    return this.browser.allure.runStep(
                        'Проверяем корректность редиректа',
                        () => this.browser.yaWaitForPageLoaded()
                            .then(() => this.browser.getUrl()
                                .should.eventually.be.link(
                                    this.params.expectedUrl, {
                                        mode: 'match',
                                    }
                                )
                            )
                    );
                },
            }),
        },
    },
});
