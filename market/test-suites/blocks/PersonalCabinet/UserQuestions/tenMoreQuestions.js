import {makeCase, makeSuite} from 'ginny';

/**
 * @param {PageObject.UserQuestions} UserQuestions
 */
export default makeSuite('Список с 30 элементами.', {
    story: {
        'По умолчанию': {
            'Отображается 10 вопросов': makeCase({
                id: 'm-touch-3112',
                issue: 'MARKETFRONT-6439',
                async test() {
                    await this.userQuestions.userQuestionsCount
                        .should.eventually.to.be.equal(10, 'На странице 10 вопросов');
                    await this.userQuestions.isLoadMoreButtonVisible()
                        .should.eventually.to.be.equal(true, 'Кнопка "Показать еще" видимая');
                },
            }),
        },
        'При клике "Показать еще"': {
            'Загружается 20 вопросов и показывается кнопка "Показать еще"': makeCase({
                id: 'm-touch-3113',
                issue: 'MARKETFRONT-6439',
                async test() {
                    await this.userQuestions.isLoadMoreButtonVisible()
                        .should.eventually.to.be.equal(true, 'Кнопка "Показать еще" видимая');

                    await this.browser.yaWaitForChangeValue({
                        action: () => this.userQuestions.clickLoadMoreButton(),
                        valueGetter: () => this.userQuestions.userQuestionsCount,
                    });
                    const questionsCount = await this.userQuestions.userQuestionsCount;
                    await this.expect(questionsCount)
                        .to.be.equal(20, 'На странице 20 вопросов');
                    await this.userQuestions.isLoadMoreButtonVisible()
                        .should.eventually.to.be.equal(true, 'Кнопка "Показать еще" видимая');
                },
            }),
        },
        'При двух последовательных кликах': {
            'загружается 30 вопросов и кнопка "Показать еще" скрывается': makeCase({
                id: 'm-touch-3114',
                issue: 'MARKETFRONT-6439',
                async test() {
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.userQuestions.clickLoadMoreButton(),
                        valueGetter: () => this.userQuestions.userQuestionsCount,
                    });
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.userQuestions.clickLoadMoreButton(),
                        valueGetter: () => this.userQuestions.userQuestionsCount,
                    });
                    const questionsCount = await this.userQuestions.userQuestionsCount;
                    await this.expect(questionsCount)
                        .to.be.equal(30, 'На странице 30 вопросов');
                    await this.userQuestions.isLoadMoreButtonVisible()
                        .should.eventually.to.be.equal(false, 'Кнопка "Показать еще" невидимая');
                },
            }),
        },
    },
});
