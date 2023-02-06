import moment from 'moment';
import {assert} from 'chai';
import {index} from 'suites/trains';
import {URL} from 'url';

import {TestTrainsApp} from 'helpers/project/trains/app/TestTrainsApp';

describe(index.name, function () {
    it('Переход на поиск поездов с формы поиска, разный формат даты - дата из датапикера', async function () {
        const date = moment().add(3, 'days').format('YYYY-MM-DD');

        const app = new TestTrainsApp(this.browser);

        await app.goToIndexPage();

        const {searchForm} = app.indexPage;

        await searchForm.fill({
            from: 'Москва',
            to: 'Санкт-Петербург',
            when: date,
        });

        await app.setSearchAutoMock();
        await searchForm.submitForm();

        const currentUrl = await this.browser.getUrl();
        const {pathname, searchParams} = new URL(currentUrl);
        const when = searchParams.get('when');

        assert.match(pathname, /moscow--saint-petersburg/);
        assert.equal(when, date);
    });
});
