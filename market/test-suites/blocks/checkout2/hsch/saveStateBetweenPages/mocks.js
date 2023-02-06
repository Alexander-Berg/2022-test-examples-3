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

export const moscowAddress = {
    id: 'moscow',
    addressId: 'moscow',
    address: 'Москва, Усачева, д. 52',
    addressSuggest: 'Усачёва улица, 52',
    city: 'Москва',
    locality: 'Москва',
    street: 'Усачева',
    house: '52',
    building: '52',
    apartment: '12',
    room: '12',
    floor: '15',
    entrance: '1',
    intercom: '12test',
    entryphone: '12test',
    comment: 'Тестирование',
    regionId: region['Москва'],
};
