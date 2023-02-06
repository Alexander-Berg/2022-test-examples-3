module.exports = {
    host: 'https://api.content.market.yandex.ru',
    route: /\/v[0-9.]+\/models\/[0-9]+\/specification/,
    response: {
        context: {
            region: {
                id: 225,
                name: 'Россия',
                type: 'COUNTRY',
                childCount: 10,
                country: { id: 225, name: 'Россия', type: 'COUNTRY', childCount: 10 },
            },
            currency: { id: 'RUR', name: 'руб.' },
            id: '1577179519197/c5bfcc2ff5770128e920fdb86f9a0500',
            time: '2019-12-24T12:25:19.222+03:00',
            marketUrl: 'https://market.yandex.ru?pp=1002',
        },
        status: 'ERROR',
        link: 'https://market.yandex.ru/product--smartfon-apple-iphone-xr-64gb/175941311/spec?hid=91491&pp=1002',
        specification: [
            {
                name: 'Общие характеристики',
                features: [
                    { name: 'Тип', value: 'смартфон' },
                    { name: 'Операционная система', value: 'iOS' },
                    { name: 'Версия ОС на начало продаж', value: 'iOS 12' },
                    { name: 'Тип корпуса', value: 'классический' },
                    { name: 'Материал корпуса', value: 'алюминий и стекло' },
                    { name: 'Конструкция', value: 'водозащита' },
                    { name: 'Количество SIM-карт', value: '2' },
                    { name: 'Тип SIM-карты', value: 'nano SIM+eSIM' },
                    { name: 'Бесконтактная оплата', value: 'есть' },
                    { name: 'Вес', value: '194 г' },
                    { name: 'Размеры (ШxВxТ)', value: '75.7x150.9x8.3 мм' },
                ],
            },
            {
                name: 'Экран',
                features: [
                    { name: 'Тип экрана', value: 'цветной IPS, сенсорный' },
                    { name: 'Тип сенсорного экрана', value: 'мультитач, емкостный' },
                    { name: 'Диагональ', value: '6.1 дюйм.' },
                    { name: 'Размер изображения', value: '1792x828' },
                    { name: 'Число пикселей на дюйм (PPI)', value: '324' },
                    { name: 'Соотношение сторон', value: '19.5:9' },
                    { name: 'Автоматический поворот экрана', value: 'есть' },
                ],
            },
            {
                name: 'Мультимедийные возможности',
                features: [
                    { name: 'Количество основных (тыловых) камер', value: '1' },
                    { name: 'Разрешение основной (тыловой) камеры', value: '12 МП' },
                    { name: 'Диафрагма основной (тыловой) камеры', value: 'F/1.80' },
                    { name: 'Фотовспышка', value: 'тыльная, светодиодная' },
                    {
                        name: 'Функции основной (тыловой) фотокамеры',
                        value: 'автофокус, оптическая стабилизация, режим макросъемки',
                    },
                    { name: 'Запись видеороликов', value: 'есть' },
                    { name: 'Макс. разрешение видео', value: '3840x2160' },
                    { name: 'Макс. частота кадров видео', value: '60 кадров/с' },
                    { name: 'Geo Tagging', value: 'есть' },
                    { name: 'Фронтальная камера', value: 'есть, 7 МП' },
                    { name: 'Аудио', value: 'MP3, AAC, WAV, WMA, стереодинамики' },
                ],
            },
            {
                name: 'Связь',
                features: [
                    { name: 'Стандарт', value: 'GSM 900/1800/1900, 3G, 4G LTE, LTE-A, VoLTE' },
                    {
                        name: 'Поддержка диапазонов LTE',
                        value:
                            'модель А2105: FDD‑LTE (Bands 1, 2, 3, 4, 5, 7, 8, 12, 13, 14, 17, 18, 19, 20, 25, 26, 29, 30, 32, 66, 71) TD‑LTE (Bands 34, 38, 39, 40, 41)',
                    },
                    { name: 'Интерфейсы', value: 'Wi-Fi 802.11ac, Wi-Fi Direct, Bluetooth 5.0, NFC' },
                    { name: 'Спутниковая навигация', value: 'GPS/ГЛОНАСС' },
                    { name: 'Cистема A-GPS', value: 'есть' },
                ],
            },
            {
                name: 'Память и процессор',
                features: [
                    { name: 'Процессор', value: 'Apple A12 Bionic' },
                    { name: 'Объем встроенной памяти', value: '64 Гб' },
                    { name: 'Объем оперативной памяти', value: '3 Гб' },
                ],
            },
            {
                name: 'Питание',
                features: [
                    { name: 'Тип аккумулятора', value: 'Li-Ion' },
                    { name: 'Аккумулятор', value: 'несъемный' },
                    { name: 'Время работы в режиме разговора', value: '25 ч' },
                    { name: 'Время работы в режиме прослушивания музыки', value: '65 ч' },
                    { name: 'Тип разъема для зарядки', value: 'Lightning' },
                    { name: 'Функция беспроводной зарядки', value: 'есть' },
                    { name: 'Функция быстрой зарядки', value: 'есть' },
                ],
            },
            {
                name: 'Другие функции',
                features: [
                    { name: 'Громкая связь (встроенный динамик)', value: 'есть' },
                    { name: 'Управление', value: 'голосовой набор, голосовое управление' },
                    { name: 'Режим полета', value: 'есть' },
                    { name: 'Датчики', value: 'освещенности, приближения, гироскоп, компас, барометр' },
                    { name: 'Фонарик', value: 'есть' },
                ],
            },
            {
                name: 'Дополнительная информация',
                features: [
                    {
                        name: 'Комплектация',
                        value:
                            'iPhone с iOS 12, EarPods с коннектором Lightning, кабель Lightning->USB, зарядное устройство USB',
                    },
                    { name: 'Особенности', value: 'Face ID' },
                    { name: 'Дата анонсирования', value: '2018-09-12' },
                ],
            },
        ],
    },
};
