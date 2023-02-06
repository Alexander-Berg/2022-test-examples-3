import {region} from '@self/root/src/spec/hermione/configs/geo';

const MOSCOW_HSCH_ADDRESS = {
    suggest: 'Усачёва улица, 62',
    apartament: '12',
    floor: '15',
    entrance: '1',
    intercom: '12test',
    comment: 'Тестирование',
    fullDeliveryInfo: ['Москва, Усачёва улица, д. 62, 12\n'] +
        ['1 подъезд, 15 этаж, домофон 12test, "Тестирование"'],
};

const HSCH_ADDRESS_FOR_DSBS = {
    suggest: 'улица Льва Толстого, 16',
    apartament: '32',
    floor: '64',
    entrance: '3',
    intercom: '76543',
    comment: 'Товар DSBS',
    fullDeliveryInfo: 'Москва, улица Льва Толстого, д. 16, 32\n3 подъезд, 64 этаж, домофон 76543, "Товар DSBS"',
};

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
    lastTouchedTime: (new Date('2021-05-21T12:00:00.223z')).toJSON(),
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

const SPB_ADDRESS = {
    id: 'defaultSpbAddress',
    addressId: 'defaultSpbAddress',
    country: 'Россия',
    address: 'Санкт-Петербург, Невский проспект, д. 20',
    regionId: 2,
    locality: 'Санкт-Петербург',
    city: 'Санкт-Петербург',
    street: 'Невский проспект',
    building: '20',
    house: '20',
    zip: '191186',
    postcode: '191186',
    location: {
        longitude: '59.936309',
        latitude: '30.321222',
    },
    lastTouchedTime: (new Date()).toJSON(),
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
    lastTouchedTime: (new Date()).toJSON(),
};

const ADDRESS_WITH_INVALID_FIELD = {
    id: 'invalidApartament',
    addressId: 'invalidApartament',
    address: 'Москва, Самотёчная улица, д. 5',
    city: 'Москва',
    locality: 'Москва',
    street: 'Самотёчная улица',
    house: '5',
    building: '5',
    apartment: '012345678012345',
    room: '012345678012345',
    regionId: region['Москва'],
};

const MOSCOW_LAST_ADDRESS = {
    id: 'lastMoscowAddress',
    addressId: 'lastMoscowAddress',
    country: 'Россия',
    address: 'Москва, Кропоткинский переулок, д. 19/33',
    locality: 'Москва',
    city: 'Москва',
    regionId: region['Москва'],
    street: 'Кропоткинский переулок',
    building: '19/33',
    house: '19/33',
    zip: '119034',
    postcode: '119034',
    lastTouchedTime: (new Date()).toJSON(),
};

const ADDRESSES = {
    ADDRESS_WITH_INVALID_FIELD,
    MOSCOW_HSCH_ADDRESS,
    HSCH_ADDRESS_FOR_DSBS,
    MOSCOW_ADDRESS,
    MOSCOW_ALTERNATIVE_ADDRESS,
    MOSCOW_LAST_ADDRESS,
    SPB_ADDRESS,
    VOLGOGRAD_ADDRESS,
};

export default ADDRESSES;
