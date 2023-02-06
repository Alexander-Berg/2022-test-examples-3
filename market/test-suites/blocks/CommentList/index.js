import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.CommentList} commentList
 */

export default makeSuite('Список комментариев', {
    story: {
        'по умолчанию': {
            'должнен присутствовать.': makeCase({
                id: 'marketfront-4212',
                async test() {
                    await this.commentList.isVisible()
                        .should.eventually.be.equal(true, 'блок присутствует на странице.');
                },
            }),
        },
    },
});
