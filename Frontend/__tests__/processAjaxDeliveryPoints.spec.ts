import { ECurrencyAvailable } from '~/types';
import { defineSelectedDeliveryOption } from '../processAjaxDeliveryPoints';

const pickupWithOnePoint = {
    minPrice: 200,
    maxPrice: 200,
    price: 0,
    label: 'Самовывоз',
    value: 'market-pickup',
    currencyId: ECurrencyAvailable.RUB,
    meta: '1 день',
    freeFrom: 0,
    type: 'PICKUP',
    minDurationInDays: 1,
    maxDurationInDays: 1,
    points: {
        pickup_point_1_option_1: {
            pointId: 'option_1',
            optionId: 'pickup_point_1',
            price: 200,
            currencyId: ECurrencyAvailable.RUB,
            serviceName: 'PickPoint',
            minDurationInDays: 1,
            maxDurationInDays: 1,
            address: 'Москва, Малая Бронная, д. 5',
            coordX: '37.59683',
            coordY: '55.759106',
            workTime: [['Будни', '08:30 — 17:30']],
        },
    },
};

const pickupWithTwoPoints = {
    minPrice: 200,
    maxPrice: 200,
    price: 0,
    label: 'Самовывоз',
    value: 'market-pickup',
    currencyId: ECurrencyAvailable.RUB,
    meta: '1 день',
    freeFrom: 0,
    type: 'PICKUP',
    minDurationInDays: 1,
    maxDurationInDays: 1,
    points: {
        pickup_point_1_option_1: {
            pointId: 'option_1',
            optionId: 'pickup_point_1',
            price: 200,
            currencyId: ECurrencyAvailable.RUB,
            serviceName: 'PickPoint',
            minDurationInDays: 1,
            maxDurationInDays: 1,
            address: 'Москва, Малая Бронная, д. 5',
            coordX: '37.59683',
            coordY: '55.759106',
            workTime: [['Будни', '08:30 — 17:30']],
        },
        pickup_point_2_option_2: {
            pointId: 'option_2',
            optionId: 'pickup_point_2',
            price: 200,
            currencyId: ECurrencyAvailable.RUB,
            serviceName: 'PickPoint',
            minDurationInDays: 1,
            maxDurationInDays: 1,
            address: 'Москва, Малая Бронная, д. 5',
            coordX: '37.59683',
            coordY: '55.759106',
            workTime: [['Будни', '08:30 — 17:30']],
        },
    },
};

const courierDelivery = {
    price: 299,
    label: 'Курьер',
    value: 'courier_delivery_id',
    currencyId: ECurrencyAvailable.RUB,
    meta: '5 дней',
    freeFrom: 0,
    type: 'DELIVERY',
};

describe('processAjaxDeliveryPoints', () => {
    describe('defineSelectedDeliveryOption', () => {
        it('Возвращает первый не pickup вариант', () => {
            expect(
                defineSelectedDeliveryOption([pickupWithOnePoint, courierDelivery, pickupWithTwoPoints]))
                .toEqual('courier_delivery_id');
        });

        it('Если из вариантов только самовывоз с одним пунктом - предвыбирает его', () => {
            expect(
                defineSelectedDeliveryOption([pickupWithOnePoint]))
                .toEqual({
                    type: 'market-pickup',
                    selectedOptionId: 'pickup_point_1',
                    selectedPointId: 'option_1',
                });
        });

        it('Не может предвыбрать вариант, если есть только самовывоз с несколькими пунктами', () => {
            expect(
                defineSelectedDeliveryOption([pickupWithTwoPoints]))
                .toEqual(undefined);
        });
    });
});
