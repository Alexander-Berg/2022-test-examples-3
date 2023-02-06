import {makeCase, makeSuite} from 'ginny';

/**
 * @param {PageObject.Remove} questionRemoveLink
 */
export default makeSuite('Блок "Удалить вопрос". Если пользователь не может удалять вопрос', {
    story: {
        'По умолчанию': {
            'не отображается': makeCase({
                async test() {
                    return this.questionRemoveLink.isVisible()
                        .should.eventually.to.be.equal(false, 'Блок "Удалить вопрос" отсутствует на странице');
                },
            }),
        },
    },
});
