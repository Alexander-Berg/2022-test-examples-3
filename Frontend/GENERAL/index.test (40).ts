import main from '../__dumps__/index.json';

import { RECYCLE_TIME, stateReconcilerByDate } from './helpers/persist';
import { MainPersistState } from './slices/main';

import getStore, { getPersistor } from '.';

const lsSetItem = localStorage.setItem as jest.MockedFunction<typeof localStorage.setItem>;
const lsClear = localStorage.clear as jest.MockedFunction<typeof localStorage.clear>;
const spyStateReconcilerByDate = stateReconcilerByDate as jest.MockedFunction<typeof stateReconcilerByDate>;

jest.mock('../lib/rum');
jest.mock('./helpers/persist', () => {
    const persist = jest.requireActual('./helpers/persist');

    return {
        ...persist,
        stateReconcilerByDate: jest.fn((...args) => persist.stateReconcilerByDate(...args))
    };
});

describe('redux', () => {
    const persist = { _persist: JSON.stringify({ version: 2 }) };

    beforeEach(() => {
        lsClear();
        spyStateReconcilerByDate.mockClear();
    });

    it('чистый лист / стейт должен инициализироваться', async() => {
        const store = getStore();
        const persistor = getPersistor(store);

        persistor.persist();

        await new Promise(resolve => setTimeout(resolve, 100));

        const state = store.getState();

        expect(JSON.stringify(state.main.data)).toBe('{}');
        expect(spyStateReconcilerByDate).toHaveBeenCalledTimes(0);
    });

    it('localStorage / стейт должен инициализироваться', async() => {
        const lState = { maincity: { data: { foo: 'bar' } } };

        lsSetItem('persist:weather.main', JSON.stringify({
            ...persist,
            data: JSON.stringify(lState)
        }));

        const store = getStore();
        const persistor = getPersistor(store);
        let state;

        state = store.getState();

        expect(JSON.stringify(state.main.data)).toBe('{}');

        persistor.persist();

        await new Promise(resolve => setTimeout(resolve, 100));

        state = store.getState();

        expect(state.main.data).toMatchObject(lState);
        expect(spyStateReconcilerByDate).toHaveBeenCalledTimes(1);
    });

    it('localStorage / стейт должен быть отфильтрован по свежести', async() => {
        const lState = {
            maincity: { data: { city: 'main' }, updatedAt: Date.now() - RECYCLE_TIME },
            testcity: { data: { city: 'test' }, updatedAt: Date.now() }
        };

        lsSetItem('persist:weather.main', JSON.stringify({
            ...persist,
            data: JSON.stringify(lState)
        }));

        const store = getStore();
        const persistor = getPersistor(store);
        let state;

        state = store.getState();

        expect(JSON.stringify(state.main.data)).toBe('{}');

        persistor.persist();

        await new Promise(resolve => setTimeout(resolve, 100));

        state = store.getState();

        expect(JSON.stringify(state.main.data)).toBe(JSON.stringify({ testcity: lState.testcity }));
        expect(spyStateReconcilerByDate).toHaveBeenCalledTimes(1);
    });

    it('preloadedState / стейт должен инициализироваться', async() => {
        const data = {
            ...main,
            /**
             * Без localStorage не будет происходить persist.stateReconciler
             * В котором определен способ отбрасывания устаревших данных
             *
             * Детали: в persistReducer прилетает action.type = persist/REHYDRATE
             * Без payload, и в этом случае reconciler не вызывается
             */
            updatedAt: Date.now() - RECYCLE_TIME
        } as unknown as MainPersistState['main']['data']['key'];
        const pState = {
            main: {
                data: {
                    maincity: data
                },
                ui: {},
                clientTimeOffset: null
            }
        };

        const store = getStore(pState);
        const persistor = getPersistor(store);
        let state;

        state = store.getState();

        expect(state.main.data).toEqual(pState.main.data);

        persistor.persist();

        await new Promise(resolve => setTimeout(resolve, 100));

        state = store.getState();

        expect(state.main.data).toEqual(pState.main.data);
        expect(spyStateReconcilerByDate).toHaveBeenCalledTimes(0);
    });

    it('preloadedState + lS / непересекающиеся данные должны быть склеены', async() => {
        const data = {
            ...main,
            updatedAt: Date.now()
        } as unknown as MainPersistState['main']['data']['key'];
        const pState = {
            main: {
                data: {
                    preloaded: data
                },
                ui: {},
                clientTimeOffset: null
            }
        };

        const lState = { localStored: { data: { foo: 'bar' } } };

        lsSetItem('persist:weather.main', JSON.stringify({
            ...persist,
            data: JSON.stringify(lState)
        }));

        const store = getStore(pState);
        const persistor = getPersistor(store);
        let state;

        state = store.getState();

        expect(state.main.data).toMatchObject(pState.main.data);

        persistor.persist();

        await new Promise(resolve => setTimeout(resolve, 100));

        state = store.getState();

        expect(state.main.data).toMatchObject({ ...pState.main.data, ...lState });
        expect(spyStateReconcilerByDate).toHaveBeenCalledTimes(1);
    });

    it('preloadedState + lS / пересекающиеся данные должны быть слиты по свежести', async() => {
        const rotten = 15;

        const preloadedFresh = {
            ...main,
            updatedAt: Date.now()
        } as unknown as MainPersistState['main']['data']['key'];
        const preloadedRotten = {
            ...main,
            updatedAt: Date.now() - rotten
        } as unknown as MainPersistState['main']['data']['key'];
        const pState = {
            main: {
                data: {
                    freshInPreload: preloadedFresh,
                    freshInLs: preloadedRotten
                },
                ui: {},
                clientTimeOffset: null
            }
        };

        const lState = {
            freshInPreload: { data: { foo: 'rottenLs' }, updatedAt: Date.now() - rotten },
            freshInLs: { data: { foo: 'freshLs' }, updatedAt: Date.now() }
        };

        lsSetItem('persist:weather.main', JSON.stringify({
            ...persist,
            data: JSON.stringify(lState)
        }));

        const store = getStore(pState);
        const persistor = getPersistor(store);
        let state;

        state = store.getState();

        expect(state.main.data).toMatchObject(pState.main.data);

        persistor.persist();

        await new Promise(resolve => setTimeout(resolve, 100));

        state = store.getState();

        expect(state.main.data).toMatchObject({
            freshInPreload: preloadedFresh,
            freshInLs: lState.freshInLs
        });

        expect(spyStateReconcilerByDate).toHaveBeenCalledTimes(1);
    });
});
