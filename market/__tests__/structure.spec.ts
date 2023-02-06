import { isVoid } from '../guards';
import { omit, omitBy, toObject } from '../structure';

describe('toObject', () => {
    it('должен собирать пары ключ-значение в объект', () => {
        expect(toObject([['some', 32]])).toEqual({ some: 32 });

        const obj = toObject([
            ['question', 'Amazingly Accurate Answer to Life, the Universe and Everything'],
            ['answer', 42],
        ] as const);

        expect(obj.question).toBe('Amazingly Accurate Answer to Life, the Universe and Everything');
        expect(obj.answer).toBe(42);
    });
});

describe('omitBy', () => {
    it('должен быть авто-каррирован', () => {
        expect(omitBy(Number.isFinite)).toBeInstanceOf(Function);
        expect(omitBy(Number.isFinite)({ ds: 3 })).toEqual(omitBy(Number.isFinite, { ds: 3 }));
    });

    it('должен всегда возвращать новый объект', () => {
        const obj = {};
        expect(omitBy(Number.isFinite, obj)).not.toBe(obj);
        expect(omitBy(Number.isFinite, obj)).toEqual(obj);
    });

    it('должен выкидывать поля по предикату', () => {
        expect(omitBy(isVoid, { nullish: null, notNull: 32 })).toEqual({ notNull: 32 });
        expect(omitBy(Number.isFinite, { nan: NaN, infinite: Infinity, notnan: 32 })).toEqual({
            nan: NaN,
            infinite: Infinity,
        });
        expect(omitBy(Number.isNaN, { nan: NaN, infinite: Infinity, notnan: 32 })).toEqual({
            infinite: Infinity,
            notnan: 32,
        });
    });
});

describe('omit', () => {
    it('должен вернуть тот же объект, если не передать соответствующие аргументы', () => {
        const obj = {};
        expect(omit(obj, [])).toBe(obj);
    });

    it('должен выкинуть поля с соответствующими ключами', () => {
        const a = omit({ some: 1, other: 2 }, ['some']);
        expect(omit({ some: 1, other: 2 }, ['some'])).toEqual({ other: 2 });
        expect(omit({ some: 1, other: 2 }, ['some', 'other'])).toEqual({});
    });
});
