import {assert} from 'chai';
import {order} from 'suites/trains';

import {SAPSAN_TRAIN} from 'helpers/constants/imMocks';

import {TestTrainsApp} from 'helpers/project/trains/app/TestTrainsApp';
import {CONTACTS} from 'helpers/project/trains/data/contacts';
import {PASSENGER_WITH_ROAD_CARD} from 'helpers/project/trains/data/roadCard';

describe(order.steps.pay, () => {
    it('Бронирование Сапсана с дорожной картой', async function () {
        const app = new TestTrainsApp(this.browser);
        const {
            orderPlacesStepPage,
            orderPassengersStepPage,
            orderConfirmationStepPage,
        } = app;

        await orderPlacesStepPage.browseToPageWithoutTransfer(SAPSAN_TRAIN);
        await orderPlacesStepPage.waitTrainDetailsLoaded();

        await orderPlacesStepPage.selectAvailablePlace();
        await orderPlacesStepPage.goNextStep();

        await app.setFirstPassengerViaFields(
            PASSENGER_WITH_ROAD_CARD,
            CONTACTS,
        );

        await app.setTestContext();
        await orderPassengersStepPage.layout.goNextStep();
        await orderConfirmationStepPage.waitOrderLoaded();

        const firstTrain =
            await orderConfirmationStepPage.layout.orderSummary.trains.first();

        assert.isTrue(
            await firstTrain.places.places.some(async item =>
                /тариф карта/i.test(await item.title.getText()),
            ),
            'Тариф карта',
        );
    });
});
