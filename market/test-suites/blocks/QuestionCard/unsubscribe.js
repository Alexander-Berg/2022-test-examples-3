import {makeCase, makeSuite} from 'ginny';

/**
 * @param {PageObject.QuestionCard} questionCard
 */
export default makeSuite('Подписка на ответы. Юзер подписан.', {
    feature: 'Подписка',
    story: {
        'При клике на кнопку "Вы подписаны"': {
            'должна происходить отписка': makeCase({
                id: 'm-touch-2439',
                issue: 'MOBMARKET-10251',
                async test() {
                    await this.expect(await this.questionCard.getSubscribtionText())
                        .to.be.equal('Вы подписаны', 'Текст кнопки должен быть "Вы подписаны"');
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.questionCard.clickSubscribeButton(),
                        valueGetter: () => this.questionCard.getSubscribtionText(),
                    });
                    await this.expect(await this.questionCard.getSubscribtionText())
                        .to.be.equal('Подписаться', 'Текст кнопки должен измениться на "Подписаться"');
                },
            }),
        },
    },
});
