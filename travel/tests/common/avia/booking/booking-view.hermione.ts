import {random} from 'lodash';
import {assert} from 'chai';
import {every} from 'p-iteration';
import moment from 'moment';

import {AVIA_SUCCESS_TEST_CONTEXT_PARAMS} from 'helpers/constants/testContext';

import dateFormats from 'helpers/utilities/date/formats';
import {TestAviaApp} from 'helpers/project/avia/app/TestAviaApp';

describe('Авиабилеты: Бронирование', function () {
    it('Общий вид страницы бронирования', async function () {
        const date = moment().add(1, 'month').add(random(1, 10), 'day');

        const app = new TestAviaApp(
            this.browser,
            AVIA_SUCCESS_TEST_CONTEXT_PARAMS,
        );
        const {searchPage, orderPage, createOrderPage} = app;

        await app.goToSearchPage({
            from: {name: 'Москва', id: 'c213'},
            to: {name: 'Сочи', id: 'c239'},
            startDate: date.format(dateFormats.ROBOT),
            travellers: {
                adults: 2,
                children: 0,
                infants: 0,
            },
            klass: 'economy',
            filters: 'pt=aeroflot&c=0,26',
        });
        await searchPage.waitForSearchComplete();

        const searchVariant = await searchPage.variants.first();

        await searchVariant.moveToOrder();

        const companyOffer = await orderPage.offers.company;

        await companyOffer.scrollIntoView();
        await companyOffer.click();

        // явным образом переключаем вкладку, т.к. без этой команды
        // вебдрайвер продолжит взаимодействоватьс неактивной вкладкой
        await this.browser.switchToNextTab();

        await createOrderPage.waitPageReadyForInteraction();

        const flights = await createOrderPage.flightInfo.items;
        const flightData = await flights[0].getFlightData();

        assert(
            await createOrderPage.header.isDisplayed(),
            'не отображается шапка портала',
        );

        assert(
            await every(
                [
                    createOrderPage.banner.logo.isDisplayed(),
                    createOrderPage.banner.text.isDisplayed(),
                ],
                Boolean,
            ),
            'проблемы с отображением баннера Аэрофлота',
        );

        assert(
            await every(
                [
                    createOrderPage.breadcrumbs.search.isDisplayed(),
                    createOrderPage.breadcrumbs.order.isDisplayed(),
                    createOrderPage.breadcrumbs.form.isDisplayed(),
                    createOrderPage.breadcrumbs.payment.isDisplayed(),
                ],
                Boolean,
            ),
            'проблемы с отображением хлебных крошек',
        );

        // flight data
        assert(
            Boolean(flightData.departure && flightData.arrival),
            'не отображаются данные о времени отправления / прибытия',
        );
        assert(Boolean(flightData.flightNumber), 'не отображается номер рейса');
        assert(
            Boolean(flightData.baggage || flightData.carryOn),
            'не отображаются данные о багаже / ручной клади',
        );

        assert(
            await createOrderPage.tariffs.isDisplayed(),
            'не отображается блок с тарифами',
        );

        assert(
            await createOrderPage.priceInfo.isDisplayed(),
            'не отображаются данные о цене',
        );

        assert(
            await createOrderPage.submitButton.isDisplayed(),
            'не отображается кнопка "Оплатить"',
        );

        const passengerForms = await createOrderPage.form.passengers.items;

        assert.equal(
            passengerForms.length,
            2,
            'отображается некорректное число форм пользователя',
        );

        assert(
            await createOrderPage.form.contacts.isDisplayed(),
            'не отображается форма контактных данных',
        );

        assert(
            await createOrderPage.footer.isDisplayed(),
            'не отображается футер',
        );
    });
});
