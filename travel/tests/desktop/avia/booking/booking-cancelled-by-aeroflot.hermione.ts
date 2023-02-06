import {assert} from 'chai';
import {random} from 'lodash';
import moment from 'moment';

import {phoneNumber} from 'helpers/project/common/phoneNumber';
import {TestAviaApp} from 'helpers/project/avia/app/TestAviaApp';
import {ADULT_MAN_WITH_PASSPORT} from 'helpers/project/avia/pages/CreateOrderPage/passengers';
import dateFormats from 'helpers/utilities/date/formats';

describe('ТК Авиа', function () {
    it('Не подтверждение оплаты аэрофлотом', async function () {
        const date = moment().add(1, 'month').add(random(1, 10), 'day');
        const app = new TestAviaApp(this.browser, {
            checkAvailabilityOnRedirOutcome: 'CAOR_SUCCESS',
            checkAvailabilityBeforeBookingOutcome: 'CAO_SUCCESS',
            tokenizationOutcome: 'TO_SUCCESS',
            confirmationOutcome: 'CO_PAYMENT_FAILED',
            mqEventOutcome: 'MEO_SUCCESS',
        });

        await app.book(
            {
                from: {name: 'Москва', id: 'c213'},
                to: {name: 'Сочи', id: 'c239'},
                startDate: date.format(dateFormats.ROBOT),
                travellers: {
                    adults: 1,
                    children: 0,
                    infants: 0,
                },
                klass: 'economy',
                filters: 'pt=aeroflot&c=0,26',
            },
            {
                passengers: [ADULT_MAN_WITH_PASSPORT],
                contacts: {
                    phone: phoneNumber,
                    email: 'test@test.ru',
                },
            },
        );

        const {happyPage} = app;

        assert.isTrue(
            await happyPage.isOrderHasError(),
            'Сообщение "Мы не смогли подтвердить заказ" не было отображено',
        );
    });
});
