import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.BigForm} commentForm
 */

export default makeSuite('Форма оставления комментария', {
    story: {
        'по умолчанию': {
            'должна присутствовать.': makeCase({
                id: 'marketfront-4211',
                async test() {
                    await this.commentForm.isTextFieldTextareaVisible()
                        .should.eventually.be.equal(true, 'блок присутствует на странице.');
                },
            }),
        },
    },
});
