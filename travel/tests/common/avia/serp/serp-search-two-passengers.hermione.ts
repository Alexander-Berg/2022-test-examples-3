import moment from 'moment';
import {random} from 'lodash';
import {serp} from 'suites/avia';
import {assert} from 'chai';

import dateFormats from 'helpers/utilities/date/formats';
import {TestAviaApp} from 'helpers/project/avia/app/TestAviaApp';

const airportsMoscow = ['SVO', 'DME', 'VKO', 'ZIA'];
const airportSaintPetersburg = ['LED'];

describe(serp.name, () => {
    it('Авиа: Поиск город_рф-город_рф на двоих туда-обратно', async function () {
        const app = new TestAviaApp(this.browser);

        const when = moment().add(1, 'month').add(random(1, 7), 'day');
        const whenForSearch = when.format(dateFormats.ROBOT);
        const whenForOrder = when.format(dateFormats.HUMAN);
        const returnDate = moment().add(1, 'month').add(random(8, 14), 'day');
        const returnForSearch = returnDate.format(dateFormats.ROBOT);
        const returnForOrder = returnDate.format(dateFormats.HUMAN);

        await app.goToIndexPage();
        await app.indexPage.search({
            fromName: 'Москва',
            toName: 'Санкт-Петербург',
            klass: 'economy',
            when: whenForSearch,
            return_date: returnForSearch,
            adult_seats: '2',
        });

        await app.searchPage.waitForSearchComplete();

        const firstVariant = await app.searchPage.variants.first();

        const forwardVariantPointData =
            await firstVariant.getForwardFlightInfo();
        const backwardVariantPointData =
            await firstVariant.getBackwardFlightInfo();

        assert.isTrue(
            airportsMoscow.includes(forwardVariantPointData.departureIATA),
            'Должен быть соответствующий пункт отправления Туда',
        );

        assert.isTrue(
            airportSaintPetersburg.includes(
                forwardVariantPointData.arrivalIATA,
            ),
            'Должен быть соответствующий пункт прибытия Туда',
        );

        assert.isTrue(
            airportSaintPetersburg.includes(
                backwardVariantPointData.departureIATA,
            ),
            'Должен быть соответствующий пункт отправления Обратно',
        );

        assert.isTrue(
            airportsMoscow.includes(backwardVariantPointData.arrivalIATA),
            'Должен быть соответствующий пункт прибытия Обратно',
        );

        await firstVariant.moveToOrder();

        await app.orderPage.waitForLoading();

        const orderPassengersTitle =
            await app.orderPage.offers.title.passengers.getText();
        const orderKlassTitle =
            await app.orderPage.offers.title.klass.getText();
        const forwardPointsTitle =
            await app.orderPage.forward.title.points.getText();
        const forwardDate = (
            await app.orderPage.forward.title.dates.getText()
        ).split(',');
        const backwardPointsTitle =
            await app.orderPage.backward.title.points.getText();
        const backwardDate = (
            await app.orderPage.backward.title.dates.getText()
        ).split(',');
        const partnerButton = (await app.orderPage.offers.cheapest).button;

        assert.equal(
            orderPassengersTitle,
            '2 пассажира',
            'Должно быть указано корректное количество пассажиров',
        );

        assert.equal(
            orderKlassTitle,
            'эконом-класс',
            'Должен быть указан корректный класс перелёта',
        );

        assert.equal(
            forwardPointsTitle,
            'Москва — Санкт-Петербург',
            'Должен быть указан корректный заголовок с пунктами отправления/прибытия для направления Туда',
        );

        assert.equal(
            whenForOrder,
            forwardDate[0],
            'Должна совпадать дата вылета Туда',
        );

        assert.equal(
            backwardPointsTitle,
            'Санкт-Петербург — Москва',
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
