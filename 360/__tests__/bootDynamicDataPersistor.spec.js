import localforage from 'localforage';

import device from 'device';
import * as session from 'configs/session';

import {BootDynamicDataPersistor} from '../bootDynamicDataPersistor';

const stubStorageName = 'configsStub';

describe('BootDynamicDataPersistor', () => {
  beforeEach(() => {
    sinon.stub(session, 'storageName').value(stubStorageName);
    jest.spyOn(localforage, 'createInstance').mockImplementation(() => ({
      localForageStorage: true,
      getItem: jest.fn(),
      setItem: jest.fn()
    }));
  });

  describe('#constructor', () => {
    test('должен создать коннект для хранилища данных, если есть флаг allowPersistData', () => {
      sinon.stub(device, 'allowPersistData').value(true);

      const persistor = new BootDynamicDataPersistor();

      expect(localforage.createInstance).toHaveBeenCalledWith({
        driver: localforage.INDEXEDDB,
        name: stubStorageName,
        storeName: 'configs',
        description: 'Persisting calendar configs'
      });

      expect(persistor.storage).toBeInstanceOf(Object);
    });

    test('не должен создавать коннект для хранилища данных, если нет флага allowPersistData', () => {
      const persistor = new BootDynamicDataPersistor();

      expect(localforage.createInstance).toHaveBeenCalledTimes(0);
      expect(persistor.storage).toBe(null);
    });
  });

  describe('#persist', () => {
    test('не должен ничего делать, если нет коннекта к хранилищу данных', () => {
      const persistor = new BootDynamicDataPersistor();

      persistor.storage = null;

      expect(persistor.persist({})).resolves.toBe(undefined);
    });

    test('должен записать данные, если есть коннект к хранилищу данных', async () => {
      sinon.stub(device, 'allowPersistData').value(true);

      const data = {dynamicConfig: true};

      const persistor = new BootDynamicDataPersistor();
      persistor.storage.setItem.mockResolvedValue(true);

      await persistor.persist(data);

      expect(persistor.storage.setItem).toHaveBeenCalledWith(
        'bootDynamicData',
        JSON.stringify(data)
      );
    });
  });

  describe('#getData', () => {
    test('не должен ничего делать, если нет коннекта к хранилищу данных', () => {
      const persistor = new BootDynamicDataPersistor();

      persistor.storage = null;

      expect(persistor.getData()).resolves.toBe(undefined);
    });

    test('должен выдать данные, если есть коннект к хранилищу данных', async () => {
      sinon.stub(device, 'allowPersistData').value(true);

      const stringifiedData = JSON.stringify({dynamicConfig: true});

      const persistor = new BootDynamicDataPersistor();
      persistor.storage.getItem.mockResolvedValue(stringifiedData);

      const data = await persistor.getData();

      expect(persistor.storage.getItem).toHaveBeenCalledWith('bootDynamicData');
      expect(data).toEqual(JSON.parse(stringifiedData));
    });
  });
});
