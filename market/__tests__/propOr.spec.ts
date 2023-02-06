/* eslint-disable no-unused-expressions */
import {InvariantViolation} from '@yandex-market/invariant';

import {propOr} from '..';
import {expectType} from '../helpers';

describe('propOr', () => {
    it('возвращает переданное поле от объекта', () => {
        const sample = {offers: 30};

        expect(propOr(null, 'offers', sample)).toEqual(30);
        expect(propOr(null, 'offers')(sample)).toEqual(30);
    });

    it('возвращает defaultValue, если поле равно undefined', () => {
        const sample = {offers: 30};

        expect(propOr(333, 'unknown-field', sample)).toEqual(333);
        expect(propOr(333, 'unknown-field')(sample)).toEqual(333);

        // Ориентируется на значение, а не на принадлежность к собственным свойствам объекта
        const sampleWithUndefinedValue = {offers: undefined};
        expect(propOr(333, 'offers', sampleWithUndefinedValue)).toEqual(333);
        expect(propOr(333, 'offers')(sampleWithUndefinedValue)).toEqual(333);
    });

    it('поддерживает массивы с числовыми индексами', () => {
        const sample: [1, 2] = [1, 2];

        expect(propOr(null, 1, sample)).toEqual(2);
        expect(propOr(null, 1)(sample)).toEqual(2);

        expect(propOr('test', 2, sample)).toEqual('test');
        expect(propOr('test', 2)(sample)).toEqual('test');
    });

    it('выводит типы у объектов', () => {
        /* eslint-disable no-unused-expressions */
        const sample: {
            [x: string]: number | undefined | null
        } = {a: 123};

        expectType<number | 'test'>(propOr('test', 'a', sample));
        expectType<number | 'test'>(propOr<typeof sample, 'test', 'a'>('test', 'a')(sample));

        expectType<number | 'test'>(propOr('test', 'b', sample));
        expectType<number | 'test'>(propOr<typeof sample, 'test', 'b'>('test', 'b')(sample));
        /* eslint-enable no-unused-expressions */
    });

    it('выводит типы у литеральных объектов', () => {
        /* eslint-disable no-unused-expressions */
        const sample = {a: 123};

        expectType<number>(propOr('abc', 'a' as const, sample));
        expectType<number>(propOr<typeof sample, 'abc', 'a'>('abc', 'a' as const)(sample));

        expectType<333>(propOr(333, 'b', sample));
        expectType<333>(propOr<typeof sample, 333, 'b'>(333, 'b')(sample));

        // @ts-expect-error
        expectType<444>(propOr(333, 'b', sample));
        // @ts-expect-error
        expectType<444>(propOr(333, 'b')(sample));
        /* eslint-enable no-unused-expressions */
    });

    it('поддерживает вывод типов для кортежей', () => {
        /* eslint-disable no-unused-expressions */
        const sample0: [] = [];

        expectType<'wow'>(propOr('wow', 10 as const, sample0));
        expectType<'wow'>(propOr<typeof sample0, 'wow'>('wow', 10)(sample0));

        // @ts-expect-error
        expectType<'differentString'>(propOr('wow', 10, sample0));
        // @ts-expect-error
        expectType<'differentString'>(propOr<typeof sample0, 'wow'>('wow', 10)(sample0));

        const sample1: [1] = [1];
        expectType<number>(propOr(null, 0, sample1));
        expectType<number>(propOr<typeof sample1, null>(null, 0)(sample1));

        expectType<number>(propOr(333, 1, sample1));
        expectType<number>(propOr<typeof sample1, 333>(333, 1)(sample1));

        const sample2: [1, 2] = [1, 2];
        expectType<333 | 1 | 2>(propOr(333, 2, sample2));

        // @ts-expect-error
        expectType<444>(propOr(333, 2, sample2));
        // @ts-expect-error
        expectType<444>(propOr(333, 2, sample2));
        /* eslint-enable no-unused-expressions */
    });

    it('корректно выводит тип для обычных массивов', () => {
        /* eslint-disable no-unused-expressions */
        const sample: number[] = [1, 2, 3, 4];

        // Поскольку размер массива неизвестен, возвращается элемент массива & defaultValue
        expectType<number | 'a'>(propOr('a', 0, sample));
        expectType<number | 'a'>(propOr<typeof sample, 'a'>('a', 0)(sample));

        // @ts-expect-error
        expectType<number>(propOr('a', 0, sample));
        // @ts-expect-error
        expectType<number>(propOr<typeof sample, 'a'>('a', 0)(sample));

        // Нет ошибки при несуществующем индексе, если массив неизвестной длины
        expectType<number | 'a'>(propOr('a', 10, sample));
        expectType<number | 'a'>(propOr<typeof sample, 'a'>('a', 10)(sample));
        /* eslint-enable no-unused-expressions */
    });

    it('works fine with opaque types', () => {
        /* eslint-disable no-unused-expressions */
        type ID = string;

        const id: ID = 'abc';

        const sample: {
            [k in ID]: number;
        } = {abc: 123};

        expectType<number | null>(propOr(null, id, sample));
        expectType<number | null>(propOr<typeof sample, null, typeof id>(null, id)(sample));
        // expectType<number | null>(propOr('qwe', id)(sample));

        // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error
        expectType<number>(propOr(null, 'a', sample));

        // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error
        expectType<number>(propOr<typeof sample, null, 'a'>(null, 'a')(sample));

        /* eslint-enable no-unused-expressions */
    });

    it('бросает исключение, если второй аргумент не строка и не число', () => {
        function propWithWrongKeyType() {
            // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error
            propOr(null, null, {});
            // @ts-expect-error curry
            propOr(null, null)({});
        }

        expect(propWithWrongKeyType).toThrow(InvariantViolation);
    });
});
