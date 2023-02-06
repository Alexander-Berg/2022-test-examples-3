import { memo } from '../memo';

describe('#memo', () => {
    let counter = 0;
    const fn = memo((a: number, b: number) => {
        counter++;

        return a + b;
    });

    afterEach(() => {
        counter = 0;
    });

    it('Функция должна вызываться каждый раз при получении нового аргумента', () => {
        fn(1, 1);
        fn(1, 2);
        fn(2, 3);

        expect(counter).toBe(3);
    });

    it('Функция должна вызываться для один раз для одинаковых аргументов', () => {
        fn(1, 2);
        fn(1, 2);
        fn(1, 2);

        expect(counter).toBe(1);
    });

    it('Должна возвращать значения и вызываться каждый раз при новых аргументах', () => {
        const res1 = fn(1, 1);
        const res2 = fn(2, 3);
        const res3 = fn(2, 3);

        expect(res1).toBe(2);
        expect(res2).toBe(5);
        expect(res3).toBe(5);
        expect(counter).toBe(2);
    });
});
