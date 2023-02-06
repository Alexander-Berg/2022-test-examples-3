import {flightPage} from 'suites/avia';
import moment from 'moment';
import {random} from 'lodash';
import {assert} from 'chai';

import {TestAviaApp} from 'helpers/project/avia/app/TestAviaApp';
import dateFormats from 'helpers/utilities/date/formats';

describe(flightPage.name, () => {
    it(`Проверка перехода на страницу заказа со страницы рейса`, async function () {
        const when = moment().add(1, 'month').add(random(1, 7), 'day');
        const whenUrl = when.format(dateFormats.ROBOT);
        const app = new TestAviaApp(this.browser);

        await app.flightPage.goToFlightPage('SU-2', whenUrl);
        await app.flightPage.waitForLoaded();

        assert.equal(
            await app.flightPage.title.getText(),
            'Рейс SU 2 Москва – Санкт-Петербург авиакомпании Аэрофлот',
            'Должно совпадать содержание заголовка',
        );

        assert.isTrue(
            await app.flightPage.bookBlock.isVisible(),
            'Должен присутствовать блок Билеты',
        );

        await app.flightPage.bookBlock.scrollIntoView();

        // Запоминаем дату в блоке, а не используем ту с которой изначально искали, т.к.
        // при попадании на дату, в которую рейса нет произойдёт редирект на дату, когда рейс есть
        const whenHuman = await app.flightPage.bookBlock.getDate();

        await app.flightPage.bookBlock.button.click();

        await app.orderPage.waitForLoading();

        assert.equal(
            await (
                await app.orderPage.forward.flights.first()
            ).planeNumber.getText(),
            'SU 2',
            'Должен отображаться корректный номер рейса',
        );

        assert.include(
            await app.orderPage.forward.title.dates.getText(),
            whenHuman,
            'Должна отображаться корректная дата',
        );
    });
});
