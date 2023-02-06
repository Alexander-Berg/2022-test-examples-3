import { getQueryParam, filterEmptyValueParams } from '..';

describe('getQueryParam', () => {
    it('Возвращает первый элемент из массива, если такой параметр существует', () => {
        const req = {
            text: ['https://market.yandex.ru/product/1234'],
        };
        const expected = 'https://market.yandex.ru/product/1234';
        const actual = getQueryParam('text', req);

        expect(actual).toEqual(expected);
    });

    it('Отдаёт undefined, если такого параметра нет', () => {
        const expected = undefined;
        const actual = getQueryParam('parent-req-id', { text: [''] });

        expect(actual).toEqual(expected);
    });
});

describe('filterEmptyValueParams', () => {
    it('Фильтрует все параметры, которые содержат undefined и null', () => {
        const queryParams = {
            a: undefined,
            b: '',
        };
        const expected = { b: '' };
        const actual = filterEmptyValueParams(queryParams);

        expect(actual).toEqual(expected);
    });
});
