import {makeCase, makeSuite} from 'ginny';

/**
 * @param {PageObject.Answer} answer
 * @param {PageObject.CommentsList} commentsList
 * @param {PageObject.CommentForm} commentForm
 */
export default makeSuite('Блок комментариев. У ответа ровно 3 комментария.', {
    story: {
        'По умолчанию': {
            'отображается 2 комментария': makeCase({
                id: 'marketfront-3655',
                issue: 'MARKETVERSTKA-35432',
                async test() {
                    await this.answer.isVisible().should.eventually.be.equal(
                        true,
                        'На странице виден ответ'
                    );
                    const answerCommentsCount = await this.commentsList.getCommentsCount();
                    await this.expect(answerCommentsCount)
                        .to.be.equal(2, 'Под ответом 2 сниппета комментариев');
                    await this.answer.expandComment.isVisible().should.eventually.to.be.equal(
                        true, 'Кнопка Показать N ответов'
                    );
                    await this.answer.expandComment.getText().should.eventually.to.be.equal('Ещё 1 комментарий');
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.answer.clickExpandComment(),
                        valueGetter: () => this.answer.expandComment.getText(),
                    });
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.answer.clickExpandComment(),
                        valueGetter: () => this.answer.expandComment.getText(),
                    });
                    await this.commentsList.isVisible()
                        .should.eventually.to.be.equal(false, 'Под ответом нет комментариев');
                },
            }),
        },
        'При нажатии на кнопку "Отправить"': {
            'Добавляется новый комментарий': makeCase({
                id: 'marketfront-3651',
                issue: 'MARKETVERSTKA-35432',
                async test() {
                    const commentText = 'lol';
                    await this.answer.isVisible().should.eventually.be.equal(
                        true,
                        'На странице виден ответ'
                    );
                    let answerCommentsCount = await this.commentsList.getCommentsCount();
                    await this.expect(answerCommentsCount)
                        .to.be.equal(2, 'Под ответом 2 сниппета комментариев');
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.answer.clickCommentButton(),
                        valueGetter: () => this.commentForm.isVisible(),
                    });
                    await this.commentForm.clickTextarea();
                    await this.commentForm.setText(commentText);
                    await this.commentForm.isSendButtonVisible().should.eventually.to.be.equal(
                        true, 'Появилась кнопка "Отправить"'
                    );
                    await this.commentForm.isClearButtonVisible().should.eventually.to.be.equal(
                        true, 'Появился крестик для очистки инпута'
                    );
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.commentForm.clickSendButton(),
                        valueGetter: () => this.commentsList.getCommentsCount(),
                    });
                    answerCommentsCount = await this.commentsList.getCommentsCount();
                    await this.expect(answerCommentsCount)
                        .to.be.equal(3, 'Под ответом стало 3 сниппета комментариев');
                    await this.commentsList.getCommentTextByIndex(3).should.eventually.to.be.equal(
                        commentText, 'Текст комментария соответствует введённому'
                    );
                    await this.commentForm.isVisible().should.eventually.to.be.equal(
                        false, 'Инпут для добавления комментария отсутствует'
                    );
                },
            }),
        },
    },
});
