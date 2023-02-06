import {makeCase, makeSuite} from 'ginny';

/**
 * @param {PageObject.CabinetQuestionSnippet} questionSnippet
 * @param {PageObject.QuestionHeader} questionHeader
 * @param {PageObject.VoteButton} voteButton
 */
export default makeSuite('Сниппет вопроса пользователя с ответами.', {
    environment: 'kadavr',
    story: {
        'Голосовалка': {
            'При клике по лайку': {
                'счетчик лайков увеличивается': makeCase({
                    id: 'm-touch-3122',
                    issue: 'MARKETFRONT-6439',
                    async test() {
                        const beforeLikeCount = await this.voteButton.getVotesCount();
                        await this.browser.yaWaitForChangeValue({
                            action: () => this.voteButton.clickVote(),
                            valueGetter: () => this.voteButton.getVotesCount(),
                        });
                        const afterLikeCount = await this.voteButton.getVotesCount();
                        await this.expect(afterLikeCount)
                            .to.be.greaterThan(beforeLikeCount, 'лайков стало больше');

                        await this.browser.refresh();
                        await this.expect(afterLikeCount)
                            .to.be.greaterThan(beforeLikeCount, 'лайков стало больше');
                    },
                }),
            },
            'При клике два раза подряд': {
                'счетчик не изменится': makeCase({
                    id: 'm-touch-3123',
                    issue: 'MARKETFRONT-6439',
                    async test() {
                        const beforeLikeCount = await this.voteButton.getVotesCount();

                        await this.browser.yaWaitForChangeValue({
                            action: () => this.voteButton.clickVote(),
                            valueGetter: () => this.voteButton.getVotesCount(),
                        });
                        await this.browser.yaWaitForChangeValue({
                            action: () => this.voteButton.clickVote(),
                            valueGetter: () => this.voteButton.getVotesCount(),
                        });

                        const afterLikeCount = await this.voteButton.getVotesCount();

                        await this.expect(afterLikeCount)
                            .to.be.equal(beforeLikeCount, 'лайков осталось столько же');
                    },
                }),
            },
        },
        'Кнопка удаления ответа': {
            'по умолчанию': {
                'отсутствует': makeCase({
                    id: 'm-touch-3115',
                    issue: 'MARKETFRONT-6439',
                    async test() {
                        await this.questionHeader.isDeleteButtonVisible()
                            .should.eventually.to.be.equal(false,
                                'Кнопка удаления ответа отсутствует');
                    },
                }),
            },
        },
    },
});
