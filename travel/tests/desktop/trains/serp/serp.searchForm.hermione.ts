import {assert} from 'chai';
import {serp} from 'suites/trains';

import {TestTrainsApp} from 'helpers/project/trains/app/TestTrainsApp';
import {msk, spb} from 'helpers/project/trains/data/cities';
import skipBecauseProblemWithIM from 'helpers/skips/skipBecauseProblemWithIM';

const {name: suiteName} = serp;

const tomorrow = {
    id: 'tomorrow',
    name: 'Завтра',
};

describe(suiteName, () => {
    skipBecauseProblemWithIM();
    it('Очистка саджеста в поисковой форме', async function () {
        const app = new TestTrainsApp(this.browser);

        await app.setSearchAutoMock();
        await app.goToSearchPage(msk.slug, spb.slug, tomorrow.id);

        const {
            searchPage: {searchForm},
        } = app;

        await searchForm.fromSuggest.resetValue();

        const from = await searchForm.fromSuggest.getInputValue();

        assert.equal(from, '', `Поле "Откуда" не очистилось"`);
    });
});
