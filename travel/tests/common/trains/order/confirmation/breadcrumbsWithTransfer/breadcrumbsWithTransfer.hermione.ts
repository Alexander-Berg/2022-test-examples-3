import {order} from 'suites/trains';
import {assert} from 'chai';

import {TestTrainsApp} from 'helpers/project/trains/app/TestTrainsApp';
import {
    PASSENGER,
    PASSENGER_SECOND,
} from 'helpers/project/trains/data/passengers';

describe(order.steps.confirmation, () => {
    it('ЖД+ЖД Хлебные крошки', async function () {
        const app = new TestTrainsApp(this.browser);

        const {
            orderPlacesStepPage,
            orderPassengersStepPage,
            orderConfirmationStepPage,
        } = app;

        await orderPlacesStepPage.browseToPageWithTransfer();
        await orderPlacesStepPage.waitTrainDetailsLoaded();

        assert.equal(
            await orderPlacesStepPage.layout.orderSteps.getActiveStepText(),
            'Выбор мест. Первый поезд',
            'Изначально должен быть активен шаг выбора мест для первого поезда',
        );

        assert.deepEqual(
            await orderPlacesStepPage.layout.orderSteps.getDisabledStepsTexts(),
            [
                'Выбор мест. Второй поезд',
                'Данные пассажиров',
                'Подтверждение',
                'Оплата',
            ],
            'Изначально все шаги после выбора мест в первом поезде должны быть недоступны',
        );

        await orderPlacesStepPage.selectPassengers({adults: 2});
        await orderPlacesStepPage.selectAnyPlacesInPlatzkarte();

        assert.deepEqual(
            await orderPlacesStepPage.layout.orderSteps.getDisabledStepsTexts(),
            [
                'Выбор мест. Второй поезд',
                'Данные пассажиров',
                'Подтверждение',
                'Оплата',
            ],
            'После выбора первого места в первом поезде, должны остаться недоступны последующие шаги',
        );

        await orderPlacesStepPage.selectAnyPlacesInPlatzkarte();

        assert.deepEqual(
            await orderPlacesStepPage.layout.orderSteps.getDisabledStepsTexts(),
            ['Данные пассажиров', 'Подтверждение', 'Оплата'],
            'После выбора второго места в первом поезде должен стать доступным шаг выбора мест во втором поезде',
        );

        const secondPlacesStep =
            await orderPlacesStepPage.layout.orderSteps.getStepByText(
                'Выбор мест. Второй поезд',
            );

        await secondPlacesStep?.click();

        assert.equal(
            await orderPlacesStepPage.layout.orderSteps.getActiveStepText(),
            'Выбор мест. Второй поезд',
            'После клика в хлебную крошку выбора мест во втором поезде, она должна стать активной',
        );

        assert.deepEqual(
            await orderPlacesStepPage.layout.orderSteps.getDisabledStepsTexts(),
            ['Данные пассажиров', 'Подтверждение', 'Оплата'],
            'После перехода на выбор мест во втором поезде, должны остаться недоступны последующие шаги',
        );

        await orderPlacesStepPage.selectAnyPlacesInPlatzkarte();

        assert.deepEqual(
            await orderPlacesStepPage.layout.orderSteps.getDisabledStepsTexts(),
            ['Данные пассажиров', 'Подтверждение', 'Оплата'],
            'После выбора первого места во втором поезде шаг ввода данных должен остаться недоступен для выбора',
        );

        await orderPlacesStepPage.selectAnyPlacesInPlatzkarte();

        assert.deepEqual(
            await orderPlacesStepPage.layout.orderSteps.getDisabledStepsTexts(),
            ['Подтверждение', 'Оплата'],
            'После выбора второго места во втором поезде шаг ввода данных должен стать доступен',
        );

        const passengersStep =
            await orderPlacesStepPage.layout.orderSteps.getStepByText(
                'Данные пассажиров',
            );

        await passengersStep?.click();

        assert.equal(
            await orderPassengersStepPage.layout.orderSteps.getActiveStepText(),
            'Данные пассажиров',
            'После клика в хлебную крошку ввода данных пассажиров, она должна стать активной',
        );

        assert.deepEqual(
            await orderPassengersStepPage.layout.orderSteps.getDisabledStepsTexts(),
            ['Подтверждение', 'Оплата'],
            'После перехода на страницу ввода данных пассажиров, должны остаться недоступны последующие шаги',
        );

        await app.setTestContext();

        await orderPassengersStepPage.fillPassengers([
            PASSENGER,
            PASSENGER_SECOND,
        ]);

        await orderPassengersStepPage.fillContacts();
        await orderPassengersStepPage.layout.goNextStep();
        await orderConfirmationStepPage.waitOrderLoaded();

        assert.equal(
            await orderConfirmationStepPage.layout.orderSteps.getActiveStepText(),
            'Подтверждение',
            'После создания заказа, должна стать активной вкладка "Подтверждение"',
        );

        assert.deepEqual(
            await orderConfirmationStepPage.layout.orderSteps.getDisabledStepsTexts(),
            ['Оплата'],
            'После перехода на страницу подтверждения, должен быть недоступен только шаг оплаты',
        );

        const firstPlacesStep =
            await orderConfirmationStepPage.layout.orderSteps.getStepByText(
                'Выбор мест. Первый поезд',
            );

        await firstPlacesStep?.click();

        assert.equal(
            await orderPlacesStepPage.layout.orderSteps.getActiveStepText(),
            'Выбор мест. Первый поезд',
            'При возвращении со страницы подтверждения на первую страницу выбора мест, должна стать активной вкладка выбора мест для первого поезда',
        );

        assert.deepEqual(
            await orderPlacesStepPage.layout.orderSteps.getDisabledStepsTexts(),
            ['Данные пассажиров', 'Подтверждение', 'Оплата'],
            'При возвращении со страницы выбора мест, должны быть недоступны шаги после выбора мест во второй поезд',
        );
    });
});
