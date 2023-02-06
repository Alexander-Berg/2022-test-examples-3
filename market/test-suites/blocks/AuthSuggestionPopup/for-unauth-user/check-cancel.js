import {makeCase, makeSuite} from 'ginny';

/**
 * Тесты на блок AuthSuggestionPopup
 * @param {PageObject.AuthSuggestionPopup} authSuggestionPopup
 */
export default makeSuite('Предложение авторизоваться. Кнопка "Не сейчас".', {
    feature: 'Предложение авторизоваться',
    environment: 'kadavr',
    story: {
        'Если юзер нажал кнопку "Не сейчас",': {
            'нотификация должна скрыться.': makeCase({
                async test() {
                    await this.tumbler.click();
                    await this.authSuggestionPopup.waitForAppearance();
                    await this.authSuggestionPopup.cancelClick();

                    return this.browser.allure.runStep(
                        'Проверяем, что нотификация закрылась',
                        () => this.authSuggestionPopup.waitForHidden()
                            .should.eventually.to.be.equal(true, 'Нотификация закрылась.')
                    );
                },
            }),
        },
    },
});
