import {assert} from 'chai';
import {serp} from 'suites/trains';
import moment from 'moment';
import {random} from 'lodash';

import cities from 'helpers/project/trains/data/cities';
import {TestTrainsApp} from 'helpers/project/trains/app/TestTrainsApp';

const {name: suiteName} = serp;

describe(suiteName, () => {
    it('Подсказки при неизвестном пункте отправления/прибытия', async function () {
        const app = new TestTrainsApp(this.browser);
        const {indexPage, searchPage} = app;

        await app.goToIndexPage();
        await indexPage.searchForm.fromSuggest.setInputValue('Альба-Лонга');
        await indexPage.searchForm.toSuggest.setInputValue('Брундизий');

        await indexPage.searchForm.submitButton.click();

        assert.isTrue(
            await indexPage.isOpened(),
            'Перехода на выдачу не происходит',
        );

        assert.equal(
            await indexPage.searchForm.errorTooltip.getText(),
            'Неизвестный пункт отправления',
            'На index page tooltipError должен содержать текст "Неизвестный пункт отправления"',
        );

        await indexPage.searchForm.fromSuggest.setSuggestValue('Москва');
        await indexPage.searchForm.submitButton.click();

        assert.isTrue(
            await indexPage.isOpened(),
            'Перехода на выдачу не происходит',
        );

        assert.equal(
            await indexPage.searchForm.errorTooltip.getText(),
            'Неизвестный пункт прибытия',
            'На index page tooltipError должен содержать текст "Неизвестный пункт прибытия"',
        );

        const afterMonthDate = moment()
            .add(1, 'month')
            .add(random(0, 10), 'day')
            .format('YYYY-MM-DD');

        await app.setSearchAutoMock();
        await app.goToSearchPage(
            cities.msk.slug,
            cities.spb.slug,
            afterMonthDate,
        );

        await searchPage.searchForm.fromSuggest.resetValue();
        await searchPage.searchForm.fromSuggest.setInputValue('Альба-Лонга');
        await searchPage.searchForm.toSuggest.resetValue();
        await searchPage.searchForm.toSuggest.setInputValue('Брундизий');
        await searchPage.searchForm.submitButton.click();

        assert.isTrue(
            await searchPage.isOpened('moscow', 'saint-petersburg'),
            'Перепоиска на выдаче не происходит',
        );

        assert.equal(
            await searchPage.searchForm.errorTooltip.getText(),
            'Неизвестный пункт отправления',
            'На search page tooltipError должен содержать текст "Неизвестный пункт отправления"',
        );

        await searchPage.searchForm.fromSuggest.resetValue();
        await searchPage.searchForm.fromSuggest.setSuggestValue('Москва');

        await searchPage.searchForm.submitButton.click();

        assert.isTrue(
            await searchPage.isOpened('moscow', 'saint-petersburg'),
            'Перепоиска на выдаче не происходит',
        );

        assert.equal(
            await searchPage.searchForm.errorTooltip.getText(),
            'Неизвестный пункт прибытия',
            'На search page tooltipError должен содержать текст "Неизвестный пункт прибытия"',
        );
    });
});
