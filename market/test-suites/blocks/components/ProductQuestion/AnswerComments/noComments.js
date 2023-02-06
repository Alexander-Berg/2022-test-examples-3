import {makeCase, makeSuite} from 'ginny';

/**
 * @param {PageObject.Answer} answer
 * @param {PageObject.CommentsList} commentsList
 * @param {PageObject.CommentForm} commentForm
 */
export default makeSuite('Блок комментариев. У ответа нет других комментариев.', {
    story: {
        'При нажатии на кнопку "Отправить"': {
            'Добавляется новый комментарий': makeCase({
                id: 'marketfront-3650',
                issue: 'MARKETVERSTKA-35432',
                async test() {
                    const commentText = 'lol';
                    await this.answer.isVisible().should.eventually.be.equal(
                        true,
                        'На странице виден ответ'
                    );
                    await this.commentsList.isVisible()
                        .should.eventually.to.be.equal(false, 'Под ответом нет комментариев');
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.answer.clickCommentButton(),
                        valueGetter: () => this.commentForm.isVisible(),
                    });
                    await this.commentForm.setText(commentText);
                    await this.commentForm.isSendButtonVisible().should.eventually.to.be.equal(
                        true, 'Появилась кнопка "Отправить"'
                    );
                    await this.commentForm.isClearButtonVisible().should.eventually.to.be.equal(
                        true, 'Появился крестик для очистки инпута'
                    );
                    this.commentForm.clickSendButton();
                    await this.commentsList.waitForVisible();
                    await this.expect(this.commentsList.getCommentsCount())
                        .to.be.equal(1, 'Под сниппетом ответа появился сниппет комментария');
                    await this.commentsList.getCommentsText().should.eventually.to.be.equal(
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
