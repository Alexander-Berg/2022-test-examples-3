import {region} from '@self/root/src/spec/hermione/configs/geo';

const MOSCOW_ADDRESS = {
    id: 'defaultMoscowAddress',
    addressId: 'defaultMoscowAddress',
    country: 'Россия',
    address: 'Москва, 2-я улица Энтузиастов, д. 5',
    locality: 'Москва',
    city: 'Москва',
    regionId: region['Москва'],
    street: '2-я улица Энтузиастов',
    building: '5',
    house: '5',
    zip: '111024',
    postcode: '111024',
    location: {
        longitude: '55.75019',
        latitude: '37.729017',
    },
    lastTouchedTime: (new Date()).toJSON(),
};

const MOSCOW_ALTERNATIVE_ADDRESS = {
    id: 'alternativeMoscowAddress',
    addressId: 'alternativeMoscowAddress',
    country: 'Россия',
    address: 'Москва, 1-я улица Энтузиастов, д. 20',
    locality: 'Москва',
    city: 'Москва',
    regionId: region['Москва'],
    street: '1-я улица Энтузиастов',
    building: '20',
    house: '20',
    zip: '111024',
    postcode: '111024',
    location: {
        longitude: '55.750256',
        latitude: '37.728263',
    },
    lastTouchedTime: (new Date('2021-05-20T12:00:00.223z')).toJSON(),
};


const MOSCOW_HSCH_ADDRESS = {
    city: 'Москва',
    street: 'Усачёва улица',
    house: '62',
    apartment: '12',
    floor: '15',
    entrance: '1',
    intercom: '12test',
    comment: 'Тестирование',
    fullDeliveryInfo: ['Москва, Усачёва улица, д. 62, 12\n'] +
        ['1 подъезд, 15 этаж, домофон 12test, "Тестирование"'],
};

const HSCH_ADDRESS_FOR_DSBS = {
    city: 'Москва',
    street: 'улица Льва Толстого',
    house: '16',
    apartment: '32',
    floor: '64',
    entrance: '3',
    intercom: '76543',
    comment: 'DSBS',
    fullDeliveryInfo: 'Москва, улица Льва Толстого, д. 16, 32\n3 подъезд, 64 этаж, домофон 76543, "DSBS"',
};

const VOLGOGRAD_ADDRESS = {
    id: 'defaultVolgogradAddress',
    addressId: 'defaultVolgogradAddress',
    country: 'Россия',
    address: 'Волгоград, улица Льва Толстого, д. 1',
    locality: 'Волгоград',
    city: 'Волгоград',
    regionId: 3,
    street: 'улица Льва Толстого',
    building: '1',
    house: '1',
    zip: '400119',
    postcode: '400119',
    location: {
        longitude: '48.679443',
        latitude: '44.471898',
    },
    lastTouchedTime: (new Date('2021-05-21T12:00:00.223z')).toJSON(),
};

const MINIMAL_ADDRESS = {
    id: 'minimalMoscowAddress',
    addressId: 'minimalMoscowAddress',
    country: 'Россия',
    regionId: region['Москва'],
    address: 'Москва, Никольская улица, д. 1',
    city: 'Москва',
    locality: 'Москва',
    street: 'Никольская улица',
    house: '1',
    building: '1',
    fullDeliveryInfo: 'Москва, Никольская улица, д. 1',
    lastTouchedTime: (new Date('2021-05-21T12:00:00.223z')).toJSON(),
};

const ADDRESSES = {
    MOSCOW_ADDRESS,
    MOSCOW_ALTERNATIVE_ADDRESS,
    MOSCOW_HSCH_ADDRESS,
    HSCH_ADDRESS_FOR_DSBS,
    VOLGOGRAD_ADDRESS,
    MINIMAL_ADDRESS,
};

export default ADDRESSES;
