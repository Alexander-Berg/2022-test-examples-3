import { counter } from './counter';

describe('counter', function () {
    it('0', function () {
        const cnt = counter();
        expect(cnt()).toBe('0 items');
    });

    it('1', function () {
        const cnt = counter();

        cnt();

        expect(cnt()).toBe('1 item');
    });

    it('2', function () {
        const cnt = counter();

        cnt();
        cnt();

        expect(cnt()).toBe('2 items');
    });
});
