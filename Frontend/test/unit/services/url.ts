/* eslint-disable */
import test from 'ava';
import { tryNormalizeUrl } from '../../../services/url';

test('tryNormalizeUrl for various input should return expected result', t => {
    const cases = [
        ['HTTP://Ремонт-спб.РФ', 'http://ремонт-спб.рф'], // русские домены uppercase протокол и домен (punycode)
        ['q.com', 'http://q.com'], // дефолтный протокол
        ['user:password@sindresorhus.com', 'http://sindresorhus.com'], // выпиливание аутентификации
        ['https://q.com', 'https://q.com'],
        ['//q.com', 'http://q.com'],
        ['http://q.com?q=1', 'http://q.com/?q=1'],
        ['   http://q.com ', 'http://q.com'],
        ['', undefined],
        ['  ', undefined],
        [undefined, undefined],
        [null, undefined],
    ] as Array<[string | undefined, string | undefined]>;

    for (const [input, expected] of cases) {
        t.is(tryNormalizeUrl(input), expected);
    }
});
