import {order} from 'suites/trains';
import {assert} from 'chai';

import {TestTrainsApp} from 'helpers/project/trains/app/TestTrainsApp';
import checkSegments from './utilities/checkSegments';

describe(order.steps.places, () => {
    it('ЖД+ЖД Блок с данными о рейсах на выборе мест', async function () {
        const {orderPlacesStepPage} = new TestTrainsApp(this.browser);

        const {fromName, toName, firstSegmentDepartureMoment} =
            await orderPlacesStepPage.browseToPageWithTransfer();

        await orderPlacesStepPage.waitTrainDetailsLoaded();

        await checkSegments({
            fromName,
            toName,
            orderPlacesStepPage,
            firstSegmentDepartureMoment,
        });

        await orderPlacesStepPage.selectPassengers({adults: 1});
        await orderPlacesStepPage.selectAnyPlacesInPlatzkarte();
        await orderPlacesStepPage.goNextStep();

        const activeStepText =
            await orderPlacesStepPage.layout.orderSteps.getActiveStepText();

        assert.equal(
            activeStepText,
            'Выбор мест. Второй поезд',
            'Должны находиться на второй странице выбора мест',
        );

        await checkSegments({
            fromName,
            toName,
            orderPlacesStepPage,
            firstSegmentDepartureMoment,
        });
    });
});
