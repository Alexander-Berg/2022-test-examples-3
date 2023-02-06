import {order} from 'suites/trains';
import {assert} from 'chai';

import {TestTrainsApp} from 'helpers/project/trains/app/TestTrainsApp';

describe(order.steps.places, () => {
    it('ЖД+ЖД Блок с выбором пассажиров', async function () {
        const {orderPlacesStepPage} = new TestTrainsApp(this.browser);

        await orderPlacesStepPage.browseToPageWithTransfer();
        await orderPlacesStepPage.waitTrainDetailsLoaded();

        const activeStepText =
            await orderPlacesStepPage.layout.orderSteps.getActiveStepText();

        assert.equal(
            activeStepText,
            'Выбор мест. Первый поезд',
            'Должны находиться на первой странице выбора мест',
        );

        /* Обсудить добавление subtitle для пассажиров */
        // assert.include(
        //     await passengersCountSection.subtitle.getText(),
        //     'Количество пассажиров на всех частях маршрута должно быть одинаковым',
        //     'Должна отображаться приписка про одинаковое количество мест на первой странице выбора мест',
        // );

        await orderPlacesStepPage.selectPassengers({adults: 2});
        await orderPlacesStepPage.selectAnyPlacesInPlatzkarte(2);
        await orderPlacesStepPage.goNextStep();

        const nextActiveStepText =
            await orderPlacesStepPage.layout.orderSteps.getActiveStepText();

        assert.equal(
            nextActiveStepText,
            'Выбор мест. Второй поезд',
            'Должны находиться на второй странице выбора мест',
        );

        /* Обсудить добавление subtitle для пассажиров */
        // assert.include(
        //     await passengersCountSection.subtitle.getText(),
        //     'Количество пассажиров на всех частях маршрута должно быть одинаковым',
        //     'Должна отображаться приписка про одинаковое количество мест на второй странице выбора мест',
        // );

        assert.equal(
            await orderPlacesStepPage.passengersCountSection.adults.passengersCount.count.getText(),
            '2',
            'На странице выбора мест для второго поезда должно быть выбрано 2 взрослых пассажира',
        );

        assert.isTrue(
            await orderPlacesStepPage.passengersCountSection.adults.passengersCount.plusButton.isDisabled(),
            'Кнопка увеличения количества пассажиров должна быть заблокирована',
        );

        assert.isTrue(
            await orderPlacesStepPage.passengersCountSection.adults.passengersCount.minusButton.isDisabled(),
            'Кнопка уменьшения количества пассажиров должна быть заблокирована',
        );
    });
});
