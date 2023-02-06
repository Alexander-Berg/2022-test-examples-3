import {index, serp} from 'suites/avia';
import moment from 'moment';
import {assert} from 'chai';
import {random} from 'lodash';

import {
    TestIndexAviaPage,
    AviaOrderPage,
    AviaSearchResultsPage,
} from 'helpers/project/avia/pages';
import dateFormats from 'helpers/utilities/date/formats';

describe(serp.name, () => {
    it('Авиа: Поиск аэропорт-аэропорт туда', async function () {
        await this.browser.url(index.url);

        const indexPage = new TestIndexAviaPage(this.browser);

        const when = moment().add(1, 'month').add(random(1, 7), 'day');
        const whenForSearch = when.format(dateFormats.ROBOT);
        const whenForOrder = when.format(dateFormats.HUMAN);

        await indexPage.search({
            fromName: 'Домодедово',
            toName: 'Хитроу',
            when: whenForSearch,
        });

        const searchPage = new AviaSearchResultsPage(this.browser);

        await searchPage.waitForSearchComplete();

        const firstVariant = await searchPage.variants.first();

        const variantPointData = await firstVariant.getForwardFlightInfo();

        assert.equal(
            variantPointData.departureIATA,
            'DME',
            'Должен быть соответствующий пункт отправления на сниппете',
        );

        assert.equal(
            variantPointData.arrivalIATA,
            'LHR',
            'Должен быть соответствующий пункт прибытия на сниппете',
        );

        await firstVariant.moveToOrder();

        const orderPage = new AviaOrderPage(this.browser);

        await orderPage.waitForLoading();

        const orderPassengersTitle =
            await orderPage.offers.title.passengers.getText();
        const orderKlassTitle = await orderPage.offers.title.klass.getText();
        const forwardPointsTitle =
            await orderPage.forward.title.points.getText();
        const forwardDate = (
            await orderPage.forward.title.dates.getText()
        ).split(',');
        const partnerButton = (await orderPage.offers.cheapest).button;

        assert.equal(
            orderPassengersTitle,
            '1 пассажир',
            'Должно быть указано корректное количество пассажиров',
        );

        assert.equal(
            orderKlassTitle,
            'эконом-класс',
            'Должен быть указан корректный класс перелёта',
        );

        assert.equal(
            forwardPointsTitle,
            'Москва — Лондон',
            'Должен быть указан корректный заголовок с пунктами отправления/прибытия',
        );

        assert.equal(
            whenForOrder,
            forwardDate[0],
            'Должна совпадать дата вылета',
        );
        assert.isTrue(
            await partnerButton.isVisible(),
            'Должна присутствовать хотя бы одна кнопка перехода к партнеру',
        );
    });
});
