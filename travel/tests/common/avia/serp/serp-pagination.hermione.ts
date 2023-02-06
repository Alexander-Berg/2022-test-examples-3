import {assert} from 'chai';
import moment from 'moment';
import {index, serp} from 'suites/avia';

import {
    TestIndexAviaPage,
    AviaSearchResultsPage,
} from 'helpers/project/avia/pages';

describe(serp.name, () => {
    beforeEach(async function () {
        try {
            await this.browser.url(index.url);
            await new TestIndexAviaPage(this.browser).search({
                fromName: 'Москва',
                toName: 'Санкт-Петербург',
                when: moment().add(3, 'day').format('YYYY-MM-DD'),
            });
        } catch (e) {
            console.error(serp.name, e instanceof Error ? e.message : e);
        }
    });

    it('Пагинация', async function () {
        let searchPage = new AviaSearchResultsPage(this.browser);

        await searchPage.waitForSearchComplete();

        const variants = await searchPage.variants.items;

        await variants[variants.length - 1].scrollIntoView();
        // Надо заново инициализировать AviaSearchResultsPage, чтобы получить новые variants из DOM
        searchPage = new AviaSearchResultsPage(this.browser);

        const enrichedVariants = await searchPage.variants.items;

        assert(
            enrichedVariants.length > variants.length,
            'не стало видно больше офферов',
        );
    });
});
