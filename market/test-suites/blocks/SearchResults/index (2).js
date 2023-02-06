import {makeSuite, makeCase} from 'ginny';
import {subtract, flip, eq, curryN} from 'lodash/fp';

const flippedSubtract = curryN(2, flip(subtract));

/**
 * @param {PageObject.SearchResults} SearchResults
 */
export default makeSuite('Список выдачи.', {
    story: {
        'По умолчанию': {
            'должен содержать 8 товаров': makeCase({
                id: 'm-touch-1986',
                issue: 'MOBMARKET-7701',
                test() {
                    return this.searchResults.snippetsCount()
                        .should.eventually.to.be.equal(8, 'отображается 8 товаров');
                },
            }),
        },

        'При скролле ниже и клике на кнопку "Показать еще"': {
            'должно продолжать загружаться по 8 сниппетов': makeCase({
                id: 'm-touch-2058',
                issue: 'MOBMARKET-7911',
                async test() {
                    const snippetsLoaded = initialCount => () =>
                        this.searchResults
                            .snippetsCount()
                            .then(flippedSubtract(initialCount))
                            .then(eq(8));

                    let initialCount;

                    // После 5 проскролов (до 96 элементов) автоскрол перестает работать
                    // и нужно руками нажать "Показать еще"
                    for (let i = 1; i <= 5; i++) {
                        initialCount = await this.searchResults.snippetsCount();

                        await this.browser.allure.runStep(`Скроллим к кнопке "Показать еще" (${i})`, () =>
                            this.searchResults.showMore.getLocation().then(location =>
                                this.browser.scroll(location.x, location.y))
                        );

                        await this.browser.waitUntil(snippetsLoaded(initialCount), 15000, 'Не дождались загрузки ' +
                            'сниппетов или загрузилось неправильное количество');
                    }

                    initialCount = await this.searchResults.snippetsCount();
                    await this.searchResults.showMore.click();

                    await this.browser.waitUntil(snippetsLoaded(initialCount), 15000, 'Не дождались загрузки ' +
                        'сниппетов или загрузилось неправильное количество');
                },
            }),
        },
    },
});
