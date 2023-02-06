import {InvariantViolation} from '@yandex-market/invariant';

import {prop} from '..';
import {expectType} from '../helpers';

describe('функция prop', () => {
    it('возвращает переданное поле от объекта', () => {
        const sample = {offers: 30};

        expect(prop('offers', sample)).toEqual(30);
        expect(prop<typeof sample>('offers')(sample)).toEqual(30);
    });

    it('возвращает undefined, если этого поля нет', () => {
        const sample = {offers: 30};

        // @ts-expect-error
        expect(prop('unknown-field', sample)).toEqual(undefined);
        // @ts-expect-error
        expect(prop<typeof sample, 'unknown-field'>('unknown-field')(sample)).toEqual(undefined);
    });

    it('поддерживает массивы с числовыми индексами', () => {
        const sample: [1, 2] = [1, 2];

        expect(prop(1, sample)).toEqual(2);
        expect(prop(1)(sample)).toEqual(2);

        // ok for ts, workaround for getting actual array's length is kinda ugly
        expect(prop(2, sample)).toEqual(undefined);
        expect(prop(2)(sample)).toEqual(undefined);
    });

    it('выводит типы у объектов', () => {
        const sample: {
            [x: string]: number | undefined | null
        } = {a: 123};

        expectType<number>(prop('a', sample));
        expectType<number>(prop<typeof sample>('a')(sample));
    });

    it('выводит типы у литеральных объектов', () => {
        const sample = {a: 123} as const;

        expectType<123>(prop('a', sample));
        expectType<123>(prop<typeof sample>('a')(sample));

        // @ts-expect-error
        expectType<333>(prop('a', sample));
        // @ts-expect-error
        expectType<333>(prop<typeof sample>('a')(sample));
    });

    it('works fine with opaque types', () => {
        type ID = string;

        const id: ID = 'abc';

        const sample: {
            [k in ID]: number;
        } = {abc: 123};

        expectType<number>(prop(id, sample));
        expectType<number>(prop<typeof sample>(id)(sample));

        // @ts-expect-error
        expectType<string>(prop(id, sample));

        expectType<number>(prop('a', sample));
        expectType<number>(prop<typeof sample>('a')(sample));
    });

    it('поддерживает вывод типов для кортежей', () => {
        const sample0: [] = [];

        // ok for ts, workaround for getting actual tuples' length is kinda ugly
        prop(10, sample0);
        prop(10)(sample0);

        const sample2: [1, 2] = [1, 2];
        expectType<number>(prop(0, sample2));
        expectType<number>(prop<typeof sample2>(0)(sample2));
        (prop(2, sample2));
        (prop(2)(sample2));
    });

    it('корректно выводит тип для обычных массивов', () => {
        const sample: number[] = [1, 2, 3, 4];

        expectType<number>(prop(0, sample));
        expectType<number>(prop(0)(sample));

        expectType<number>(prop(3, sample));
        expectType<number>(prop(3)(sample));

        // Нет ошибки при несуществующем индексе, если массив неизвестной длины
        expectType<number>(prop(10, sample));
        expectType<number>(prop(10)(sample));
    });

    it('бросает исключение, если из второго аргумента нельзя взять свойство', () => {
        function propWithNull() {
            // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error
            prop('a', null);
            // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error
            prop('a')(null);
        }

        function propWithUndefined() {
            // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error
            prop('a', undefined);
            // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error
            prop('a')(undefined);
        }

        expect(propWithNull).toThrow(TypeError);
        expect(propWithUndefined).toThrow(TypeError);
    });

    it('бросает исключение, если первый аргумент не строка и не число', () => {
        function propWithWrongKeyType() {
            // @ts-expect-error
            prop(null, {});
            // @ts-expect-error
            prop(null)({});
        }

        expect(propWithWrongKeyType).toThrow(InvariantViolation);
    });
});
