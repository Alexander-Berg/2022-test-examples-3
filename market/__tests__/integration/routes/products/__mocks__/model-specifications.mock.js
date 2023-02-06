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
        id: '1572131633816/c74ccf938e8e68d594e5926bd8950500',
        time: '2019-10-27T02:13:53.835+03:00',
        marketUrl: 'https://market.yandex.ru?pp=1002',
    },
    status: 'OK',
    link:
        'https://market.yandex.ru/product--yandex-stantsiia-umnaia-kolonka-dlia-umnogo-doma/1971204201/spec?hid=15553892&pp=1002',
    specification: [
        {
            name: 'Звук',
            features: [
                {
                    name: 'Диапазон воспроизводимых частот',
                    value: '50 Гц — 20 кГц',
                },
                {
                    name: 'Поддержка потоковых аудиосервисов',
                    value: 'есть',
                },
                {
                    name: 'Суммарная мощность',
                    value: '50 Вт',
                },
                {
                    name: 'НЧ-динамики',
                    value: '1 шт., 30 Вт, 85 мм',
                },
                {
                    name: 'ВЧ-динамики',
                    value: '2 шт., 10 Вт, 20 мм',
                },
                {
                    name: 'Пассивные излучатели',
                    value: '2 шт., 95 мм',
                },
                {
                    name: 'Отношение сигнал/шум',
                    value: '96 дБ',
                },
            ],
        },
        {
            name: 'Видео',
            features: [
                {
                    name: 'Поддержка потоковых видеосервисов',
                    value: 'есть',
                },
                {
                    name: 'Разрешение',
                    value: '1080p',
                },
            ],
        },
        {
            name: 'Программное обеспечение',
            features: [
                {
                    name: 'Встроенный голосовой помощник',
                    value: 'Алиса',
                },
            ],
        },
        {
            name: 'Подключение',
            features: [
                {
                    name: 'Выход HDMI',
                    value: 'HDMI 1.4',
                },
                {
                    name: 'Wi-Fi',
                    value: 'IEEE 802.11 b/g/n/ac, 2.4 ГГц / 5 ГГц',
                },
                {
                    name: 'Bluetooth',
                    value: 'Bluetooth 4.1/BLE',
                },
            ],
        },
        {
            name: 'Питание',
            features: [
                {
                    name: 'Адаптер',
                    value: '220-240 В переменного тока, 50 Гц',
                },
                {
                    name: 'Напряжение питания',
                    value: '20 В постоянного тока',
                },
            ],
        },
        {
            name: 'Дополнительно',
            features: [
                {
                    name: 'Количество микрофонов',
                    value: '7',
                },
                {
                    name: 'Дизайн',
                    value: 'аудиоткань, верх из алюминия',
                },
                {
                    name: 'Размеры (ШxВxГ)',
                    value: '141x231x141 мм',
                },
                {
                    name: 'Вес',
                    value: '2.9 кг',
                },
                {
                    name: 'Экосистема',
                    value: 'Умный дом Яндекса',
                },
                {
                    name: 'Работает в системе "умный дом"',
                    value: 'есть',
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
