const search = require('../page-objects/client-search-form');
const { assert } = require('chai');

describe('Саджесты -> ', () => {
    it('diskclient-1635: Саджесты. Скролл саджеста', async function() {
        const testData = {
            user: 'yndx-ufo-test-540',
            searches: ['хлебные', 'mp4', 'jpg', 'крошки', 'Санкт-Петербург', 'Москва', 'Море', 'Мишки', 'Зима', 'Горы']
        };
        const bro = this.browser;
        const lastSearch = search.common.searchResultItems.item() +
            `[title="${testData.searches[testData.searches.length - 1]}"]`;

        await bro.yaClientLoginFast(testData.user);
        await bro.click(search.common.searchForm());
        await bro.yaWaitForVisible(search.common.searchResultItems());

        const resultItems = await bro.$$(search.common.searchResultItems.item());
        const actualSearches = await Promise.all(resultItems.map((item) => item.getAttribute('title')));

        assert.deepEqual(actualSearches, testData.searches);

        await bro.yaAssertInViewport(lastSearch, false);
        await bro.yaScrollIntoView(lastSearch);
        await bro.yaAssertInViewport(lastSearch, true);
    });
});
