import {getProjectUrl} from 'utilities/url';

describe('getProjectUrl', () => {
    test('Должен вернуть production url c tld=ru сервиса Путешествий', () => {
        expect(getProjectUrl('ru')).toBe('https://travel.yandex.ru');
    });

    test('Должен вернуть production url c tld=kz сервиса Путешествий', () => {
        expect(getProjectUrl('kz')).toBe('https://travel.yandex.kz');
    });
});
