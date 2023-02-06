import {order} from 'suites/trains';
import {assert} from 'chai';

import {MINUTE} from 'helpers/constants/dates';

import {ETestFieldName} from 'components/TestBookingPassengerForm/types';

import {TestTrainsApp} from 'helpers/project/trains/app/TestTrainsApp';
import {
    PASSENGER,
    PASSENGER_BABY,
    PASSENGER_CHILD,
} from 'helpers/project/trains/data/passengers';
import dateFormats from 'helpers/utilities/date/formats';
import {CONTACTS} from 'helpers/project/trains/data/contacts';

describe(order.steps.places, () => {
    hermione.config.testTimeout(4 * MINUTE);
    it('ЖД+ЖД сложный заказ в ЛК, подробности заказа', async function () {
        const app = new TestTrainsApp(this.browser);

        const {
            orderPlacesStepPage,
            orderPassengersStepPage,
            orderConfirmationStepPage,
            paymentPage,
            genericHappyPage,
            genericOrderPage,
        } = app;

        const routeInfo = await orderPlacesStepPage.browseToPageWithTransfer();

        await orderPlacesStepPage.waitTrainDetailsLoaded();

        await app.setTestContext();
        await app.paymentTestContextHelper.setPaymentTestContext();

        await orderPlacesStepPage.selectPassengers({
            adults: 1,
            children: 1,
            babies: 1,
        });
        await orderPlacesStepPage.selectAnyPlacesInPlatzkarte(2);
        await orderPlacesStepPage.goNextStep();

        await orderPlacesStepPage.selectAnyPlacesInPlatzkarte(2);
        await orderPlacesStepPage.goNextStep();

        const specifiedPassengers = [
            PASSENGER,
            PASSENGER_CHILD,
            PASSENGER_BABY,
        ];

        await orderPassengersStepPage.fillPassengers(specifiedPassengers);

        await orderPassengersStepPage.fillContacts(CONTACTS);
        await orderPassengersStepPage.layout.goNextStep();

        await orderConfirmationStepPage.waitOrderLoaded();
        await orderConfirmationStepPage.addInsurance();
        // Castyl: Чтобы кнопка Оплатить влезла в экран
        await orderConfirmationStepPage.layout.footer.scrollIntoView();
        await orderConfirmationStepPage.goNextStep();

        await paymentPage.waitUntilLoaded();

        await genericHappyPage.waitUntilLoaded();
        await genericHappyPage.orderActions.detailsLink.click();

        await genericOrderPage.waitOrderLoaded();

        /**
         * Шапка заказа
         */

        assert.isTrue(
            await genericOrderPage.header.numberBlock.isVisible(),
            'Должен отображается номер заказа',
        );
        assert.isTrue(
            await genericOrderPage.header.status.isVisible(),
            'Должен отображается статус заказа',
        );

        if (!genericOrderPage.isTouch) {
            assert.isTrue(
                await genericOrderPage.orderActions.printButton.isVisible(),
                'Должна отображаться кнопка "Распечатать"',
            );
        }

        assert.isTrue(
            await genericOrderPage.orderActions.downloadButton.isVisible(),
            'Должна отображаться кнопка "Скачать билеты"',
        );

        assert.isTrue(
            await genericOrderPage.orderActions.cancelButton.isVisible(),
            'Должна отображаться кнопка "Сдать все билеты"',
        );

        /**
         * Блок с данными о поездах
         */

        assert.isTrue(
            await genericOrderPage.segmentsInfo.title.isVisible(),
            'Должен отображаться блок с данными о маршруте и поездах',
        );
        assert.isTrue(
            await genericOrderPage.passengers.isVisible(),
            'Должен отображаться блок с данными о пассажирах',
        );
        assert.isTrue(
            await genericOrderPage.contacts.isVisible(),
            'Должен отображаться блок с контактной информацией',
        );

        const [segmentTitleFrom, segmentTitleTo] =
            await genericOrderPage.segmentsInfo.title.getDepartureAndArrival();

        assert.equal(
            segmentTitleFrom,
            routeInfo.fromName,
            'В заголовке данных о поездке должен быть указан верный пункт отправления',
        );
        assert.equal(
            segmentTitleTo,
            routeInfo.toName,
            'В заголовке данных о поездке должен быть указан верный пункт прибытия',
        );

        const segmentTitleDate =
            await genericOrderPage.segmentsInfo.title.getDepartureDate();

        assert.equal(
            segmentTitleDate,
            routeInfo.firstSegmentDepartureMoment.format(
                dateFormats.HUMAN_DATE_WITH_FULL_WEEKDAY,
            ),
            'В заголовке данных о поездке должен быть указана верная дата отбытия первого поезда',
        );

        const segments = await genericOrderPage.segmentsInfo.segments.items;

        assert.equal(
            segments.length,
            2,
            'Должны отображаться два блока с информацией о двух поездах',
        );

        for (const segment of segments) {
            assert.isNotEmpty(
                await segment.getCompany(),
                'Для каждого сегмента отображается название перевозчика',
            );
            assert.isNotEmpty(
                await segment.numberAndDirection.getText(),
                'Для каждого сегмента отображается номер поезда и его маршрут',
            );
            assert.isNotEmpty(
                await segment.car.getText(),
                'Для каждого сегмента отображается номер вагона',
            );
            assert.isNotEmpty(
                await segment.carType.getText(),
                'Для каждого сегмента отображается тип вагона',
            );
            assert.equal(
                (await segment.getPlaces()).length,
                2,
                'Для каждого сегмента отображается два места',
            );
            assert.isNotEmpty(
                await segment.timeAndDuration.departure.date.getText(),
                'Для каждого сегмента отображается дата отправления',
            );
            assert.isNotEmpty(
                await segment.timeAndDuration.departure.time.getText(),
                'Для каждого сегмента отображается время отправления',
            );
            assert.isNotEmpty(
                await segment.timeAndDuration.arrival.date.getText(),
                'Для каждого сегмента отображается дата прибытия',
            );
            assert.isNotEmpty(
                await segment.timeAndDuration.arrival.time.getText(),
                'Для каждого сегмента отображается время прибытия',
            );
            assert.isNotEmpty(
                await segment.timeAndDuration.duration.getText(),
                'Для каждого сегмента отображается время в пути',
            );
            assert.isNotEmpty(
                await segment.stations.departure.city.getText(),
                'Для каждого сегмента отображается город отправления',
            );
            assert.isNotEmpty(
                await segment.stations.departure.station.getText(),
                'Для каждого сегмента отображается станция отправления',
            );
            assert.isNotEmpty(
                await segment.stations.arrival.city.getText(),
                'Для каждого сегмента отображается город прибытия',
            );
            assert.isNotEmpty(
                await segment.stations.arrival.station.getText(),
                'Для каждого сегмента отображается: станция прибытия',
            );
        }

        assert.isTrue(
            await genericOrderPage.segmentsInfo.transfer.description.isVisible(),
            'Должна отображаться информация о пересадке',
        );
        assert.isTrue(
            await genericOrderPage.segmentsInfo.transfer.duration.isVisible(),
            'Должна отображаться продолжительность пересадки',
        );

        assert.equal(
            (await genericOrderPage.segmentsInfo.getPartnerReservationNumbers())
                .length,
            2,
            'Должны отображаться два номера заказа РЖД',
        );

        /**
         * Информация о пассажирах и их билетах
         */

        const passengers = await genericOrderPage.passengers.passengers.items;

        assert.equal(
            passengers.length,
            specifiedPassengers.length - 1,
            'Должны отображаться все пассажиры с местами',
        );

        for (let i = 0; i < passengers.length; i++) {
            /**
             * Порядок пассажиров в ЛК должен быть такой же как и при заполнении
             */
            const passenger = passengers[i];
            const specifiedPassenger = specifiedPassengers[i];

            assert.equal(
                await passenger.info.name.getText(),
                `${specifiedPassenger[ETestFieldName.lastName]} ${
                    specifiedPassenger[ETestFieldName.firstName]
                } ${specifiedPassenger[ETestFieldName.patronymicName]}`,
                'Для каждого пассажира должно отображаться правильное фио',
            );
            assert.equal(
                await passenger.info.getDocumentNumber(),
                specifiedPassenger[ETestFieldName.documentNumber],
                'Для каждого пассажира должен отображаться правильный номер документа',
            );
            assert.equal(
                await passenger.info.getBirthDate(),
                specifiedPassenger[ETestFieldName.birthdate],
                'Для каждого пассажира должна отображаться правильная дата рождения',
            );
            assert.equal(
                await passenger.info.getGender(),
                specifiedPassenger[ETestFieldName.sex],
                'Для каждого пассажира должен отображаться правильный пол',
            );

            if (specifiedPassenger === PASSENGER) {
                const baby = passenger.babyInfo;

                assert.isTrue(
                    await baby.isVisible(),
                    'Должен отображаться блок с ребенком для взрослого пассажира',
                );

                assert.equal(
                    await baby.name.getText(),
                    `${PASSENGER_BABY[ETestFieldName.lastName]} ${
                        PASSENGER_BABY[ETestFieldName.firstName]
                    } ${PASSENGER_BABY[ETestFieldName.patronymicName]}`,
                    'У ребенка должно отображаться правильное фио',
                );
                assert.equal(
                    await baby.getDocumentNumber(),
                    PASSENGER_BABY[ETestFieldName.documentNumber],
                    'У ребенка должен отображаться правильный номер документа',
                );
                assert.equal(
                    await baby.getBirthDate(),
                    PASSENGER_BABY[ETestFieldName.birthdate],
                    'У ребенка должна отображаться правильная дата рождения',
                );
                assert.equal(
                    await baby.getGender(),
                    PASSENGER_BABY[ETestFieldName.sex],
                    'У ребенка должен отображаться правильный пол',
                );
            }

            const tickets = await passenger.tickets.tickets.items;

            assert.equal(
                tickets.length,
                2,
                'Для каждого пассажира должны отображаться два билета',
            );

            for (const ticket of tickets) {
                assert.isTrue(
                    await ticket.routeTitle.isVisible(),
                    'Для каждого билета должны отображаться пункты отправления и прибытия',
                );

                assert.isTrue(
                    await ticket.insurance.price.isVisible(),
                    'Для каждого билета должна отображаться цена страховки',
                );

                assert.isTrue(
                    await ticket.insurance.questionButton.isVisible(),
                    'Для каждого билета должна отображаться иконка вопроса у страховки',
                );

                const isBaby = await ticket.tariff.tariffBaby.isVisible();

                if (isBaby) {
                    /**
                     * Это билет детский без места, больше информации в нем нет
                     */
                    continue;
                }

                assert.isNotEmpty(
                    await ticket.tariff.tariffName.getText(),
                    'Для каждого билета должно отображаться название тарифа',
                );

                assert.isNotEmpty(
                    await ticket.tariff.price.getText(),
                    'Для каждого билета должно отображаться цена тарифа',
                );

                assert.isTrue(
                    await ticket.tariff.priceExplanation.questionIcon.isVisible(),
                    'Для каждого билета должен отображаться иконка вопроса рядом с тарифом с детализацией тарифа',
                );

                await ticket.tariff.priceExplanation.open();

                assert.isTrue(
                    (await ticket.tariff.priceExplanation.priceItems.items)
                        .length > 0,
                    'Для каждого билета клик по иконке вопроса рядом с тарифом показывает детализацию цены',
                );

                await ticket.tariff.priceExplanation.close();

                assert.isTrue(
                    await ticket.actions.refundTicketButton.isVisible(),
                    'Для каждого билета, кроме детских без места должна отображаться кнопка возврата',
                );
            }
        }

        /**
         * Информация о сумме за все билеты и страховки
         */

        assert.isTrue(
            await genericOrderPage.passengers.totalPrice.total.isVisible(),
            'Должна отображаться итоговая цена за все билеты и страховки',
        );
        assert.isTrue(
            await genericOrderPage.passengers.totalPrice.receipt.isVisible(),
            'Должна отображаться ссылка на скачивание чека',
        );
        assert.isTrue(
            await genericOrderPage.passengers.totalPrice.insuranceDetails.isVisible(),
            'Должна отображаться ссылка на полные условия страховки',
        );

        /**
         * Блок с контактной информацией
         */

        assert.isTrue(
            await genericOrderPage.contacts.info.isVisible(),
            'В блоке контактной информации должен отображаться текст о том, что билет отправлен на почту',
        );
        assert.equal(
            await genericOrderPage.contacts.email.getText(),
            CONTACTS.email,
            'В блоке контактной информации должен отображаться правильный email',
        );
        assert.equal(
            await genericOrderPage.contacts.phone.getText(),
            CONTACTS.phone,
            'В блоке контактной информации должен отображаться правильный телефон',
        );
    });
});
