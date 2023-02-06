import {makeCase, makeSuite} from 'ginny';

/**
 * @param {PageObject.Remove} questionRemoveLink
 */
export default makeSuite('Блок "Удалить вопрос". Если пользователь может удалять вопрос', {
    story: {
        'По умолчанию': {
            'отображается': makeCase({
                async test() {
                    await this.questionRemoveLink.isVisible()
                        .should.eventually.to.be.equal(true, 'Блок "Удалить вопрос" присутствует на странице');

                    await this.questionRemoveLink.getText()
                        .should.eventually.to.be.equal('Удалить вопрос');
                },
            }),
        },
    },
});
