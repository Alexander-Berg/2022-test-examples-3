import {assert} from 'chai';
import {order} from 'suites/trains';

import {MOCK_PAYMENT_URL} from 'helpers/constants/testContext';
import {MINUTE} from 'helpers/constants/dates';
import {NO_SCHEMA_TRAIN} from 'helpers/constants/imMocks';

import {ECoachType} from 'helpers/project/trains/types/coachType';
import {ETestGender} from 'components/TestBookingPassengerForm/types';
import {ITestFormContacts} from 'components/TestBookingContactsForm/types';

import extractNumber from 'helpers/utilities/extractNumber';
import {phoneNumber} from 'helpers/project/common/phoneNumber';
import {TestTrainsApp} from 'helpers/project/trains/app/TestTrainsApp';
import skipBecauseProblemWithIM from 'helpers/skips/skipBecauseProblemWithIM';
import {ITrainsTestFormDocument} from 'helpers/project/trains/components/TestTrainsBookingPassengerForm';

const PASSENGER: ITrainsTestFormDocument = {
    lastName: 'Автотестов',
    firstName: 'Автотест',
    patronymicName: 'Автотестович',
    sex: ETestGender.MALE,
    birthdate: '01.01.1990',
    documentNumber: '1212121212',
};

const CONTACTS: ITestFormContacts = {
    email: 'autotest@autotest.ru',
    phone: phoneNumber,
};

describe(order.steps.pay, () => {
    hermione.config.testTimeout(4 * MINUTE);
    skipBecauseProblemWithIM();
    it('Покупка билетов на поезд, у которого нет схемы и требований', async function () {
        const app = new TestTrainsApp(this.browser);

        const {
            orderPlacesStepPage,
            orderPassengersStepPage,
            orderConfirmationStepPage,
            paymentPage,
            genericOrderPage,
            happyPage,
        } = app;

        await orderPlacesStepPage.browseToPageWithoutTransfer(NO_SCHEMA_TRAIN);
        await orderPlacesStepPage.waitTrainDetailsLoaded();
        await orderPlacesStepPage.coachTypeTabsSelector.setActiveCoachType(
            ECoachType.SITTING,
        );

        const firstCoach =
            await orderPlacesStepPage.trainCoaches.coaches.first();

        if (!firstCoach) {
            throw new Error(`Не найден вагон с типом: ${ECoachType.SITTING}`);
        }

        assert.isTrue(
            await firstCoach.autoSeat.label.isDisplayed(),
            'Должен отображаться лейбл об автоматическом выборе мест для вагона без схемы',
        );

        assert.isTrue(
            await firstCoach.autoSeat.button.isDisplayed(),
            'Должна отображаться кнопка об автоматическом выборе мест для вагона без схемы',
        );

        await firstCoach.autoSeat.button.click();

        await app.setTestContext();
        await app.paymentTestContextHelper.setPaymentTestContext({
            minUserActionDelay:
                app.paymentTestContextHelper.getProgressiveDelay(
                    10,
                    this.retryCount,
                ),
            paymentOutcome: 'PO_SUCCESS',
            paymentUrl: MOCK_PAYMENT_URL,
        });

        if (!orderPassengersStepPage.isTouch) {
            assert.isNotNaN(
                await orderPassengersStepPage.layout.orderSummary.totalPrice.price.getPriceValue(),
                'Должна отображаться корректная стоимость',
            );
            assert.isTrue(
                await orderPassengersStepPage.layout.orderSummary.totalPrice.price.isExactValue(),
                'Должна отображаться стоимость, без "от"',
            );
        }

        const passengerBlock = await orderPassengersStepPage.passengers.first();

        await passengerBlock.fill(PASSENGER);

        await orderPassengersStepPage.contacts.fill(CONTACTS);
        await orderPassengersStepPage.layout.goNextStep();

        await orderConfirmationStepPage.waitOrderLoaded();

        const placesBlock = await orderConfirmationStepPage.getPlaces();
        const coachNumber = await placesBlock.coachNumber.getText();
        const places = await placesBlock.places.getText();

        assert.isFalse(
            await placesBlock.schemaWrapper.isDisplayed(),
            'Схема вагона не отображается',
        );

        const hasNumberRegExp = /\d+/;
        const hasUndefinedRegExp = /undefined/;
        const isCorrect = (str: string): boolean =>
            hasNumberRegExp.test(str) && !hasUndefinedRegExp.test(str);

        assert.isTrue(
            isCorrect(coachNumber),
            'На странице подтверждения данных вагон указан корректно (нет undefined)',
        );
        assert.isTrue(
            isCorrect(places),
            'На странице подтверждения данных место указано корректно (нет undefined)',
        );

        assert.isNotNaN(
            await orderConfirmationStepPage.layout.orderSummary.totalPrice.price.getPriceValue(),
            'Должна отображаться корректная стоимость',
        );
        assert(
            await orderConfirmationStepPage.layout.orderSummary.totalPrice.price.isExactValue(),
            'Стоимость должна отображаться точно, без "от"',
        );

        await orderConfirmationStepPage.goNextStep();

        await paymentPage.waitUntilLoaded();

        assert.isTrue(
            await paymentPage.orderDetails.directions.isVisible(),
            'Указана информация о направлении поездки',
        );

        assert.equal(
            await paymentPage.orderDetails.extractCoachNumber(),
            extractNumber(coachNumber),
            'Вагон указан корректно (нет undefined)',
        );
        assert.equal(
            await paymentPage.orderDetails.extractPlaceNumber(),
            extractNumber(places),
            'Место указано корректно (нет undefined)',
        );

        await happyPage.waitUntilLoaded();
        await happyPage.orderActions.detailsLink.click();

        await genericOrderPage.waitOrderLoaded();

        const genericOrderPageSegment =
            await genericOrderPage.segmentsInfo.segments.at(0);

        assert.equal(
            extractNumber(await genericOrderPageSegment.car.getText()),
            extractNumber(coachNumber),
            'Номер вагона совпадает со страницей подтверждения',
        );

        assert.equal(
            extractNumber(await genericOrderPageSegment.places.getText()),
            extractNumber(places),
            'Номер места совпадает со страницей подтверждения',
        );
    });
});
