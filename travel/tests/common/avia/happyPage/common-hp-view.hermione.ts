import {random} from 'lodash';
import moment from 'moment';

import {AVIA_SUCCESS_TEST_CONTEXT_PARAMS} from 'helpers/constants/testContext';
import {MINUTE} from 'helpers/constants/dates';

import {TestAviaApp} from 'helpers/project/avia/app/TestAviaApp';
import {ADULT_MAN_WITH_PASSPORT} from 'helpers/project/avia/pages/CreateOrderPage/passengers';
import {phoneNumber} from 'helpers/project/common/phoneNumber';
import dateFormats from 'helpers/utilities/date/formats';

describe('Авиабилеты: Happy page', () => {
    hermione.config.testTimeout(4 * MINUTE);
    it('Общий вид HP', async function () {
        const date = moment().add(1, 'month').add(random(1, 10), 'day');
        const app = new TestAviaApp(
            this.browser,
            AVIA_SUCCESS_TEST_CONTEXT_PARAMS,
        );

        const searchPage = await app.goToSearchPage({
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
        });

        await app.moveToBooking(searchPage);

        const {createOrderPage, paymentPage, happyPage} = app;

        await createOrderPage.waitPageReadyForInteraction();
        await createOrderPage.priceInfo.getPrice();
        await createOrderPage.fillBookingForm([ADULT_MAN_WITH_PASSPORT], {
            phone: phoneNumber,
            email: 'test@test.ru',
        });
        await createOrderPage.goToPayment();

        await paymentPage.loader.waitUntilLoaded(120000);

        await happyPage.waitForPageLoading();
        await happyPage.test();
    });
});
