import {assert} from 'chai';

import {
    IDataConfirmationStep,
    TestTrainsApp,
} from 'helpers/project/trains/app/TestTrainsApp';
import {ITrainsTestFormDocument} from 'helpers/project/trains/components/TestTrainsBookingPassengerForm';
import extractNumber from 'helpers/utilities/extractNumber';

export async function checkTrainInfo(
    app: TestTrainsApp,
    dataFromConfirmation: IDataConfirmationStep,
): Promise<void> {
    const {genericOrderPage} = app;
    const {
        price,
        departureDate,
        departureTime,
        //arrivalDate,
        arrivalTime,
        trainNumber,
        coachNumber,
        places,
    } = dataFromConfirmation;

    assert.equal(
        await genericOrderPage.passengers.totalPrice.total.getPriceValue(),
        price,
        'Общая стоимость совпадает со страницей подтверждения',
    );

    const segment = await genericOrderPage.segmentsInfo.segments.at(0);

    if (departureDate) {
        assert.match(
            await segment.timeAndDuration.departure.date.getText(),
            new RegExp(departureDate),
            'Дата отправления совпадает со страницей подтверждения',
        );
    }

    assert.match(
        await segment.timeAndDuration.departure.time.getText(),
        new RegExp(departureTime),
        'Время отправления совпадает со страницей подтверждения',
    );
    // https://st.yandex-team.ru/TRAVELFRONT-3519
    // assert.match(
    //     await orderPage.orderTrainsInfo.arrivalDate.getTextQA(),
    //     new RegExp(arrivalDate),
    //     'Дата прибытия совпадает со страницей подтверждения',
    // );
    assert.match(
        await segment.timeAndDuration.arrival.time.getText(),
        new RegExp(arrivalTime),
        'Время прибытия совпадает со страницей подтверждения',
    );
    assert.match(
        await segment.numberAndDirection.getText(),
        new RegExp(trainNumber),
        'Номер поезда совпадает со страницей подтверждения',
    );
    assert.equal(
        extractNumber(await segment.car.getText()),
        extractNumber(coachNumber),
        'Номер вагона совпадает со страницей подтверждения',
    );

    assert.equal(
        extractNumber(await segment.places.getText()),
        extractNumber(places),
        'Номер места совпадает со страницей подтверждения',
    );
}

export async function checkPassengerInfo(
    app: TestTrainsApp,
    passengerExcepted: ITrainsTestFormDocument,
): Promise<void> {
    const {genericOrderPage} = app;
    const passenger = await genericOrderPage.passengers.passengers.at(0);

    assert.equal(
        await passenger.info.getFirstName(),
        passengerExcepted.firstName,
        'Совпадают данные пассажира - имя',
    );
    assert.equal(
        await passenger.info.getLastName(),
        passengerExcepted.lastName,
        'Совпадают данные пассажира - фамилия',
    );
    assert.equal(
        await passenger.info.getPatronymic(),
        passengerExcepted.patronymicName,
        'Совпадают данные пассажира - отчество',
    );
    assert.equal(
        await passenger.info.getBirthDate(),
        passengerExcepted.birthdate,
        'Совпадают данные пассажира - дата рождения',
    );
    assert.equal(
        await passenger.info.getDocumentNumber(),
        passengerExcepted.documentNumber,
        'Совпадают данные пассажира - номер документа',
    );
    assert.equal(
        await passenger.info.getGender(),
        passengerExcepted.sex,
        'Совпадают данные пассажира - пол',
    );
}
