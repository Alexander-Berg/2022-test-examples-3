import {METRIKA_COUNTER_ID} from 'constants/common';

import {reachGoal} from '../index';

jest.useFakeTimers();

describe('reachGoal', () => {
    let initialYaCounter;

    beforeEach(() => {
        initialYaCounter = window[`yaCounter${METRIKA_COUNTER_ID}`];
    });

    afterEach(() => {
        window[`yaCounter${METRIKA_COUNTER_ID}`] = initialYaCounter;
    });

    it('Должна вызваться функция метрики с нужными аргументами', () => {
        window[`yaCounter${METRIKA_COUNTER_ID}`] = {
            reachGoal: jest.fn(),
        };

        const prop1 = {importantProp: 108};
        const prop2 = {abc: '108'};

        reachGoal('some_event', prop1);

        jest.runAllTimers();

        expect(
            window[`yaCounter${METRIKA_COUNTER_ID}`].reachGoal,
        ).toHaveBeenLastCalledWith('some_event', prop1);

        reachGoal('some_event_2', prop1, prop2);

        jest.runAllTimers();

        expect(
            window[`yaCounter${METRIKA_COUNTER_ID}`].reachGoal,
        ).toHaveBeenLastCalledWith('some_event_2', prop1, prop2);
    });

    it('Очередь событий должна создаваться при событии, если ее нет', () => {
        const initialYaCounterQueue =
            window[`yaCounter${METRIKA_COUNTER_ID}Queue`];

        window[`yaCounter${METRIKA_COUNTER_ID}`] = undefined;
        window[`yaCounter${METRIKA_COUNTER_ID}Queue`] = undefined;

        reachGoal('some_event');

        jest.runAllTimers();

        expect(
            Array.isArray(window[`yaCounter${METRIKA_COUNTER_ID}Queue`]),
        ).toBe(true);

        window[`yaCounter${METRIKA_COUNTER_ID}Queue`] = initialYaCounterQueue;
    });

    it('Добавить событие в очередь, если метрика не загрузилась', () => {
        const initialYaCounterQueue =
            window[`yaCounter${METRIKA_COUNTER_ID}Queue`];

        window[`yaCounter${METRIKA_COUNTER_ID}`] = undefined;
        window[`yaCounter${METRIKA_COUNTER_ID}Queue`] = [];

        reachGoal('some_event', {importantProp: 108});

        jest.runAllTimers();

        expect(window[`yaCounter${METRIKA_COUNTER_ID}Queue`]).toEqual([
            {
                target: 'some_event',
                props: [{importantProp: 108}],
            },
        ]);

        window[`yaCounter${METRIKA_COUNTER_ID}Queue`] = undefined;

        reachGoal('some_event', {importantProp: 108}, {abc: '108'});
        reachGoal('some_event_2', {importantProp: 22});

        jest.runAllTimers();

        expect(window[`yaCounter${METRIKA_COUNTER_ID}Queue`]).toEqual([
            {
                target: 'some_event',
                props: [{importantProp: 108}, {abc: '108'}],
            },
            {
                target: 'some_event_2',
                props: [{importantProp: 22}],
            },
        ]);

        window[`yaCounter${METRIKA_COUNTER_ID}Queue`] = initialYaCounterQueue;
    });
});
