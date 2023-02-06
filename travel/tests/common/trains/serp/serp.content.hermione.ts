import {assert} from 'chai';
import {serp} from 'suites/trains';

import {getNearestWednesdayDate} from 'helpers/project/trains/utils/getNearestWednesdayDate';
import {ekb, sakhalinsk} from 'helpers/project/trains/data/cities';
import {TestTrainsApp} from 'helpers/project/trains/app/TestTrainsApp';

const {name: suiteName} = serp;

describe(suiteName, function () {
    it('Поиск на дату, когда нет поездов', async function () {
        const date = getNearestWednesdayDate();
        const app = new TestTrainsApp(this.browser);

        await app.goToSearchPage(ekb.slug, sakhalinsk.slug, date, false);

        const {searchPage} = app;

        assert.isTrue(
            await searchPage.emptySerp.isVisible(),
            'Должна отображаться пустая страница поиска с указанием об отсутствии рейсов',
        );

        assert.isFalse(
            await searchPage.filters.isVisible(),
            'Фильтры не должны отображаться на странице без результатов поиска',
        );

        assert.isFalse(
            await searchPage.searchToolbar.sorting.isVisible(),
            'Сортировки не должны отображаться на странице без результатов поиска',
        );
    });
});
