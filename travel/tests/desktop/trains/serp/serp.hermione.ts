import {assert} from 'chai';
import {serp} from 'suites/trains';

import {msk, spb} from 'helpers/project/trains/data/cities';
import {TestTrainsApp} from 'helpers/project/trains/app/TestTrainsApp';

describe(serp.name, function () {
    it('Сортировка по цене', async function () {
        const app = new TestTrainsApp(this.browser);

        await app.setSearchAutoMock();
        await app.goToSearchPage(msk.slug, spb.slug, 'tomorrow', false);

        const {searchPage} = app;
        const {variants, searchToolbar} = searchPage;

        await searchToolbar.sorting.setSortBy('price');

        let url = await this.browser.getUrl();

        assert.include(
            url,
            'sortBy=price',
            'URL страницы должен содержать параметр: sortBy=price',
        );

        const sortedPricesByMinValue = await variants.getVariantMinPrices();

        assert.isAbove(
            sortedPricesByMinValue.length,
            2,
            'Вариантов с ценами должны быть не менее 2',
        );

        for (let i = 1; i < sortedPricesByMinValue.length; i++) {
            assert(
                sortedPricesByMinValue[i] >= sortedPricesByMinValue[i - 1],
                'Цены должны быть отсортированы по минимальной цене',
            );
        }

        await searchToolbar.sorting.changeSortDirection();

        url = await this.browser.getUrl();
        assert.include(
            url,
            'sortBy=-price',
            'URL страницы должен содержать параметр: sortBy=-price',
        );

        const sortedPricesByMaxValue = await variants.getVariantMinPrices();

        assert.isAbove(
            sortedPricesByMaxValue.length,
            2,
            'Вариантов с ценами должны быть не менее 2',
        );

        for (let i = 1; i < sortedPricesByMaxValue.length; i++) {
            assert(
                sortedPricesByMaxValue[i] <= sortedPricesByMaxValue[i - 1],
                'Цены должны быть отсортированы по максимальной цене',
            );
        }
    });
});
