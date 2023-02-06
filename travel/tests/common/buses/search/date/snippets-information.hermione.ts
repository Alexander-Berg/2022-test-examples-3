import {assert} from 'chai';
import {SUITES} from 'suites/buses';

import {MINUTE} from 'helpers/constants/dates';

import getHumanWhen from 'helpers/utilities/date/getHumanWhen';
import {TestBusesApp} from 'helpers/project/buses/app/TestBusesApp';

describe(SUITES.pages.search.date.name, function () {
    hermione.config.testTimeout(4 * MINUTE);
    it('Сниппеты', async function () {
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

        assert.equal(
            await searchDatePage.title.getText(),
            `Автобусы из ${from.name.from} в ${to.name.to}, ${humanWhen}`,
            'Должен отображаться верный заголовок поиска.',
        );

        const segments = await searchDatePage.segments.items.items;

        for (const segment of segments) {
            assert.isTrue(
                await segment.departure.station.isDisplayed(),
                'Должен отображаться пункт отправления на сниппете',
            );

            assert.isTrue(
                await segment.departure.time.isDisplayed(),
                'Должно отображаться время отправления на сниппете',
            );

            assert.isTrue(
                await segment.arrival.isDisplayedCorrectly(),
                'Должна отображаться информация о прибытии, либо "Неизвестно"',
            );

            assert.isTrue(
                await segment.isDurationDisplayedCorrectly(),
                'Должно отображаться время в пути, либо "Неизвестно" в прибытии',
            );

            assert.isTrue(
                await segment.carrier.isDisplayed(),
                'Должно отображаться название перевозчика',
            );

            assert.isTrue(
                await segment.price.isDisplayed(),
                'Должна отображаться стоимость билета',
            );

            assert.isTrue(
                await segment.places.isDisplayed(),
                'Должно отображаться наличие мест',
            );
        }
    });
});
