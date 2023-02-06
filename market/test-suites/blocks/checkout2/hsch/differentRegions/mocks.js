import {keyBy} from 'ambar';

import * as kettle from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';
import {region} from '@self/root/src/spec/hermione/configs/geo';
import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';

export const carts = [
    buildCheckouterBucket({
        items: [{
            skuMock: kettle.skuMock,
            offerMock: kettle.offerMock,
            count: 1,
        }],
    }),
];

export const samaraAddress = {
    id: 'samara',
    addressId: 'samara',
    address: 'Самара, Ленинская улица, д. 147',
    addressSuggest: 'Ленинская улица, 147',
    city: 'Самара',
    locality: 'Самара',
    street: 'Ленинская улица',
    house: '147',
    building: '147',
    apartment: '12',
    room: '12',
    floor: '15',
    entrance: '1',
    intercom: '12test',
    entryphone: '12test',
    comment: 'Тестирование',
    regionId: region['Самара'],
};

export const moscowAddress = {
    id: 'moscow',
    addressId: 'moscow',
    address: 'Москва, Усачёва улица, д. 52',
    addressSuggest: 'Усачёва улица, 52',
    city: 'Москва',
    locality: 'Москва',
    street: 'Усачёва',
    house: '52',
    building: '52',
    regionId: region['Москва'],
};

export const orders = [
    {
        id: 124,
        delivery: {
            type: 'DELIVERY',
            regionId: region['Самара'],
            buyerAddress: samaraAddress,
        },
    },
];

export const contact1 = {
    id: '123',
    contactId: '123',
    recipient: 'тест тестович',
    email: 'test@yandex.ru',
    phoneNum: '89096667888',
};

export const addressState = keyBy(address => address.id, [
    moscowAddress,
    samaraAddress,
]);

export const contactState = keyBy(contact => contact.id, [
    contact1,
]);
