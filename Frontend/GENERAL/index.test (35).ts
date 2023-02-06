import { SECONDS_IN_DAY, SECONDS_IN_HOUR, SECONDS_IN_MINUTE } from 'utils/time';

import { formatDelay, formatWillHappensIn } from './index';

const getTestTime = (d: number = 0, h: number = 0, m: number = 0, s: number = 0) =>
    d * SECONDS_IN_DAY + h * SECONDS_IN_HOUR + m * SECONDS_IN_MINUTE + s;

describe('blocks/time: formatWillHappensIn', () => {
    it('30 секунд', () => {
        expect(formatWillHappensIn(getTestTime(0, 0, 0, 30))).toEqual('Через несколько секунд');
    });

    it('1 минута', () => {
        expect(formatWillHappensIn(getTestTime(0, 0, 1, 0))).toEqual('Через 1 минуту');
    });

    it('1 час', () => {
        expect(formatWillHappensIn(getTestTime(0, 1, 0, 0))).toEqual('Через 1 час');
    });

    it('1 день', () => {
        expect(formatWillHappensIn(getTestTime(1, 0, 0, 0))).toEqual('Через 1 день');
    });

    it('1 день, 1 час', () => {
        expect(formatWillHappensIn(getTestTime(1, 1, 0, 0))).toEqual('Через 1 день и 1 час');
    });

    it('1 день, 1 минута', () => {
        expect(formatWillHappensIn(getTestTime(1, 0, 1, 0))).toEqual('Через 1 день и 1 минуту');
    });

    it('1 день, 1 минута, 25 секунд', () => {
        expect(formatWillHappensIn(getTestTime(1, 0, 1, 25))).toEqual('Через 1 день и 1 минуту');
    });

    it('1 день, 1 час, 1 минута', () => {
        expect(formatWillHappensIn(getTestTime(1, 1, 1, 25))).toEqual('Через 1 день 1 час и 1 минуту');
    });

    it('1 день, 1 час, 1 минута, 25 секунд', () => {
        expect(formatWillHappensIn(getTestTime(1, 1, 1, 25), 2)).toEqual('Через 1 день и 1 час');
    });

    it('1 день, 23 часа, 1 минута', () => {
        expect(formatWillHappensIn(getTestTime(1, 23, 1, 25))).toEqual('Через 1 день 23 часа и 1 минуту');
    });

    it('1 день, 23 часа', () => {
        expect(formatWillHappensIn(getTestTime(1, 23, 0, 25))).toEqual('Через 1 день и 23 часа');
    });

    it('1 день, 23 минуты', () => {
        expect(formatWillHappensIn(getTestTime(1, 0, 23, 0))).toEqual('Через 1 день и 23 минуты');
    });

    it('3 дней, 23 часов, 33 минут', () => {
        expect(formatWillHappensIn(getTestTime(3, 23, 33, 59))).toEqual('Через 3 дня 23 часа и 33 минуты');
    });

    it('5 дней, 6 часов, 37 минут', () => {
        expect(formatWillHappensIn(getTestTime(5, 6, 37, 59))).toEqual('Через 5 дней 6 часов и 37 минут');
    });
});

describe('blocks/time: formatDelay', () => {
    it('30 секунд', () => {
        expect(formatDelay(getTestTime(0, 0, 0, 30))).toEqual('30 секунд');
    });

    it('1 минута', () => {
        expect(formatDelay(getTestTime(0, 0, 1, 0))).toEqual('1 минута');
    });

    it('1 час', () => {
        expect(formatDelay(getTestTime(0, 1, 0, 0))).toEqual('1 час');
    });

    it('1 день', () => {
        expect(formatDelay(getTestTime(1, 0, 0, 0))).toEqual('1 день');
    });

    it('1 день, 1 час', () => {
        expect(formatDelay(getTestTime(1, 1, 0, 0))).toEqual('1 день и 1 час');
    });

    it('1 день, 1 минута', () => {
        expect(formatDelay(getTestTime(1, 0, 1, 0))).toEqual('1 день и 1 минута');
    });

    it('1 день, 1 минута, 25 секунд', () => {
        expect(formatDelay(getTestTime(1, 0, 1, 25))).toEqual('1 день 1 минута и 25 секунд');
    });

    it('1 день, 1 час, 1 минута, 25 секунд', () => {
        expect(formatDelay(getTestTime(1, 1, 1, 25))).toEqual('1 день 1 час 1 минута и 25 секунд');
    });

    it('1 день, 1 час, 1 минута, 25 секунд (maxTimeParts)', () => {
        expect(formatDelay(getTestTime(1, 1, 1, 25), 2)).toEqual('1 день и 1 час');
    });

    it('1 день, 23 часа, 1 минута, 23 секунд', () => {
        expect(formatDelay(getTestTime(1, 23, 1, 23))).toEqual('1 день 23 часа 1 минута и 23 секунды');
    });

    it('1 день, 23 часа, 25 секунд', () => {
        expect(formatDelay(getTestTime(1, 23, 0, 25))).toEqual('1 день 23 часа и 25 секунд');
    });

    it('1 день, 23 минуты', () => {
        expect(formatDelay(getTestTime(1, 0, 23, 0))).toEqual('1 день и 23 минуты');
    });

    it('3 дней, 23 часов, 33 минут, 59 секунд', () => {
        expect(formatDelay(getTestTime(3, 23, 33, 59))).toEqual('3 дня 23 часа 33 минуты и 59 секунд');
    });

    it('5 дней, 6 часов, 37 минут, 40 секунд', () => {
        expect(formatDelay(getTestTime(5, 6, 37, 40))).toEqual('5 дней 6 часов 37 минут и 40 секунд');
    });
});
