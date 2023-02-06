import argsHelpers from '../js/argsHelpers';

describe('Тесты хелперов njs модуля', () => {
    test('parseQuery корректно парсит пустую строку', () => {
        const empty = '';

        expect(argsHelpers.parseQuery(empty)).toEqual({});
    });

    test('parseQuery корректно парсит обычный key value', () => {
        const single = 'key=value';

        expect(argsHelpers.parseQuery(single)).toEqual({ key: ['value'] });
    });

    test('parseQuery корректно парсит несколько вхождений ключа', () => {
        const multipleValues = 'exp_flags=value1&exp_flags=value2';

        expect(argsHelpers.parseQuery(multipleValues)).toEqual({ exp_flags: ['value1', 'value2'] });
    });

    test('parseQuery корректно парсит мусорные значения', () => {
        const junkValues = 'key=undefined&key2=null&key3=[]';

        expect(argsHelpers.parseQuery(junkValues)).toEqual({ key: ['undefined'], key2: ['null'], key3: ['[]'] });
    });

    test('parseQuery декодирует значения', () => {
        const encoded = `key=${encodeURIComponent('?&=<>[]')}`;

        expect(argsHelpers.parseQuery(encoded)).toEqual({ key: ['?&=<>[]'] });
    });

    test('parseQuery корректно парсит флаги без значения', () => {
        const query = 'debug';

        expect(argsHelpers.parseQuery(query)).toEqual({ debug: ['1'] });
    });

    test('stringifyQuery возвращает корректную строку для базовых параметров', () => {
        const args = {
            foo: ['bar'],
            bar: ['foo'],
        };

        expect(argsHelpers.stringifyQuery(args)).toBe('foo=bar&bar=foo');
    });

    test('stringifyQuery возвращает корректную строку для повторяющихся параметров', () => {
        const args = {
            exp_flags: ['foo', 'bar'],
            key: [],
        };

        expect(argsHelpers.stringifyQuery(args)).toBe('exp_flags=foo&exp_flags=bar&key=1');
    });

    test('stringifyQuery энкодит значения', () => {
        const args = {
            foo: ['[]<>?&'],
        };

        expect(argsHelpers.stringifyQuery(args)).toBe(`foo=${encodeURIComponent(args.foo)}`);
    });

    test('stringifyQuery корректно работает с пустыми данными', () => {
        expect(argsHelpers.stringifyQuery({})).toBe('');
        expect(argsHelpers.stringifyQuery()).toBe('');
    });
});
