import 'jest';
import { searchLinkInText, SubStrings, Type } from '../index';

const tests: {
    input: string,
    output: SubStrings[] | undefined
}[] = [
    {
        input: 'www.yandex.ru',
        output: [{ str: 'www.yandex.ru', type: Type.link }],
    },
    {
        input: 'http://www.yandex.ru',
        output: [{ str: 'http://www.yandex.ru', type: Type.link }],
    },
    {
        input: 'https://www.yandex.ru',
        output: [{ str: 'https://www.yandex.ru', type: Type.link }],
    },
    {
        input: 'Привет мир yandex.ru',
        output: [
            { str: 'Привет мир ', type: Type.string },
            { str: 'yandex.ru', type: Type.link },
        ],
    },
    {
        input: 'Привет мир yandex.ru?rrr=w',
        output: [
            { str: 'Привет мир ', type: Type.string },
            { str: 'yandex.ru?rrr=w', type: Type.link },
        ],
    },
    {
        input: 'Привет мир 32яру.рф?%20D=11, я тут',
        output: [
            { str: 'Привет мир ', type: Type.string },
            { str: '32яру.рф?%20D=11', type: Type.link },
            { str: ', я тут', type: Type.string },
        ],
    },
    {
        input: 'Привет мир yandex.ru, я тут',
        output: [
            { str: 'Привет мир ', type: Type.string },
            { str: 'yandex.ru', type: Type.link },
            { str: ', я тут', type: Type.string },
        ],
    },
    {
        input: 'Привет мир ya@yandex.ru, я тут',
        output: [
            { type: Type.string, str: 'Привет мир ' },
            { type: Type.email, str: 'ya@yandex.ru' },
            { type: Type.string, str: ', я тут' },
        ],
    },
    {
        input: 'Привет мир ya@yandex.ru, я.тут',
        output: [
            { type: Type.string, str: 'Привет мир ' },
            { type: Type.email, str: 'ya@yandex.ru' },
            { type: Type.string, str: ', я.тут' },
        ],
    },
    {
        input: 'Привет мир jjj@ya@yandex.ru, я.тут',
        output: [
            { type: Type.string, str: 'Привет мир ' },
            { type: Type.string, str: 'jjj@ya@yandex.ru' },
            { type: Type.string, str: ', я.тут' },
        ],
    },
    {
        input: 'Привет мир ya@yandex.ru?ddd=rr, я.тут',
        output: [
            { type: Type.string, str: 'Привет мир ' },
            { type: Type.string, str: 'ya@yandex.ru?ddd=rr' },
            { type: Type.string, str: ', я.тут' },
        ],
    },
    {
        input: 'Привет мир y_a@yandex.ru?ddd=rr, я.тут',
        output: [
            { type: Type.string, str: 'Привет мир ' },
            { type: Type.string, str: 'y_a@yandex.ru?ddd=rr' },
            { type: Type.string, str: ', я.тут' },
        ],
    },
    {
        input: 'Привет мир.yandex.ru, я тут',
        output: [
            { type: Type.string, str: 'Привет ' },
            { type: Type.link, str: 'мир.yandex.ru' },
            { type: Type.string, str: ', я тут' },
        ],
    },
    {
        input: 'Привет мирhttps://yandex.ru, я тут',
        output: [
            { type: Type.string, str: 'Привет мир' },
            { type: Type.link, str: 'https://yandex.ru' },
            { type: Type.string, str: ', я тут' },
        ],
    },
    {
        input: 'Привет мирhttps://yandex.ru,, я тут',
        output: [
            { type: Type.string, str: 'Привет мир' },
            { type: Type.link, str: 'https://yandex.ru' },
            { type: Type.string, str: ',, я тут' },
        ],
    },
    {
        input: 'Привет мир.http://www.yandex.ru, я тут',
        output: [
            { type: Type.string, str: 'Привет мир.http' },
            { type: Type.string, str: '://' },
            { type: Type.link, str: 'www.yandex.ru' },
            { type: Type.string, str: ', я тут' },
        ],
    },
    {
        input: ', я тут ggg.ru',
        output: [
            { type: Type.string, str: ', я тут ' },
            { type: Type.link, str: 'ggg.ru' },
        ],
    },
    {
        input: 'Привет мирhttps://yandex.ru, я тут ggg.ru',
        output: [
            { type: Type.string, str: 'Привет мир' },
            { type: Type.link, str: 'https://yandex.ru' },
            { type: Type.string, str: ', я тут ' },
            { type: Type.link, str: 'ggg.ru' },
        ],
    },
];

describe('link-Wrapper', () => {
    tests.forEach(({ input, output }) => {
        it(`Должна распарсить строку "${input}"`, () => {
            expect(searchLinkInText(input)).toEqual(output);
        });
    });
});
