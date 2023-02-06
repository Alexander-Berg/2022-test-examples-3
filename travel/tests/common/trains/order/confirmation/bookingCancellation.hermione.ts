import {assert} from 'chai';
import {order} from 'suites/trains';
import {sum, ceil} from 'lodash';

import {ETestFieldName} from 'components/TestBookingPassengerForm/types';

import {TestTrainsApp} from 'helpers/project/trains/app/TestTrainsApp';
import {PASSENGER} from 'helpers/project/trains/data/passengers';
import {CONTACTS} from 'helpers/project/trains/data/contacts';

describe(order.steps.confirmation, () => {
    it('Общий вид страницы подтверждения. Отмена брони.', async function () {
        const app = new TestTrainsApp(this.browser);
        const {orderPlacesStepPage} = app;

        await orderPlacesStepPage.browseToPageWithoutTransfer();
        await orderPlacesStepPage.waitTrainDetailsLoaded();

        await app.setTestContext();

        const placesStepSegment =
            await app.orderPlacesStepPage.orderSegments.getSegment();
        const placesSegmentInfo = await placesStepSegment.getInfo();

        await app.orderPlacesStepPage.selectPassengers({adults: 1});

        const {coachNumber} =
            await app.orderPlacesStepPage.selectAnyPlacesInPlatzkarte();

        await app.orderPlacesStepPage.goNextStep();

        await app.orderPassengersStepPage.fillPassengers([PASSENGER]);
        await app.orderPassengersStepPage.fillContacts(CONTACTS);

        await app.orderPassengersStepPage.layout.goNextStep();

        await app.orderConfirmationStepPage.waitOrderLoaded();

        assert.isTrue(
            await app.orderConfirmationStepPage.isVisible(),
            'Открывается страница Подтверждения',
        );
        assert.equal(
            await app.orderConfirmationStepPage.timer.getMinutes(),
            29,
            'Отображается таймер бронирования (29 минут) на оплату заказа',
        );
        assert.isTrue(
            await app.orderConfirmationStepPage.cancelButton.isVisible(),
            'Есть Отменить заказ (если передумали)',
        );

        const confirmationStepSegment =
            await app.orderPlacesStepPage.orderSegments.getSegment();
        const confirmationSegmentInfo = await confirmationStepSegment.getInfo();

        /**
         * Секция Проверьте поезд и места
         */

        assert.equal(
            confirmationSegmentInfo.departureDate,
            placesSegmentInfo.departureDate,
            'Дата отправления соответствует выбранному поезду',
        );
        assert.equal(
            confirmationSegmentInfo.departureTime,
            placesSegmentInfo.departureTime,
            'Время отправления соответствует выбранному поезду',
        );
        assert.equal(
            confirmationSegmentInfo.departureStation,
            placesSegmentInfo.departureStation,
            'Станция отправления соответствует выбранному поезду',
        );

        assert.equal(
            confirmationSegmentInfo.arrivalDate,
            placesSegmentInfo.arrivalDate,
            'Дата прибытия соответствует выбранному поезду',
        );
        assert.equal(
            confirmationSegmentInfo.arrivalTime,
            placesSegmentInfo.arrivalTime,
            'Время прибытия соответствует выбранному поезду',
        );
        assert.equal(
            confirmationSegmentInfo.arrivalStation,
            placesSegmentInfo.arrivalStation,
            'Станция прибытия соответствует выбранному поезду',
        );

        assert.equal(
            confirmationSegmentInfo.number,
            placesSegmentInfo.number,
            'Номер поезда соответствует выбранному поезду',
        );
        assert.equal(
            confirmationSegmentInfo.direction,
            placesSegmentInfo.direction,
            'Маршрут поезда соответствует выбранному поезду',
        );
        assert.equal(
            confirmationSegmentInfo.firm,
            placesSegmentInfo.firm,
            'Название поезда соответствует выбранному поезду',
        );

        assert.isNotEmpty(
            confirmationSegmentInfo.timeMessage,
            'Отображается информация, что время местное',
        );

        assert.isTrue(
            await confirmationStepSegment.hasElectronicRegistration(),
            'Отображается бейдж электронной регистрации',
        );

        const trainPlace = await app.orderConfirmationStepPage.places.first();

        assert.equal(
            await trainPlace.getCoachNumber(),
            coachNumber,
            'Отображается номер вагона соответствует выбранному',
        );

        assert.isTrue(
            await trainPlace.transportSchema.isVisible(),
            'Отображается схема вагона',
        );

        const firstPassenger =
            await app.orderConfirmationStepPage.passengers.passengers.first();

        assert.equal(
            await firstPassenger.lastName.value.getText(),
            PASSENGER[ETestFieldName.lastName],
            'Отображается фамилия пассажира соответствует заполненному',
        );
        assert.equal(
            await firstPassenger.firstName.value.getText(),
            PASSENGER[ETestFieldName.firstName],
            'Отображается имя пассажира соответствует заполненному',
        );
        assert.equal(
            await firstPassenger.patronymic.value.getText(),
            PASSENGER[ETestFieldName.patronymicName],
            'Отображается отчество пассажира соответствует заполненному',
        );
        assert.equal(
            await firstPassenger.birthDate.value.getText(),
            PASSENGER[ETestFieldName.birthdate],
            'Отображается дата рождения пассажира соответствует заполненному',
        );
        assert.equal(
            await firstPassenger.getDocumentNumber(),
            PASSENGER[ETestFieldName.documentNumber],
            'Отображается номер паспорта пассажира соответствует заполненному',
        );

        const ticket = await firstPassenger.tickets.first();

        assert.isTrue(
            await ticket.tariff.isVisible(),
            'Отображается вид тарифа билета (например, полный)',
        );

        assert.isTrue(
            await ticket.priceExplanation.questionIcon.isVisible(),
            'Отображается кнопка i информационная',
        );

        assert.equal(
            await app.orderConfirmationStepPage.contacts.getPhoneNumber(),
            CONTACTS.phone,
            'Номер мобильного телефона соответствует заполненному',
        );
        assert.equal(
            await app.orderConfirmationStepPage.contacts.email.value.getText(),
            CONTACTS.email,
            'Электронная почта соответствует заполненному',
        );

        await app.orderConfirmationStepPage.layout.orderSummary.goToConfirmStep?.clickButtonAndAwaitAnimation();

        assert.isFalse(
            await app.orderConfirmationStepPage.layout.orderSummary.insurance.checkbox.isChecked(),
            'Чекбокс добавить страховку выключен',
        );

        assert.isTrue(
            await app.orderConfirmationStepPage.layout.orderSummary.insurance.logo.isVisible(),
            'Отображается название страховой компании',
        );

        assert.isTrue(
            await app.orderConfirmationStepPage.layout.orderSummary.totalPrice.isVisible(),
            'Отображается секция итоговой суммы',
        );

        assert.equal(
            await app.orderConfirmationStepPage.layout.orderSummary.orderButton.getText(),
            'Ввести карту и оплатить',
            'Отображается кнопка "Ввести карту и оплатить"',
        );

        const totalPriceWithoutInsurance =
            await app.orderConfirmationStepPage.layout.orderSummary.totalPrice.price.getPriceValue();
        const insurancePrice =
            await app.orderConfirmationStepPage.layout.orderSummary.insurance.price.getPriceValue();

        await app.orderConfirmationStepPage.layout.orderSummary.insurance.checkbox.click();

        const totalPriceWithInsurance =
            await app.orderConfirmationStepPage.layout.orderSummary.totalPrice.price.getPriceValue();
        const expectedTotalPriceWithInsurance = ceil(
            totalPriceWithoutInsurance + insurancePrice,
            2,
        );

        assert.equal(
            totalPriceWithInsurance,
            expectedTotalPriceWithInsurance,
            'Итоговая сумма увеличилась ровно на указанную стоимость страховки',
        );

        await ticket.priceExplanation.open();

        assert.isTrue(
            (await ticket.priceExplanation.priceItems.count()) > 0,
            'Появляется информационное сообщение о том, из чего складывается стоимость билета',
        );

        assert.isTrue(
            await ticket.priceExplanation.priceItems.some(async priceItem => {
                const text = await priceItem.getText();

                return text.includes('Цена билета');
            }),
            'Отображается цена билета',
        );

        assert.isTrue(
            await ticket.priceExplanation.priceItems.some(async priceItem => {
                const text = await priceItem.getText();

                return text.includes('Сервисный сбор');
            }),
            'Отображается сервисный сбор',
        );

        assert.equal(
            await ticket.price.getPriceValue(),
            sum(
                await ticket.priceExplanation.priceItems.map(item =>
                    item.price.getPriceValue(),
                ),
            ),
            'Общая сумма билета посчитана верно (цена билета + сервисный сбор)',
        );

        await ticket.priceExplanation.close();

        await app.orderConfirmationStepPage.cancelButton.click();

        await app.orderPlacesStepPage.waitTrainDetailsLoaded();

        assert.isTrue(
            await app.orderPlacesStepPage.isVisible(),
            'Перешли к странице выбора мест',
        );

        assert.isTrue(
            !(await app.orderPlacesStepPage.orderError.isVisible()),
            'На странице не появляется ошибка',
        );
    });
});
