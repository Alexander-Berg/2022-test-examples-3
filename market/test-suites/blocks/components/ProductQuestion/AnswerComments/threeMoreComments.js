import {makeCase, makeSuite} from 'ginny';

/**
 * @param {PageObject.Answer} answer
 * @param {PageObject.CommentsList} commentsList
 * @param {PageObject.CommentForm} commentForm
 */
export default makeSuite('Блок комментариев. У ответа больше 3 комментариев.', {
    story: {
        'По умолчанию': {
            'отображается 2 комментария': makeCase({
                id: 'marketfront-3654',
                issue: 'MARKETVERSTKA-35432',
                async test() {
                    const commentsCount = 4;
                    await this.answer.isVisible().should.eventually.be.equal(
                        true,
                        'На странице виден ответ'
                    );
                    let answerCommentsCount = await this.commentsList.getCommentsCount();
                    await this.expect(answerCommentsCount)
                        .to.be.equal(2, 'Под ответом 2 сниппета комментариев');
                    await this.commentForm.isVisible().should.eventually.to.be.equal(
                        false, 'Инпут для добавления комментария отсутствует'
                    );
                    await this.answer.expandComment.isVisible().should.eventually.to.be.equal(
                        true, 'Кнопка Ещё 2 комментария присутствует'
                    );
                    await this.answer.expandComment.getText().should.eventually.to.be.equal(
                        'Ещё 2 комментария'
                    );
                    await this.browser.allure.runStep(
                        'Нажимаем кнопку "Показать ещё 2 комментария"',
                        () => this.browser.yaWaitForChangeValue({
                            action: () => this.answer.clickExpandComment(),
                            valueGetter: () => this.commentsList.getCommentsCount(),
                        }));
                    answerCommentsCount = await this.commentsList.getCommentsCount();
                    await this.expect(answerCommentsCount)
                        .to.be.equal(commentsCount, `Под ответом стало ${commentsCount} сниппета комментариев`);
                    await this.answer.expandComment.getText().should.eventually.to.be.equal(
                        'Скрыть комментарии', 'Текст кнопки поменялся'
                    );
                    await this.commentForm.isVisible().should.eventually.to.be.equal(
                        false, 'Инпут для добавления комментария отсутствует'
                    );
                    await this.browser.allure.runStep(
                        'Нажимаем кнопку "Скрыть комментарии"',
                        () => this.browser.yaWaitForChangeValue({
                            action: () => this.answer.clickExpandComment(),
                            valueGetter: () => this.commentsList.isVisible(),
                        }));
                    await this.answer.expandComment.getText().should.eventually.to.be.equal(
                        `${commentsCount} комментария`, 'Текст кнопки поменялся'
                    );
                    await this.commentsList.isVisible()
                        .should.eventually.to.be.equal(false, 'Под ответом нет комментариев');
                    await this.browser.allure.runStep(
                        'Нажимаем кнопку "4 комментрия"',
                        () => this.browser.yaWaitForChangeValue({
                            action: () => this.answer.clickExpandComment(),
                            valueGetter: () => this.commentsList.isVisible(),
                        }));
                    answerCommentsCount = await this.commentsList.getCommentsCount();
                    await this.expect(answerCommentsCount)
                        .to.be.equal(4, 'Под ответом 4 сниппета комментариев');
                },
            }),
        },
        'Видны 2 комментария.': {
            'При нажатии на кнопку "Отправить" добавляется новый комментарий': makeCase({
                id: 'marketfront-3652',
                issue: 'MARKETVERSTKA-35432',
                async test() {
                    const commentsCount = 4;
                    const commentText = 'lol';
                    await this.answer.isVisible().should.eventually.be.equal(
                        true,
                        'На странице виден ответ'
                    );
                    let answerCommentsCount = await this.commentsList.getCommentsCount();
                    await this.expect(answerCommentsCount)
                        .to.be.equal(2, 'Под ответом 2 сниппета комментариев');
                    await this.answer.clickExpandComment();
                    await this.commentForm.isVisible().should.eventually.to.be.equal(
                        false, 'Инпут для добавления комментария отсутствует'
                    );
                    await this.answer.commentButton.isVisible().should.eventually.to.be.equal(
                        true, 'Кнопка "Комментировать" присутствует'
                    );
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
                    await this.commentForm.isVisible().should.eventually.to.be.equal(
                        false, 'Инпут для добавления комментария отсутствует'
                    );
                    answerCommentsCount = await this.commentsList.getCommentsCount();
                    await this.expect(answerCommentsCount)
                        .to.be.equal(commentsCount + 1, 'Под ответом стало 5 сниппета комментариев');
                    await this.commentsList.getCommentTextByIndex(5).should.eventually.to.be.equal(
                        commentText, 'Текст комментария соответствует введённому'
                    );
                },
            })},
        'Видны все комментарии.': {
            'При нажатии на кнопку "Отправить" добавляется новый комментарий': makeCase({
                id: 'marketfront-3653',
                issue: 'MARKETVERSTKA-35432',
                async test() {
                    const commentsCount = 4;
                    const commentText = 'lol';
                    await this.answer.isVisible().should.eventually.be.equal(
                        true,
                        'На странице виден ответ'
                    );
                    let answerCommentsCount = await this.commentsList.getCommentsCount();
                    await this.expect(answerCommentsCount)
                        .to.be.equal(2, 'Под ответом 2 сниппета комментариев');
                    await this.commentForm.isVisible().should.eventually.to.be.equal(
                        false, 'Инпут для добавления комментария отсутствует'
                    );
                    await this.answer.expandComment.isVisible().should.eventually.to.be.equal(
                        true, 'Кнопка "N комментариев" присутствует'
                    );
                    await this.answer.expandComment.getText().should.eventually.to.be.equal(
                        'Ещё 2 комментария'
                    );
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.answer.clickExpandComment(),
                        valueGetter: () => this.commentsList.getCommentsCount(),
                    });
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
                        valueGetter: () => this.commentForm.isVisible(),
                    });
                    answerCommentsCount = await this.commentsList.getCommentsCount();
                    await this.expect(answerCommentsCount)
                        .to.be.equal(commentsCount + 1, 'Под ответом стало 5 сниппета комментариев');
                    await this.commentsList.getCommentTextByIndex(5).should.eventually.to.be.equal(
                        commentText, 'Текст комментария соответствует введённому'
                    );
                },
            })},
    },
});
