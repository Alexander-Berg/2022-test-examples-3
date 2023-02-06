import {assert} from 'chai';
import {SUITES} from 'suites/buses';

import getHumanWhen from 'helpers/utilities/date/getHumanWhen';
import {TestBusesApp} from 'helpers/project/buses/app/TestBusesApp';

describe(SUITES.pages.search.date.name, function () {
    it('Общий вид страницы', async function () {
        const app = new TestBusesApp(this.browser);
        const searchDatePage = app.searchPage;

        const {
            route: [from, to],
            when,
        } = await app.goToFilledSearchPage();

        assert.isTrue(
            await searchDatePage.isDisplayed(),
            'Должна отображаться страница поиска.',
        );

        await searchDatePage.waitUntilLoaded();

        const humanWhen = getHumanWhen(when);

        assert.isTrue(
            await searchDatePage.header.isDisplayed(),
            'Должен отображаться хэдер страницы',
        );

        assert.isTrue(
            await searchDatePage.filtersDesktop.isDisplayed(),
            'Должны отображаться фильтры на странице поиска',
        );

        assert.equal(
            await searchDatePage.title.getText(),
            `Автобусы из ${from.name.from} в ${to.name.to}, ${humanWhen}`,
            'Должен отображаться верный заголовок поиска',
        );

        assert.isTrue(
            await searchDatePage.sortsDesktop.isDisplayed(),
            'Должны отображаться сортировки на странице поиска',
        );

        assert.isTrue(
            await searchDatePage.searchForm.isDisplayed(),
            'Форма поиска должна отображаться',
        );

        assert.isTrue(
            await searchDatePage.segments.isItemsDisplayedCorrectly(),
            'Должны корректно отображаться поисковые сегменты.',
        );

        assert.isTrue(
            await searchDatePage.footer.isDisplayed(),
            'Должен отображаться футер страницы',
        );
    });
});
