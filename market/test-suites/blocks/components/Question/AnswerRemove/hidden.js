import {makeCase, makeSuite} from 'ginny';

/**
 * @param {PageObject.Remove} answerRemoveLink
 */
export default makeSuite('Блок "Удалить ответ". Если пользователь не может удалять ответ', {
    story: {
        'По умолчанию': {
            'не отображается': makeCase({
                async test() {
                    return this.answerRemoveLink.isVisible()
                        .should.eventually.to.be.equal(false, 'Блок "Удалить ответ" отсутствует на странице');
                },
            }),
        },
    },
});
