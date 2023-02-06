import {TLD} from 'constants/tld';

import {getProjectUrl} from 'utilities/url';

describe('getProjectUrl', () => {
    test('Должен вернуть production url c tld=ru сервиса Путешествий', () => {
        expect(getProjectUrl(TLD.RU)).toBe('https://travel.yandex.ru');
    });

    test('Должен вернуть production url c tld=kz сервиса Путешествий', () => {
        expect(getProjectUrl(TLD.KZ)).toBe('https://travel.yandex.kz');
    });
});
