import {assert} from 'chai';
import {SUITES} from 'suites/buses';

import getHumanWhen from 'helpers/utilities/date/getHumanWhen';
import {TestBusesApp} from 'helpers/project/buses/app/TestBusesApp';

describe(SUITES.pages.search.date.name, () => {
    it('Общий вид выдачи.', async function () {
        const app = new TestBusesApp(this.browser);
        const searchDatePage = app.searchPage;
        const {searchForm} = searchDatePage;

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

        assert.equal(
            await searchDatePage.title.getText(),
            `Автобусы из ${from.name.from} в ${to.name.to}, ${humanWhen}`,
            'Должен отображаться верный заголовок поиска.',
        );

        if (searchDatePage.isTouch) {
            assert.isTrue(
                await searchDatePage.filtersAndSortsMobile.isDisplayed(),
                'Должны отображаться фильтры и сортировки в таче.',
            );
        } else {
            assert.isTrue(
                await searchDatePage.filtersDesktop.isDisplayed(),
                'Должны отображаться фильтры в десктопе.',
            );

            assert.isTrue(
                await searchDatePage.sortsDesktop.isDisplayed(),
                'Должны отображаться сортировки в десктопе.',
            );
        }

        if (searchDatePage.isTouch) {
            assert.isTrue(
                (await searchDatePage.header.searchInformation.isDisplayed()) &&
                    !(await searchDatePage.searchForm.isDisplayedInViewport()),
                'Должна отображаться свернутая поисковая форма.',
            );

            const [departure, arrival] =
                await searchDatePage.header.searchInformation.getDirections();

            assert.equal(
                departure,
                from.name.base,
                'Должен отображаться правильный пункт отправления в свернутой поисковой форме.',
            );

            assert.equal(
                arrival,
                to.name.base,
                'Должен отображаться правильный пункт прибытия в свернутой поисковой форме.',
            );

            assert.include(
                (
                    await searchDatePage.header.searchInformation.when.getText()
                ).toLowerCase(),
                humanWhen,
                'Должно отображаться правильное время отправления в свернутой поисковой форме.',
            );
        } else {
            const fromSuggestValue =
                await searchForm.fromSuggest.getInputValue();
            const toSuggestValue = await searchForm.toSuggest.getInputValue();
            const whenTriggerValue =
                await searchForm.datePicker.startTrigger.value.getText();

            assert.equal(
                fromSuggestValue,
                from.name.base,
                'Должен отображаться правильный пункт отправления в поисковой форме.',
            );

            assert.equal(
                toSuggestValue,
                to.name.base,
                'Должен отображаться правильный пункт прибытия в поисковой форме.',
            );

            assert.include(
                whenTriggerValue,
                humanWhen,
                'Должно отображаться правильное время отправления в поисковой форме.',
            );
        }

        assert.isTrue(
            await searchDatePage.segments.isItemsDisplayedCorrectly(),
            'Должны корректно отображаться поисковые сегменты.',
        );
    });
});
