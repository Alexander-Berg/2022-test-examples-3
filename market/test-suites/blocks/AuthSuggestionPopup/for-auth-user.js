import {makeCase, makeSuite} from 'ginny';

/**
 * Тесты на блок AuthSuggestionPopup
 * @param {PageObject.AuthSuggestionPopup} authSuggestionPopup
 */
export default makeSuite('Предложение авторизоваться. Отстутствие.', {
    environment: 'kadavr',
    feature: 'Предложение авторизоваться',
    story: {
        'При клике на тумблере': {
            'юзер не должен увидеть нотификацию.': makeCase({
                async test() {
                    await this.tumbler.click();
                    return this.browser.allure.runStep(
                        'Проверяем, что нотификация не появилась',
                        () => this.authSuggestionPopup.checkNonAppearance()
                            .should.eventually.to.be.equal(true, 'Нотификации не появилась.')
                    );
                },
            }),
        },
    },
});
