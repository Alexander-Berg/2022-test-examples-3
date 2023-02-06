import { createRoutine } from 'redux-saga-routines';
import { COMMON_STORE } from './common.actions';
import { commonDataReducer, errorReducer, loadingReducer } from './common.reducers';

const payloadCreator = {
    success: (id, payload) => (payload),
    failure: (id, payload) => (payload),
};

const complexMetaCreator = {
    request: id => ({ id, needDictionary: true }),
    failure: id => ({ id, needDictionary: true }),
    fulfill: id => ({ id, needDictionary: true }),
};

const prefix = 'abc-www/Feature/Component';
const exampleRoutine = createRoutine(`${prefix}/example`);
const exampleRoutineWithId = createRoutine(`${prefix}/example`, payloadCreator, complexMetaCreator);

const error = new Error('test error');

const initialState = { feature: { component: {}, otherComponent: {} }, otherFeature: {} };

describe('Should handle actions', () => {
    describe('loading reducer', () => {
        it('TRIGGER should do nothing', () => {
            expect(loadingReducer(initialState, exampleRoutine.trigger())).toEqual(initialState);
        });

        it('TRIGGER with id should do nothing', () => {
            expect(loadingReducer(initialState, exampleRoutineWithId.trigger(1))).toEqual(initialState);
        });

        it('REQUEST should set loading:true', () => {
            const state = loadingReducer(initialState, exampleRoutine.request());

            expect(state[`${prefix}/example`]).toBe(true);
        });

        it('REQUEST with id set loading:true in right id', () => {
            const state = loadingReducer(initialState, exampleRoutineWithId.request(1));

            expect(state[`${prefix}/example`][1]).toBe(true);
        });

        it('SUCCESS should do nothing', () => {
            expect(loadingReducer(initialState, exampleRoutine.success())).toEqual(initialState);
        });

        it('SUCCESS with id should do nothing', () => {
            expect(loadingReducer(initialState, exampleRoutineWithId.success(1))).toEqual(initialState);
        });

        it('FAILURE should do nothing', () => {
            expect(loadingReducer(initialState, exampleRoutine.failure())).toEqual(initialState);
        });

        it('FAILURE with id should do nothing', () => {
            expect(loadingReducer(initialState, exampleRoutineWithId.failure(1))).toEqual(initialState);
        });

        it('FULFILL should set loading:false', () => {
            const state = loadingReducer(initialState, exampleRoutine.fulfill());
            expect(state[`${prefix}/example`]).toBe(false);
        });

        it('FULFILL with id loading:false in right id', () => {
            const state = loadingReducer(initialState, exampleRoutineWithId.fulfill(1));

            expect(state[`${prefix}/example`][1]).toBe(false);
        });
    });

    describe('error reducer', () => {
        it('TRIGGER should do nothing', () => {
            expect(errorReducer(initialState, exampleRoutine.trigger())).toEqual(initialState);
        });

        it('TRIGGER with id should do nothing', () => {
            expect(errorReducer(initialState, exampleRoutineWithId.trigger(1))).toEqual(initialState);
        });

        it('REQUEST should set error: null', () => {
            const state = errorReducer(initialState, exampleRoutine.request());

            expect(state[`${prefix}/example`]).toBe(null);
        });

        it('REQUEST with id should set error: null in right id', () => {
            const state = errorReducer(initialState, exampleRoutineWithId.request(1));

            expect(state[`${prefix}/example`][1]).toBe(null);
        });

        it('SUCCESS should do nothing', () => {
            expect(errorReducer(initialState, exampleRoutine.success())).toEqual(initialState);
        });

        it('SUCCESS with id should do nothing', () => {
            expect(errorReducer(initialState, exampleRoutineWithId.success(1))).toEqual(initialState);
        });

        it('FAILURE should put error', () => {
            const state = errorReducer(initialState, exampleRoutine.failure(error));

            expect(state[`${prefix}/example`]).toBe(error);
        });

        it('FAILURE with id should put error in right id', () => {
            const state = errorReducer(initialState, exampleRoutineWithId.failure(1, error));

            expect(state[`${prefix}/example`][1]).toBe(error);
        });

        it('FULFILL should do nothing', () => {
            expect(errorReducer(initialState, exampleRoutine.fulfill())).toEqual(initialState);
        });

        it('FULFILL with id should do nothing', () => {
            expect(errorReducer(initialState, exampleRoutineWithId.fulfill(1))).toEqual(initialState);
        });
    });

    describe('commonDataReducer', () => {
        const serviceData = {
            id: 989,
            slug: 'abc',
            name: {
                ru: 'ABC (Каталог)',
                en: 'ABC (Catalogue)',
            },
        };

        it('Should store data for the known action', () => {
            const actual = commonDataReducer({}, {
                type: COMMON_STORE,
                payload: serviceData,
                meta: { type: 'services', id: 989 },
            });

            expect(actual.services[989]).toEqual(serviceData);
        });

        it('Should ignore unknown actions', () => {
            expect(commonDataReducer({}, {
                type: 'NOT_' + COMMON_STORE,
                payload: serviceData,
                meta: { type: 'services', id: 989 },
            })).toEqual({});

            expect(commonDataReducer({}, {
                type: 'NOT_' + COMMON_STORE,
            })).toEqual({});

            expect(commonDataReducer({}, {})).toEqual({});
        });

        it('Should return initial state for undefined store', () => {
            expect(commonDataReducer(undefined, {})).toEqual({});
        });

        it('Should apply known action over initial state immediately', () => {
            expect(commonDataReducer(undefined, {
                type: COMMON_STORE,
                payload: serviceData,
                meta: { type: 'services', id: 989 },
            })).toEqual({ services: { '989': serviceData } });
        });
    });
});
