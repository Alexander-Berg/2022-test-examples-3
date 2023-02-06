import moment from 'moment';
import {assert} from 'chai';
import {serp} from 'suites/avia';

import * as cities from 'helpers/project/avia/data/cities';
import {TestAviaApp} from 'helpers/project/avia/app/TestAviaApp';
import {ESortingType} from 'helpers/project/avia/pages/SearchResultsPage/components/Sorting';

describe(serp.name, function () {
    /**
     * https://st.yandex-team.ru/TRAVELFRONT-2171
     */
    it('Сортировка вариантов устанавливает начальное значение из кук', async function () {
        const later = moment().add(2, 'months').format('YYYY-MM-DD');

        const app = new TestAviaApp(this.browser);
        const page = app.searchPage;

        await app.goToSearchPage({
            from: cities.msk,
            to: cities.ekb,
            startDate: later,
            travellers: {adults: 2, children: 1, infants: 1},
            klass: 'business',
        });

        const {sorting} = page;

        // Сортировка по цене
        await sorting.selectSortOption(ESortingType.PRICE);

        const selectSortValue = await sorting.typeSelect.getValue();

        await this.browser.refresh();

        await page.waitForSearchComplete();

        assert.equal(
            await sorting.typeSelect.getValue(5000),
            selectSortValue,
            'Значение сортировки изменилось после обновления страницы',
        );
    });
});
