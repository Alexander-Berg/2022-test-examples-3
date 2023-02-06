import { paramStringToObject, parseUrl } from '../utils';

describe('paramStringToObject', () => {
    test.each`
        search                                   | result
        ${'some=query'}                          | ${{ some: 'query' }}
        ${encodeURIComponent('картошка=вкусна')} | ${{ картошка: 'вкусна' }}
        ${'some=query&someOther=query'}          | ${{ some: 'query', someOther: 'query' }}
        ${'some=query&some=query'}               | ${{ some: 'query' }}
        ${'some=query&some=query2'}              | ${{ some: ['query', 'query2'] }}
    `('для строки $search вернёт результат $result', ({ search, result }) =>
        expect(paramStringToObject(search)).toEqual(result),
    );
});

describe('parseUrl', () => {
    it('должен правильно парсить любые простые кейсы с URL', () => {
        expect(parseUrl('google.ru')).toEqual({
            scheme: undefined,
            domain: 'google.ru',
            origin: 'google.ru',
            port: undefined,
            path: '',
            query: undefined,
            hash: undefined,
            queryParams: {},
        });

        expect(parseUrl('google.ru?some=query')).toEqual({
            scheme: undefined,
            domain: 'google.ru',
            origin: 'google.ru',
            port: undefined,
            path: '',
            query: 'some=query',
            hash: undefined,
            queryParams: { some: 'query' },
        });

        expect(parseUrl('google.ru#some=query')).toEqual({
            scheme: undefined,
            domain: 'google.ru',
            origin: 'google.ru',
            port: undefined,
            path: '',
            query: undefined,
            hash: 'some=query',
            queryParams: {},
        });

        expect(parseUrl('google.ru:40')).toEqual({
            scheme: undefined,
            domain: 'google.ru',
            origin: 'google.ru:40',
            port: '40',
            path: '',
            query: undefined,
            hash: undefined,
            queryParams: {},
        });

        expect(parseUrl('google.ru:23/sdf')).toEqual({
            scheme: undefined,
            domain: 'google.ru',
            origin: 'google.ru:23',
            port: '23',
            path: '/sdf',
            query: undefined,
            hash: undefined,
            queryParams: {},
        });

        expect(parseUrl('http://localhost:3000/')).toEqual({
            scheme: 'http',
            domain: 'localhost',
            origin: 'http://localhost:3000',
            port: '3000',
            path: '',
            query: undefined,
            hash: undefined,
            queryParams: {},
        });

        expect(parseUrl('http://someStrangeHost/and/path')).toEqual({
            scheme: 'http',
            domain: 'someStrangeHost',
            origin: 'http://someStrangeHost',
            port: undefined,
            path: '/and/path',
            query: undefined,
            hash: undefined,
            queryParams: {},
        });

        expect(parseUrl('someStrangeHost:80/and/path')).toEqual({
            scheme: undefined,
            domain: 'someStrangeHost',
            origin: 'someStrangeHost:80',
            port: '80',
            path: '/and/path',
            query: undefined,
            hash: undefined,
            queryParams: {},
        });
    });

    it('должен правильно распознавать уместное использование косой черты в пути', () => {
        expect(parseUrl('http://google.ru')).toEqual({
            scheme: 'http',
            domain: 'google.ru',
            origin: 'http://google.ru',
            port: undefined,
            path: '',
            query: undefined,
            hash: undefined,
            queryParams: {},
        });

        expect(parseUrl('http://google.ru/')).toEqual({
            scheme: 'http',
            domain: 'google.ru',
            origin: 'http://google.ru',
            port: undefined,
            path: '',
            query: undefined,
            hash: undefined,
            queryParams: {},
        });

        expect(parseUrl('http://google.ru/dsad/dasdasd/')).toEqual({
            scheme: 'http',
            domain: 'google.ru',
            origin: 'http://google.ru',
            port: undefined,
            path: '/dsad/dasdasd/',
            query: undefined,
            hash: undefined,
            queryParams: {},
        });

        expect(parseUrl('http://google.ru/dsad/dasdasd/?da=23')).toEqual({
            scheme: 'http',
            domain: 'google.ru',
            origin: 'http://google.ru',
            port: undefined,
            path: '/dsad/dasdasd/',
            query: 'da=23',
            hash: undefined,
            queryParams: { da: '23' },
        });

        expect(parseUrl('http://google.ru/dsad/dasdasd/?da=23')).toEqual({
            scheme: 'http',
            domain: 'google.ru',
            origin: 'http://google.ru',
            port: undefined,
            path: '/dsad/dasdasd/',
            query: 'da=23',
            hash: undefined,
            queryParams: { da: '23' },
        });

        expect(parseUrl('http://google.ru/dsad/dasdasd?da=23')).toEqual({
            scheme: 'http',
            domain: 'google.ru',
            origin: 'http://google.ru',
            port: undefined,
            path: '/dsad/dasdasd',
            query: 'da=23',
            hash: undefined,
            queryParams: { da: '23' },
        });
    });

    it('должен парсить относительные и абсолютные пути без прописанных хостов', () => {
        expect(parseUrl('/dsad/dasdasd')).toEqual({
            scheme: undefined,
            domain: '',
            port: undefined,
            origin: '',
            path: '/dsad/dasdasd',
            query: undefined,
            hash: undefined,
            queryParams: {},
        });

        expect(parseUrl('dsad/dasdasd')).toEqual({
            scheme: undefined,
            domain: '',
            origin: '',
            port: undefined,
            path: 'dsad/dasdasd',
            query: undefined,
            hash: undefined,
            queryParams: {},
        });
    });

    it('должен парсить сложные URL с двоеточиями не только при задании порта', () => {
        expect(
            parseUrl(
                'http://iskra.karty.ru:8080/ords/f?p=10000:3220::TOKEN-1-LNT6YYR:KEY-CBBD796D017FFF7BFAB1F0B598E03A24:',
            ),
        ).toEqual({
            scheme: 'http',
            domain: 'iskra.karty.ru',
            origin: 'http://iskra.karty.ru:8080',
            port: '8080',
            path: '/ords/f',
            query: 'p=10000:3220::TOKEN-1-LNT6YYR:KEY-CBBD796D017FFF7BFAB1F0B598E03A24:',
            hash: undefined,
            queryParams: { p: '10000:3220::TOKEN-1-LNT6YYR:KEY-CBBD796D017FFF7BFAB1F0B598E03A24:' },
        });
    });

    it('должен парсить сложные URL со знаками вопроса в параметрах запроса или в хэше', () => {
        expect(parseUrl('google.ru?some=i?dont?know')).toEqual({
            scheme: undefined,
            domain: 'google.ru',
            origin: 'google.ru',
            port: undefined,
            path: '',
            query: 'some=i?dont?know',
            hash: undefined,
            queryParams: { some: 'i?dont?know' },
        });

        expect(parseUrl('google.ru#some=i?dont?know')).toEqual({
            scheme: undefined,
            domain: 'google.ru',
            origin: 'google.ru',
            port: undefined,
            path: '',
            query: undefined,
            hash: 'some=i?dont?know',
            queryParams: {},
        });

        expect(parseUrl('google.ru?some=i?dont?know#some=i?dont?know')).toEqual({
            scheme: undefined,
            domain: 'google.ru',
            origin: 'google.ru',
            port: undefined,
            path: '',
            query: 'some=i?dont?know',
            hash: 'some=i?dont?know',
            queryParams: { some: 'i?dont?know' },
        });
    });

    it('должен парсить сложные URL с закодированными строками', () => {
        expect(parseUrl('https://guru.ru/архив-новостей?from=2019-07-05&to=2019-07-05&type=all')).toEqual({
            scheme: 'https',
            domain: 'guru.ru',
            origin: 'https://guru.ru',
            port: undefined,
            path: '/архив-новостей',
            query: 'from=2019-07-05&to=2019-07-05&type=all',
            hash: undefined,
            queryParams: { from: '2019-07-05', to: '2019-07-05', type: 'all' },
        });
    });
});
