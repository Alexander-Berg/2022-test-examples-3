import {makeCase, makeSuite, mergeSuites} from 'ginny';

/**
 * @param {PageObject.widgets.content.NextAchievement} nextAchievement
 */
export default makeSuite('Виджет агитации следующей ачивкой.', {
    params: {
        nextAchievementName: 'Название ачивки',
        nextAchievementDescription: 'Условия выдачи ачивки',
        nextAchievementProgress: 'Прогресс в получении ачивки',
    },
    story: mergeSuites({
        'По умолчанию отображает верную ачивку, название, условие и пргресс.': makeCase({
            id: 'marketfront-3974',
            async test() {
                await this.nextAchievement.isVisible()
                    .should.eventually.be.equal(true, 'Виджет агитации агитации следующей ачивкой отображается');

                await this.nextAchievement.name.getText()
                    .should.eventually.be.equal(this.params.nextAchievementName,
                        'Отображается корректное название ачивки');

                await this.nextAchievement.description.getText()
                    .should.eventually.be.equal(this.params.nextAchievementDescription,
                        'Отображается корректное условия выдачи ачивки');

                await this.nextAchievement.events.getText()
                    .should.eventually.be.equal(this.params.nextAchievementProgress,
                        'Отображается корректный прогресс в получении ачивки');
            },
        }),
    }),
});
