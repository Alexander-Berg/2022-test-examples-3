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
    comment: 'Товар DSBS',
    fullDeliveryInfo: 'Москва, улица Льва Толстого, д. 16, 32\n3 подъезд, 64 этаж, домофон 76543, "Товар DSBS"',
};

const ADDRESSES = {
    MOSCOW_HSCH_ADDRESS,
    HSCH_ADDRESS_FOR_DSBS,
};

export default ADDRESSES;
