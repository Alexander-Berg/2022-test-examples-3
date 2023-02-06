/**
 * @jest-environment jsdom
 */
jest.useFakeTimers();

import {getCounterCode, getInitEventId, reachGoal} from '../yaMetrika';

describe('getCounterCode', () => {
    it('Вернёт номер счетчика для тача', () =>
        expect(getCounterCode(true)).toBe(22352497));

    it('Вернёт номер счетчика для десктопа', () =>
        expect(getCounterCode()).toBe(99704));
});

describe('getInitEventId', () => {
    it('Вернёт идентификатор для тачевого события', () =>
        expect(getInitEventId(true)).toBe('yacounter22352497inited'));

    it('Вернёт идентификатор для десктопного события', () =>
        expect(getInitEventId()).toBe('yacounter99704inited'));
});

describe('reachGoal', () => {
    let initialYaCounter;

    beforeEach(() => {
        initialYaCounter = window.yaCounter;
        jest.clearAllTimers();
    });

    afterEach(() => {
        window.yaCounter = initialYaCounter;
    });

    it('Должна вызваться функция метрики с нужными аргументами', () => {
        window.yaCounter = {
            reachGoal: jest.fn(),
        };

        const prop1 = {importantProp: 108};
        const prop2 = {abc: '108'};

        reachGoal('some_event', prop1);

        jest.advanceTimersByTime(100);
        expect(window.yaCounter.reachGoal).toHaveBeenLastCalledWith(
            'some_event',
            prop1,
            undefined,
            undefined,
        );

        reachGoal('some_event_2', prop1, prop2);

        jest.advanceTimersByTime(100);
        expect(window.yaCounter.reachGoal).toHaveBeenLastCalledWith(
            'some_event_2',
            prop1,
            prop2,
            undefined,
        );
    });

    it('Очередь событий должна создаваться при событии, если ее нет', () => {
        const initialYaCounterQueue = window.yaCounterQueue;

        window.yaCounter = undefined;
        window.yaCounterQueue = undefined;

        reachGoal('some_event');

        expect(Array.isArray(window.yaCounterQueue)).toBe(true);

        window.yaCounterQueue = initialYaCounterQueue;
    });

    it('Добавить событие в очередь, если метрика не загрузилась', () => {
        const initialYaCounterQueue = window.yaCounterQueue;

        window.yaCounter = undefined;
        window.yaCounterQueue = [];

        reachGoal('some_event', {importantProp: 108});

        expect(window.yaCounterQueue).toEqual([
            {
                target: 'some_event',
                props: [{importantProp: 108}],
            },
        ]);

        window.yaCounterQueue = undefined;

        reachGoal('some_event', {importantProp: 108}, {abc: '108'});
        reachGoal('some_event_2', {importantProp: 22});

        expect(window.yaCounterQueue).toEqual([
            {
                target: 'some_event',
                props: [{importantProp: 108}, {abc: '108'}],
            },
            {
                target: 'some_event_2',
                props: [{importantProp: 22}],
            },
        ]);

        window.yaCounterQueue = initialYaCounterQueue;
    });
});
