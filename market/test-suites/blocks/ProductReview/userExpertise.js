import {makeCase, makeSuite} from 'ginny';

/**
 * Тест на компонент пользовательской экспертизы в отзыве о продукте.
 * @param {PageObject.AuthorExpertise} authorExpertise
 * @param {PageObject.UserExpertisePopup} userExpertisePopup
 */
export default makeSuite('Значок экспертизы пользователя', {
    params: {
        publicId: 'Публичный id пользователя',
    },
    story: {
        'По умолчанию': {
            'должен отображаться': makeCase({
                feature: 'Экспертиза пользователя',
                id: 'm-touch-3347',
                issue: 'MARKETFRONT-13214',
                async test() {
                    const isVisible = await this.authorExpertise.isVisible();
                    return this.browser.allure.runStep('Проверяем видимость значка', () =>
                        this.expect(isVisible).to.be.equal(true, 'Значок виден'));
                },
            }),
        },
        'При клике': {
            'открывается модальное окно': makeCase({
                feature: 'Экспертиза пользователя',
                id: 'm-touch-3346',
                issue: 'MARKETFRONT-13214',
                async test() {
                    await this.authorExpertise.clickExpertise();
                    await this.userExpertisePopup.waitForContentVisible();
                    const isModalVisible = await this.userExpertisePopup.isModalVisible();
                    return this.browser.allure.runStep('Проверяем видимость модалки', () =>
                        this.expect(isModalVisible).to.be.equal(true, 'Модалка видна'));
                },
            }),
            'открывается модальное окно, которое содержит корректную ссылку на профиль': makeCase({
                feature: 'Экспертиза пользователя',
                id: 'm-touch-3345',
                issue: 'MARKETFRONT-13214',
                async test() {
                    await this.authorExpertise.clickExpertise();
                    await this.userExpertisePopup.waitForContentVisible();

                    const expectedLink = await this.browser.yaBuildURL(
                        'market:user-public', {publicId: this.params.publicId});
                    const actualLink = await this.userExpertisePopup.getAuthorLinkUrl();

                    await this.expect(actualLink, 'Ссылка корректная')
                        .to.be.link({pathname: expectedLink}, {
                            skipProtocol: true,
                            skipHostname: true,
                        });
                },
            }),
        },
        'При клике на крестик в модальном окне': {
            'модальное окно закрывается': makeCase({
                feature: 'Экспертиза пользователя',
                id: 'm-touch-3344',
                issue: 'MARKETFRONT-13214',
                async test() {
                    await this.authorExpertise.clickExpertise();
                    await this.userExpertisePopup.waitForContentVisible();
                    await this.userExpertisePopup.clickCloseButton();
                    const isModalVisible = await this.userExpertisePopup.isModalVisible();
                    return this.browser.allure.runStep('Проверяем видимость модалки', () =>
                        this.expect(isModalVisible).to.be.equal(false, 'Модалка закрылась'));
                },
            }),
        },
    },
});
