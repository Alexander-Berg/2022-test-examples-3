import {order} from 'suites/trains';
import {assert} from 'chai';

import {MINUTE} from 'helpers/constants/dates';

import {TestTrainsApp} from 'helpers/project/trains/app/TestTrainsApp';

const SEGMENTS_COUNT = 2;

describe(order.steps.happyPage, () => {
    hermione.config.testTimeout(3 * MINUTE);
    it('ЖД+ЖД Данные о рейсах на happy page', async function () {
        const app = new TestTrainsApp(this.browser);

        const {
            orderPlacesStepPage,
            orderPassengersStepPage,
            orderConfirmationStepPage,
            paymentPage,
            genericHappyPage,
        } = app;

        await orderPlacesStepPage.browseToPageWithTransfer();
        await orderPlacesStepPage.waitTrainDetailsLoaded();

        const segmentsInfo =
            await orderPlacesStepPage.orderSegments.segments.map(segment =>
                segment.getInfo(),
            );

        const transferInfo = {
            description:
                await orderPlacesStepPage.orderSegments.transfer.description.getText(),
            duration:
                await orderPlacesStepPage.orderSegments.transfer.duration.getText(),
        };

        await orderPlacesStepPage.selectPassengers({adults: 1});
        await orderPlacesStepPage.selectAnyPlacesInPlatzkarte();
        await orderPlacesStepPage.goNextStep();

        await orderPlacesStepPage.selectAnyPlacesInPlatzkarte();
        await orderPlacesStepPage.goNextStep();

        await orderPassengersStepPage.fillPassengers();
        await orderPassengersStepPage.fillContacts();

        await app.setTestContext();
        await app.paymentTestContextHelper.setPaymentTestContext();

        await orderPassengersStepPage.layout.goNextStep();
        await orderConfirmationStepPage.waitOrderLoaded();
        await orderConfirmationStepPage.goNextStep();

        await paymentPage.waitUntilLoaded();
        await genericHappyPage.waitUntilLoaded();

        const hpSegments = await genericHappyPage.orderInfo.segments.segments
            .items;

        for (let i = 0; i < SEGMENTS_COUNT; i++) {
            const segmentInfo = segmentsInfo[i];
            const hpSegmentInfo = await hpSegments[i].getInfo();

            assert.equal(
                segmentInfo.number,
                hpSegmentInfo.number,
                `${i} поезд, номер поезда должен совпадать на странице выбора мест и странице happyPage`,
            );

            assert.equal(
                segmentInfo.departureDate,
                hpSegmentInfo.departureDate,
                `${i} поезд, дата отправления должна совпадать на странице выбора мест и странице happyPage`,
            );

            assert.equal(
                segmentInfo.departureTime,
                hpSegmentInfo.departureTime,
                `${i} поезд, время отправления должно совпадать на странице выбора мест и странице happyPage`,
            );

            assert.equal(
                segmentInfo.arrivalDate,
                hpSegmentInfo.arrivalDate,
                `${i} поезд, дата прибытия должна совпадать на странице выбора мест и странице happyPage`,
            );

            assert.equal(
                segmentInfo.departureCity,
                hpSegmentInfo.departureCity,
                `${i} поезд, город отправления должен совпадать на странице выбора мест и странице happyPage`,
            );

            assert.equal(
                segmentInfo.arrivalCity,
                hpSegmentInfo.arrivalCity,
                `${i} поезд, город прибытия должен совпадать на странице выбора мест и странице happyPage`,
            );
        }

        const confirmationTransferInfo = {
            description:
                await genericHappyPage.orderInfo.segments.transfer.description.getText(),
            duration:
                await genericHappyPage.orderInfo.segments.transfer.duration.getText(),
        };

        assert.equal(
            transferInfo.description,
            confirmationTransferInfo.description,
            'Описание пересадки должно совпадать на странице выбора мест и странице happyPage',
        );

        assert.equal(
            transferInfo.duration,
            confirmationTransferInfo.duration,
            'Продолжительность пересадки должна совпадать на странице выбора мест и странице happyPage',
        );

        assert.isTrue(
            await genericHappyPage.orderActions.detailsLink.isDisplayed(),
            'Должна отображаться кнопка "Подробнее о заказе"',
        );

        if (genericHappyPage.isDesktop) {
            assert.isTrue(
                await genericHappyPage.orderActions.printButton.isDisplayed(),
                'Должна отображаться кнопка "Распечатать"',
            );
        }

        assert.isTrue(
            await genericHappyPage.orderActions.downloadButton.isDisplayed(),
            'Должна отображаться кнопка "Скачать"',
        );
    });
});
