import {makeCase, makeSuite} from 'ginny';

/**
 * Тесты на блок AuthSuggestionPopup
 * @param {PageObject.AuthSuggestionPopup} authSuggestionPopup
 */
export default makeSuite('Предложение авторизоваться. Повторный вызов.', {
    environment: 'kadavr',
    feature: 'Предложение авторизоваться',
    story: {
        'Если юзер закрыл нотификацию по паранже,': {
            'при повторном клике на тумблере': {
                'юзер не должен увидеть нотификацию.': makeCase({
                    async test() {
                        await this.tumbler.click();
                        await this.authSuggestionPopup.waitForAppearance();
                        await this.authSuggestionPopup.paranjaClick();
                        await this.authSuggestionPopup.waitForHidden();
                        await this.tumbler.click();

                        return this.browser.allure.runStep(
                            'Проверяем, что нотификация не появилась повторно',
                            () => this.authSuggestionPopup.checkNonAppearance()
                                .should.eventually.to.be.equal(true, 'Нотификация не появилась повторно.'));
                    },
                }),
            },
        },
    },
});
