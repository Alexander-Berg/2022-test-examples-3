import {assert} from 'chai';
import {serp} from 'suites/trains';
import {random} from 'lodash';
import moment from 'moment';

import {TestTrainsApp} from 'helpers/project/trains/app/TestTrainsApp';
import {msk, spb} from 'helpers/project/trains/data/cities';
import skipBecauseProblemWithIM from 'helpers/skips/skipBecauseProblemWithIM';

const {name: suiteName} = serp;
const date = moment().add(random(0, 10), 'day').add(1, 'month');
const dateString = date.format('YYYY-MM-DD');

describe(suiteName, () => {
    skipBecauseProblemWithIM();
    it('Проверка заполения поисковой формы', async function () {
        const app = new TestTrainsApp(this.browser);

        await app.setSearchAutoMock();
        await app.goToSearchPage(msk.slug, spb.slug, dateString);

        const {searchPage} = app;
        const {searchForm, portalHeader} = searchPage;

        if (searchPage.isTouch) {
            await portalHeader.openSearchForm();
        }

        const from = await searchForm.fromSuggest.getInputValue();
        const to = await searchForm.toSuggest.getInputValue();

        const when =
            await searchForm.whenDatePicker.startTrigger.value.getText();

        assert.equal(
            from,
            msk.name,
            `Поле "Откуда" не заполнено "${msk.name}"`,
        );
        assert.equal(to, spb.name, `Поле "Куда" не заполнено "${spb.name}"`);
        assert.equal(
            when,
            date.locale('ru').format(searchPage.isDesktop ? 'D MMM' : 'D MMMM'),
            'Когда не содержит правильной даты',
        );
    });

    skipBecauseProblemWithIM();
    it('Проверка списка подсказок в саджесте в поисковой форме', async function () {
        const app = new TestTrainsApp(this.browser);

        await app.setSearchAutoMock();
        await app.goToSearchPage(msk.slug, spb.slug, dateString);

        const {searchPage} = app;

        const {
            portalHeader,
            searchForm: {fromSuggest},
        } = searchPage;

        if (searchPage.isTouch) {
            await portalHeader.openSearchForm();
        }

        await fromSuggest.resetValue();
        await fromSuggest.setInputValue('Сама');

        await this.browser.pause(2000);

        const titles = await fromSuggest.suggestItems.map(item =>
            item.title.getText(),
        );

        const titlesNotStartsWith = titles.filter(
            title => !title.startsWith('Сама'),
        );

        assert.isEmpty(
            titlesNotStartsWith,
            'Не все предложения начинаются с верного префикса',
        );
    });

    skipBecauseProblemWithIM();
    it('Выбор из списка саджестов в форме поиска', async function () {
        const app = new TestTrainsApp(this.browser);

        await app.setSearchAutoMock();
        await app.goToSearchPage(msk.slug, spb.slug, dateString);

        const {searchPage} = app;

        const {
            portalHeader,
            searchForm: {fromSuggest},
        } = searchPage;

        if (searchPage.isTouch) {
            await portalHeader.openSearchForm();
        }

        await fromSuggest.setSuggestValue('Саратов');

        const value = await fromSuggest.getInputValue();

        assert.equal(
            value,
            'Саратов',
            'Значение саджеста не соответствует выбранному',
        );
    });
});
