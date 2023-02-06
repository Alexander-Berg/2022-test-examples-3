import {makeCase, makeSuite, mergeSuites} from 'ginny';

/**
 * @param {PageObject.widgets.content.UserTasks} userTasks
 * @param {PageObject.widgets.content.NextAchievement} nextAchievement
 * @param {PageObject.widgets.content.ReviewPolls} reviewPolls
 * @param {PageObject.widgets.content.QuestionAgitations} questionAgitations
 */
export default makeSuite('Виджет вопросов других пользователей.', {
    params: {
        title: 'Заголовок Zero-стейт',
        subtitle: 'Текст агитации',
    },
    story: mergeSuites({
        'По умолчанию': {
            'показывает зеро-стейт': makeCase({
                id: 'marketfront-3997',
                async test() {
                    await this.userTasks.title.isVisible()
                        .should.eventually.be.equal(true, 'Отображается заголовок');
                    await this.userTasks.title.getText()
                        .should.eventually.be.equal(this.params.title, 'Отображается правильный текст заголовока');
                    await this.userTasks.isSubtitleVisible()
                        .should.eventually.be.equal(true, 'Отображается сообщение с агитацией');
                    await this.userTasks.subtitle.getText()
                        .should.eventually.be.equal(this.params.subtitle, 'Отображается правильный текст в агитации');
                    await this.nextAchievement.isVisible()
                        .should.eventually.be.equal(true, 'Виджет со следующей ачивкой отображается');
                    await this.questionAgitations.isVisible()
                        .should.eventually.be.equal(false, 'Виджет с вопросами других пользователей не отображается');
                    return this.reviewPolls.isVisible()
                        .should.eventually.be.equal(false, 'Виджет с агитацией оставить отзыв не отображается');
                },
            }),
        },
    }),
});
