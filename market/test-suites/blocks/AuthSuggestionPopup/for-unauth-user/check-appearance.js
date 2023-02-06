import {makeCase, makeSuite} from 'ginny';

/**
 * Тесты на блок AuthSuggestionPopup
 * @param {PageObject.AuthSuggestionPopup} authSuggestionPopup
 */
export default makeSuite('Предложение авторизоваться. Появление.', {
    feature: 'Предложение авторизоваться',
    environment: 'kadavr',
    params: {
        description: 'Ожидаемый текст уведомления',
    },
    story: {
        'При клике на тумблере': {
            'юзер должен увидеть нотификацию.': makeCase({
                async test() {
                    await this.tumbler.click();
                    await this.authSuggestionPopup.waitForAppearance();

                    return this.authSuggestionPopup.getDescription()
                        .should.eventually.to.be.equal(this.params.description,
                            'Нотификация появилась с ожидаемым текстом уведомления');
                },
            }),
        },
    },
});
