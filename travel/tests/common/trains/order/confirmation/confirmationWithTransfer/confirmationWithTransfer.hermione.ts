import {order} from 'suites/trains';
import {assert} from 'chai';

import {MINUTE} from 'helpers/constants/dates';

import {TestTrainsApp} from 'helpers/project/trains/app/TestTrainsApp';
import {
    PASSENGER,
    PASSENGER_SECOND,
} from 'helpers/project/trains/data/passengers';

const PASSENGERS_COUNT = 2;
const SEGMENTS_COUNT = 2;

describe(order.steps.confirmation, () => {
    hermione.config.testTimeout(3 * MINUTE);
    it('ЖД+ЖД Бронирование и данные о рейсах на странице подтверждения', async function () {
        const app = new TestTrainsApp(this.browser);

        const {
            orderPlacesStepPage,
            orderPlacesStepPage: {orderSegments: placesStepOrderSegments},
            orderPassengersStepPage,
            orderConfirmationStepPage,
        } = app;

        const {fromName, toName, transferName} =
            await orderPlacesStepPage.browseToPageWithTransfer();

        await orderPlacesStepPage.waitTrainDetailsLoaded();

        const beforeOrderSegmentsInfo =
            await placesStepOrderSegments.segments.map(segment =>
                segment.getInfo(),
            );

        const beforeOrderTransferInfo = {
            description:
                await placesStepOrderSegments.transfer.description.getText(),
            duration: await placesStepOrderSegments.transfer.duration.getText(),
        };

        await orderPlacesStepPage.selectPassengers({adults: PASSENGERS_COUNT});
        await orderPlacesStepPage.selectAnyPlacesInPlatzkarte(PASSENGERS_COUNT);
        await orderPlacesStepPage.goNextStep();

        await orderPlacesStepPage.selectAnyPlacesInPlatzkarte(PASSENGERS_COUNT);
        await orderPlacesStepPage.goNextStep();

        await orderPassengersStepPage.fillPassengers([
            PASSENGER,
            PASSENGER_SECOND,
        ]);

        await orderPassengersStepPage.fillContacts();

        await app.setTestContext();

        await orderPassengersStepPage.layout.goNextStep();

        await orderConfirmationStepPage.waitOrderLoaded();

        assert.equal(
            await orderConfirmationStepPage.layout.orderSteps.getActiveStepText(),
            'Подтверждение',
            'Должна открыться страница подтверждения заказа',
        );

        const confirmationSegments = await orderConfirmationStepPage.segments
            .segments.items;
        const confirmationPlaces = await orderConfirmationStepPage.places.items;

        for (let i = 0; i < SEGMENTS_COUNT; i++) {
            const beforeOrderSegmentInfo = beforeOrderSegmentsInfo[i];
            const confirmationSegmentInfo = await confirmationSegments[
                i
            ].getInfo();

            assert.equal(
                beforeOrderSegmentInfo.number,
                confirmationSegmentInfo.number,
                `${
                    i + 1
                } сегмент: номер поезда должен совпадать на странице выбора мест и странице подтверждения`,
            );

            assert.equal(
                beforeOrderSegmentInfo.departureTime,
                confirmationSegmentInfo.departureTime,
                `${
                    i + 1
                } сегмент: время отправления должно совпадать на странице выбора мест и странице подтверждения`,
            );

            assert.equal(
                beforeOrderSegmentInfo.departureCity,
                confirmationSegmentInfo.departureCity,
                `${
                    i + 1
                } сегмент: город отправления должен совпадать на странице выбора мест и странице подтверждения`,
            );

            assert.equal(
                beforeOrderSegmentInfo.arrivalCity,
                confirmationSegmentInfo.arrivalCity,
                `${
                    i + 1
                } сегмент: город прибытия должен совпадать на странице выбора мест и странице подтверждения`,
            );

            assert.isTrue(
                confirmationSegmentInfo.hasElectronicRegistration,
                `${i + 1} сегмент: на странице подтверждения должна быть ЭР`,
            );

            const placesInfo = confirmationPlaces[i];

            if (i === 0) {
                assert.equal(
                    await placesInfo.direction.getText(),
                    `${fromName} — ${transferName}`,
                    `${
                        i + 1
                    } вагон и места: должно быть верное направление поезда`,
                );
            } else {
                assert.equal(
                    await placesInfo.direction.getText(),
                    `${transferName} — ${toName}`,
                    `${
                        i + 1
                    } вагон и места: должно быть верное направление поезда`,
                );
            }

            assert.isTrue(
                await placesInfo.places.isDisplayed(),
                `${i + 1} вагон и места: должны отображаться выбранные места`,
            );

            assert.isTrue(
                await placesInfo.schemaWrapper.isDisplayed(),
                `${i + 1} вагон и места: должна отображаться схема вагона`,
            );
        }

        const confirmationPassengers = await orderConfirmationStepPage
            .passengers.passengers.items;

        for (let i = 0; i < PASSENGERS_COUNT; i++) {
            const passengerInfo = confirmationPassengers[i];

            const tickets = await passengerInfo.tickets.items;

            for (let j = 0; j < SEGMENTS_COUNT; j++) {
                const ticket = tickets[j];

                assert.equal(
                    await ticket.direction.getText(),
                    j === 0
                        ? `${fromName} — ${transferName}`
                        : `${transferName} — ${toName}`,
                    `${i} пассажир. ${
                        i + 1
                    } билет: должно быть верное направление поезда`,
                );

                assert.isTrue(
                    await ticket.places.isDisplayed(),
                    `${i} пассажир. ${
                        i + 1
                    } билет: должен отображаться номер места`,
                );

                assert.isTrue(
                    await ticket.tariff.isDisplayed(),
                    `${i} пассажир. ${i + 1} билет: должен отображаться тариф`,
                );
            }
        }

        const confirmationTransfer =
            orderConfirmationStepPage.segments.transfer;

        const confirmationTransferInfo = {
            description: await confirmationTransfer.description.getText(),
            duration: await confirmationTransfer.duration.getText(),
        };

        assert.equal(
            beforeOrderTransferInfo.description,
            confirmationTransferInfo.description,
            'Описание пересадки должно совпадать на странице выбора мест и странице подтверждения',
        );

        assert.equal(
            beforeOrderTransferInfo.duration,
            confirmationTransferInfo.duration,
            'Продолжительность пересадки должна совпадать на странице выбора мест и странице подтверждения',
        );
    });
});
