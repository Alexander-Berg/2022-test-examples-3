import updateQuery from '../updateQuery';

describe('updateQuery', () => {
    const sampleUrl = 'http://sample.ru/';
    const sampleQuery = {
        param1: 'sampleValue1',
        param2: 'тестовое значение 2',
    };
    const encodedQuery =
        'param1=sampleValue1&param2=%D1%82%D0%B5%D1%81%D1%82%D0%' +
        'BE%D0%B2%D0%BE%D0%B5+%D0%B7%D0%BD%D0%B0%D1%87%D0%B5%D0%BD%D0%B8%D0%B5+2';

    it('Добавляет строку запроса, если её не было', () => {
        const url = sampleUrl;
        const query = sampleQuery;

        expect(updateQuery(url, query)).toBe(`${url}?${encodedQuery}`);
    });

    it('Добавляет строку запроса, если её не было, перед хэшем', () => {
        const path = sampleUrl;
        const hash = '#sampleHash';

        const url = path + hash;
        const query = sampleQuery;

        expect(updateQuery(url, query)).toBe(`${path}?${encodedQuery}${hash}`);
    });

    it('Добавляет к строке запроса, перед хэшем', () => {
        const path = sampleUrl;
        const queryString = '?param=1';
        const hash = '#sampleHash';

        const url = path + queryString + hash;
        const query = sampleQuery;

        expect(updateQuery(url, query)).toBe(
            `${path}${queryString}&${encodedQuery}${hash}`,
        );
    });

    it('Заменяет параметр строки запроса', () => {
        const path = sampleUrl;
        const queryString = '?param1=1&param2=2';
        const hash = '#sampleHash';

        const url = path + queryString + hash;
        const query = {param1: 2};

        expect(updateQuery(url, query)).toBe(
            `${path}?param1=2&param2=2${hash}`,
        );
    });

    it('Удаляет параметр строки запроса', () => {
        const path = sampleUrl;
        const queryString = `?${encodedQuery}`;

        const url = path + queryString;
        const query = {param2: undefined};

        expect(updateQuery(url, query)).toBe(`${path}?param1=sampleValue1`);
    });

    it("Удаляет знак '?', когда нет параметров", () => {
        const path = sampleUrl;
        const queryString = '?param=1';
        const hash = '#sampleHash';

        const url = path + queryString + hash;
        const query = {param: undefined};

        expect(updateQuery(url, query)).toBe(path + hash);
    });
});
