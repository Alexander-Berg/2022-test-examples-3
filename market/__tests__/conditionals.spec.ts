import { and, each, not, or, some } from '../conditionals';

const isTrue = () => true;
const isFalse = () => false;
const identity = (v: boolean) => v;
const negation = (v: boolean) => !v;

describe('and', () => {
    it("должен объединять предикаты под логическим 'И'", () => {
        const f1 = and(isTrue, isTrue);
        const f2 = and(isTrue, isFalse);
        const f3 = and(isFalse, isFalse);

        const f4 = and(identity, identity);
        const f5 = and(identity, negation);
        const f6 = and(negation, negation);

        expect(f1()).toBe(true);
        expect(f2()).toBe(false);
        expect(f3()).toBe(false);

        expect(f4(true)).toBe(true);
        expect(f4(false)).toBe(false);
        expect(f5(true)).toBe(false);
        expect(f5(false)).toBe(false);
        expect(f6(true)).toBe(false);
        expect(f6(false)).toBe(true);
    });
});

describe('or', () => {
    it("должен объединять предикаты под логическим 'ИЛИ'", () => {
        const f1 = or(isTrue, isTrue);
        const f2 = or(isTrue, isFalse);
        const f3 = or(isFalse, isFalse);

        const f4 = or(identity, identity);
        const f5 = or(identity, negation);
        const f6 = or(negation, negation);

        expect(f1()).toBe(true);
        expect(f2()).toBe(true);
        expect(f3()).toBe(false);

        expect(f4(true)).toBe(true);
        expect(f4(false)).toBe(false);
        expect(f5(true)).toBe(true);
        expect(f5(false)).toBe(true);
        expect(f6(true)).toBe(false);
        expect(f6(false)).toBe(true);
    });
});

describe('and', () => {
    it('должен развернуть результат выполнения предиката', () => {
        const f1 = not(isTrue);
        const f2 = not(isFalse);
        const f3 = not(identity);
        const f4 = not(negation);

        expect(f1()).toBe(false);
        expect(f2()).toBe(true);
        expect(f3(true)).toBe(false);
        expect(f3(false)).toBe(true);
        expect(f4(false)).toBe(false);
        expect(f4(true)).toBe(true);
    });
});

describe('each', () => {
    test.each`
        array               | result
        ${[0, 2]}           | ${true}
        ${[2, Infinity]}    | ${false}
        ${[Infinity, 3]}    | ${false}
        ${[NaN, -Infinity]} | ${false}
    `('должен вернуть $result для предиката isFinite, если передать $array', ({ array, result }) =>
        expect(each(Number.isFinite)(array)).toBe(result),
    );
});

describe('some', () => {
    test.each`
        array               | result
        ${[0, 2]}           | ${true}
        ${[2, Infinity]}    | ${true}
        ${[Infinity, 3]}    | ${true}
        ${[NaN, -Infinity]} | ${false}
    `('должен вернуть $result для предиката isFinite, если передать $array', ({ array, result }) =>
        expect(some(Number.isFinite)(array)).toBe(result),
    );
});
