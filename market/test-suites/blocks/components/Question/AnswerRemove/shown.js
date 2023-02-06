import {makeCase, makeSuite} from 'ginny';

/**
 * @param {PageObject.Remove} answerRemoveLink
 */
export default makeSuite('Блок "Удалить ответ". Если пользователь может удалять ответ', {
    story: {
        'По умолчанию': {
            'отображается': makeCase({
                async test() {
                    await this.answerRemoveLink.isVisible()
                        .should.eventually.to.be.equal(true, 'Блок "Удалить ответ" присутствует на странице');

                    await this.answerRemoveLink.getText()
                        .should.eventually.to.be.equal('Удалить');
                },
            }),
        },
    },
});
