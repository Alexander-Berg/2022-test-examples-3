const {
    isCustomDomainByHeader,
    isCustomDomainByHost,
    buildCustomDomainUrl,
    getCustomDomainValue,
} = require('../../core/utils/custom-domains');

describe('Утилитарные функции custom domains', () => {
    test('isCustomDomainByHost вовзвращает true для корректных доменов', () => {
        expect(isCustomDomainByHost('m.gazeta.ru')).toBe(true);
        expect(isCustomDomainByHost('www.gazeta.ru')).toBe(true);
        expect(isCustomDomainByHost('gazeta.ru')).toBe(true);
        expect(isCustomDomainByHost('rozhdestvenskiy.ru')).toBe(true);
    });

    test('isCustomDomainByHost возвращает false для доменов не входящих в список', () => {
        expect(isCustomDomainByHost('novayagazeta.ru')).toBe(false);
        expect(isCustomDomainByHost('google.com')).toBe(false);
        expect(isCustomDomainByHost('turbopages.org')).toBe(false);
    });

    test('isCustomDomainByHeader отдает true если проставлен заголовок x-turbo-custom', () => {
        expect(isCustomDomainByHeader({ 'x-turbo-custom': 'yes' })).toBe(true);
        expect(isCustomDomainByHeader({ foo: 'bar' })).toBe(false);
    });

    test('buildCustomDomainUrl возвращает правильный URL', () => {
        expect(
            buildCustomDomainUrl(
                'https://www.gazeta.ru/business/news/2020/01/21/n_13941452.shtml',
                'www.gazeta.ru'
            )
        ).toEqual('https://turbo.gazeta.ru/business/news/2020/01/21/n_13941452.shtml');
        expect(
            buildCustomDomainUrl(
                'https://rozhdestvenskiy.ru/',
                'rozhdestvenskiy.ru'
            )
        ).toEqual('https://turbo.rozhdestvenskiy.ru/');
    });

    test('buildCustomDomainUrl возвращает пустую строку, если не может построить URL', () => {
        expect(
            buildCustomDomainUrl(
                'http://turbopages.org/turbo?text=https%3A%2F%2Flenta.ru%2Fnews%2F2019%2F11%2F21%2Fuvhken%2F',
                'turbopages.org'
            )
        ).toBeFalsy();
    });

    test('getCustomDomainValue возвращает правильный домен', () => {
        expect(getCustomDomainValue('m.gazeta.ru')).toEqual('turbo.gazeta.ru');
        expect(getCustomDomainValue('www.gazeta.ru')).toEqual('turbo.gazeta.ru');
        expect(getCustomDomainValue('www.m.gazeta.ru')).toEqual('turbo.gazeta.ru');
        expect(getCustomDomainValue('rozhdestvenskiy.ru')).toEqual('turbo.rozhdestvenskiy.ru');
    });

    test('getCustomDomainValue возвращает пустую строку, если совпадений не найдено', () => {
        expect(getCustomDomainValue('turbopages.org')).toBeFalsy();
        expect(getCustomDomainValue('crimea.ria.ru')).toBeFalsy();
        expect(getCustomDomainValue('m.crimea.ria.ru')).toBeFalsy();
        expect(getCustomDomainValue('www.crimea.ria.ru')).toBeFalsy();
    });
});
