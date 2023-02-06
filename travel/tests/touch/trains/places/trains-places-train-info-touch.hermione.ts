import {order} from 'suites/trains';
import {assert} from 'chai';

import {TestTrainsApp} from 'helpers/project/trains/app/TestTrainsApp';

const {steps} = order;

describe(steps.places, () => {
    it('ЖД: Тач: блок с информацией о поезде на странице выбора мест', async function () {
        const app = new TestTrainsApp(this.browser);
        const {orderPlacesStepPage} = app;

        await orderPlacesStepPage.browseToPageWithoutTransfer();
        await orderPlacesStepPage.waitTrainDetailsLoaded();

        assert.isTrue(
            await orderPlacesStepPage.orderSegments.title.isVisible(),
            'Должны отображаться город отправления и прибытия',
        );

        assert.isTrue(
            await orderPlacesStepPage.orderSegments.title.departure.isVisible(),
            'Должна отображаться дата поездки',
        );

        const segmentInfo =
            await orderPlacesStepPage.orderSegments.getSegment();

        assert.isTrue(
            await segmentInfo.numberAndDirection.isVisible(),
            'должны отображаться номер поезда и города следования поезда',
        );

        assert.isTrue(
            await segmentInfo.timeAndDuration.departure.time.isVisible(),
            'Должно отображаться время отправления',
        );

        assert.isTrue(
            await segmentInfo.timeAndDuration.arrival.time.isVisible(),
            'Должно отображаться время прибытия',
        );

        assert.isTrue(
            await segmentInfo.timeAndDuration.duration.isVisible(),
            'Должна отображаться длительность в пути',
        );

        assert.isTrue(
            await segmentInfo.timeAndDuration.timeMessage.isVisible(),
            'Должна отображаться приписка Время местное',
        );

        assert.isTrue(
            await segmentInfo.stations.departure.isVisible(),
            'Должна отображаться станция отправления',
        );

        assert.isTrue(
            await segmentInfo.stations.arrival.isVisible(),
            'Должна отображаться станция прибытия',
        );
    });
});
