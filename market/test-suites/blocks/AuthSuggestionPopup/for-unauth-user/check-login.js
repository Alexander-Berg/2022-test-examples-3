import {makeCase, makeSuite} from 'ginny';

import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';

/**
 * Тесты на блок AuthSuggestionPopup
 * @param {PageObject.AuthSuggestionPopup} authSuggestionPopup
 */
export default makeSuite('Предложение авторизоваться. Кнопка "Войти".', {
    feature: 'Предложение авторизоваться',
    environment: 'kadavr',
    story: {
        'Если юзер нажал кнопку "Войти",': {
            'должна открыться страница авторизации в паспорте.': makeCase({
                async test() {
                    const pageComletelyLoaded = () =>
                        this.browser.yaExecute(() => ({state: document.readyState}))
                            .then(({value}) => value.state === 'complete');

                    await this.tumbler.click();
                    await this.authSuggestionPopup.waitForAppearance();
                    await this.authSuggestionPopup.loginClick();
                    await this.browser.waitUntil(pageComletelyLoaded, 20000, 'Не дождались загрузки страницы');

                    const pageUrl = await this.browser.getUrl();
                    const expectedPagePath = await this.browser.yaBuildURL(PAGE_IDS_COMMON.LOGIN, {region: 'ru'});

                    return this.browser.allure.runStep(
                        'Проверяем, что URL открывшейся страницы содержит ожидаемое доменное имя и путь в паспорте',
                        () => this.expect(pageUrl).to.include(expectedPagePath,
                            'Открылась страница авторизации в паспорте.')
                    );
                },
            }),
        },
    },
});
