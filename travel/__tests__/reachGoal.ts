import {METRIKA_COUNTER_ID} from 'constants/common';

import {TMetrikaGoal} from 'utilities/metrika/types/goals/all';
import {TMetrikaParams} from 'utilities/metrika/types/params/all';

import {reachGoal} from '../index';

jest.useFakeTimers();

const fakeEvent1 = 'some_event' as TMetrikaGoal;
const fakeEvent2 = 'some_event_2' as TMetrikaGoal;

const prop1 = {importantProp: 108} as unknown as TMetrikaParams;
const prop2 = {abc: '108'} as unknown as TMetrikaParams;
const prop3 = {importantProp: 22} as unknown as TMetrikaParams;

describe('reachGoal', () => {
    let initialYaCounter: IMetrika | undefined;

    beforeEach(() => {
        initialYaCounter = window[`yaCounter${METRIKA_COUNTER_ID}`];
    });

    afterEach(() => {
        window[`yaCounter${METRIKA_COUNTER_ID}`] = initialYaCounter;
    });

    it('Должна вызваться функция метрики с нужными аргументами', () => {
        window[`yaCounter${METRIKA_COUNTER_ID}`] = {
            reachGoal: jest.fn(),
            params: jest.fn(),
            userParams: jest.fn(),
            hit: jest.fn(),
            getClientID: jest.fn(),
            setUserID: jest.fn(),
        };

        reachGoal(fakeEvent1, prop1);

        jest.runAllTimers();

        expect(
            window[`yaCounter${METRIKA_COUNTER_ID}`]?.reachGoal,
        ).toHaveBeenLastCalledWith('some_event', prop1);

        reachGoal(fakeEvent2, prop1, prop2);

        jest.runAllTimers();

        expect(
            window[`yaCounter${METRIKA_COUNTER_ID}`]?.reachGoal,
        ).toHaveBeenLastCalledWith('some_event_2', prop1, prop2);
    });

    it('Очередь событий должна создаваться при событии, если ее нет', () => {
        const initialYaCounterQueue =
            window[`yaCounter${METRIKA_COUNTER_ID}Queue`];

        window[`yaCounter${METRIKA_COUNTER_ID}`] = undefined;
        window[`yaCounter${METRIKA_COUNTER_ID}Queue`] = undefined;

        reachGoal(fakeEvent1);

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

        reachGoal(fakeEvent1, prop1);

        jest.runAllTimers();

        expect(window[`yaCounter${METRIKA_COUNTER_ID}Queue`]).toEqual([
            {
                target: 'some_event',
                props: [{importantProp: 108}],
            },
        ]);

        window[`yaCounter${METRIKA_COUNTER_ID}Queue`] = undefined;

        reachGoal(fakeEvent1, prop1, prop2);
        reachGoal(fakeEvent2, prop3);

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
