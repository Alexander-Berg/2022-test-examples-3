import moment from 'moment';
import {random} from 'lodash';
import {index, serp} from 'suites/avia';
import {assert} from 'chai';

import dateFormats from 'helpers/utilities/date/formats';
import {
    TestIndexAviaPage,
    AviaOrderPage,
    AviaSearchResultsPage,
} from 'helpers/project/avia/pages';

const when = moment().add(1, 'month').add(random(1, 7), 'day');
const whenForSearch = when.format(dateFormats.ROBOT);
const whenForOrder = when.format(dateFormats.HUMAN);
const returnDate = moment().add(1, 'month').add(random(8, 14), 'day');
const returnForSearch = returnDate.format(dateFormats.ROBOT);
const returnForOrder = returnDate.format(dateFormats.HUMAN);
const airportsTokyo = ['HND', 'NRT'];
const airportsOsaka = ['KIX', 'ITM', 'UKB'];

describe(serp.name, () => {
    it('Авиа: Поиск город_не_рф-город_не_рф бизнес', async function () {
        await this.browser.url(index.url);

        const indexPage = new TestIndexAviaPage(this.browser);

        await indexPage.search({
            fromName: 'Токио',
            toName: 'Осака',
            klass: 'business',
            when: whenForSearch,
            return_date: returnForSearch,
        });

        const searchPage = new AviaSearchResultsPage(this.browser);

        await searchPage.waitForSearchComplete();

        const firstVariant = await searchPage.variants.first();

        const forwardVariantPointData =
            await firstVariant.getForwardFlightInfo();
        const backwardVariantPointData =
            await firstVariant.getBackwardFlightInfo();

        assert.isTrue(
            airportsTokyo.includes(forwardVariantPointData.departureIATA),
            'Должен быть соответствующий пункт отправления Туда',
        );

        assert.isTrue(
            airportsOsaka.includes(forwardVariantPointData.arrivalIATA),
            'Должен быть соответствующий пункт прибытия Обратно',
        );

        assert.isTrue(
            airportsOsaka.includes(backwardVariantPointData.departureIATA),
            'Должен быть соответствующий пункт отправления Обратно',
        );

        assert.isTrue(
            airportsTokyo.includes(backwardVariantPointData.arrivalIATA),
            'Должен быть соответствующий пункт прибытия Обратно',
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
        const backwardPointsTitle =
            await orderPage.backward.title.points.getText();
        const backwardDate = (
            await orderPage.backward.title.dates.getText()
        ).split(',');
        const partnerButton = (await orderPage.offers.cheapest).button;

        assert.equal(
            orderPassengersTitle,
            '1 пассажир',
            'Должно быть указано корректное количество пассажиров',
        );

        assert.equal(
            orderKlassTitle,
            'бизнес-класс',
            'Должен быть указан корректный класс перелёта',
        );

        assert.equal(
            forwardPointsTitle,
            'Токио — Осака',
            'Должен быть указан корректный заголовок с пунктами отправления/прибытия для направления Туда',
        );

        assert.equal(
            whenForOrder,
            forwardDate[0],
            'Должна совпадать дата вылета Туда',
        );

        assert.equal(
            backwardPointsTitle,
            'Осака — Токио',
            'Должен быть указан корректный заголовок с пунктами отправления/прибытия для направления Обратно',
        );

        assert.equal(
            returnForOrder,
            backwardDate[0],
            'Должна совпадать дата вылета Обратно',
        );

        assert.isTrue(
            await partnerButton.isVisible(),
            'Должна присутствовать хотя бы одна кнопка перехода к партнеру',
        );
    });
});
