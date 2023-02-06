import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.ageConfirmation} ageConfirmation
 */
export default makeSuite('Подтверждение возраста — наличие на странице', {
    feature: 'Подтверждение возраста',
    story: {
        async beforeEach() {
            await this.browser.deleteCookie('adult');
            await this.browser.refresh();
        },
        'По умолчанию': {
            'присутствует на странице': makeCase({
                async test() {
                    const isVisible = this.ageConfirmation.isExisting();

                    return this.expect(isVisible).to.be.equal(true, 'Информер присутствует на странице');
                },
            }),
        },
    },
});
