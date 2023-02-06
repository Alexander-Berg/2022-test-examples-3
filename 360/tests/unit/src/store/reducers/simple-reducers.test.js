import deepFreeze from 'deep-freeze';
import { overrideState, assignState } from '../../../../../src/store/reducers/simple-reducers';

const TEST_OVERRIDE_ACTION_TYPE = 'TEST_OVERRIDE_ACTION_TYPE';
const TEST_OVERRIDE_WITH_DEFAULT_ACTION_TYPE = 'TEST_OVERRIDE_WITH_DEFAULT_ACTION_TYPE';
const TEST_ASSIGN_ACTION_TYPE = 'TEST_ASSIGN_ACTION_TYPE';

const testOverrideReducer = overrideState(
    TEST_OVERRIDE_ACTION_TYPE,
    'testOverridenState'
);
const testOverrideReducerWithDefault = overrideState(
    TEST_OVERRIDE_WITH_DEFAULT_ACTION_TYPE,
    'testOverridenWithDefaultState',
    {
        default: true
    }
);
const testAssignReducer = assignState(
    TEST_ASSIGN_ACTION_TYPE,
    'testAssignedState'
);

describe('simple override reducer', () => {
    it('состояние по умолчанию', () => {
        expect(testOverrideReducer(undefined, {})).toEqual(null);
    });

    it('состояние по умолчанию с дефолтным значением', () => {
        const stateAfter = testOverrideReducerWithDefault(undefined, {});
        expect(typeof stateAfter).toEqual('object');
        expect(Object.keys(stateAfter)).toEqual(['default']);
        expect(stateAfter.default).toEqual(true);
    });

    it('перезапись state-а примитивным значением', () => {
        const newState = 42;
        expect(testOverrideReducer(undefined, {
            type: TEST_OVERRIDE_ACTION_TYPE,
            testOverridenState: newState
        })).toEqual(42);
    });

    it('перезапись state-а объектом', () => {
        const newState = {
            someField: 'some value',
            subObject: {
                subField: 'sub value'
            },
            subArray: [1, 'a', null]
        };
        deepFreeze(newState);
        const stateAfter = testOverrideReducer(undefined, {
            type: TEST_OVERRIDE_ACTION_TYPE,
            testOverridenState: newState
        });
        expect(stateAfter).toEqual(newState);
    });

    it('перезапись state-а массивом', () => {
        const newState = [1, 'a', null];
        deepFreeze(newState);
        const stateAfter = testOverrideReducer(undefined, {
            type: TEST_OVERRIDE_ACTION_TYPE,
            testOverridenState: newState
        });
        expect(stateAfter).toEqual(newState);
    });

    it('перезапись state-а с дефолтным значением примитивом', () => {
        const newState = 43;
        expect(testOverrideReducerWithDefault(undefined, {
            type: TEST_OVERRIDE_WITH_DEFAULT_ACTION_TYPE,
            testOverridenWithDefaultState: newState
        })).toEqual(43);
    });

    it('перезапись state-а с дефолтным значением объектом', () => {
        const newState = {
            anotherField: 'another value',
            subObject: {
                subField: 'sub value'
            },
            subArray: [2, 'b', undefined]
        };
        deepFreeze(newState);
        const stateAfter = testOverrideReducerWithDefault(undefined, {
            type: TEST_OVERRIDE_WITH_DEFAULT_ACTION_TYPE,
            testOverridenWithDefaultState: newState
        });
        expect(stateAfter).toEqual(newState);
    });

    it('перезапись state-а с дефолтным значением массовом', () => {
        const newState = [2, 'b', undefined];
        deepFreeze(newState);
        const stateAfter = testOverrideReducerWithDefault(undefined, {
            type: TEST_OVERRIDE_WITH_DEFAULT_ACTION_TYPE,
            testOverridenWithDefaultState: newState
        });
        expect(stateAfter).toEqual(newState);
    });
});

describe('simple assign reducer', () => {
    it('состояние по умолчанию', () => {
        expect(testAssignReducer(undefined, {})).toEqual({});
    });

    it('запись объекта', () => {
        const newState = {
            someField: 'some value',
            subObject: {
                subField: 'sub value'
            },
            subArray: [1, 'a', null]
        };
        deepFreeze(newState);
        expect(testAssignReducer(undefined, {
            type: TEST_ASSIGN_ACTION_TYPE,
            testAssignedState: newState
        })).toEqual(newState);
    });

    it('дополнение (assign) объекта', () => {
        const firstState = {
            someField: 'some value',
            anotherField: 'another value',
            subObject: {
                subField: 'sub value'
            },
            subArray: [1, 'a', null]
        };
        const secondState = {
            anotherField: 'not a value at all',
            oneMoreValue: 'wat?',
            subObject: {
                aaa: 'new sub object is here'
            },
            subArray: [2, 'b']
        };
        deepFreeze(firstState);
        deepFreeze(secondState);
        let state = testAssignReducer(undefined, {
            type: TEST_ASSIGN_ACTION_TYPE,
            testAssignedState: firstState
        });
        expect(state).toEqual(firstState);
        deepFreeze(state);
        state = testAssignReducer(state, {
            type: TEST_ASSIGN_ACTION_TYPE,
            testAssignedState: secondState
        });

        // проверяем что и правда был assign
        expect(state).toEqual(Object.assign({}, firstState, secondState));
    });
});
