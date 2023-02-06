import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.ageConfirmation} ageConfirmation
 */
export default makeSuite('Подтверждение возраста — контекстный сниппет', {
    feature: 'Подтверждение возраста',
    story: {
        async beforeEach() {
            await this.browser.deleteCookie('adult');
            await this.browser.refresh();
        },
        'При нажатии на кнопку «нет»': {
            'информер скрывается': makeCase({
                async test() {
                    await this.ageConfirmation.clickDecline();

                    const isVisible = await this.ageConfirmation.isExisting();

                    return this.expect(isVisible).to.be.equal(false, 'Информер отстуствует на странице');
                },
            }),
        },
    },
});
