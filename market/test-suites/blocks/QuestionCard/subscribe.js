import {makeCase, makeSuite} from 'ginny';

/**
 * @param {PageObject.QuestionCard} questionCard
 */
export default makeSuite('Подписка на ответы. Юзер не подписан.', {
    feature: 'Подписка',
    story: {
        'При клике на кнопку "Подписаться"': {
            'должна происходить подписка': makeCase({
                id: 'm-touch-2438',
                issue: 'MOBMARKET-10250',
                async test() {
                    await this.expect(await this.questionCard.getSubscribtionText())
                        .to.be.equal('Подписаться', 'Текст кнопки должен быть "Подписаться"');
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.questionCard.clickSubscribeButton(),
                        valueGetter: () => this.questionCard.getSubscribtionText(),
                    });
                    await this.expect(await this.questionCard.getSubscribtionText())
                        .to.be.equal('Вы подписаны', 'Текст кнопки должен измениться на "Вы подписаны"');
                },
            }),
        },
    },
});
