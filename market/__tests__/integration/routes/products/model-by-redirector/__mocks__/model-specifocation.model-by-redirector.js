/* eslint-disable max-len */

const HOST = 'https://api.content.market.yandex.ru';

const ROUTE = /\/v[0-9.]+\/models\/[0-9]+\/specification/;

const RESPONSE = {
    context: {
        region: {
            id: 213,
            name: 'Москва',
            type: 'CITY',
            childCount: 14,
            country: {
                id: 225,
                name: 'Россия',
                type: 'COUNTRY',
                childCount: 10,
            },
        },
        currency: {
            id: 'RUR',
            name: 'руб.',
        },
        id: '1571596216752/196e82142e800b1454da3ac25b950500',
        time: '2019-10-20T21:30:16.772+03:00',
        marketUrl: 'https://market.yandex.ru?pp=1002',
    },
    status: 'OK',
    link: 'https://market.yandex.ru/product--zhestkii-disk-seagate-st2000dm008/130084187/spec?pp=1002',
    specification: [
        {
            name: 'Общие характеристики',
            features: [
                {
                    name: 'Тип',
                    value: 'HDD',
                },
                {
                    name: 'Поддержка секторов размером 4 КБ',
                    value: 'есть',
                },
                {
                    name: 'Назначение',
                    value: 'для настольного компьютера',
                },
                {
                    name: 'Форм-фактор',
                    value: '3.5"',
                },
            ],
        },
        {
            name: 'Характеристики накопителя',
            features: [
                {
                    name: 'Объем',
                    value: '2000 ГБ',
                },
                {
                    name: 'Скорость записи/Скорость чтения',
                    value: '220/220 МБ/с',
                },
                {
                    name: 'Объем буферной памяти',
                    value: '256 МБ',
                },
                {
                    name: 'Количество головок',
                    value: '2',
                },
                {
                    name: 'Количество пластин',
                    value: '1',
                },
                {
                    name: 'Скорость вращения',
                    value: '7200 rpm',
                },
            ],
        },
        {
            name: 'Интерфейс',
            features: [
                {
                    name: 'Подключение',
                    value: 'SATA 6Gbit/s',
                },
                {
                    name: 'Макс. скорость интерфейса',
                    value: '600 МБ/с',
                },
                {
                    name: 'Поддержка NCQ',
                    value: 'есть',
                },
            ],
        },
        {
            name: 'Механика/Надежность',
            features: [
                {
                    name: 'Максимальная рабочая температура',
                    value: '60 °C',
                },
            ],
        },
        {
            name: 'Дополнительно',
            features: [
                {
                    name: 'Потребляемая мощность',
                    value: '4.30 Вт',
                },
                {
                    name: 'Размеры (ШхВхД)',
                    value: '101.6x20.2x146.99 мм',
                },
                {
                    name: 'Вес',
                    value: '415 г',
                },
                {
                    name: 'Гарантийный срок',
                    value: '2 г.',
                },
            ],
        },
    ],
};

module.exports = {
    host: HOST,
    route: ROUTE,
    response: RESPONSE,
};
