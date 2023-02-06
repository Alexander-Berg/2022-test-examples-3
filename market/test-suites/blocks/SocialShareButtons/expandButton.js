import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на разворачивалку в блоке соц кнопок
 * @property {PageObject.SocialShareButtons} socialShareButtons
 */
export default makeSuite('Разворачивалка', {

    story: {
        'должна присутствовать.': makeCase({
            test() {
                return this.socialShareButtons
                    .isExpandButtonExists()
                    .should.eventually.be.equal(true, 'кнопка присутствует в блоке');
            },
        }),
        'должна быть кликабельна.': makeCase({
            test() {
                return this.socialShareButtons
                    .clickExpandButton()
                    .then(() => this.socialShareButtons.isExpandButtonExists())
                    .should.eventually.be.equal(false, 'кнопка отсутствует в блоке');
            },
        }),
    },
});
