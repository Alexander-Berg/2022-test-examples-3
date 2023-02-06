import localforage from 'localforage';
import {persistStore} from 'redux-persist';

import * as session from 'configs/session';
import device from 'device';

import {EventsPersistor, RowKey} from '../eventsPersistor';
import getStoredState from '../utils/getStoredState';

jest.mock('redux-persist');
jest.mock('../ShardedStore', () => {
  return {
    ShardedStore: class {}
  };
});
jest.mock('../utils/createWhitelister', () => cb => cb);
jest.mock('../utils/getStoredState', () => jest.fn());

const stubStorageName = 'commonStub';

describe('EventsPersistor', () => {
  beforeEach(() => {
    sinon.stub(session, 'storageName').value(stubStorageName);
  });

  test('должен правильно инициализировать опции при создании экземпляра', () => {
    const persistor = new EventsPersistor();

    expect(persistor.options.keyPrefix).toBe(`${stubStorageName}:`);
    expect(persistor.whitelist_all).toBe(false);
    expect(persistor.instance).toBe(null);
  });

  describe('#init', () => {
    beforeEach(() => {
      persistStore.mockImplementation((store, options, cb) => cb());
      jest
        .spyOn(localforage, 'createInstance')
        .mockImplementation(() => ({localForageStorage: true}));
    });

    test('должен просто вызвать callback, если не проставлен флаг, разрешающий кешировать данные', () => {
      const persistor = new EventsPersistor();

      const cb = jest.fn();

      persistor.init({}, cb);

      expect(cb).toHaveBeenCalledTimes(1);
      expect(localforage.createInstance).toHaveBeenCalledTimes(0);
      expect(persistStore).toHaveBeenCalledTimes(0);
    });

    test('должен правильно инициализировать персист стора', () => {
      sinon.stub(device, 'allowPersistData').value(true);

      const persistor = new EventsPersistor();

      const cb = jest.fn();

      persistor.init({}, cb);

      expect(localforage.createInstance).toHaveBeenCalledTimes(1);
      expect(persistStore).toHaveBeenCalledTimes(1);
      expect(cb).toHaveBeenCalledTimes(1);
      expect(persistor.whitelist_all).toBe(true);
    });

    test('не должен создавать коннект к хранилищу, если уже есть один коннект', () => {
      sinon.stub(device, 'allowPersistData').value(true);

      const persistor = new EventsPersistor();

      persistor.init({}, jest.fn());

      expect(localforage.createInstance).toHaveBeenCalledTimes(1);
      expect(persistor.options.storage).toEqual({localForageStorage: true});

      localforage.createInstance.mockClear();

      persistor.init({}, jest.fn());

      expect(localforage.createInstance).toHaveBeenCalledTimes(0);
    });
  });

  describe('#rehydrateEvents', () => {
    beforeEach(() => {
      getStoredState.mockResolvedValue({});
    });

    test('не должен ничего делать, если нет инстанса redux-persist', async () => {
      const persistor = new EventsPersistor();

      await persistor.rehydrateEvents({from: null, to: null, layersIds: null});

      expect(getStoredState).toHaveBeenCalledTimes(0);
    });
    test('должен правильно отфильтровать и регидрировать эвенты', async () => {
      const store = {
        lastUpdateTs: 1000,
        [new RowKey(1, 1000).toString()]: {eventId: 1},
        [new RowKey(1, 2000).toString()]: {eventId: 2},
        [new RowKey(1, 3000).toString()]: {eventId: 3},
        [new RowKey(2, 1000).toString()]: {eventId: 4},
        [new RowKey(3, 1000).toString()]: {eventId: 5},
        [new RowKey(3, 3000).toString()]: {eventId: 6}
      };

      getStoredState.mockImplementationOnce((options, whitelister) => {
        return Promise.resolve().then(() => {
          return Object.keys(store)
            .filter(key => whitelister(key))
            .reduce((state, key) => {
              state[key] = store[key];
              return state;
            }, {});
        });
      });

      const persistor = new EventsPersistor();
      persistor.instance = {
        rehydrate: jest.fn()
      };

      const events = await persistor.rehydrateEvents({from: 1000, to: 2000, layersIds: ['1', '2']});

      expect(events).toEqual({
        lastUpdateTs: 1000,
        [new RowKey(1, 1000).toString()]: {eventId: 1},
        [new RowKey(1, 2000).toString()]: {eventId: 2},
        [new RowKey(2, 1000).toString()]: {eventId: 4}
      });
    });
  });
});
