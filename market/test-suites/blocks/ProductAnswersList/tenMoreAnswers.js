import {makeCase, makeSuite} from 'ginny';

/**
 * @param {PageObject.ProductAnswersList} productAnswersList
 */
export default makeSuite('Блок списка ответов c количеством больше 10.', {
    story: {
        'Кнопка "Показать еще". ': {
            'По умолчанию': {
                'должна отображаться': makeCase({
                    id: 'm-touch-2238',
                    issue: 'MOBMARKET-9096',
                    feature: 'Структура страницы',
                    async test() {
                        const isLoadMoreButtonShown = await this.productAnswersList.isLoadMoreButtonShown();
                        await this.expect(isLoadMoreButtonShown)
                            .to.be.equal(true, 'Кнопка отображается');
                    },
                }),
                'подгружает еще ответов': makeCase({
                    id: 'm-touch-2275',
                    issue: 'MOBMARKET-9100',
                    feature: 'Структура страницы',
                    async test() {
                        await this.browser.yaWaitForChangeValue({
                            action: () => this.productAnswersList.clickLoadMore(),
                            valueGetter: () => this.productAnswersList.productAnswersCount,
                        });
                        const productAnswersCount = await this.productAnswersList.productAnswersCount;
                        await this.expect(productAnswersCount)
                            .to.be.greaterThan(10);
                    },
                }),
            },
        },
        'По умолчанию': {
            'отображается верное количество ответов': makeCase({
                id: 'm-touch-2241',
                issue: 'MOBMARKET-9103',
                feature: 'Структура страницы',
                async test() {
                    const productAnswersCount = await this.productAnswersList.productAnswersCount;
                    await this.expect(productAnswersCount)
                        .to.be.equal(10);
                },
            }),
        },
    },
});
