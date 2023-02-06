import {SUITES} from 'suites/buses';
import moment from 'moment';
import {assert} from 'chai';

import {TestBusesApp} from 'helpers/project/buses/app/TestBusesApp';

const when = moment().add(1, 'month');
const from = SUITES.regions.msk;
const to = SUITES.regions.newport;

describe(SUITES.pages.search.date.name, function () {
    it('Автобусы. Проверка получения пустой выдачи', async function () {
        const app = new TestBusesApp(this.browser);
        const searchDatePage = app.searchPage;

        await app.goToSearchPage(from.slug, to.slug, when);

        assert.isTrue(
            await searchDatePage.isDisplayed(),
            'Должна отображаться страница поиска.',
        );

        const fromName = from.name.base;
        const toName = to.name.base;

        assert.equal(
            await searchDatePage.emptySerp.title.getText(),
            `Билеты на автобус ${fromName} — ${toName}`,
            'Должен быть корректный заголовок пустой выдачи',
        );

        assert.equal(
            await searchDatePage.emptySerp.text.getText(),
            'Мы не нашли билетов на это направление.\n' +
                'Давайте поищем куда-нибудь еще!',
            'Должен быть корректный текст для пустой выдачи',
        );
    });
});
