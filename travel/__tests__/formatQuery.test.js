import {formatQuery} from '../index';

describe('formatQuery', () => {
    test('Должен вернуть пустую строку для пустого объекта', () => {
        expect(formatQuery({})).toBe('');
    });

    test('Должен вернуть foo=bar', () => {
        expect(formatQuery({foo: 'bar'})).toBe('foo=bar');
    });

    test('Должен вернуть baz=yarrr&foo=bar (сортировка по ключам)', () => {
        expect(formatQuery({foo: 'bar', baz: 'yarrr'})).toBe(
            'baz=yarrr&foo=bar',
        );
    });

    test('Должен вернуть foo для значения null', () => {
        expect(formatQuery({foo: null})).toBe('foo');
    });

    test('Должен вернуть пустую строку для значения foo=null', () => {
        expect(formatQuery({foo: null}, {filterNull: true})).toBe('');
    });
});

describe('formatQuery', () => {
    test.each`
        params                  | expected
        ${{a: 1, b: 2}}         | ${'a=1&b=2'}
        ${{a: 1, b: null}}      | ${'a=1'}
        ${{a: 1, b: undefined}} | ${'a=1'}
        ${{b: undefined}}       | ${''}
        ${{}}                   | ${''}
        ${{a: false}}           | ${'a=false'}
        ${{a: true}}            | ${'a=true'}
        ${{a: 'true'}}          | ${'a=true'}
        ${{a: ''}}              | ${'a='}
        ${{a: 0}}               | ${'a=0'}
    `('formatQuery($params) must be $expected', ({params, expected}) => {
        expect(formatQuery(params, {filterNull: true})).toBe(expected);
    });
});
