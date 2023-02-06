import isAnotherYandexService from '../isAnotherYandexService';

const anotherYandexServices = [
    'trains.yandex.ru',
    'yandex.ru',
    'https://avia.yandex.ru',
    'http://testing.trains.common.yandex.ru/order/?some_id=2',
    '//tickets-www-prestable.common.yandex.ru/bus/ride/c54',
    'ftp://trains.yandex.ru',
    'raspredelenie.yandex.ru',
    'porasp.yandex.ru',
];

const notAnotherYandexServices = [
    'rasp.yandex.ru',
    'http://rasp.common.yandex.ru',
    'google.com',
    '/suburban',
    'hyyandex.ru',
];

describe('isAnotherYandexService', () => {
    it('Подтвердит что это ссылки на другие сервисы Яндекса', () => {
        anotherYandexServices.forEach(serviceUrl => {
            expect(isAnotherYandexService(serviceUrl)).toBe(true);
        });
    });

    it('Подтвердит что это ссылки на Расписания или левые сервисы', () => {
        notAnotherYandexServices.forEach(serviceUrl => {
            expect(isAnotherYandexService(serviceUrl)).toBe(false);
        });
    });
});
